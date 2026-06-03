# kudos-ability-security-common

跨认证方案的公共安全 bean：`PasswordEncoder`（密码哈希）+ `Authenticator`（TOTP 一次性口令）。

业务工程把本模块作为依赖加入即可获得：

1. **`PasswordEncoder`** —— Spring Security `DelegatingPasswordEncoder` + BCrypt 默认，写入新
   hash 时自动打 `{bcrypt}` 前缀，未来切 Argon2 / SCrypt 不需要数据库迁移
2. **`Authenticator`** —— `TotpAuthenticator`，标准 RFC 6238 TOTP 实现，兼容 Google
   Authenticator / Microsoft Authenticator / Authy / 1Password 等所有标准 TOTP app

`PasswordEncoder` 的底层就是 Spring Security 自带的 `BCryptPasswordEncoder`；TOTP 算法
inline 在 `TotpAuthenticator` 内（约 12 行 HMAC-SHA1 + 截断），verify 复用 kudos-base 的
`GoogleAuthenticator.checkCode`。

## 设计要点

### 为什么不直接 port soul 的 PasswordTool / PasswordAlg

soul 的 `PasswordAlg.alg()` 实现里有个 bug：

```java
String rs = "";
for (int i = 0; i < 11; i++) {
    rs = DigestTool.getMD5(raw, salt);  // 每次都对 raw 算 MD5，没真的迭代
}
return rs;
```

迭代变量 `rs` 没参与下一轮计算，11 次循环等价于一次 `MD5(raw, salt)`。而且 MD5 在 2026 年也
不是合适的密码哈希选项（rainbow table 攻击成本极低）。

kudos-base 已经有 `PasswordKit`：直接 BCrypt，自带随机 salt，自描述格式（不需要单独存 salt），
设计上比 PasswordTool 更现代。本模块不再 port `PasswordTool` —— 业务侧直接用
`PasswordKit.hash()` / `PasswordKit.matches()`（静态调用，不需要 DI），或者注入本模块装配的
`PasswordEncoder` bean。

### 为什么把 GoogleAuthenticator / MicrosoftAuthenticator 合并成一个 TotpAuthenticator

soul 在 `otp/vendor/` 下放了两个类：`GoogleAuthenticator`（完整实现）和 `MicrosoftAuthenticator`
（TODO 桩）。但其实：

- **Microsoft Authenticator app 本身就遵循 RFC 6238 TOTP**，跟 Google Authenticator app 用的
  是同一套协议（30 秒窗口 + HMAC-SHA1 + 6 位截断）
- 两个 vendor class 在不同 namespace 重复同一份算法没有意义
- `MicrosoftAuthenticator` 抛 `UnsupportedOperationException("todo: waiting for implement")`
  说明 soul 自己也没真实现过

所以 kudos 端只保留单个 `TotpAuthenticator`，文档说明它对所有标准 TOTP app 兼容。如果未来真有
"非 TOTP"的 OTP 方案（HOTP、push-based、WebAuthn），新增 `HotpAuthenticator` 等类实现同样的
`Authenticator` 接口，业务侧通过 `@Qualifier` 选择。

### 为什么删了 AuthenticatorFactory + AuthenticatorType 枚举

soul 用了一个 `AuthenticatorFactory.generate(type)` 加 enum 派发的模式：

```java
public Authenticator generate(AuthenticatorType currentType) {
    switch (currentType) {
        case GOOGLE: return checkerGoogle;
        case MICROSOFT: return checkerMicrosoft;
        default: return checkerGoogle;
    }
}
```

在只有一个真实现的前提下，工厂 + 枚举纯属样板。即使将来加多个 `Authenticator` 实现，Spring
的 bean-name qualifier（`@Qualifier("totp")`）足够，不需要自建 dispatcher。

### TotpAuthenticator 相对 soul / kudos-base 的几处改进

1. **`generateKey()` 用全新 `SecureRandom`，不再 seed 固定字符串**。soul 和 kudos-base 的
   `GoogleAuthenticator.generateSecretKey()` 都用了 `sr.setSeed(Base64.decodeBase64(SEED))`
   —— 这会把熵注入到 SecureRandom 而不是替换，理论上削弱了密钥强度（如果有人误以为 SEED 是
   唯一随机源就更糟）。本类的 `generateKey()` 用 `SecureRandom()` 默认实例，让 JDK 提供熵
2. **默认 `windowSize = 1`（±30 秒）**。soul 默认 3（±90 秒），对 30 秒 TOTP 来说窗口过宽，
   暴力破解可尝试码数翻 6 倍。生产业务有时钟漂移问题时再调高
3. **HMAC-SHA1 异常时抛出**，不像 soul `printStackTrace + return null`。失败不是验证结果，
   是 JVM 配错
4. **`Clock` 可注入** —— 测试可固定时间窗口验证算法正确性

### 没 port 的项

| soul 类 / 字段 | 为什么没 port |
|---|---|
| `PasswordTool` + `PasswordAlg` | 见上节 |
| `AuthenticatorFactory` + `AuthenticatorType` | 见上节 |
| `MicrosoftAuthenticator` | TODO 桩，跟 Google 用同一协议，已合并 |
| `AuthenticatorCheckerException` | soul-common 自己没人抛它（`verify` 返回 boolean） |
| `SecurityException` 基类 | 同上，unused |
| `SecurityCommonProperties.algTimes` | soul 自己没读过这个值 |
| `SecurityCommonProperties.keys: Map<String, String>` | soul 自己没读过 |
| `SecurityCommonEnvironmentPostProcessor` | soul 自己标了 `@Deprecated` |
| `SecurityJwtConfiguration` 等 JWT 相关 | 已在 `kudos-ability-security-jwt` |

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/SecurityCommonAutoConfiguration` | 装配入口；发布 `PasswordEncoder` + `Authenticator` bean |
| `init/properties/SecurityCommonProperties` | `kudos.ability.security.common.*` 配置（目前仅 `totp.window-size`） |
| `support/Authenticator` | OTP 抽象接口（`generateKey` / `verify` / `generateCode`） |
| `support/TotpAuthenticator` | RFC 6238 TOTP 实现 |
| `resources/kudos-ability-security-common.yml` | 默认配置 |

## 配置示例

```yaml
kudos:
  ability:
    security:
      common:
        totp:
          window-size: 1   # ±30 秒；生产推荐
```

## 使用示例

### 密码哈希

```kotlin
@Service
class UserService(private val passwordEncoder: PasswordEncoder) {

    fun register(username: String, plainPassword: String) {
        val hash = passwordEncoder.encode(plainPassword)   // `{bcrypt}$2a$10$...`
        userRepo.save(User(username, hash))
    }

    fun authenticate(username: String, plainPassword: String): Boolean {
        val user = userRepo.findByUsername(username) ?: return false
        return passwordEncoder.matches(plainPassword, user.passwordHash)
    }
}
```

不需要 DI 的场景直接用 kudos-base 的 `PasswordKit.hash()` / `PasswordKit.matches()`。

### 双因素 (TOTP)

```kotlin
@Service
class TwoFactorService(private val authenticator: Authenticator) {

    /** 用户开启 2FA：生成并保存 secret，把 secret + otpauth URI 返回给前端显示二维码 */
    fun enroll(userId: Long): String {
        val secret = authenticator.generateKey()
        userRepo.saveTotpSecret(userId, secret)
        return "otpauth://totp/myapp:$userId?secret=$secret&issuer=myapp"
    }

    /** 用户登录时校验 6 位码 */
    fun verify(userId: Long, userTypedCode: Int): Boolean {
        val secret = userRepo.getTotpSecret(userId)
        return authenticator.verify(secret, userTypedCode)
    }
}
```

### 自定义 PasswordEncoder

业务工程注入自己的 `PasswordEncoder` bean，本模块的 `@ConditionalOnMissingBean` 会让出：

```kotlin
@Configuration
class CustomCryptoConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
}
```

## 测试覆盖

- `TotpAuthenticatorTest` —— 纯单元：key 随机性 / 6 位码格式 / 同时刻 round-trip / 窗外失败 /
  相邻窗口通过 / 错码失败
- `SecurityCommonAutoConfigurationTest` —— Spring 上下文集成：默认装配 + BCrypt `{bcrypt}`
  前缀验证 + 自定义 `window-size` 仍能 round-trip

未覆盖：长时间窗口漂移测试（受 Spring 注入的真实 `Clock` 限制；底层算法测试由
`TotpAuthenticatorTest` 用注入 `Clock` 覆盖）。

## 已知限制 / 后续工作

- ❗ TOTP 只支持 SHA1 + 6 位码 + 30 秒周期（与 Google Authenticator app 兼容性的最大公约数）。
  需要 SHA256 / 8 位 / 60 秒的场景需自建 `Authenticator` 实现
- ❗ 不带 QR 码 URI 拼接工具 —— 业务侧自己拼 `otpauth://totp/...` URI（格式简单，参考使用示例）。
  未来可加 `TotpUri.build(issuer, account, secret)` 辅助
- ❗ 没有 anti-replay：同一窗口内重复使用同一个 OTP code 不会被 `verify()` 拒绝。需要 anti-replay
  的工程在业务层加缓存（Redis SETNX，30 秒 TTL）拦截重放
- ❗ `PasswordEncoder` 默认 BCrypt strength=10。对高安全场景可在业务侧用 `Argon2PasswordEncoder`
  覆盖；本模块的 `@ConditionalOnMissingBean` 兼容此用法
- ❗ TOTP 算法在 `TotpAuthenticator.computeTotp` 里 inline 实现了一份（约 12 行 HMAC-SHA1 + 截断），
  与 kudos-base 的 `GoogleAuthenticator.verifyCode` 重复。后者目前是 `internal` 可见性，跨模块不可
  访问。未来若把 verifyCode 提到 public 即可消除这份重复

## 依赖

```kotlin
api(project(":kudos-context"))
api(project(":kudos-base"))             // GoogleAuthenticator.checkCode (verify path)
api(libs.spring.security.crypto)        // PasswordEncoder / BCryptPasswordEncoder

implementation(libs.apache.commons.codec)  // Base32 编码

testImplementation(project(":kudos-test:kudos-test-common"))
```
