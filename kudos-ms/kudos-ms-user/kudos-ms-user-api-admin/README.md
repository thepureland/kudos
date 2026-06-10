# kudos-ms-user-api-admin

User 服务的**管理端 REST 控制器层**。路径前缀 `/api/admin/user/...`。

## 控制器清单

| 控制器 | 路由前缀 | 委托 Service |
|---|---|---|
| `UserAccountAdminController` | `/api/admin/user/account` | `IUserAccountService`——账号 CRUD、改密、冻结/解冻、激活态、内置位 |
| `UserAccountThirdAdminController` | `/api/admin/user/accountThird` | `IUserAccountThirdService`——第三方账号绑定/解绑（OAuth / SSO） |
| `UserAccountProtectionAdminController` | `/api/admin/user/accountProtection` | `IUserAccountProtectionService`——错误次数 / 冻结策略查询 |
| `UserContactWayAdminController` | `/api/admin/user/contactWay` | `IUserContactWayService`——联系方式（手机 / 邮箱）多条管理 |
| `UserOrgAdminController` | `/api/admin/user/org` | `IUserOrgService`——组织树 + 组织 ↔ 用户绑定 |
| `UserLoginRememberMeAdminController` | `/api/admin/user/rememberMe` | `IUserLoginRememberMeService`——记住登录 token 失效 / 列表 |

> 通行证（`IPassportApi` 登录 / 登出 / 校验 / 改密）**没有 Admin 控制器**——属于用户态行为，
> 通过 `kudos-ms-user-client` 的 `IPassportProxy` 暴露给 auth 服务等内部调用方。

## 设计约束

- 控制器只调用 `user-core` 的 `IUser*Service`，**不直接接触 DAO** / 缓存层
- 返回类型直接用 `user-common` 的 VO（`*Detail` / `*Row` / `*Edit`），不在 controller 层
  做额外组装
- 入参用 `user-common` 的 `*FormCreate` / `*FormUpdate` / `*Query`，由 `kudos-ability-web-springmvc`
  的统一参数解析 / 校验器处理

## 依赖

- `kudos-ms-user-core`
- `kudos-ability-web-springmvc`（Spring MVC + 统一异常 / 参数校验 / 响应包装）

## 部署形态

本模块**自身就是一个 Spring Boot 应用**——`UserApiAdminApplication` 是 `@EnableKudos`
main 类，可独立 `bootRun`，专门跑管理端 HTTP（与 `api-public` / `api-internal` 是三套
对等的启动入口，各起一个进程）。

```bash
./gradlew :kudos-ms:kudos-ms-user:kudos-ms-user-api-admin:bootRun
```

`UserApiAdminAutoConfiguration` 负责把本模块的 controller 扫描注入 Spring 上下文，
配合 `user-core` 的 `UserAutoConfiguration` 完成装配。

> 当前 build 中**没有其他模块依赖 `kudos-ms-user-api-admin`**——它既不向 `api-public`
> 也不向 `api-internal` 提供 controller bean。如需把管理 API 与用户 / 内部 API
> 合并到同一进程，应在对应模块的 `build.gradle.kts` 显式 `api(project(...))` 拉入。

## 已知限制 / 后续工作

- ❗ **控制器无 `@PreAuthorize`** — 路由 `/api/admin/user/...` 鉴权完全依赖上游网关 +
  `kudos-ms-auth` 的 `UserContextWebFilter`；网关漏配会让账户 CRUD 等敏感操作直接暴露
- ❗ **`Passport` 没有 admin 路径** — 改他人密码、强制下线等"特权登录态操作"目前需直接调
  `IUserAccountService.changePassword`，绕过 `IPassportApi` 的状态机；缺少专用的 admin
  passport API，操作语义不够明确
- ❗ **批量操作缺幂等控制** — `batchDelete` / `batchActivate` 等没有 idempotencyKey；
  重试可能导致重复审计 / 缓存抖动
- ❗ **没有审计字段过滤** — controller 返回的 `*Detail` 直接暴露 `login_password` / `authentication_key`
  之类哈希字段（虽然 service 层应已脱敏，但缺少 controller 层守卫；脱敏遗漏会泄露敏感数据）
- ❗ **未与 `api-public` / `api-internal` 联合部署** — 当前是独立进程；若运维想"三合一"部署
  减少端口数，需要业务方在 build.gradle.kts 主动聚合并手动协调 controller 路径冲突

## 改进建议（自动分析 2026-06-11）

- ❗ **明文新密码走 `@RequestParam`**（`controller/account/UserAccountAdminController.kt#resetPassword/resetSecurityPassword`）：
  `newPassword` 以请求参数传递，若客户端拼到 query string，明文密码会进入网关 / 容器访问日志与浏览器历史。
  建议改为 `@RequestBody` VO（属 API 契约变更，未直接修改）。
- ❗ **`listByUserId` 直接返回 PO**（`controller/account/UserAccountThirdAdminController.kt`）：
  返回 ktorm 实体 `UserAccountThird` 而非 `user-common` 的 `UserAccountThirdRow` VO，破坏"controller 只出 VO"
  的分层约定，且实体字段变更会直接改变线上的 JSON 契约。
- ✅ 已修复（2026-06-11）**`getOrgUsers` / `getOrgAdmins` 泄露凭据字段**（`controller/org/UserOrgAdminController.kt`）：
  两端点返回前统一调用 `user-common` 新增的 `eraseCredentials()` 脱敏拷贝（置空 `loginPassword` /
  `securityPassword` / `authenticationKey` / `sessionKey`）；同时 `UserAccountAdminController`
  覆写 `pagingSearch / getDetail / getEdit` 对 CRUD 读出口做同样脱敏，管理端不再可见任何凭据字段。
- **`cleanExpiredFreezes` 端点无防护说明**：作为"应急 / 调试通道"的写操作端点，建议至少在网关侧
  限制为运维角色，或加显式确认参数防止误触。
