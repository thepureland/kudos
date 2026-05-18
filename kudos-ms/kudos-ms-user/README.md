# kudos-ms-user

**定位**：**用户（`user`）原子服务**的 Gradle 聚合模块——承载用户账号、第三方登录、账号保护、
登录日志、组织 / 组织用户、记住登录等领域能力。

**在工程中的角色**：`SysConsts.ATOMIC_SERVICE_NAME = "user"`；管理端 HTTP + 服务间 Feign。

---

## 子模块文档索引

| 子模块 | 说明 |
|--------|------|
| [kudos-ms-user-common](kudos-ms-user-common/README.md) | 跨模块共享契约 |
| kudos-ms-user-sql | Flyway 迁移脚本（user 库表） |
| kudos-ms-user-core | DAO / Service / 缓存 / API 实现 |
| kudos-ms-user-api-admin | 管理端 REST 控制器 |
| kudos-ms-user-api-public | 对外 Web 启动入口 |
| kudos-ms-user-api-internal | 对内 Provider 启动入口 |
| kudos-ms-user-client | Feign 代理 + 降级 |

---

## 依赖关系（概念）

```
                    ┌──────────────────┐
                    │ kudos-ms-user-   │
                    │ common           │
                    └────────┬─────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   │
┌─────────────────┐  ┌─────────────────┐         │
│ kudos-ms-user-  │  │ kudos-ms-user-  │         │
│ sql             │  │ client          │         │
└────────┬────────┘  └────────┬────────┘         │
         │                    │ only common      │
         └──────────┬─────────┘                  │
                    ▼                            │
            ┌───────────────┐                    │
            │ kudos-ms-user-│                    │
            │ core          │◄───────────────────┘
            └───────┬───────┘
                    │
    ┌───────────────┼───────────────┐
    ▼               ▼               ▼
┌────────┐   ┌────────────┐   ┌────────────┐
│ api-   │   │ api-       │   │ api-       │
│ admin  │   │ public     │   │ internal   │
└────────┘   └────────────┘   └────────────┘
```

---

## 关键概念

- **账号（`sys_user`）**：用户主数据
- **第三方账号（`sys_user_third`）**：OAuth / SSO 登录的第三方身份绑定
- **账号保护**：密码策略 / 风控登录限制
- **登录日志**：每次登录的审计记录
- **组织（`sys_org`） + 组织用户（`sys_org_user`）**：树形组织结构 + 用户归属
  - 与 auth_group 配合：组织树是物理隶属，auth_group 是权限分组，二者独立
- **记住登录**：长效 token 持久化
- 与 `kudos-ms-auth` 的关系：auth 引用本服务的 `sys_user.id` 做角色 / 组的归属
