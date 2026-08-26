# kudos-ms-user

**定位**：**用户（`user`）原子服务**的 Gradle 聚合模块——承载用户账号、第三方登录、账号保护、
登录日志、组织 / 组织用户、记住登录等领域能力。

**在工程中的角色**：`SysConsts.ATOMIC_SERVICE_NAME = "user"`。

**启动入口（3 个对等的 Spring Boot 进程）**：

| 模块 | Application 类 | 暴露面 | 端点示例 |
|---|---|---|---|
| `api-public` | `UserApiPublicApplication` | 终端用户态 HTTP + `UserContextWebFilter`（session → KudosContext） | `/api/public/user/passport/*` |
| `api-admin` | `UserApiAdminApplication` | 管理端 HTTP | `/api/admin/user/*` |
| `api-internal` | `UserApiInternalApplication` | 服务间 Feign provider；带 Nacos 发现 / 配置 | `IUser*Api` 接口注解路径 |

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
  - `authentication_key` 使用透明 AES-GCM 加密，兼容历史明文读取；加密列的 Ktorm 参数日志会被脱敏
- **第三方账号（`user_account_third`）**：OAuth / SSO 外部身份绑定；以租户 Provider 实例的
  `issuer + subject` 定位身份，并限制一个用户在同一 Provider 实例只有一个绑定
- **第三方账号审计（`user_account_third_audit`）**：绑定/解绑成功与拒绝的追加式日志，外部
  `subject` 仅保存 SHA-256
- **外部账号 JIT 预配**：原子创建无本地密码的 `user_account` 与首条外部身份绑定；并发失败
  整体回滚，不遗留孤儿账号，首次绑定审计动作是 `JIT_BIND`；Provider 默认组织会同时建立
  `user_org_user` 成员关系，其他经校验的账号默认值一并写入
- **账号保护（`user_account_protection`）**：登录错误次数 / 冻结策略等保护字段
- **联系方式（`user_contact_way`）**：手机 / 邮箱，可多条
- **登录日志（`user_log_login`）**：每次登录的审计记录
- **记住登录（`user_login_remember_me`）**：长效 token 持久化（区别于 passport 的瞬时鉴权）
- **通行证（passport）**：`PassportService` 执行请求限流、账号/冻结判定、密码/TOTP/恢复码独立失败
  窗口及登录审计；恢复码通过 User 定义、Auth 实现的消费端口接入，不把恢复码存入 User 域
- **本地密码策略**：账号创建、通用更新、管理员重置和用户自助改密共用 `IPasswordPolicy`；默认采用
  长度优先策略（12～64 个 Unicode 字符），拒绝常见密码、单字符重复和包含用户名的密码，大小写、
  数字及特殊字符组合规则可选启用。BCrypt 输入统一限制为最多 72 个 UTF-8 字节，避免静默截断；
  与 Auth Core 同进程时还会拒绝并归档最近 5 个登录/安全历史密码哈希
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
- ✅ **passport 防爆破已分层** — 服务端 IP 与租户+用户名承担请求频率限制，密码/TOTP 分桶承担
  因素失败窗口，旧 `login_error_times` + `autoLoginLock` 继续承担账号级密码连续失败冻结；详细流程及
  配置见 `docs/authentication-and-federated-login-design.md` 的“登录防护”章节
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
- ✅ **Passport 登录审计已接通**：成功与终态失败由 `PassportService` 投喂 `user_log_login`，
  IP、终端、浏览器、OS 和 User-Agent 由公共控制器从服务端请求提取；更完整的 auth 域风险事件、
  不存在用户标识哈希和集中检索仍属后续工作。
- ✅ **认证尝试限流已接通**：按服务端 IP、租户+用户名限制请求频率，密码和 TOTP 失败独立计数；
  多节点使用 Redis Lua 原子检查/消费，无 Redis 回退单机内存，存储故障默认 fail-closed。响应新增
  `RATE_LIMITED` 和 `retryAfterSeconds`，旧状态及账号自动锁定语义不变。
- ✅ **公网用户名枚举防护已接通**：公共 Passport 登录将用户不存在、密码错误、停用、自动锁定及
  管理员冻结统一返回 `INVALID_CREDENTIALS`，不暴露错误次数或冻结原因；核心审计保留真实状态，
  快速拒绝分支执行 BCrypt 虚拟校验以缓解明显的时序差异。
- ✅ **认证生命周期事件已接通**：登录密码/安全密码变更、账号停用、冻结和 TOTP 认证器变更发布专用
  `UserAuthenticationInvalidated`，账号删除沿用带租户快照的单删/批删事件；不复用同时覆盖登录时间、
  失败次数的通用更新事件。Auth 同进程部署会在提交后递增 token epoch，并撤销全部 Refresh family
  与逻辑会话；独立进程部署必须用可靠消息/Outbox 转发这些事件。
- ✅ **TOTP 正式密钥已加密落库**：`authentication_key` 使用迁移友好的 AES-GCM 字段，历史明文可继续
  读取，新写入只保存随机 IV 的认证密文；Flyway 已将列扩为 512 字符。Auth 自助注册只通过
  `activateVerifiedAuthKey` 写入验证码已确认的密钥，普通账号更新仍不能覆盖该字段。
- ✅ **密码持久化边界已收口**：普通创建/编辑表单中的非空密码必须先通过策略并统一写为
  `{bcrypt}` 版本化编码；历史无前缀 BCrypt 可继续登录并在完整认证成功后以 CAS 透明升级。通用
  更新不能跨租户移动账号，也不能直接覆盖 TOTP secret 或 session key。JIT 的空登录密码继续表示
  “未登记本地密码”；仅可信的内部 `UserAccount` 迁移对象允许原样携带受支持编码，避免二次哈希。

### API 契约 / 分层

- admin / internal / public 三层划分总体清晰合理（public 仅 passport、internal 仅缓存条目读、admin 全量 CRUD）。
  ~~internal 面返回的 `UserAccountCacheEntry` 让"内部读接口"事实上拥有了"读密码哈希"的权限~~——
  已于 2026-06-11 通过出口脱敏修复（见上「安全性」第 1 条）。
- **批量 / 列表接口无分页与数量上限**：`IUserAccountApi.getUserIds(tenantId)`、`IUserAccountService.getUsersByTenantId/getUsersByOrgId`、
  `UserOrgAdminController.getOrgUsers` 等均为全量返回，租户用户量大时单响应可达数十 MB。建议补 `limit/offset` 或复用 `ListSearchPayload` 分页。

### 测试覆盖

- `kudos-ms-user-core` 覆盖 cache / dao / service / passport；`PassportPublicController` 已有纯单元测试覆盖
  session 写入与轮换、logout 失效、当前主体归属校验以及公网登录错误收敛。其余 public/internal/client
  边界仍应继续补充契约与安全默认值测试。
