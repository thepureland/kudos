# kudos-ability-security-jwt-resourceserver

为 `kudos-ability-security-jwt` 加上 OAuth2 Resource Server 默认 `SecurityFilterChain` 装配。
基于 Spring Security 6 + spring-security-oauth2-resource-server。

业务工程加依赖 + 一行 `kudos.ability.security.jwt.resource-server.enabled=true` 即可获得：

1. **默认 filter chain**：所有请求要求 JWT 认证（除非配 `permitted-paths`）
2. **JWT 走 `kudos-ability-security-jwt` 装好的 `JwtDecoder`**（RS256 + PKCS12 keystore）
3. **`JwtExpValidator` 自动叠加到 decoder 的 validator 链**，过期 token 自动拒
4. **CSRF 默认关**（无状态 REST 用 Bearer token 已自带 CSRF 防护）
5. **opt-in**：模块默认不装 filter chain，避免污染业务工程自己的 security 配置

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
| `init/properties/JwtResourceServerProperties` | yml 绑定：`enabled` / `permittedPaths` |
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
          permitted-paths:
            - /actuator/health
            - /api/public/**
```

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

- `JwtResourceServerAutoConfigurationTest` (3) —— 用 `ApplicationContextRunner` 测装配条件 + properties 绑定：
  - 默认 `enabled` 缺失 → `@Configuration` 类整体跳过，`JwtResourceServerProperties` bean 也不装
  - `enabled=true` → properties 绑定生效，`enabled` 字段 true，`permittedPaths` 默认空
  - yml 索引列表 `permitted-paths[0]` / `[1]` / `[2]` 按顺序绑定到 `List<String>`
- `JwtResourceServerFilterChainIT` (5) —— `@SpringBootTest` + MockMvc 端到端鉴权：
  - BC 生成 PKCS12 keystore + 父模块 `JwtEncoder` 签发真实 token
  - `/private` 无 Authorization → 401
  - `/private` + 有效 JWT → 200，body 含 `sub` claim 解析后的用户名
  - `/private` + 过期 JWT → 401（`JwtExpValidator` 命中）
  - `/api/public/echo`（在 `permitted-paths` 里）无 token → 200
  - `/api/public/echo` 带有效 token → 200（permitAll 不拒绝带 token 的请求）

## 已知限制 / 后续工作

- ❗ 仅支持 RS256 + PKCS12 keystore，跟父模块 `kudos-ability-security-jwt` 一致
- ❗ 默认 `anyRequest().authenticated()`；细粒度授权（role-based、claim-based）业务自定义
- ❗ 多 issuer / 多 audience 不支持，需自定义 SecurityFilterChain
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
