# kudos-ms-auth-api-admin

## 定位

鉴权（`auth`）原子服务的**管理端 HTTP API 层**：在 `kudos-ms-auth-core` 之上提供**面向控制台 /
管理网关**的 Spring MVC 控制器，路径统一落在 **`/api/admin/auth/...`** 下。与 `api-public` /
`api-internal` 的区别在于：本模块**包含具体 Controller 类**，而不仅是启动入口。

---

## 入口与自动配置

| 类型 | 类 | 说明 |
|------|----|------|
| 启动类 | `AuthApiAdminApplication` | `@EnableKudos`，可作为独立 Spring Boot `main` 运行 |
| 自动配置 | `AuthApiAdminAutoConfiguration` | `@ComponentScan("io.kudos.ms.auth.api.admin")`，`IComponentInitializer` 组件名 **`kudos-ms-auth-api-admin`** |

依赖 **`kudos-ability-web-springmvc`**，继承工程内通用 Controller 基类（如 `BaseCrudController`）
实现标准 CRUD 与扩展接口。

**包结构**：在 **`io.kudos.ms.auth.api.admin.controller`** 下再按业务模块分子包
**`...controller.<模块>`**（与 `common` / `core` 模块名对齐）——`controller/role/`、`controller/group/`；
启动与扫描仍在 **`...api.admin.init`**。

---

## 控制器一览

主要 Controller 如下：

| 控制器 | 基础路径 | 职责概要 |
|--------|----------|----------|
| `AuthRoleAdminController` | `/api/admin/auth/role` | 角色 CRUD + 启用状态、角色 ↔ 用户、角色 ↔ 资源 |
| `AuthGroupAdminController` | `/api/admin/auth/group` | 用户组 CRUD、组 ↔ 用户、组 ↔ 角色 |
| `AuthenticationSessionAdminController` | `/api/admin/auth/sessions` | 查看同租户用户的活动会话并逐台远程撤销 |
| `WebAuthnCredentialAdminController` | `/api/admin/auth/webauthn/credentials` | 只读审计同租户用户的活动及已撤销 Passkey 凭证元数据 |
| `WebAuthnAttestationPolicyAdminController` | `/api/admin/auth/webauthn/attestationPolicy` | 查询/保存当前管理员租户的认证器准入策略与 trust source 能力 |
| `WebAuthnAuthenticatorRiskPolicyAdminController` | `/api/admin/auth/webauthn/riskPolicy` | 查询/保存当前管理员租户的认证器风险阻断策略与评估器能力 |
| `AuthSecurityEventAdminController` | `/api/admin/auth/securityEvents` | 查询当前租户安全事件，并执行分派、确认、关闭处置状态机 |
| `AuthSecurityEventNotificationAdminController` | `/api/admin/auth/securityEventNotifications` | 查询当前租户安全事件通知死信，并执行带审计的受控重放 |
| `AuthSecurityEventNotificationRouteAdminController` | `/api/admin/auth/securityEventNotificationRoutes` | 查询/保存当前租户安全事件通知路由规则，带乐观并发与变更审计 |
| `AuthSecurityEventOnCallRosterAdminController` | `/api/admin/auth/securityEventOnCallRosters` | 查询/整份替换当前租户安全事件值班表，带乐观并发与排班快照审计 |
| `ExternalIdentityAdminController` | `/api/admin/auth/externalIdentity` | 带权限、租户边界、操作原因和审计快照的管理员预绑定/代解绑 |
| `ExternalIdentityInvitationAdminController` | `/api/admin/auth/externalInvitation` | 为既有用户签发/吊销一次性第三方身份邀请 |
| `IdentityProviderJitConfigAdminController` | `/api/admin/auth/identityProviderJitConfig` | 查询/保存 Provider 一对一的 JIT 准入规则与账号默认值 |
| `IdentityProviderAdminController` | `/api/admin/auth/identityProvider` | 查询模板和租户 Provider，创建、更新及启停实例 |
| `IdentityProviderClaimMappingAdminController` | `/api/admin/auth/identityProviderClaimMapping` | 查询/保存 Provider 一对一的类型化 claim path |
| `TenantMfaPolicyAdminController` | `/api/admin/auth/mfaPolicy` | 查询/保存当前管理员租户的 MFA 模式、条件、宽限期及允许因子 |
| `MfaEnrollmentExemptionAdminController` | `/api/admin/auth/mfaEnrollmentExemptions` | 为宽限期已过、尚未注册 MFA 的同租户账号授予/撤销限期注册豁免 |
| `AuthLoginEventAdminController` | `/api/admin/auth/loginEvents` | 只读查询当前租户认证结果审计，支持按用户、尝试标识和成败筛选 |
| `AuthCredentialRevocationAdminController` | `/api/admin/auth/credentialRevocations` | 吊销同租户账号的 Passkey 或 TOTP 认证器，并查询追加式吊销记录 |

可选的 `kudos-ms-auth-provider-oauth2` 安装后还会在同一 Admin 路径空间注册
`IdentityProviderSecretAdminController`。它位于可选模块是为了不让纯鉴权部署被迫引入 Spring
Security OAuth2 Client；网关应仍将其视为 Auth Admin 接口。

会话管理使用显式权限和可信租户归属：

```text
GET  /api/admin/auth/sessions?userId={userId}
     auth:session:view

POST /api/admin/auth/sessions/{sessionId}/revoke
     auth:session:revoke
     { "userId": "...", "reason": "..." }
```

目标租户由当前管理员和 User 域账号记录共同确定，请求不能指定 `tenantId`。跨租户用户与不存在用户
统一返回 404，避免账号标识枚举；撤销仅对目标用户拥有的逻辑会话生效，原因必填并同时进入会话记录
和 Web 审计。逻辑会话撤销立即生效，远端 servlet Session 默认在下一次请求时由 Auth Filter 失效；安装可选的
[`kudos-ms-auth-session-spring`](../kudos-ms-auth-session-spring/README.md) 后，容器侧记录在撤销瞬间就被物理删除。

Passkey 凭证审计使用独立只读权限：

```text
GET /api/admin/auth/webauthn/credentials?userId={userId}
    auth:webauthn-credential:view
```

目标账号必须属于当前管理员租户，跨租户与不存在账号统一返回 404。响应同时包含活动和已撤销记录、
创建/最近使用/撤销时间、AAGUID、attestation format、transport、备份和 discoverable 状态。原始
credential ID 只以完整 SHA-256 Base64url 指纹返回；响应不包含 credential ID 原文、COSE 公钥、
user handle 或签名计数。凭证撤销另由 `/api/admin/auth/credentialRevocations` 提供，见下文；本只读接口本身不做修改。响应另包含 `authenticatorRiskLevel`、
`authenticatorRiskSources` 和 `authenticatorRiskStatusCodes`；未配置风险源或凭证没有 AAGUID 时返回
`NOT_EVALUATED`。风险字段是只读治理信号，本接口不会因 MDS 状态变化自动撤销凭证。

WebAuthn 认证器准入策略使用独立管理权限：

```text
GET  /api/admin/auth/webauthn/attestationPolicy/get
     auth:webauthn-attestation-policy:view

POST /api/admin/auth/webauthn/attestationPolicy/save
     auth:webauthn-attestation-policy:update
```

请求不能指定 `tenantId` 或操作者；两者固定取当前管理员会话。策略支持 AAGUID `NONE`、
`ALLOW_LIST`、`DENY_LIST` 模式、attestation format 白名单及 `requireTrustedAttestation`，保存原因必填并
进入领域审计字段和 Web 审计。查询响应同时返回 `configured`、`effectiveFrom` 和
`trustSourceAvailable`；部署没有唯一 trust source 时，开启可信证明要求会返回 409，而不是保存一项
运行时必然拒绝所有新凭证的策略。可选 WebAuthn provider 启用并成功加载 FIDO MDS 后，其内置
`AttestationTrustSource` 会使该能力返回 `true`。

WebAuthn 认证器风险处置使用另一组独立权限：

```text
GET  /api/admin/auth/webauthn/riskPolicy/get
     auth:webauthn-risk-policy:view

POST /api/admin/auth/webauthn/riskPolicy/save
     auth:webauthn-risk-policy:update
```

请求同样不能指定 `tenantId` 或操作者，变更原因必填。`blockedRiskLevels` 默认为空，即只审计；可选值为
`NOT_EVALUATED`、`WARNING`、`CRITICAL`，`NORMAL` 明确拒绝。查询返回 `riskEvaluationAvailable`、
`configured` 和 `effectiveFrom`；没有已部署风险评估器时，保存非空阻断集合返回 409。命中策略只阻断新的
WebAuthn 断言，不通过本管理接口自动撤销凭证或既有会话。

阻断安全事件使用独立只读权限：

```text
GET /api/admin/auth/securityEvents?userId={optional}&riskLevel={optional}&status={optional}&assigneeUserId={optional}&overdueOnly={false|true}&limit={1..500}
    auth:security-event:view

POST /api/admin/auth/securityEvents/{id}/assign
     auth:security-event:assign
     { "assigneeUserId": "...", "expectedVersion": 0, "reason": "..." }

POST /api/admin/auth/securityEvents/{id}/acknowledge
     auth:security-event:acknowledge
     { "expectedVersion": 0, "reason": "..." }

POST /api/admin/auth/securityEvents/{id}/close
     auth:security-event:close
     { "expectedVersion": 1, "resolution": "MITIGATED", "reason": "..." }

GET /api/admin/auth/securityEventNotifications/dead?eventId={optional}&limit={1..500}
    auth:security-event-notification:view

POST /api/admin/auth/securityEventNotifications/{id}/replay
     auth:security-event-notification:replay
     { "reason": "notification route repaired" }

GET /api/admin/auth/loginEvents?userId={optional}&identifier={optional}&successOnly={optional}&limit={1..500}
    auth:login-event:view

GET /api/admin/auth/credentialRevocations/list?userId={optional}&limit={1..200}
    auth:credential-revocation:view

POST /api/admin/auth/credentialRevocations/revoke
     auth:credential-revocation:revoke
     { "userId": "...", "credentialType": "WEBAUTHN|TOTP",
       "credentialRef": "<凭证审计视图中的行 id>",
       "reason": "authenticator reported stolen", "securityEventId": "<optional>" }

GET /api/admin/auth/mfaEnrollmentExemptions/list?userId={optional}&limit={1..200}
    auth:mfa-exemption:view

POST /api/admin/auth/mfaEnrollmentExemptions/grant
     auth:mfa-exemption:grant
     { "userId": "...", "expiresAt": "2026-09-01T10:00:00", "reason": "lost phone" }

POST /api/admin/auth/mfaEnrollmentExemptions/revoke
     auth:mfa-exemption:revoke
     { "userId": "...", "reason": "device returned" }

GET /api/admin/auth/securityEventNotificationRoutes/list
    auth:security-event-notification-route:view

POST /api/admin/auth/securityEventNotificationRoutes/save
     auth:security-event-notification-route:update
     {
       "notificationType": "SLA_ESCALATED", "appliesTo": "ASSIGNED|UNASSIGNED",
       "routeCode": "ASSIGNEE_EMAIL", "destination": "USER|TENANT_SECURITY_QUEUE",
       "channels": ["EMAIL"], "responderUserIds": ["..."],
       "responderRosterCode": "SECURITY",
       "includeAssignee": true, "enabled": true,
       "fallbackBehavior": "DEFAULT_ROUTE|TENANT_SECURITY_QUEUE|FAIL",
       "expectedVersion": 0, "reason": "duty roster change"
     }

GET /api/admin/auth/securityEventOnCallRosters/list
    auth:security-event-oncall:view

POST /api/admin/auth/securityEventOnCallRosters/save
     auth:security-event-oncall:update
     {
       "rosterCode": "SECURITY", "displayName": "安全值班", "enabled": true,
       "shifts": [
         { "responderUserId": "...", "tier": 1,
           "startAt": "2026-08-25T00:00:00", "endAt": "2026-08-26T00:00:00" }
       ],
       "expectedVersion": 0, "reason": "weekly rotation"
     }
```

凭证吊销用两个独立权限点:能读吊销记录不等于能拿走别人的凭证。租户与操作者固定取会话,目标账号按 User 域可信记录
校验归属。`credentialRef` 传的是**凭证审计视图里的行 id**,不是原始 credential ID——审计视图从不暴露原文,吊销也没有
理由开这个口子;服务在 core 内部按租户+用户解析该行。`WEBAUTHN` 必须带 `credentialRef`,`TOTP` 必须不带(每个账号
只有一个认证器)。

**吊销不会因为"这是最后一个因子"而被拒绝**。自助解绑会拒绝移除唯一登录方式,那是对的——用户在做选择,可以选别的;
这里的前提不同:凭证被认为已泄露,为了用户方便而留着它,保护的是攻击者。因此吊销照常执行,响应返回
`leftWithoutFactor` 与 `enrollmentBlocked` 告诉操作者账号现在的处境:后者为 `true` 表示租户要求 MFA、账号已无因子
且宽限期已过,需要用 `/api/admin/auth/mfaEnrollmentExemptions/grant` 发一个限期注册豁免把人放回来。凭证不存在或不属于
该账号返回 404,已吊销返回 409。

认证结果审计是只读的,租户固定取会话。`identifier` 传的是**明文尝试标识**,服务端按同样方式哈希后再查询——
库里始终只有 SHA-256,而按名字统计尝试次数这类问题依然能回答。`userId` 会先用 User 域可信记录校验归属,跨租户与
不存在统一 404。从未解析出租户的失败尝试(例如租户都没选中就失败)记录时 `tenant_id` 为空,**不会**出现在任何租户
的查询结果里:它无法归属给谁,展示给某个租户管理员等于把别家的噪声和被猜测的用户名哈希摆到他面前。

MFA 注册豁免的三个动作用三个独立权限点：能撤销救援不等于能授予救援。租户与操作者固定取会话，目标账号按 User 域
可信记录校验归属，跨租户与不存在统一 404。豁免只解除"必须先注册"这一阻断，不豁免已注册账号的第二因素校验——
对已注册账号授予返回 409 `MFA_EXEMPTION_ALREADY_ENROLLED`；给自己授予返回 403
`MFA_EXEMPTION_SELF_GRANT_FORBIDDEN`；窗口不在未来或超出配置上限返回 400 `MFA_EXEMPTION_WINDOW_INVALID`；
租户策略本就不要求 MFA 的账号返回 409 `MFA_EXEMPTION_NOT_REQUIRED`。撤销一次性作用于全部生效授予，返回
`revokedCount`。

通知路由同样不接受 `tenantId` 或操作者，两者固定取当前管理员会话。`responderUserIds` 先用 User 域可信账号记录
校验归属，跨租户与不存在账号统一返回 404。`expectedVersion` 为 `0` 表示创建，其余表示当前页面看到的
`configVersion`；陈旧版本、并发保存和并发创建同一 `(notificationType, appliesTo)` 统一返回 409。规则不提供物理删除，
把 `enabled` 置为 `false` 即回到内置默认路由，历史版本仍保留在变更审计中。查询直接读库而非缓存，返回当前租户全部
规则及其版本、操作者、原因和时间。`responderRosterCode` 必须指向当前租户已启用的值班表，否则以
`AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_ROSTER_NOT_FOUND` 返回 400——这是对提交规则的校验，不是"资源不存在"。

值班表接口同样固定租户与操作者。保存是**整份替换**：请求携带该值班表的全部班次，最多 100 条，`tier` 取 1..5，
时间窗为显式 UTC 且必须 `startAt < endAt`；同一响应人、层级和时间窗重复出现会被拒绝。班次响应人先用 User 域可信
账号记录校验归属，跨租户与不存在账号统一 404。`expectedVersion` 语义与路由一致，冲突返回 409。值班表同样不提供
物理删除，`enabled=false` 即停用，被路由规则引用时该规则将按其 fallback 降级。

请求不接受 `tenantId`，始终固定为当前管理员租户。不传 `userId` 时查询租户最近事件；传入时先用
User 域可信账号记录验证归属；负责人筛选和分派也执行相同校验，跨租户与不存在账号统一返回 404。
`riskLevel` 和 `status` 不区分大小写，`overdueOnly=true` 只返回截止时间早于当前 UTC 时间且尚未关闭的事件，默认
`limit=100`、硬上限 500。响应按最后发生时间倒序，只暴露 credential SHA-256 指纹、聚合次数、
首末发生时间、风险摘要及处置审计；不返回原始 credential ID、AAGUID、断言或公钥。

状态机严格为 `OPEN → ACKNOWLEDGED → CLOSED`。还在 5 分钟 UTC 聚合窗口内的事件不能确认；关闭必须从
`ACKNOWLEDGED` 开始，`resolution` 只允许 `MITIGATED`、`FALSE_POSITIVE`、`ACCEPTED_RISK`、
`DUPLICATE`、`OTHER`。客户端必须回传最新 `workflowVersion`；版本或状态冲突返回 409，不会覆盖
其他管理员的处置。分派同样使用该版本，可用于 `OPEN` 或 `ACKNOWLEDGED` 事件，记录负责人、操作者、UTC 时间、
原因和 Web 审计，并在事务提交后发布渠道无关的领域事件。三项写入都不会自动解锁、撤销凭证或恢复会话。
通知死信查询和重放同样固定当前租户；跨租户通知与不存在通知统一 404，非 `DEAD` 或并发重放冲突返回 409。
重放原因必填，成功后重置本轮尝试次数并追加不可覆盖的领域审计记录。

外部身份管理使用显式命令，不继承通用 CRUD：

```text
POST /api/admin/auth/externalIdentity/prebind
     auth:external-identity:prebind

POST /api/admin/auth/externalIdentity/unbind
     auth:external-identity:unbind
```

两项权限通过 `@RequiresPermission` 固定并参与权限点注册；`tenantId`、操作者和 Provider code/issuer
均由当前上下文及活动 Provider 配置确定，请求端不能覆盖。操作原因必填，最终写入
`user_account_third_audit` 的 `ADMIN_BIND` / `ADMIN_UNBIND` 记录。

邀请制接口同样使用显式命令：

```text
POST /api/admin/auth/externalInvitation/create
     auth:external-invitation:create

POST /api/admin/auth/externalInvitation/revoke
     auth:external-invitation:revoke
```

创建只允许当前租户内的活动用户和 `jit_policy=INVITE_ONLY` Provider。原始 token 仅在创建响应中
返回一次，因此该响应不得写入普通 Web 审计正文；领域表只保存 token SHA-256，并记录创建/吊销
操作者、原因、有效期、使用次数以及消费 subject 哈希。

JIT 配置接口同样不使用通用 CRUD：

```text
GET  /api/admin/auth/identityProviderJitConfig/get?providerId=...
     auth:identity-provider-jit:view

POST /api/admin/auth/identityProviderJitConfig/save
     auth:identity-provider-jit:update
```

仅允许当前租户内活动且 `jit_policy=JIT_CREATE` 的 Provider。保存时强制使用当前操作者并要求原因，
默认组织/上级必须是同租户活动实体；邮箱域名、locale、时区和货币会被规范化校验。响应不包含
client secret 或 secret reference。

Provider 管理使用显式接口和稳定权限点：

```text
GET  /api/admin/auth/identityProvider/listTemplates
GET  /api/admin/auth/identityProvider/list
GET  /api/admin/auth/identityProvider/get?providerId=...
     auth:identity-provider:view
POST /api/admin/auth/identityProvider/create
     auth:identity-provider:create
POST /api/admin/auth/identityProvider/update
     auth:identity-provider:update
POST /api/admin/auth/identityProvider/setActive
     auth:identity-provider:set-active
```

Provider code 和模板创建后不可变，停用代替物理删除。写入固定当前管理员租户/操作者并要求原因；
`clientSecretRef` 只接受引用，更新时可保留、替换或明确清空，所有响应只返回
`clientSecretConfigured`。含密钥引用的请求表单不会写入普通 Web 审计正文。

Claim mapping 接口为：

```text
GET  /api/admin/auth/identityProviderClaimMapping/get?providerId=...
     auth:identity-provider-claim:view
POST /api/admin/auth/identityProviderClaimMapping/save
     auth:identity-provider-claim:update
```

字段为有序 dot-separated claim path，支持嵌套 Map/List，不接受表达式。OIDC subject 只能为 `sub`；
OAuth2 可配置 subject 路径。请求同样固定当前管理员租户/操作者并要求变更原因。

可选 OAuth2 模块的密钥诊断接口为：

```text
POST /api/admin/auth/identityProviderSecret/verify
     auth:identity-provider-secret:verify
POST /api/admin/auth/identityProviderSecret/refresh
     auth:identity-provider-secret:refresh
```

请求只含 `providerId` 和原因；引用必须由服务端从当前租户 Provider 读取。响应只给 scheme、稳定状态
和检测时间，不返回引用路径、值、长度、摘要或后端异常。`refresh` 清除本节点的 resolver/注册表状态
后实时检测；默认缓存关闭，多节点若显式启用缓存则轮换后应逐节点刷新。

### Endpoint 命名约定

两个 Controller 都遵循同一套关联关系操作 verb：

| 形态 | URI 段 | 语义 |
|------|--------|------|
| 列举目标 id | `GET /list<Target>Ids` | 例：`/role/listUserIds?roleId=...` 查角色下所有用户 id |
| 反查持有方 | `GET /list<Holder>IdsBy<Target>` | 例：`/role/listRoleIdsByUser?userId=...` 查用户持有的所有角色 |
| 批量绑定 | `POST /bind<Target>s` | 例：`/role/bindUsers` body 含 `(roleId, userIds[])` |
| 单条解绑 | `DELETE /unbind<Target>` | 例：`/role/unbindUser?roleId=...&userId=...` |
| 主资源开关 | `PUT /updateActive` | 仅 role：禁用 / 启用角色 |

`AuthRoleAdminController` 的关联面覆盖 **user / resource** 两种 target；
`AuthGroupAdminController` 的关联面覆盖 **user / role** 两种 target。

> **CRUD 主路径**继承自 `BaseCrudController`——`POST /` / `PUT /` / `DELETE /{id}` /
> `GET /{id}` / `GET /list` 等不在本 README 列出，以基类为准。

---

## 依赖关系

```
kudos-ms-auth-api-admin
    └── kudos-ms-auth-core
            ├── kudos-ms-auth-sql
            └── kudos-ms-auth-common
```

依赖 **`kudos-ability-web-springmvc`** 提供基础 Controller 能力；不直接依赖 DAO / Cache，
所有调用都委托给 `core` 暴露的 `IAuth*Service`。

---

## 与 api-public / api-internal 的配合

- **api-admin**：承载**管理 REST**（本模块）。
- **api-public**：提供**对外 Web 进程**的 `main` 与极薄扫描包，同时也挂载
  `PermittedResourceController`（当前用户视角的菜单查询）；可执行应用通常
  **同时依赖 admin + core + public**，从而在网关后对外暴露 `/api/admin/auth/**` 与
  `/api/internal/auth/permittedResource/**`。
- **api-internal**：提供**对内 Provider** 形态与 Nacos 等栈，挂载 `AuthRoleInternalController`
  实现 `IAuthRoleApi`，供其他微服务通过 Feign 调用。

部署拓扑以实际可执行模块（如 gateway、ams-*-api-web）的 `build.gradle.kts` 为准。

---

## 已知限制 / 安全考量

> 与 [auth-core 已知限制](../kudos-ms-auth-core/README.md#已知限制--后续工作) 同源——管理端
> 是这些风险最终暴露的入口：

- ❗ **大部分历史端点仍无方法级权限**：外部身份管理员命令已经使用 `@RequiresPermission`；其他
  admin 端点仍主要依赖**网关 / 外部鉴权过滤器**做访问控制。
  `bindUsers` / `bindRoles` / `bindResources` / `updateActive` 等敏感写入端点若网关挂了
  或路由错配，可被未授权调用方直接命中。
- ❗ **审计日志接入不一致**：`bind*` / `unbind*` / `updateActive` 等关键鉴权变更目前未统一
  接 `AuditLogTool`——生产合规场景需自行接入。
- ❗ **批量写入未限流**：`bindUsers(roleId, userIds[])` 在 `userIds` 极大时（如导入 10 万用户）
  会触发跨服务 `UserAccountHashCache.getUsersByIds` 雪崩——上游需做尺寸校验。

---

## 扩展建议

- 新增管理接口：优先在 **core** 完成 `IAuth*Service` 能力，再在本模块新增 `*AdminController`，
  保持 VO 仅来自 **common**。
- 若 `group` 需要对外暴露给其他微服务，先在 `auth-common.group.api` 加 `IAuthGroupApi`、
  在 `auth-client` 加 `IAuthGroupProxy`，再在 `auth-api-internal` 加 `AuthGroupInternalController`——
  admin 与 internal 路径互不重叠。
