# Kudos 安全审计待办（kudos + kudos-console-ui）

> 生成日期：2026-10-03
> 来源：10 个只读审计代理按领域并行审查 kudos 后端与 kudos-console-ui 前端（authn / authz / framework-web / data-file-cache / distributed-comm-log / ms-sys-user / ms-msg-tag / ui-xss / ui-auth / config-supplychain）。
> 原始报告 41 条，合并同根因后为 **36 项**（S01–S36）。
> 核对状态：**S01–S04 已人工对照源码确认**；其余为代理自证（代理自己顺着入口追到漏洞点，并说明为什么已有防护不适用），**修之前先复核一遍**——标 `plausible` 的尤其要先证实可达。
> 已排除：kudos-ms-auth-core README §9 的 A1–A19 不重复登记（S07 例外，A6 修复不完整）。
> 前端条目（S17、S31–S34、S36）的代码在 **kudos-console-ui** 仓库，统一在此登记。

## 总览

| ID | 严重度 | 模块 | 一句话 | 核对 |
|----|------|------|--------|------|
| S01 | 🔴 Critical | ms-auth | 按编码绑定权限无校验，租户管理员绑 `*` 升平台级 | ✅ 已确认 |
| S02 | 🟠 High | ms-auth | `/api/internal/**` 无服务认证，getRoleUsers 返回密码哈希 + TOTP 种子 | ✅ 已确认 |
| S03 | 🟠 High | ms-user | resetAuthKey / unfreezeAccount 不检查职级，低职级管理员拿到上级的 TOTP 密钥 | ✅ 已确认 |
| S04 | 🟠 High | ms-auth | GENERIC_OIDC 的 clientSecretRef 不绑租户 + 自定义 issuer → 外带他租户 secret | 代理确认 |
| S05 | 🟡 Medium | ms-auth oauth2 | OIDC discovery 端点不过校验 + 匿名实时拉取 → SSRF | 代理确认 |
| S06 | 🟡 Medium | ms-auth | 自启用 TOTP 在第三方/邮件 OTP 登录被跳过，且可在该会话中关闭 | 代理确认 |
| S07 | 🟡 Medium | ms-auth | 实例分享不绑操作者租户（A6 修复不完整） | 代理确认 |
| S08 | 🟡 Medium | ms-auth | revokeAllTokens 不校验目标租户，可跨租户踢人 | 代理确认 |
| S09 | 🟡 Medium | ms-auth | legacy 模式数据范围 / SoD 写接口无租户校验 | 代理确认 |
| S10 | 🟡 Medium | web-springmvc / log-audit | 无条件信任 X-Forwarded-For，IP 限流与审计 IP 可伪造 | 代理确认 |
| S11 | 🟡 Medium | web-ktor | WebSocket maxFrameSize = Long.MAX_VALUE | plausible |
| S12 | 🟡 Medium | data-memdb-redis | Redis 默认 fastjson2 AutoType 序列化器 | 代理确认 |
| S13 | 🟡 Medium | discovery-nacos | RPC 上下文过滤器信任可伪造标记头且不清 ThreadLocal | plausible |
| S14 | 🟡 Medium | log-audit | multipart Content-Type 即跳过整条审计 | 代理确认 |
| S15 | 🟡 Medium | ms-user | 自助 verify/changePassword 无失败计数，可在线爆破 | 代理确认 |
| S16 | 🟡 Medium | ms-sys | 缓存管理 getValueJson 可导出用户凭证缓存 | plausible |
| S17 | 🟡 Medium | console-ui | 重置密码 / 数据源口令走 URL query（数据源那个还与后端不匹配） | 代理确认 |
| S18 | 🟡 Medium | ms-msg | 内部 publish 不绑调用方租户，可跨租户投递 | plausible |
| S19 | 🔵 Low | ms-auth | TOTP 无防重放，±3 时间窗 | 代理确认 |
| S20 | 🔵 Low | ms-auth email-otp | 邮件 OTP 可被匿名用于邮件轰炸 | 代理确认 |
| S21 | 🔵 Low | ms-auth | 多个 admin 读接口跨租户泄露角色/成员/权限 | 代理确认 |
| S22 | 🔵 Low | web-swagger | enabled=false / production 关不掉 /v3/api-docs | plausible |
| S23 | 🔵 Low | context | KudosContextHolder 用 InheritableThreadLocal，线程池串上下文 | plausible |
| S24 | 🔵 Low | web-springmvc | 绑定错误回显内部类型与原始值 | 代理确认 |
| S25 | 🔵 Low | file | 本地存储 tenantId/category/bucket 中 `../` 可越租户目录 | plausible |
| S26 | 🔵 Low | client-http | 上下文 HMAC 不覆盖 body、不绑目标、nonce 仅单实例去重 | plausible |
| S27 | 🔵 Low | comm-email | SMTP STARTTLS 可降级 | 代理确认 |
| S28 | 🔵 Low | ms-msg | 收件箱内部 API 存在 IDOR | 代理确认 |
| S29 | 🔵 Low | ms-tag | 手工打标信任请求体中的 operator，审计可伪造 | 代理确认 |
| S30 | 🔵 Low | ms-tag | subject type 写接口用 view 权限码 | 代理确认 |
| S31 | 🔵 Low | console-ui | mock 后端打进生产包，按 localhost 自动启用 | 代理确认 |
| S32 | 🔵 Low | console-ui | 登出不清 localStorage 里的列表数据与导航状态 | 代理确认 |
| S33 | 🔵 Low | console-ui | token 存 localStorage 且无 CSP | 代理确认 |
| S34 | 🔵 Low | web-guest | 访客 Cookie 密钥硬编码默认值 | 代理确认 |
| S35 | 🔵 Low | security-enforcement | 默认公开路径 `/actuator/**` 通配 | plausible |
| S36 | 🔵 Low | build | Gradle 解析链无完整性保护（mavenLocal 首位、无 verification-metadata） | 代理确认 |
| S37 | ⚪ Info | console-ui | Vite dev server host:true 暴露到局域网 | 代理确认 |
| S38 | ⚪ Info | 本地 | 删除 `kudos/java_pid84958.hprof`（735MB，已 gitignore，扫描未见密钥） | — |

---

## P0 — 🔴 Critical / 🟠 High

### - [ ] S01 按编码绑定权限无校验，租户管理员可绑 `*` 升平台级
- **位置**：`kudos-ms/kudos-ms-auth/kudos-ms-auth-core/src/io/kudos/ms/auth/core/policy/service/impl/AuthGrantPolicyService.kt:245`（`screenPermissionBindings`）；入口 `AuthRoleResourceService.kt:134` `savePermissionBinding`
- **问题**：对 `permissionCodes` 只做非空检查；接口契约要求的“存在 / 属于本子系统 / 本租户”均未实现（resourceId 路径有子系统校验，编码路径没有）。`PermissionCodes.matches` 中 `*` 匹配一切。默认种子 tenant-admin 带 `auth:role:*`，足以调用 `savePermissionBinding`。
- **攻击**：租户管理员 `POST /api/admin/auth/role/savePermissionBinding {roleId:<本租户角色>, permissionCode:"*", effect:"ALLOW"}` → 获得 sys tenant/dataSource/resource/cache、`revokeAllTokens` 等平台接口。
- **修法**：
  - [ ] 非平台管理员禁止绑定含通配段的编码（`*`、`x:*` 等）
  - [ ] 具体编码必须在 sys_resource 中存在且 active，`subSystemCode == role.subsysCode`
  - [ ] 操作者包含性：只能绑定自己已持有的编码（与 screenDelegation 的 Power 维度一致）
  - [ ] 审计存量 `auth_role_resource` 中的 `*` 与跨子系统编码
  - [ ] 负例测试 + shadowDemo 实测（探针：摘掉校验后测试应变红）

### - [ ] S02 `/api/internal/**` 无服务认证，getRoleUsers 返回凭证字段
- **位置**：`kudos-ms/kudos-ms-auth/kudos-ms-auth-api-internal/src/io/kudos/ms/auth/api/internal/controller/role/AuthRoleInternalController.kt:31`；`kudos-ms-user-common/.../UserAccountCacheEntry.kt:26-65`
- **问题**：返回的 `UserAccountCacheEntry` 含 `loginPassword` / `securityPassword` / `sessionKey` / `authenticationKey`（TOTP 种子）。`EnforcementProperties.enabled` 默认 false，`AdminAuthorizationFilter` 只管 `/api/admin`，`InternalRpcContextWebFilter` 只在有标记头时验签、不做认证。
- **攻击**：内部接口一旦可从外网到达（网关漏配），就能匿名拉取任意角色成员的密码哈希和 TOTP 种子。
- **修法**：
  - [ ] `/api/internal/**` 强制服务身份认证（mTLS / 服务 JWT / 强制 HMAC，不依赖标记头），失败返回 401
  - [ ] 内部接口不直接序列化 `UserAccountCacheEntry`，改用去掉凭证字段的 DTO；排查所有返回该类型的接口
  - [ ] `/api/internal/auth/authz/decide` 同样只允许已认证服务调用

### - [ ] S03 resetAuthKey / unfreezeAccount 不检查职级
- **位置**：`kudos-ms/kudos-ms-user/kudos-ms-user-core/src/io/kudos/ms/user/core/account/service/impl/UserAccountService.kt:571`（resetAuthKey）、`:818`（unfreezeAccount）
- **问题**：`resetPassword` / `resetSecurityPassword` / `cleanAuthKey` 都调用 `assertCanMaintainCredentials`，这两个没有调用；`accessibleAccount` 只校验组织，不比职级。
- **攻击**：组织内低职级管理员重置组织管理员的 TOTP，拿到新密钥。
- **修法**：
  - [ ] resetAuthKey（或 persistAuthKey）、verifyAuthCode 先取 existing 再 `assertCanMaintainCredentials(existing)`
  - [ ] unfreezeAccount 改调 `assertCanEnd(existing)`，与 freezeAccount 对称
  - [ ] `UserOwnedCrudService` 对组织账号从属记录补 `assertCanManageMember`
  - [ ] 考虑 resetAuthKey 不直接下发 secret，改由用户本人走 enrollment；管理端 verifyAuthCode 接入 `IAuthenticationAttemptLimiter`

### - [ ] S04 GENERIC_OIDC clientSecretRef 不绑租户，可外带他租户 secret
- **位置**：`kudos-ms/kudos-ms-auth/kudos-ms-auth-core/src/io/kudos/ms/auth/core/provider/management/service/impl/IdentityProviderManagementService.kt:199`；`DynamicClientRegistrationRepository.build`
- **问题**：`normalizeSecretRef` 只校验格式；各 secret 解析器的前缀白名单是全局的。租户可自定义 issuer，系统用 CLIENT_SECRET_BASIC 把解析出的 secret 发到该 issuer 的 token_endpoint。
- **修法**：
  - [ ] 解析时强制引用路径含 provider.tenantId 前缀（如 `vault:oauth/{tenantId}/…`），在 `ClientSecretResolverRegistry.resolve/verify` 中校验；或只允许平台管理员写 clientSecretRef
  - [ ] 自定义 issuer 的 provider 禁用平台级共享引用；token_endpoint 要求与 issuer 同源或在白名单内
  - [ ] verify 接口同样校验，避免成为探测 oracle

---

## P1 — 🟡 Medium

### - [ ] S05 OIDC discovery SSRF
- **位置**：`kudos-ms-auth-provider-oauth2/.../registration/DynamicClientRegistrationRepository.kt:73`
- **修法**：discovery 返回的各端点逐一 `requireSafeHttps` 并要求同源/白名单；`requireSafeHttps` 先解析 DNS，拒绝回环/私有/链路本地/元数据地址，连接时固定解析结果；discovery 结果带 TTL 缓存，或在管理端保存时解析并持久化。

### - [ ] S06 自启用 TOTP 在第三方 / 邮件 OTP 登录被跳过
- **位置**：`kudos-ms-auth-core/.../authentication/mfa/policy/AuthenticationMfaPolicyEnforcer.kt:51`
- **修法**：`decision.enrolled && achievedAcr < ACR_MFA` 时无论 mode 都返回 REQUIRE_SECOND_FACTOR；关闭/修改 MFA 因素（TOTP disable、Passkey revoke/register、恢复码生成）在账号已有 MFA 时要求 ACR_MFA 或 step-up。

### - [ ] S07 实例分享跨租户（A6 补全）
- **位置**：`kudos-ms-auth-core/.../instance/service/impl/AuthInstanceGrantService.kt:75`
- **修法**：share / unshare / list* 调 `tenantAdministrationGuard.assertCanManage(tenantId)`，unshare 取 `grant.tenantId`；或不接受请求里的 tenantId，改从 CurrentUserKit 取（平台管理员除外）。README §9 把 A6 标为“补全”。

### - [ ] S08 revokeAllTokens 跨租户踢人
- **位置**：`kudos-ms-auth-api-admin/.../version/PermissionVersionAdminController.kt:59`
- **修法**：仿照 `AuthenticationSessionAdminController`，要求目标 `user.tenantId == 当前租户`（平台管理员例外）；`current()` 同样处理。

### - [ ] S09 legacy 模式数据范围 / SoD 写接口无租户校验
- **位置**：`kudos-ms-auth-core/.../role/datascope/service/impl/AuthRoleDataScopeService.kt:108`
- **修法**：无条件 `assertCanEditRole(roleId, role.tenantId)` / `assertCanManage(rule.tenantId)`，组织所有者再走 policy()；补跨租户负例测试。

### - [ ] S10 X-Forwarded-For 可伪造（合并原 #9 + #26）
- **位置**：`kudos-ability-web-springmvc/.../support/XHttpServletRequest.kt:31`；`kudos-ability-log-audit-common/.../support/AuditLogTool.kt:389`
- **修法**：新增 `trusted-proxies` 配置（默认空）：默认只用 `remoteAddr`；仅当 remoteAddr 属于可信代理时才从 XFF 右往左取第一个不可信地址；不再读 Proxy-Client-IP / WL-Proxy-Client-IP；结果须为合法 IP。与 `EnforcementProperties.trustForwardedFor` 共用一套判定。审计同时记录 remoteAddr 与解析出的客户端 IP。

### - [ ] S11 Ktor WebSocket 帧长无上限
- **位置**：`kudos-ability-web-ktor/.../init/KtorPlugins.kt:72`
- **修法**：maxFrameSize 设有限默认值（64KB–1MB）并开放配置；WebSocket 插件默认不安装；文档要求路由置于 `authenticate {}` 内。

### - [ ] S12 Redis 默认 fastjson2 AutoType 序列化器
- **位置**：`kudos-ability-data-memdb-redis/.../RedisSerializerEnum.kt:41`
- **修法**：改用带包名白名单的 `GenericFastJsonRedisSerializer(arrayOf("io.kudos.", …))`，或改为不带多态类型的 JSON 序列化；pub/sub 失效消息改为固定结构 JSON，直接反序列化为 CacheMessage；删除/废弃 JDK 选项，把代码默认值改为安全序列化器；文档要求生产 Redis 设密码/ACL 并启用 fastjson2 safeMode。

### - [ ] S13 RPC 上下文过滤器信任标记头、不清 ThreadLocal
- **位置**：`kudos-ability-distributed-discovery-nacos/.../filter/InternalRpcContextWebFilter.kt:119`
- **修法**：doFilter 外包 try/finally 恢复或 clear；生产 profile 未配 contextSignatureSecret 时启动失败，或默认拒绝未签名的带标记请求；修正 README；理顺与 WebContextInitFilter 的顺序。

### - [ ] S14 multipart 跳过整条审计
- **位置**：`kudos-ability-log-audit-common/.../annotation/WebLogAuditAspect.kt:67`
- **修法**：multipart 只跳过读取 body，仍提交 LogVo（requestFormData 记为 `[multipart omitted]` 或只记参数名/文件名）；或加 `allowMultipart` 属性，非上传接口收到 multipart 时记录或拒绝。

### - [ ] S15 自助密码校验可在线爆破
- **位置**：`kudos-ms-user-core/.../passport/service/impl/PassportService.kt:430`
- **修法**：verifyPassword / changePassword 的旧密码校验接入 `IAuthenticationAttemptLimiter`（tenant+userId 与来源 IP）；verifySecurityPassword / changeSecurityPassword 失败时调 `incrementSecurityPasswordErrorTimes`、成功时重置，达到阈值后锁定或要求重新认证；连续失败时吊销当前会话。

### - [ ] S16 缓存管理可导出用户凭证缓存
- **位置**：`kudos-ms-sys-core/.../cache/service/impl/SysCacheService.kt:263`
- **修法**：黑名单改白名单，或由 handler 声明 sensitive / 提供脱敏视图；`USER_ACCOUNT__HASH` 至少返回 `eraseCredentials()` 副本；`REMEMBER_ME_BY_TENANT_ID_AND_USERNAME`、`USER_BY_ID` 加入禁止列表。

### - [ ] S17 【console-ui】明文口令放在 URL query（合并原 #17 + #18 + #32）
- **位置**：`kudos-console-ui/webApp/src/pages/sys/datasource/DataSourceListPage.vue:417`；`kudos-console-ui/webApp/src/pages/user/account/AccountSecurityDialog.vue:215`
- **问题**：账号重置登录密码 / 安全密码、数据源加密都通过 `paramsInQuery` 把口令放进 URL；数据源“重置密码”还用 GET，参数名与后端不一致，功能本身是坏的。
- **修法**（需前后端一起改）：
  - [ ] 后端 resetPassword / resetSecurityPassword / encrypt / 数据源 resetPassword 改为 `@RequestBody` DTO
  - [ ] 前端去掉 paramsInQuery，改用 JSON body；DataSourceListPage 用 POST + `newPassword`
  - [ ] `backendRequest` 加防护：params 含 password / newPassword / secret 等字段时拒绝放进 query
  - [ ] 补测试断言 URL 不含口令；Vite proxy 慢请求日志只打 path

### - [ ] S18 内部 publish 跨租户投递
- **位置**：`kudos-ms-msg-core/.../send/service/impl/MsgPublishService.kt:54`
- **修法**：入口要求 `request.tenantId == KudosContext.tenantId`（平台身份除外）；批量校验 receiverIds 属于该租户；`getActiveContactValuesByUserIds` 增加 tenantId 参数，在 SQL 层过滤。

---

## P2 — 🔵 Low

### - [ ] S19 TOTP 无防重放
- **位置**：`kudos-ms-auth-core/.../authentication/mfa/FederatedSecondFactorVerifier.kt:54`
- **修法**：windowSize 降到 1；按用户持久化最后成功的时间步，CAS 拒绝 ≤ 该时间步的码（RFC 6238 §5.2）；SECRET_SIZE 从 10 字节提到 20 字节。

### - [ ] S20 邮件 OTP 轰炸
- **位置**：`kudos-ms-auth-provider-email-otp/.../EmailOtpAuthenticationMethodProvider.kt:90`
- **修法**：按 tenant+email 设长窗口配额（如 15 分钟 3 封、每天 10 封）与重发冷却；超过阈值后要求人机校验；按租户做发送量告警。

### - [ ] S21 admin 读接口跨租户泄露
- **位置**：`kudos-ms-auth-core/.../role/service/impl/AuthRoleService.kt:246`
- **修法**：pagingSearch 对非平台管理员强制 tenantId；覆盖 `get(id, cls)` 加 assertCanManage；按 userId 查询的接口先校验目标租户；tenantBootstrap.status、explain 补 assertCanManage。

### - [ ] S22 swagger 开关关不掉 /v3/api-docs（合并原 #22 + #40）
- **位置**：`kudos-ability-web-swagger/.../init/SwaggerAutoConfiguration.kt:45`、`.../properties/SwaggerProperties.kt:32`
- **修法**：enabled=false 或 production=true 时通过 EnvironmentPostProcessor 写入 `springdoc.api-docs.enabled=false` 和 `springdoc.swagger-ui.enabled=false`；或默认 enabled=false，开发 profile 显式开启；修正 README。

### - [ ] S23 InheritableThreadLocal 串上下文
- **位置**：`kudos-context/src/io/kudos/context/core/KudosContextHolder.kt:15`
- **修法**：改为普通 ThreadLocal；跨线程传播用显式 TaskDecorator（提交时深拷贝快照，执行后 finally clear）；修改 KDoc。⚠️ 线程类改动，必须 shadowDemo 活体验证。

### - [ ] S24 绑定错误回显内部信息
- **位置**：`kudos-ability-web-springmvc/.../handler/BadRequestExceptionHandler.kt:154`
- **修法**：typeMismatch 返回固定文案；默认不返回 rejectedValue；不暴露 objectName。

### - [ ] S25 本地文件存储越租户目录
- **位置**：`kudos-ability-file-common/.../AbstractUploadService.kt:57`
- **修法**：tenantId / category / bucketName 做段级白名单（`^[A-Za-z0-9_-]+$`）；校验根收紧到 `basePath/bucket/tenantId`；上传用 CREATE_NEW 或服务端 UUID 文件名。

### - [ ] S26 上下文 HMAC 签名范围不足
- **位置**：`kudos-ability-distributed-client-http/.../interceptor/KudosContextRequestInterceptor.kt:101`
- **修法**：签名内容加入 body SHA-256 与目标 serviceId/host；nonce 改用 Redis `SET NX PX` 做集群级去重；拦截器只挂到内部服务分组上。

### - [ ] S27 SMTP STARTTLS 可降级
- **位置**：`kudos-ability-comm-email/.../handler/EmailHandler.kt:77`
- **修法**：ssl=false 时设 `mail.smtp.starttls.required=true`，明文须显式配置；同步 README。

### - [ ] S28 收件箱 IDOR
- **位置**：`kudos-ms-msg-core/.../receiver/service/impl/MsgReceiveService.kt:44`
- **修法**：契约加 receiverId + tenantId，或从上下文取当前用户；markRead 改为 `where id=? and receiver_id=? and tenant_id=?`；getInstanceById 加租户过滤。

### - [ ] S29 打标审计可伪造
- **位置**：`kudos-ms-tag-api-admin/.../controller/TagAssignmentAdminController.kt:33`
- **修法**：operatorId / operatorName 用当前登录用户覆盖，occurredAt 用服务端时间；加 @WebAudit；internal 代理操作单独记录 actor / onBehalfOf。

### - [ ] S30 subject type 写接口用 view 权限码
- **位置**：`kudos-ms-tag-api-admin/.../controller/TagSubjectTypeAdminController.kt:31`
- **修法**：改为 `tag:subject-type:manage`，同步权限点注册与 seed。

### - [ ] S31 【console-ui】mock 后端进生产包
- **位置**：`kudos-console-ui/webApp/src/mocks/mockBackend.ts:108`
- **修法**：门控先判断 `import.meta.env.DEV`，或用动态 import + define 常量让生产包 tree-shake 掉；`.env.production` 显式写 `VITE_USE_MOCK=false`；mock 模式加醒目横幅。

### - [ ] S32 【console-ui】登出不清本地持久化数据
- **位置**：`kudos-console-ui/webApp/src/components/widgets/Header.vue:309`
- **修法**：登出 / 切换账号或组织时统一清理列表状态 key（统一前缀如 `kudos.listState.*`，加上 tags_list、current_menu_path），重置 store；更好的做法是只持久化查询条件，不存 tableData，或把 key 绑定 userId + contextVersion。

### - [ ] S33 【console-ui】token 存 localStorage 且无 CSP
- **位置**：`kudos-console-ui/webApp/src/api/httpClient.ts:52`、`webApp/index.html`
- **修法**：短期在 index.html 或反向代理加严格 CSP（`default-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'`），移除 `window.ajax` 全局暴露；中期改为 HttpOnly + Secure + SameSite=Strict Cookie 并加 CSRF 防护。

### - [ ] S34 访客 Cookie 密钥硬编码默认值
- **位置**：`kudos-ability-web-guest/.../init/properties/GuestCookieProperties.kt:54`
- **修法**：删除默认密钥，cipherKey 必填；启用 guest 时密钥为空、等于历史默认值或过短则启动失败；从环境变量注入；Cookie 默认带 Secure。

### - [ ] S35 默认公开路径 `/actuator/**`
- **位置**：`kudos-ability-security-enforcement/.../init/properties/EnforcementProperties.kt:48`
- **修法**：收窄为 `/actuator/health/**`、`/actuator/info`；对 publicPaths 含宽通配的情况告警或启动失败。（目前项目未引入 actuator，属预防项。）

### - [ ] S36 Gradle 依赖解析链完整性
- **位置**：`kudos/settings.gradle.kts:11`
- **修法**：去掉 mavenLocal()，或用 `content { includeGroup(...) }` 限定；aliyun 镜像加 content 过滤或改为 init script 启用；`./gradlew --write-verification-metadata sha256` 生成并提交 verification-metadata.xml；`gradle-wrapper.properties` 补 `distributionSha256Sum`。

---

## P3 — ⚪ Info / 清理

- [ ] **S37**【console-ui】`webApp/vite.config.ts:72` 的 `host: true` 改为默认 `localhost`，需要局域网联调时用 `VITE_DEV_HOST` 显式开启。
- [ ] **S38** 本地删除 `kudos/java_pid84958.hprof`（735MB，Gradle 测试 worker 产生；已被 `*.hprof` 忽略、从未提交；strings 扫描未见私钥/云密钥/JWT）。

---

## 已检查、未发现问题的部分（避免重复审）

- **JWT**：校验 iss / aud / exp / nbf / token_use / pv，并回查会话；JWKSet 只有 RSA 密钥，不存在 alg 混淆。
- **Refresh token**：只存哈希、单次 CAS 消费，复用时撤销整个 family。
- **OAuth2**：state 绑会话、单次使用、强制 PKCE；不按邮箱自动关联账号；成功/失败跳转只允许本地路径。
- **WebAuthn**：rpId / origin 白名单，ceremony 一次性消费，签名计数 CAS。
- **PEP 路径**：路径规范化后再匹配（A10），未注册路径拒绝，断路器 fail-closed。
- **SQL**：Ktorm 全部参数化，ORDER BY 有白名单，RowScope 覆盖读写删各条路径；JDBC URL 有驱动与参数白名单。
- **基础组件**：CORS 默认拒绝；XXE 已防；Jackson 无默认多态；CryptoKit 使用 AES-GCM + SecureRandom；MQ 的 JDK 反序列化有精确类名白名单。
- **依赖与密钥**：`npm audit` 0 个漏洞；两个仓库 git 历史中无真实密钥；未引入 Actuator；H2 console 未开启。
