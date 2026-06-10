# kudos-ability-security-jwt-resourceserver

为 `kudos-ability-security-jwt` 加上 OAuth2 Resource Server 默认 `SecurityFilterChain` 装配。
基于 Spring Security 6 + spring-security-oauth2-resource-server。

业务工程加依赖 + 一行 `kudos.ability.security.jwt.resource-server.enabled=true` 即可获得：

1. **默认 filter chain**：所有请求要求 JWT 认证（除非配 `permitted-paths`）
2. **JWT 走 `kudos-ability-security-jwt` 装好的 `JwtDecoder`**（RS256 + PKCS12 keystore）
3. **`JwtExpValidator` 自动叠加到 decoder 的 validator 链**，过期 token 自动拒
4. **可选 issuer / audience 校验**：配 `issuer` / `audience` 后强校验 `iss` / `aud` claim，
   防多服务共用 keystore 时的 token 横向重放；不配则不校验（向后兼容，启动时 WARN 提示）
5. **CSRF 默认关**（无状态 REST 用 Bearer token 已自带 CSRF 防护）
6. **opt-in**：模块默认不装 filter chain，避免污染业务工程自己的 security 配置

## 设计要点

### 为什么必须显式 `enabled=true`

Spring Security 的 `SecurityFilterChain` bean 一旦注册就拦截所有请求。如果模块默认开启，
任何引入本模块的工程都被强制套上 JWT 鉴权 —— 而很多工程已经有自己的 security 配置
（比如用 session、HTTP Basic、自定义 token），冲突会很难调试。

`@ConditionalOnProperty(enabled=true)` 让模块**绝对默认沉默**：依赖了但没配 = 啥都没装。
业务侧 explicitly opt-in 才会出现一个新 SecurityFilterChain bean。

### 为什么 `@EnableWebSecurity` + `@ConditionalOnBean(HttpSecurity, JwtDecoder)`

`@EnableWebSecurity` 拉起 Spring Security 的 `HttpSecurityConfiguration` —— 它提供 `HttpSecurity`
bean，是构造 `SecurityFilterChain` 的标准 builder。

但 `HttpSecurity` 不在所有上下文里都存在（例如纯 properties 测试上下文）。`@ConditionalOnBean`
让我们的 `@Bean` 方法在缺少 `HttpSecurity` 或 `JwtDecoder` 时**优雅跳过**，而不是抛
`NoSuchBeanDefinitionException`。这样：
- 业务工程引入本模块 + 真实 Spring Security 上下文 → bean 装配成功
- 业务工程的 properties-only 测试 → bean 跳过，properties bean 正常注入

### 为什么默认 CSRF 关

JWT 在 `Authorization: Bearer` header 里传，跨域 CSRF 攻击者无法 forge 这个 header
（CORS 跟同源策略管 cookie / form data，但管不了自定义 header）。所以 stateless REST + JWT
天然不需要 CSRF token。

打开 CSRF 会要求每个请求带 `X-CSRF-Token` —— 没有 cookie 就没办法发 CSRF token —— 完全没意义。

如果业务工程同时用 cookie session 认证（少见但合法），需要自定义 SecurityFilterChain 覆盖。

### 默认 authorization shape

```kotlin
authorizeHttpRequests { auth ->
    if (permittedPaths.isNotEmpty()) {
        auth.requestMatchers(*permittedPaths).permitAll()
    }
    auth.anyRequest().authenticated()
}
```

最严格的默认：除了 `permitted-paths` 白名单，**所有请求都要 JWT**。业务工程在 yml 配
白名单：

```yaml
kudos:
  ability:
    security:
      jwt:
        resource-server:
          enabled: true
          permitted-paths:
            - /actuator/health
            - /actuator/info
            - /api/public/**
            - /login/**          # 发 token 的端点，不能要求 token 才能用
```

如果需要更复杂的授权（role-based、自定义 claim → Authentication 转换、多 issuer 等），
业务工程自己声明 `SecurityFilterChain`，本模块的 `@ConditionalOnMissingBean` 让位。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/JwtResourceServerAutoConfiguration` | 装配入口；条件性发布 `SecurityFilterChain` bean |
| `init/properties/JwtResourceServerProperties` | yml 绑定：`enabled` / `permittedPaths` / `issuer` / `audience` / `authorities.rolesClaim` |
| `support/JwtAudienceValidator` | aud claim 校验器；`audience` 配置后叠加进 validator 链 |
| `resources/kudos-ability-security-jwt-resourceserver.yml` | 默认值（全注释 + 模板） |

## 业务工程接入

最简配置：

```kotlin
// build.gradle.kts
dependencies {
    api(project(":kudos-ability:kudos-ability-security:kudos-ability-security-jwt-resourceserver"))
    api(libs.spring.boot.starter.web)              // SecurityFilterChain 需要 web 上下文
    api(libs.spring.boot.starter.security)         // 提供 SecurityAutoConfiguration → HttpSecurity
}
```

```yaml
kudos:
  ability:
    security:
      jwt:
        # 父模块 kudos-ability-security-jwt 的 keystore 配置
        key:
          key-store: classpath:my-jwt.p12
          store-pass: ${JWT_KEYSTORE_PASSWORD}
          alias: my-jwt-key
        # 本模块的 filter chain 开关
        resource-server:
          enabled: true
          issuer: https://my-auth-service     # 强校验 iss claim；多服务共用 keystore 时务必配置
          audience: my-service                # 强校验 aud claim（token 的 aud 列表须包含该值）
          permitted-paths:
            - /actuator/health
            - /api/public/**
          authorities:
            roles-claim: roles      # 让 hasRole(...) 工作；不需要时留空
```

## issuer / audience 校验（防横向重放）

签名校验只能证明 token 出自共享 keystore —— **多个服务共用同一 keystore 时，为服务 A 签发的
token 在服务 B 上同样验签通过**。配置 `issuer` / `audience` 后：

- `issuer` 非空 → validator 链改用 `JwtValidators.createDefaultWithIssuer(...)`（含默认时间戳
  校验 + iss 精确匹配，**替换**而非叠加 `createDefault()`，避免默认校验跑两遍）
- `audience` 非空 → 叠加 `JwtAudienceValidator`：token 的 `aud` 列表（RFC 7519）必须包含配置值，
  否则按标准 `invalid_token` 拒绝（401）；**缺 `aud` claim 同样拒绝**（否则签发侧不写 claim 即可绕过）
- 两者都留空（默认）→ 不校验，行为与旧版本完全一致；启动时输出一条 WARN 提醒风险

签发侧对应配置（`kudos-ability-security-jwt`，经 `JwtParametersTool` 签发时自动写入）：

```yaml
kudos:
  ability:
    security:
      jwt:
        claims:
          iss: https://my-auth-service
          aud: my-service
```

## 角色映射 (`authorities.roles-claim`)

Spring 默认的 `JwtGrantedAuthoritiesConverter` 只把 `scope` claim 映射成 `SCOPE_*` 权限
（OAuth2 标准 scope-based auth）。但绝大多数业务需要 **role-based** 鉴权
（`@PreAuthorize("hasRole('ADMIN')")` / `auth.hasRole("ADMIN")`），这要求当前 Authentication
的权限里有 `ROLE_ADMIN`。

设 `kudos.ability.security.jwt.resource-server.authorities.roles-claim=roles`（或
`groups` 等业务自选 claim 名）后，本模块装的
`KudosJwtRolesGrantedAuthoritiesConverter` 会：

- 读 JWT 的 `roles` claim（默认名，按 yml 改）
- `List<String>` → 每个值大写 + 加 `ROLE_` 前缀（已经有前缀的不重复加）
- `String` （空格分隔） → 同样处理（兼容 IdP 不发数组的情况）
- 其它类型 / 缺失 → 静默返回空（绝不抛异常，否则会让所有请求 500）

例：

```json
{
  "sub": "alice",
  "roles": ["admin", "auditor"],
  "scope": "read:users write:users"
}
```

最终 Authentication 的 authorities = `{ SCOPE_read:users, SCOPE_write:users, ROLE_ADMIN, ROLE_AUDITOR }`。
`hasRole("ADMIN")` ✅，`hasAuthority("SCOPE_read:users")` ✅。

业务侧 Controller 照常写，需要鉴权的端点直接拿当前用户：

```kotlin
@RestController
class UserController {
    @GetMapping("/api/me")
    fun me(authentication: JwtAuthenticationToken): UserVo {
        val username = authentication.name        // sub claim
        return UserVo(username)
    }
}
```

## 自定义 `SecurityFilterChain`

当默认 shape 不够用，业务工程声明自己的 bean，本模块自动让位：

```kotlin
@Configuration
@EnableWebSecurity
class CustomSecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity, decoder: JwtDecoder): SecurityFilterChain {
        http.csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/admin/**").hasRole("ADMIN")
                auth.requestMatchers("/api/public/**").permitAll()
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { rs ->
                rs.jwt { jwt ->
                    jwt.decoder(decoder)
                    jwt.jwtAuthenticationConverter(myCustomConverter())
                }
            }
        return http.build()
    }
}
```

## 测试覆盖

- `JwtResourceServerAutoConfigurationTest` (4) —— 用 `ApplicationContextRunner` 测装配条件 + properties 绑定：
  - 默认 `enabled` 缺失 → `@Configuration` 类整体跳过，`JwtResourceServerProperties` bean 也不装
  - `enabled=true` → properties 绑定生效，`enabled` 字段 true，`permittedPaths` 默认空，
    `issuer` / `audience` 默认空串（不校验）
  - yml 索引列表 `permitted-paths[0]` / `[1]` / `[2]` 按顺序绑定到 `List<String>`
  - `issuer` / `audience` 从 yml 绑定生效
- `JwtAudienceValidatorTest` (6) —— 纯单元测 aud 校验：包含期望值通过 / 多 audience 其一匹配通过 /
  错误 aud 拒（标准 `invalid_token` 错误码） / 缺 aud claim 拒 / 大小写不匹配拒 / 空构造参数抛
- `JwtIssuerAudienceValidationIT` (5) —— `@SpringBootTest` + MockMvc，上下文配置了
  `issuer` + `audience`（同 keystore 签发，验证"验签通过但 iss / aud 不对"的横向重放被拒）：
  - 正确 iss + aud → 200
  - 错误 issuer → 401
  - 错误 audience → 401
  - 完全不带 iss / aud claim → 401（省略 claim 不能绕过校验）
  - aud 列表含期望值（多收件人） → 200
- `KudosJwtRolesGrantedAuthoritiesConverterTest` (8) —— 纯单元测 roles claim → ROLE_* 映射：
  缺失 / 列表 / 空格分隔字符串 / 已带 ROLE_ 前缀（不重复加） / 大小写归一 / 空串过滤 /
  非字符串非集合不抛异常 / 自定义 claim 名生效
- `JwtResourceServerFilterChainIT` (8) —— `@SpringBootTest` + MockMvc 端到端鉴权：
  - BC 生成 PKCS12 keystore + 父模块 `JwtEncoder` 签发真实 token
  - `/private` 无 Authorization → 401
  - `/private` + 有效 JWT → 200，body 含 `sub` claim 解析后的用户名
  - `/private` + 过期 JWT → 401（`JwtExpValidator` 命中）
  - `/api/public/echo`（在 `permitted-paths` 里）无 token → 200
  - `/api/public/echo` 带有效 token → 200（permitAll 不拒绝带 token 的请求）
  - 带 `roles=["ADMIN", "AUDITOR"]` 的 token → Authentication.authorities 含 `ROLE_ADMIN` + `ROLE_AUDITOR`（验 converter 装上）
  - 无 roles claim 的 token → Authentication.authorities 不含任何 `ROLE_*`（防幻影权限）
  - 带任意外部 iss / aud claim 的 token → 200（该上下文未配 `issuer` / `audience`，锁定"不配置 = 不校验"的向后兼容行为）

## 已知限制 / 后续工作

- ❗ 仅支持 RS256 + PKCS12 keystore，跟父模块 `kudos-ability-security-jwt` 一致
- ❗ 默认 `anyRequest().authenticated()`；细粒度授权（role-based、claim-based）业务自定义
- ❗ issuer / audience 校验为单值（一个期望 iss、一个期望 aud）；多 issuer / 多 audience
  场景需自定义 SecurityFilterChain
- ❗ 不带 CORS 配置；业务工程自己在 SecurityFilterChain 或 WebMvcConfigurer 里加

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-security:kudos-ability-security-jwt"))
api(libs.spring.security.config)
api(libs.spring.security.web)
api(libs.spring.security.oauth2.resource.server)

testImplementation(project(":kudos-test:kudos-test-common"))
testImplementation(libs.servlet.api)
testImplementation(libs.spring.boot.starter.security)
```

## 改进建议（自动分析 2026-06-11）

### 安全性
- ✅ 已修复（2026-06-11）**缺少 issuer / audience 校验**（`src/io/kudos/ability/security/jwt/resourceserver/init/JwtResourceServerAutoConfiguration.kt`）：
  `JwtResourceServerProperties` 新增可选 `issuer` / `audience` 配置（空 = 不校验，向后兼容）；
  issuer 配置后 validator 链改用 `JwtValidators.createDefaultWithIssuer(...)`（替换而非叠加默认
  validator），audience 配置后叠加 `JwtAudienceValidator`（aud 列表须包含配置值，失败返回标准
  `invalid_token`）；两者都未配置时启动 WARN 提示横向重放风险。签发侧
  `kudos.ability.security.jwt.claims.iss` / `.aud` 本就支持写入对应 claim。
- **CORS 完全未配置**（同上文件，已知限制提过）：建议提供 `cors.*` 配置项（allowed-origins /
  methods / headers），避免业务侧图省事用 `@CrossOrigin(origins = ["*"])` 这类宽松配置；
  同时注意 CORS preflight（OPTIONS）请求当前会被 `anyRequest().authenticated()` 拦下，
  浏览器跨域调用会先死在预检上。

### 设计 / 可维护性
- **filter chain bean 方法内 mutate 共享 `JwtDecoder` bean**（同上文件）：
  `decoder.setJwtValidator(...)` 是对单例 bean 的副作用式修改 —— 任何其它注入同一 decoder bean
  的代码会"被动"获得新的 validator 链，且装配顺序决定行为。建议把 validator 组装上移到
  `kudos-ability-security-jwt` 的 decoder bean 定义处，或在本模块内用装饰器包一层新 decoder
  而不动原 bean。
- **`permittedPaths` 不区分 HTTP method**（`init/properties/JwtResourceServerProperties.kt`）：
  `requestMatchers(*paths)` 只有路径维度。无法表达"GET /api/docs 公开、POST /api/docs 要认证"，
  也无法只放行 OPTIONS。建议支持 `method:path` 形式或结构化配置。

### 可观测性
- **认证失败无审计日志**（`JwtResourceServerAutoConfiguration.kt`）：401 仅由 Spring Security
  默认 `BearerTokenAuthenticationEntryPoint` 返回，没有任何服务端日志。建议提供默认包装的
  `AuthenticationEntryPoint`：warn 级记录失败原因分类（过期 / 签名错 / 格式错）+ 远端 IP +
  请求路径（注意**不要**记录 token 原文与完整 Authorization header），或发布
  `AbstractAuthenticationFailureEvent` 供业务接审计系统。

### 测试补充建议
- 现有 IT 已覆盖过期 token / 无 token / roles 映射；建议补充（`test-src/.../JwtResourceServerFilterChainIT.kt`）：
  篡改签名的 token 走完整 filter chain 返回 401（解码层已有单测，filter chain 层缺）、
  `Authorization` 头格式异常（非 Bearer 前缀、空 token）返回 401 而非 500。
