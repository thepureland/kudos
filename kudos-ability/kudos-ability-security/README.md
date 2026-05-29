# kudos-ability-security

安全能力主题（认证 / 签名 / 凭证）。

| 子目录 | 内容 |
|---|---|
| [`kudos-ability-security-jwt`](kudos-ability-security-jwt/README.md) | JWT 签发/校验（RS256 + PKCS12 keystore + spring-security-oauth2-jose） |

后续可能新增：spring-security 整合（filter chain）、captcha（滑块/点选验证码）、device fingerprint。
对应 soul 的 `security-spring` / `security-captcha-tianai` / `security-device`，体量大，按需独立移植。
