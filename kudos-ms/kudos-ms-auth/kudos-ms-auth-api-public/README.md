# kudos-ms-auth-api-public

## 定位

认证与鉴权（`auth`）原子服务的**对外 Web 进程**入口与自动配置。本模块对外暴露当前用户
权限视图、统一认证事务和租户可用身份提供方目录。管理端 REST 仍由
`kudos-ms-auth-api-admin` 承担，本模块通过组合依赖一并装载。

---

## 入口与自动配置

| 类型 | 类 | 说明 |
|------|----|------|
| 启动类 | `AuthApiPublicApplication` | `@EnableKudos`，`main` 启动 Spring Boot |
| 自动配置 | `AuthApiPublicAutoConfiguration` | `@ComponentScan("io.kudos.ms.auth.api.public")`，`IComponentInitializer` 组件名 **`kudos-ms-auth-api-public`** |

`io.kudos.ms.auth.api.public` 下的主要包：

- `init/` —— `Application` + `AutoConfiguration`
- `controller/platform/PermittedResourceController` —— `implements IPermittedResource`，
  路径完全继承自接口方法级注解（典型为 `/api/internal/auth/permittedResource/...`）
- `controller/authentication/AuthenticationTransactionPublicController` —— 登录及 Step-up 统一认证事务入口
- `controller/authentication/WebAuthnAuthenticationPublicController` —— 事务绑定的 Passkey assertion 发起入口
- `controller/authentication/WebAuthnEnrollmentPublicController` —— 当前 Session 用户的 Passkey 注册与凭证管理
- `controller/authentication/AuthenticationSessionPublicController` —— 查询/撤销当前及多设备逻辑会话
- `controller/authentication/AuthenticationLogoutPublicController` —— 全部设备、Refresh Token 与 Access Token 统一失效
- `controller/authentication/RecoveryCodePublicController` —— 当前用户恢复码状态、一次性生成/轮换及撤销
- `controller/authentication/MfaPolicyPublicController` —— 当前用户不含密钥的有效 MFA 策略决策
- `filter/AuthenticationSessionWebFilter` —— 校验会话注册表、续期闲置窗口，填充当前主体并向声明式 ACR 校验器暴露本次请求的权威逻辑会话
- `controller/provider/IdentityProviderPublicController` —— 按租户返回无密钥 Provider 目录

---

## 控制器一览

| 控制器 | 实现接口 | 职责 |
|--------|----------|------|
| `PermittedResourceController` | `IPermittedResource`（auth-common.platform.api） | `getMenusForCurrentUser` —— 拿当前请求上下文里的 `userId`，按"用户 → 组 → 角色 → 资源"链路拼出菜单树（`List<MenuTreeNode>`） |
| `AuthenticationTransactionPublicController` | `IAuthenticationTransactionService` | 创建、查询、推进和取消登录/Step-up 事务；请求来源元数据由服务端覆盖；密码登录及第三方 MFA action 完成后才创建逻辑会话，Step-up CAS 提升原会话并轮换 Session ID |
| `WebAuthnAuthenticationPublicController` | `WebAuthnAssertionService` | `POST /api/public/auth/authentication/transactions/{id}/webauthn/assertion`；只为正等待 `VERIFY_PASSKEY` 的 Passwordless、Step-up 或第三方 MFA 事务创建 assertion，并把 ceremony 固定到事务与服务端主体 |
| `WebAuthnEnrollmentPublicController` | `WebAuthnRegistrationService` / `IWebAuthnCredentialService` | `GET /api/public/auth/mfa/webauthn/credentials`、`POST .../registrations`、`POST .../registrations/{id}/finish`、`PATCH/DELETE .../credentials/{id}`；只操作当前受管 Session 主体，并要求近期 password-or-stronger 认证 |
| `AuthenticationSessionPublicController` | `IAuthenticationSessionService` | `GET /api/public/auth/sessions`、`GET/DELETE .../current`、`DELETE .../{id}`；仅操作当前租户用户拥有的逻辑会话，不暴露容器 Session ID |
| `TotpEnrollmentPublicController` | `ITotpEnrollmentService` | `GET/POST/DELETE /api/public/auth/mfa/totp/**`；近期认证后两阶段注册、确认和撤销当前用户的 TOTP，不接受请求中的用户或租户 |
| `RecoveryCodePublicController` | `IRecoveryCodeService` | `GET/POST/DELETE /api/public/auth/mfa/recovery-codes`；固定当前会话主体，原始恢复码仅在生成响应返回一次 |
| `MfaPolicyPublicController` | `ITenantMfaPolicyService` | `GET /api/public/auth/mfa/policy`；固定当前会话主体，返回要求、注册状态、宽限期和允许因子 |
| `AuthenticationLogoutPublicController` | `IAuthenticationLifecycleService` | `POST /api/public/auth/logout-all`；递增 token epoch、撤销全部 Refresh family/逻辑会话并失效当前 HttpSession |
| `IdentityProviderPublicController` | `IIdentityProviderCatalogService` | 返回租户启用的第三方登录入口，不泄露 `client_secret_ref` |
| `ExternalIdentityBindingController`（由 `provider-oauth2` 装载） | `IUserAccountThirdService` | 当前 Session 用户查询、近期重新认证后显式绑定和软解绑外部身份；不接受可信 `userId` |

> 之所以放在 `api-public` 而非 `api-internal`：**当前用户视图**是 Web 进程内的请求级 API
> （要从 `RequestContext` 取 `userId` / `tenantId`），不是无状态的 RPC——放在 internal 用 Feign
> 调会丢上下文。

Step-up 仅允许已有受管逻辑会话发起：

```text
POST /api/public/auth/authentication/transactions/step-up
Content-Type: application/json

{
  "requiredAcr": "urn:kudos:acr:mfa",
  "requestedMethod": "password"
}
```

用户、租户和源逻辑 Session ID 均从服务端 Session 取得。完成后的认证主体必须仍是发起人，结果 ACR
必须达到请求等级；否则事务 fail-closed。成功只提升原逻辑会话并合并 `amr`，不会借 Step-up 延长其
绝对生命周期。

Passkey Passwordless 登录先创建 `requestedMethod=passkey` 的普通登录事务，再调用事务专用 assertion
入口取得浏览器 options，最后把浏览器响应作为 `VERIFY_PASSKEY` action 的 attributes 提交。ceremony
固定绑定 transaction id；无用户名登录只允许 discoverable credential 决定主体。成功后与其他登录方法
共用逻辑会话签发、HttpSession ID 旋转和主体绑定。Passkey Step-up 使用同一流程，但 assertion 还会固定
到源会话用户。

密码登录若被策略要求补充 WebAuthn，正确密码 action 只把服务端确认的主体、`password` AMR/ACR 写入
事务并返回 `VERIFY_PASSKEY`，不会保存密码或提前创建 Session。客户端随后调用同一 assertion begin 与
`VERIFY_PASSKEY` action；只有绑定该 transaction 和主体的 assertion 成功后才签发 Session。

登录后的 Passkey 自助登记使用独立入口：

```text
GET    /api/public/auth/mfa/webauthn/credentials
POST   /api/public/auth/mfa/webauthn/registrations
POST   /api/public/auth/mfa/webauthn/registrations/{ceremonyId}/finish
PATCH  /api/public/auth/mfa/webauthn/credentials/{credentialId}
DELETE /api/public/auth/mfa/webauthn/credentials/{credentialId}
```

租户、用户只取当前受管 Session，finish 的 ceremony id 只取路径；请求体仅携带浏览器 credential response
和显示名。所有操作默认要求 300 秒内的 password-or-stronger 认证，且 request attribute 中的权威逻辑
会话必须与 HttpSession principal 一致。查询只返回公开摘要，不返回 COSE 公钥；PATCH 请求体只包含
`displayName`，重命名仅更新展示元数据，不撤销会话。注册或撤销成功会进入凭证生命周期失效链路。

第三方登录若被租户策略要求补充 MFA，OAuth2/OIDC 回调会把原事务标记为
`CHALLENGE_REQUIRED`，并返回 transaction id 与挑战类型，不创建登录 Session。客户端随后调用：

```text
POST /api/public/auth/authentication/transactions/{id}/actions/VERIFY_TOTP
POST /api/public/auth/authentication/transactions/{id}/actions/VERIFY_RECOVERY_CODE
POST /api/public/auth/authentication/transactions/{id}/webauthn/assertion
POST /api/public/auth/authentication/transactions/{id}/actions/VERIFY_PASSKEY
```

只有 action 验证成功且事务变为 `COMPLETED` 后，本控制器才签发逻辑会话、旋转 HttpSession ID 并
绑定当前主体。恢复码 action 是否出现由租户策略开关和实际可用码共同决定；Passkey action 只在策略
允许 WebAuthn、用户已有活动 credential 且可选 provider 已启用时出现，其 assertion 固定到回调已经确认的
本地主体和原 transaction id。

---

## 启动

```bash
java -jar auth-api-public-<version>.jar
```

或本仓库本地运行：

```bash
./gradlew :kudos-ms:kudos-ms-auth:kudos-ms-auth-api-public:bootRun
```

---

## Gradle 依赖

```kotlin
dependencies {
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-core"))
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-provider-oauth2"))
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-provider-webauthn"))
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-token-jwt"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
    testImplementation(project(":kudos-test:kudos-test-container"))
}
```

依赖 **`core`** 间接拉入 `auth-sql` / `auth-common` 与所有缓存 / DAO / Service Bean；
**`provider-oauth2`** 提供标准 OIDC/OAuth2 浏览器登录、安全回调和动态 Provider；
**`provider-webauthn`** 默认关闭，启用后提供事务绑定的 Passkey assertion/Passwordless/Step-up 以及
近期再认证保护的自助注册、凭证查询、重命名与撤销；
**`token-jwt`** 默认关闭，启用并提供 `JwtEncoder`/`JwtDecoder` 后增加 token pair、refresh、revoke
端点与 Bearer Filter；
**`web-springmvc`** 拉入 Filter / Interceptor / 异常映射 / 序列化等 Web 栈。
**不强制依赖 `api-admin`**——若需要同时对外暴露 `/api/admin/auth/**`，由上层聚合
（如 `*-api-web`）一并引入 `api-admin`。

浏览器会话默认使用 30 分钟闲置过期和 12 小时绝对过期，可通过
`kudos.ms.auth.session.idle-timeout-seconds` 与 `absolute-timeout-seconds` 调整。Redis 可用时会话
注册表自动跨节点共享；无 Redis 的回退模式要求会话请求保持在同一进程。
用户撤销远端会话后，权威注册表立即失效；远端浏览器下一次请求由 Filter 拒绝并清理其
HttpSession。Spring Session 存储中的远端记录尚不会在撤销瞬间主动物理删除。
“退出全部设备”还会递增 token epoch，因此并非只清浏览器 Session：已有 Access Token 立即变旧，
Refresh Token 也不能继续轮换。

---

## 依赖关系（概念）

```
kudos-ms-auth-api-public
    ├── kudos-ms-auth-core
    │       ├── kudos-ms-auth-sql
    │       └── kudos-ms-auth-common
    └── kudos-ability-web-springmvc
```

---

## 与 api-admin / api-internal 的对比

| 维度 | api-public | api-admin | api-internal |
|------|------------|-----------|--------------|
| 主类 | `AuthApiPublicApplication` | `AuthApiAdminApplication` | `AuthApiInternalApplication` |
| 是否含 Controller | 是（权限视图、认证事务、Provider 目录） | 是（`AuthRoleAdminController` / `AuthGroupAdminController`） | 是（`AuthRoleInternalController`，1 个） |
| 额外分布式能力 | 无（仅 core + MVC） | 无 | Nacos + interservice 缓存 |
| 暴露面 | 当前用户视图 + 管理 HTTP（由 admin 组合时） | 管理 REST `/api/admin/auth/**` | 服务间 Feign `/api/internal/auth/**` |
| 适用场景 | 控制台前端 / 网关后用户路径 | 控制台前端 | 微服务间调用 |

---

## 扩展建议

- 若需"纯 Web 网关入口"专用配置（如自定义 Filter），可放在 `io.kudos.ms.auth.api.public`
  下并由本模块扫描；**业务管理接口仍建议放在 api-admin**，便于权限与路由前缀统一。
- 新增"当前用户视图"型接口（同样需要 RequestContext）：在 `controller/platform/` 下扩展，
  在 `auth-common.platform.api` 加对应接口契约，保持 controller 类只做"取上下文 + 委托
  `core` 实现"。

## 已知限制 / 后续工作

- ❗ **路径 `/api/internal/auth/permittedResource/...` 名义上是 internal 但实际在 public 暴露** —
  这是有意为之（需取 RequestContext），但与"internal 仅集群内可达"约定冲突；运维容易误以为
  本路径不需外网鉴权
- ❗ **`getMenusForCurrentUser` 缺少 `@PreAuthorize`** — 只检查"有 userId"，不校验"会话有效"；
  绕过 session filter 直接构造 RequestContext 可越权拿菜单
- ❗ **菜单树没有缓存** — 每次请求都跑"用户 → 组 → 角色 → 资源"完整链路；高并发场景应在
  `IPermittedResource` 实现侧加 user 级缓存（与 `ResourceIdsByUserIdCache` 配合）
- ❗ **未提供 admin 视角下的"看他人菜单"** — admin 排查权限问题时无法通过 API 看到指定用户的
  菜单视图；需要业务方自建 admin 接口走 service 层
- ❗ **`AuthApiPublicApplication` 单独运行无 admin 路径** — 管理用户角色等仍需 admin 进程；
  本模块只提供面向最终用户的权限视图、认证事务与 Provider 发现端点
- ✅ **邀请制发起已由可选 OAuth2 Provider 模块接入** — `INVITE_ONLY` 可在 authorize 请求携带
  一次性邀请 token，并将无密 invitation id 固定到认证事务；管理员创建/吊销仍属于 api-admin。
  JIT 与非邀请邮箱匹配尚未进入公共合同；link/unlink 仍要求与当前 user/tenant/session 一致且在
  有效窗口内完成的认证事务。
