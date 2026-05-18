# kudos-ms-auth

**定位**：**鉴权（`auth`）原子服务**的 Gradle 聚合模块——承载角色 / 用户组 /
角色-资源关联 / 角色-用户关联 / 组-用户关联等领域能力，配合 `kudos-ms-sys` 的资源主数据
和 `kudos-ms-user` 的用户主数据，构成"用户 → 组 → 角色 → 资源"权限闭环。

**在工程中的角色**：`SysConsts.ATOMIC_SERVICE_NAME` 取值为 `"auth"`；管理端通过 HTTP，
其他微服务通过 Feign / `client` 调用。

---

## 子模块文档索引

| 子模块 | 说明 |
|--------|------|
| [kudos-ms-auth-common](kudos-ms-auth-common/README.md) | 跨模块共享契约 |
| kudos-ms-auth-sql | Flyway 迁移脚本（auth 库表） |
| kudos-ms-auth-core | DAO / Service / 缓存 / API 实现 |
| kudos-ms-auth-api-admin | 管理端 REST（`/api/admin/auth/...`） |
| kudos-ms-auth-api-public | 对外 Web 启动入口 |
| kudos-ms-auth-api-internal | 对内 Provider 启动入口 |
| kudos-ms-auth-client | Feign 代理 + 降级 |

---

## 依赖关系（概念）

```
                    ┌──────────────────┐
                    │ kudos-ms-auth-   │
                    │ common           │
                    └────────┬─────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   │
┌─────────────────┐  ┌─────────────────┐         │
│ kudos-ms-auth-  │  │ kudos-ms-auth-  │         │
│ sql             │  │ client          │         │
└────────┬────────┘  └────────┬────────┘         │
         │                    │ only common      │
         └──────────┬─────────┘                  │
                    ▼                            │
            ┌───────────────┐                    │
            │ kudos-ms-auth-│                    │
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

- **角色（`auth_role`）** + **角色-资源（`auth_role_resource`）** + **角色-用户（`auth_role_user`）**：
  RBAC 三件套
- **用户组（`auth_group`）** + **组-用户（`auth_group_user`）**：用户分组管理。可作为
  角色赋予的目标，让"组里所有人"自动持有该角色权限
- **生效权限** = `直接给用户的角色` ∪ `用户所在组的角色` ∪ `用户所在组的所有上级路径` 的角色
  （详见 `auth_group` 的层级 path 字段）
- 与 `kudos-ms-sys` 的关系：`sys.sys_resource` 是资源主数据，本模块只持有"哪个角色绑了哪些资源 id"
- 与 `kudos-ms-user` 的关系：`user.sys_user` 是用户主数据，本模块只持有"哪个用户属于哪些组 / 哪些角色"
