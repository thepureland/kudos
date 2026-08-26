# kudos-ms-auth-provider-webauthn

可选的 WebAuthn/Passkey 协议适配模块。使用 Yubico `webauthn-server-core` 处理浏览器 ceremony，
账号、租户 MFA 策略、凭证持久化和 Session/Token 生命周期仍由 `kudos-ms-auth-core` 负责。

模块默认关闭。部署时必须显式配置 RP ID、RP 名称和可信 HTTPS origin；challenge 只允许短期、一次性
消费，不能由浏览器指定租户、用户、RP ID 或 origin。

当前已完成：

- 注册 ceremony 发起，生成 Yubico `PublicKeyCredentialCreationOptions`；
- 用户、租户、RP ID、origin、user verification、resident key 均由服务端确定；
- 已注册 credential 自动进入 `excludeCredentials`；
- 256-bit 随机 ceremony id，保存完整 library request JSON；
- 有 `RedisTemplates` 时使用 Lua 原子创建/消费和 Redis TTL，无 Redis 时回退进程内一次性存储；
- 注册响应通过 Yubico `finishRegistration` 验证 challenge、type、RP ID hash、origin、user presence、
  user verification 和 attestation；失败状态同样已消费，不能重放；
- 协议验证成功后、凭证落库前执行租户 attestation 策略，可限制 attestation format、使用 AAGUID
  allow/deny list，并按部署能力要求可信证明；策略拒绝同样不会留下可重放 ceremony；
- 协议验证结果才会转换为 core 的 `VerifiedWebAuthnCredentialRegistration`，落库只包含 credential ID、
  user handle、COSE 公钥、签名计数和认证器公开属性；请求 `credProps` 并记录 discoverable 状态。
- assertion ceremony 同时支持已知用户和无用户名（discoverable credential）发起；已知用户只下发其
  `allowCredentials`，无用户名流程不接受客户端指定主体；
- assertion 响应通过 Yubico `finishAssertion` 验证 challenge、type、RP ID hash、origin、签名、UP/UV、
  备份状态和签名计数；协议结果还会重新绑定租户内的活动 credential、user handle 与活动账号；
- 验证成功后才调用 core 的 `recordVerifiedAssertion`，由数据库旧计数 CAS 推进 sign count；失败或主体
  错配都不会更新凭证，且已消费的 ceremony 不能重放；
- `passkey` 已作为统一认证事务方法接入公开断言入口，支持无用户名 Passwordless 登录和已有会话的
  Step-up；ceremony 与 transaction id 绑定，不能跨事务替换；
- 租户允许且用户已注册 WebAuthn 时，第三方登录的 MFA 挑战会增加 `VERIFY_PASSKEY`；assertion 固定到
  第三方第一因素已经确认的本地用户，成功后才合并 AMR 并签发逻辑会话；
- 密码验证成功后也可在同一事务选择 `VERIFY_PASSKEY`；事务仅保存服务端确认的 user id、password AMR
  与 password ACR，不保存或要求在 Passkey action 中重传密码；
- 当前受管 Session 用户可通过公开 API 查询自己的活动凭证、发起/完成注册、重命名及撤销指定凭证；接口从
  Session 固定租户和用户，注册 ceremony id 只取路径参数，并要求近期 password-or-stronger 认证；
- assertion 完成后仍由统一事务控制器签发或提升逻辑 Session。UV 成功使用 phishing-resistant ACR，
  非 UV assertion 只使用 WebAuthn ACR，不会被错误地当作 MFA。

浏览器 Passwordless 登录的最小流程：

```text
POST /api/public/auth/authentication/transactions
     { "tenantId": "...", "requestedMethod": "passkey" }
POST /api/public/auth/authentication/transactions/{id}/webauthn/assertion
POST /api/public/auth/authentication/transactions/{id}/actions/VERIFY_PASSKEY
     { "attributes": {
         "webauthnCeremonyId": "...",
         "webauthnCredentialResponseJson": "..."
       } }
```

自助注册与凭证管理入口为：

```text
GET    /api/public/auth/mfa/webauthn/credentials
POST   /api/public/auth/mfa/webauthn/registrations
POST   /api/public/auth/mfa/webauthn/registrations/{ceremonyId}/finish
PATCH  /api/public/auth/mfa/webauthn/credentials/{credentialId}
DELETE /api/public/auth/mfa/webauthn/credentials/{credentialId}
```

注册、查询、重命名和撤销均不接受可信 user id/tenant id；默认要求认证时间不超过 300 秒，可通过
`operation-reauthentication-max-age-seconds` 调整。非 UV assertion 遇到必须补充第二因素的策略时会
fail-closed，不会降级绕过。浏览器结果始终
只能进入本模块的协议 verifier，不能直接交给 core 的 `registerVerified`/`recordVerifiedAssertion`。

重命名请求体仅包含 `displayName`；服务端裁剪首尾空白，拒绝空值、超过 100 字符或包含控制字符的
名称。重命名只更新展示元数据和乐观版本，不改变凭证安全状态，也不撤销 Session/Token；注册和撤销
仍会进入凭证生命周期失效链路。

最小配置示例：

```yaml
kudos:
  ms:
    auth:
      webauthn:
        enabled: true
        rp-id: example.com
        rp-name: Kudos
        origins:
          - https://login.example.com
        ceremony-ttl-seconds: 300
        operation-reauthentication-max-age-seconds: 300
        browser-timeout-millis: 60000
        user-verification: REQUIRED
        resident-key: PREFERRED
```

生产环境的 origin 只接受不带 path、query、fragment 和 user-info 的 HTTPS origin。仅本地开发可显式
设置 `allow-insecure-localhost: true` 后使用 HTTP localhost。

未配置租户 attestation 策略时保持兼容：不限制 format/AAGUID，也不要求可信证明。部署可以贡献一个
Yubico `AttestationTrustSource` Bean；只有容器中恰好存在一个该类型 Bean 时，provider 才将其交给
Yubico verifier，并向 core 报告可信证明能力可用。没有或存在多个候选时均按不可用处理，租户不能保存
`requireTrustedAttestation=true`，避免策略在运行时必然拒绝所有新凭证。当前已完成策略持久化和注册
执行点；管理端查询/保存 API 还会返回 `trustSourceAvailable` 供控制台安全配置。

本模块内置 Yubico FIDO Metadata Service 适配，但默认关闭且不会发起网络请求。启用示例：

```yaml
kudos:
  ms:
    auth:
      webauthn:
        enabled: true
        fido-mds:
          enabled: true
          cache-directory: /var/lib/kudos/fido-mds
          legal-headers:
            - ${FIDO_MDS_LEGAL_HEADER}
          refresh-interval-seconds: 3600
```

`cache-directory` 必须是可写的绝对路径；`legal-headers` 必须由部署方按其已经接受的 FIDO MDS 法律条款
显式提供，框架不替部署方默认接受。loader 使用 Yubico 默认 trust root/BLOB 地址、下载验签、trust root
与 BLOB 文件缓存以及推荐的 `verifyDownloadsOnly=true`。首次启动必须取得一份有效快照，否则以
`WEBAUTHN_FIDO_MDS_INITIAL_LOAD_FAILED` 失败；此后单线程定期调用 `loadCachedBlob()`，只有成功构建新的
`FidoMetadataService` 才原子替换，刷新异常保留最后一份已验证快照并记录错误。若应用已提供自定义
`AttestationTrustSource` Bean，内置 MDS Bean 自动退让，避免产生两个候选。

同一份已验签 BLOB 还构建独立的只读认证器状态视图，供 core 的凭证审计风险 SPI 使用。信任链判断继续
使用 Yubico 默认过滤；状态视图保留 `REVOKED`、密钥泄露和 user verification bypass 等条目，避免审计
反而看不到已被信任源拒绝的型号。风险级别为 `NORMAL` / `WARNING` / `CRITICAL` / `NOT_EVALUATED`，同时
返回 `FIDO_MDS` 来源和原始稳定状态码。只有已生效的 status report 参与判断；现有库未保存 attestation
证书链及认证器版本，因此存量评估只能按 AAGUID 保守判断。MDS 状态本身不会自动撤销凭证或锁定账号；
租户可通过 core 的显式风险策略阻断 `WARNING`、`CRITICAL` 或 `NOT_EVALUATED` 的新断言。provider 在真实
签名、凭证归属、user handle 与账号校验通过后、签名计数和最近使用时间更新前执行该策略；命中后认证事务
边界只向匿名客户端暴露通用的无效 Passkey 结果，不泄露设备风险明细。策略不会追溯撤销既有会话。
命中事件使用 credential SHA-256 指纹关联内部审计，不包含浏览器提交的原始 ID 或断言载荷；core 在部署
Micrometer 时同时递增只按风险级别分组的低基数计数器，供运维系统配置告警。

只有租户策略实际限制 format/AAGUID 或要求可信证明时，registration begin 才请求 `DIRECT`
attestation；普通租户继续请求 `NONE`，不扩大默认认证器信息收集范围。企业 PKI trust source 适配仍待
接入。

只有确认所有承载公开认证流量的节点都启用了本 provider 和上述自助注册入口后，才允许租户保存
WebAuthn-only 的 REQUIRED/CONDITIONAL 策略：

```yaml
kudos:
  ms:
    auth:
      mfa:
        policy:
          allow-webauthn-only: true
```

该开关默认 `false`；TOTP 仍可在未启用此开关时作为强制策略的可部署兜底方法。
