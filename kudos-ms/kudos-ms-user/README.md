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
