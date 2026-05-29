# kudos-ability-security-jwt

基于 Spring Security OAuth2 Resource Server + Nimbus JWT 库的 RS256 JWT 签发 / 校验模块。

业务工程把本模块作为依赖加入并配置 PKCS12 keystore 即可获得：

1. **`JwtEncoder`** —— Nimbus 后端、RS256 算法、密钥来自配置的 keystore
2. **`JwtDecoder`** —— 同 keystore 验签；同时识别 RSA / EC / HMAC 家族
3. **`JwtParametersTool`** —— 读取 yml 默认 claims（iss / exp / nbf / iat / jti）构造 `JwtEncoderParameters`
4. **`JwtExpValidator`** —— 只校验 `exp` 的轻量 token validator（其它 claim 校验交由
   `NimbusJwtDecoder` 默认 claim-set verifier）

## 设计要点

### 为什么 `key.key-store` 缺失就不装配 bean

依赖了本模块但 profile 中暂时不需要 JWT 是合法用例（例如某些 dev 环境无密钥）。所有 JWT bean
都加了 `@ConditionalOnProperty(prefix = "kudos.ability.security.jwt.key", name = "key-store")`，
默认值为 null —— 不配 key-store 就完全不装配，不会因为 NPE 报错而拒绝启动。

### 为什么固定 RS256

soul 的实现也是 RS256，且 `SecurityJwtConfiguration` 直接 hardcode 这个算法。kudos 继续这条
路线：

- RS256 是 JWT 业界默认（被 OAuth2 / OIDC / 微信小程序 / 各种 IdP 广泛采用）
- 对称算法（HS256）密钥泄漏即等于伪造能力，不适合公开服务
- ES256 需要 EC 密钥派生，业务工程一般无现成 EC keystore

需要 ES256 / HS256 的工程跳过本模块的 `JwtParametersTool`，直接构造 `JwtEncoderParameters`。

### 为什么 `JwtExpValidator` 抛 `JwtExpiredException` 而不是返回 failure

soul 实现的方式 —— 但仔细看是有道理的：
- `BadJwtException` 在 Spring Security 异常翻译链里已经映射成 401，业务侧无需自定义 mapper
- 抛异常的语义比 "validator 返回 failure result" 在 stack trace 里更易定位
- 与下游业务的 `@ResponseExceptionHandler` 配合更自然（用 try-catch 拦不到 result 对象）

### 为什么禁用 Nimbus 默认 claim-set verifier

```kotlin
jwtProcessor.setJWTClaimsSetVerifier { _, _ -> }
```

`NimbusJwtDecoder` 已经在自己的层里跑 `exp` / `nbf` 校验。如果同时启用 Nimbus `DefaultJWTProcessor`
默认的 `DefaultJWTClaimsVerifier`，**同一个 `exp` 会被验两遍**：失败时 stack trace 会出现两条
不同的 BadJwtException 抛点，调试时极易绕进去。Soul 也是这么做，原因相同。

### Soul 实现里清理掉的死代码

- `SecurityKeyProperties.cert` 字段：在 soul 里被 set 但从来没读取过（`initPublicKey`
  / `initPrivateKey` 是 unreferenced private methods）。kudos 端直接删掉了
- `JwtParametersTool` 的两个 `@Deprecated` `createDefault(String subject, Date expireAt, ...)`
  签名：用 `Date` 是 soul 早期遗留，`JwtClaimsSet.Builder` 本身用 `Instant`。kudos 端只保留
  非 deprecated 接口
- soul 把 `SecurityJwtClaimProperties` 用 field setter 注入：kudos 改成构造器注入，避免 NPE

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/SecurityJwtAutoConfiguration` | 装配入口；条件性发布 JWKSource / JwtEncoder / JwtDecoder / JwtParametersTool |
| `init/properties/SecurityKeyProperties` | `kudos.ability.security.jwt.key.*` keystore 配置 |
| `init/properties/SecurityJwtClaimProperties` | `kudos.ability.security.jwt.claims.*` 默认 claim |
| `support/JwtParametersTool` | 工具类：读 properties → `JwtEncoderParameters` |
| `support/JwtExpValidator` | `OAuth2TokenValidator<Jwt>` 只校验 `exp` |
| `exception/JwtExpiredException` | `BadJwtException` 子类，由 validator 抛出 |
| `resources/kudos-ability-security-jwt.yml` | 默认值 + 配置注释模板 |

## 配置示例

```yaml
kudos:
  ability:
    security:
      jwt:
        key:
          key-store: classpath:my-jwt.p12   # 或 file:/etc/secrets/jwt.p12
          store-pass: ${JWT_KEYSTORE_PASSWORD}
          alias: my-jwt-key
        claims:
          iss: my-service
          aud: my-frontend
          exp: 3600
          nbf: now()
          iat: now()
          jti: uuid()
```

## 使用示例

### 签发 token

```kotlin
@RestController
class LoginController(
    private val encoder: JwtEncoder,
    private val params: JwtParametersTool,
) {
    @PostMapping("/login")
    fun login(req: LoginRequest): String {
        // ...校验用户名密码...
        val parameters = params.createDefault(
            subject = req.username,
            customClaims = mapOf("roles" to req.user.roles),
        )
        return encoder.encode(parameters).tokenValue
    }
}
```

### 验证 token + 自定义 exp 校验

```kotlin
@Bean
fun jwtDecoderWithExpValidator(decoder: JwtDecoder): JwtDecoder {
    val nimbus = decoder as NimbusJwtDecoder
    nimbus.setJwtValidator(JwtExpValidator(clockSkew = Duration.ofSeconds(30)))
    return nimbus
}
```

### 需要 spring-security 完整 wiring 的工程

本模块**不**装配 Spring Security 的 `SecurityFilterChain` / `JwtAuthenticationConverter` 等下游
组件 —— 它只提供 token mint + verify 能力。要构建完整的 OAuth2 Resource Server，业务工程
按需添加：

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity, decoder: JwtDecoder) = http
        .oauth2ResourceServer { it.jwt { jwt -> jwt.decoder(decoder) } }
        .build()
}
```

## 生成 PKCS12 keystore

```bash
keytool -genkeypair \
  -alias my-jwt-key \
  -keyalg RSA -keysize 2048 \
  -validity 3650 \
  -keystore my-jwt.p12 \
  -storetype PKCS12 \
  -storepass "$JWT_KEYSTORE_PASSWORD" \
  -dname "CN=my-service, O=my-org, C=CN"
```

生产环境密钥应该走 KMS / Vault 注入，不要在 jar 里 bundle。

## 测试覆盖

- `JwtExpValidatorTest` —— 纯单元：缺失 exp / 过期 / 未过期 / clockSkew 容忍区间
- `SecurityJwtClaimPropertiesTest` —— 纯单元：`now()` / 数字 / 其它字符串的解析规则
- `JwtParametersToolTest` —— 纯单元：默认 claim 组装 / subject 覆盖 / customClaims 合并
- `SecurityJwtAutoConfigurationTest` —— 完整 Spring 上下文：程序化生成临时 keystore +
  encode-decode 往返 / `key-store` 缺失时 bean 不装配

未覆盖：与下游 `oauth2ResourceServer` / `SecurityFilterChain` 的集成（属业务工程范畴）。

## 已知限制 / 后续工作

- ❗ 只支持 PKCS12 keystore。JKS 已被 Java 17+ 标记为 deprecated；需要时业务侧自建 JwtDecoder
- ❗ 算法固定 RS256；ES256 / HS256 需自建 `JwtEncoderParameters`
- ❗ 不支持密钥轮换（key rotation）—— 单 keystore 单 RSA key。生产 zero-downtime 轮换需要
  自定义 `JWKSource` 持有多个 RSA key + 按 `kid` 选择
- ❗ `SecurityJwtClaimProperties` 的 `"now()"` / `"uuid()"` 字符串约定是 soul 遗产；语义清晰
  但格式 funky，未来可考虑引入 `Duration` 类型

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.spring.security.oauth2.jose)

testImplementation(project(":kudos-test:kudos-test-common"))
```
