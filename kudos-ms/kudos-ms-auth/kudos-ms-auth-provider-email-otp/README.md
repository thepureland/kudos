# kudos-ms-auth-provider-email-otp

`kudos-ms-auth` 的无密码邮箱验证码认证方式。模块实现 `email_otp` 的认证事务 provider；验证码签发、校验、失败次数、一次性消费和用户映射由 Kudos 负责，应用只需提供邮件投递适配器。

## 安全模型

- 验证码为 6–8 位数字，只保存绑定 `transactionId + tenantId + email` 的 HMAC-SHA-256 摘要，不保存明文；HMAC 密钥必须至少 32 UTF-8 bytes。
- Redis 存储使用 Lua 原子签发、校验和消费；验证码只能成功一次，过期或达到最大尝试次数后失效。没有 Redis bean 时回落到单进程内存实现。
- 请求频率和失败窗口复用 `IAuthenticationAttemptLimiter` 的 `EMAIL_OTP` 因子。
- 邮箱仅在验证码成功后写入 `user_contact_way`，状态为已验证；账号创建和 `user_account_third` 身份绑定复用 `IExternalAccountProvisioningService` 的同一事务。
- 生产投递实现不得记录验证码；若产品需要本地日志投递，必须由应用在明确的本地 profile 下提供，不能成为生产默认。

## 接入

`kudos-ms-auth-api-public` 已传递依赖本模块。启用方必须提供 `IEmailOtpDelivery` bean；默认的 `KudosEmailOtpPrincipalService` 在 user core 可用时自动装配，也可由应用实现 `IEmailOtpPrincipalService` 覆盖映射策略。

```yaml
kudos:
  ms:
    auth:
      email-otp:
        enabled: true
        code-hmac-secret: ${EMAIL_OTP_HMAC_SECRET}
        code-ttl-seconds: 600
        max-verification-attempts: 5
        code-digits: 6
        auto-provision: true
        identity-provider-id: my-email-otp
        provider-code: email_otp
```

认证事务的 `method` 使用 `email_otp`。第一次 `VERIFY_EMAIL` action 传 `username=email` 且不传 `code` 以签发验证码；第二次对同一事务传相同邮箱与 `code` 完成认证。租户、邮箱或事务任一不匹配都不会通过。

## 自动建号

`auto-provision=true` 时，未绑定邮箱会创建 Kudos 用户并建立固定 identity provider 下的外部身份绑定；稳定用户名由邮箱摘要派生，避免把邮箱直接塞入短用户名字段。已有绑定只解析到活动、未冻结的本租户账号，不按邮箱隐式合并其他身份。

可选默认值：`default-locale`、`default-timezone`、`default-currency`、`default-org-id`、`default-supervisor-id`、`account-type-dict-code`、`account-status-dict-code`。
