# kudos-ms-user

**定位**：**用户（`user`）原子服务**的 Gradle 聚合模块——承载用户账号、第三方登录、账号保护、
登录日志、组织 / 组织用户、记住登录等领域能力。

**在工程中的角色**：`SysConsts.ATOMIC_SERVICE_NAME = "user"`。

**启动入口（3 个对等的 Spring Boot 进程）**：

| 模块 | Application 类 | 暴露面 | 端点示例 |
|---|---|---|---|
| `api-public` | `UserApiWebApplication` | 终端用户态 HTTP + `UserContextWebFilter`（session → KudosContext） | `/api/public/user/passport/*` |
| `api-admin` | `UserApiAdminApplication` | 管理端 HTTP | `/api/admin/user/*` |
| `api-internal` | `UserApiProviderApplication` | 服务间 Feign provider；带 Nacos 发现 / 配置 | `IUser*Api` 接口注解路径 |

> 三者**互不依赖**——各自单独 bootRun，端口 / 注册中心 / 暴露面通过 yml 区分。
> `kudos-ms-user-client` 的 Feign proxy 默认调的是 `api-internal` 进程。

---

## 子模块文档索引

| 子模块 | 说明 |
|--------|------|
| [kudos-ms-user-common](kudos-ms-user-common/README.md) | 跨模块共享契约（VO / 枚举 / API 接口） |
| [kudos-ms-user-sql](kudos-ms-user-sql/README.md) | Flyway 迁移脚本（user 库表，`user_*` 前缀） |
| [kudos-ms-user-core](kudos-ms-user-core/README.md) | DAO / Service / 缓存 / API 实现 + 通行证（登录鉴权） |
| [kudos-ms-user-api-admin](kudos-ms-user-api-admin/README.md) | 管理端 REST 控制器（`/api/admin/user/...`） |
| [kudos-ms-user-api-public](kudos-ms-user-api-public/README.md) | 对外 Web 启动入口 |
| [kudos-ms-user-api-internal](kudos-ms-user-api-internal/README.md) | 对内 Provider 启动入口（服务间 Feign 调用面） |
| [kudos-ms-user-client](kudos-ms-user-client/README.md) | Feign 代理 + 降级 |

---

## 依赖关系（概念）

```
                              ┌──────────────┐
                              │ user-common  │  (契约 / VO / 枚举)
                              └──────┬───────┘
                                     │
                ┌────────────────────┼──────────────────────┐
                │                    │                      │
                ▼                    ▼                      ▼
        ┌──────────────┐     ┌──────────────┐       ┌──────────────┐
        │ user-sql     │     │ user-client  │       │   user-core  │
        │ (空依赖,     │     │ (only common │       │ deps:        │
        │  纯 SQL 资源)│     │  + feign)    │       │  user-common │
        └──────┬───────┘     └──────────────┘       │  user-sql    │
               │                                    │  sys-core    │
               └───────── 被 core 依赖 ──────────►  │  …ktorm/flyway│
                                                    │  …caffeine/redis
                                                    └──────┬───────┘
                                                           │
                                ┌──────────────────────────┼──────────────────────────┐
                                ▼                          ▼                          ▼
                       ┌────────────────┐         ┌────────────────┐         ┌────────────────┐
                       │ user-api-admin │         │ user-api-public│         │ user-api-internal│
                       │ + own boot main│         │ + own boot main│         │ + own boot main │
                       └────────────────┘         └────────────────┘         └────────────────┘
                              （3 个独立进程，互不依赖；都各自只 `api(user-core)`）
```

> 注意：
> - `user-sql` **不依赖** `user-common`（纯空依赖资源模块）
> - `user-client` **只**依赖 `user-common`（保持 client 端轻量，不拉持久层）
> - 三个 `api-*` 模块**不互相依赖**——是 3 个对等的独立 boot 进程
> - **`kudos-ms-user-api-admin` 当前没有任何上游依赖**（除了 settings.gradle.kts 注册）

---

## 关键概念

- **账号（`user_account`）**：用户主数据（含 `login_password` / `security_password` / `authentication_key` / freeze* 等敏感字段）
- **第三方账号（`user_account_third`）**：OAuth / SSO 登录的第三方身份绑定；按 `(user_id, provider_code)` 唯一
- **账号保护（`user_account_protection`）**：登录错误次数 / 冻结策略等保护字段
- **联系方式（`user_contact_way`）**：手机 / 邮箱，可多条
- **登录日志（`user_log_login`）**：每次登录的审计记录
- **记住登录（`user_login_remember_me`）**：长效 token 持久化（区别于 passport 的瞬时鉴权）
- **通行证（passport）**：登录鉴权状态机——`PassportService` 跑 user_account 校验 + 冻结判定 + OTP 校验
- **组织（`user_org`） + 组织用户（`user_org_user`）**：树形组织结构 + 用户归属
  - `user_org.path` 列做祖先链，方便查"某组织及全部后代"
  - 与 `auth_group` 配合：组织树是物理隶属，`auth_group` 是权限分组，二者独立

## 与其他服务的关系

- **`kudos-ms-auth`**：auth 引用本服务的 `user_account.id` 做角色 / 组的归属
- **`kudos-ms-sys`**：user-core **同进程**依赖 `kudos-ms-sys-core`（不是 Feign 远调），
  拿子系统 / 租户元数据（active / accountTypeDictCode 等枚举）

## 已知限制 / 后续工作

- ❗ **user-core 直接依赖 sys-core 是耦合反模式** — 强同进程 jar 依赖让 user / sys 必须共部署，
  违反"原子服务独立部署"目标；后续应迁移到 `sys-client` Feign 调用
- ❗ **三个 `api-*` 模块不共享 controller / filter** — `UserContextWebFilter` 只在 `api-public`，
  admin / internal 默认无 session→KudosContext 转换；切 admin 进程跑用户 API 会拿不到 context
- ❗ **`api-admin` 当前是孤岛模块** — 未被任何 build 拉入，仅靠 `settings.gradle.kts` 注册；
  CI 上能编译但生产部署需要业务方主动决定是否启用
- ❗ **passport 内部状态机文档化不足** — 登录鉴权（密码 / OTP / 冻结判定 / 错误次数）的实际流程
  分散在 `PassportService` + `UserAccountProtectionService`，README 仅提及"状态机"未图示
- ❗ **组织树 path 列没有触发器维护** — `user_org.path` 为祖先链字符串，靠 service 层手动维护；
  绕过 service 直接 DML 改 parent 不会同步 path，会导致树查询错乱

## 改进建议（自动分析 2026-06-11）

> 本次自动审查已直接修复两处低风险问题：
> 1. `kudos-ms-user-core/src/io/kudos/ms/user/core/org/cache/UserOrgHashCache.kt` —
>    `getOrgsByTenantId` 的 `@HashCacheableBySecondary(filterExpressions = ["#tenantId", "#active"])`
>    引用了不存在的 `#active` 参数，SpEL 求值为 null 导致该方法的缓存**每次调用都被静默绕过**，已改为 `["#tenantId"]`。
> 2. `kudos-ms-user-api-public/.../PassportPublicController.kt` — 公网 `/qrCode` 端点的 `size`
>    参数无上限（单请求可申请超大 BufferedImage 造成内存 DoS），已 clamp 到 [64, 1024]。
>
> 以下为**不宜直接修改**（涉及 public API 契约 / 需设计决策）的发现，按维度归类：

### 安全性（最高优先级）

- ✅ 已修复（2026-06-11）**密码哈希 / TOTP 秘钥随响应 VO 全链路泄露**：采用"出口脱敏"方案——新增
  `user-common` 的 `UserAccountCredentialsErasure.kt`（`eraseCredentials()` 拷贝并置空
  `loginPassword` / `securityPassword` / `authenticationKey` / `sessionKey`），在全部服务边界出口统一调用：
  ① api-internal `UserAccountInternalController.getUserById/getUsersByIds` 与
  `UserOrgInternalController.getOrgUsers/getOrgAdmins`；
  ② api-admin `UserOrgAdminController.getOrgUsers/getOrgAdmins`；
  ③ api-admin `UserAccountAdminController` 覆写 `pagingSearch/getDetail/getEdit`。
  登录校验仍走 user-core 进程内缓存 / DAO 直查（未脱敏专用通道），不受影响。VO 类结构未变更（保持跨服务契约兼容），
  彻底拆分"鉴权专用 VO 与公开资料 VO"仍为后续待办。
- ✅ 已修复（2026-06-11）**登录失败无锁定阈值**：实现简化版"阈值 + 时间窗口"锁定——
  `PassportService` 在失败累计达 `kudos.ms.user.passport.login-lock.max-error-times`（默认 5，≤0 关闭）后，
  复用账号冻结机制以专用冻结类型 `autoLoginLock` 自动冻结
  `kudos.ms.user.passport.login-lock.lock-minutes`（默认 30，≤0 表示锁定至人工解冻）分钟，
  期间一律返回 `LOCKED`（含正确密码），登录成功重置计数；`user_account_protection`
  保护策略表（按用户/租户差异化阈值）的接入仍为待办。
- ❗ **登录审计未落库**：`user_log_login` 表 + `UserLogLoginService` 完整存在，但 `PassportService.login/logout`
  不写任何登录日志记录——安全审计（异地登录、爆破检测）无数据来源。

### API 契约 / 分层

- admin / internal / public 三层划分总体清晰合理（public 仅 passport、internal 仅缓存条目读、admin 全量 CRUD）。
  ~~internal 面返回的 `UserAccountCacheEntry` 让"内部读接口"事实上拥有了"读密码哈希"的权限~~——
  已于 2026-06-11 通过出口脱敏修复（见上「安全性」第 1 条）。
- **批量 / 列表接口无分页与数量上限**：`IUserAccountApi.getUserIds(tenantId)`、`IUserAccountService.getUsersByTenantId/getUsersByOrgId`、
  `UserOrgAdminController.getOrgUsers` 等均为全量返回，租户用户量大时单响应可达数十 MB。建议补 `limit/offset` 或复用 `ListSearchPayload` 分页。

### 测试覆盖

- `kudos-ms-user-core` 覆盖良好（25+ 测试类，passport 状态机全枚举）；但 `api-admin` / `api-public` / `api-internal` /
  `client` 四个模块 **0 个测试**：`PassportPublicController` 的 session 写入与 logout 失效、`UserContextWebFilter` 的
  Order 语义、各 Fallback 的安全默认值均无回归保护。建议至少为 public 控制器补 MockMvc 测试。
