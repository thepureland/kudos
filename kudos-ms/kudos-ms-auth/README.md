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
| [kudos-ms-auth-common](kudos-ms-auth-common/README.md) | 跨模块共享契约（`IAuthRoleApi` / `IPermittedResource` / VO / 错误码） |
| [kudos-ms-auth-sql](kudos-ms-auth-sql/README.md) | Flyway 迁移脚本（`V1.0.0.20+` 为 `auth_*` 表 DDL；前段为 `sys_*` 种子） |
| [kudos-ms-auth-core](kudos-ms-auth-core/README.md) | DAO / Service / 多级缓存 / 事件订阅 / `IAuth*Api` 实现 |
| [kudos-ms-auth-api-admin](kudos-ms-auth-api-admin/README.md) | 管理端 REST：`/api/admin/auth/role/**`、`/api/admin/auth/group/**` |
| [kudos-ms-auth-api-public](kudos-ms-auth-api-public/README.md) | 对外 Web 启动入口 + `PermittedResourceController`（当前用户视图） |
| [kudos-ms-auth-api-internal](kudos-ms-auth-api-internal/README.md) | 对内 Provider 启动入口 + `AuthRoleInternalController`（Nacos / interservice 缓存） |
| [kudos-ms-auth-client](kudos-ms-auth-client/README.md) | `IAuthRoleProxy` Feign 代理 + `AuthRoleFallback` 降级 |

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

---

## 命名与约定（跨模块）

- **原子服务名**：`SysConsts.ATOMIC_SERVICE_NAME = "auth"`——所有 Feign 服务名、缓存 namespace、
  Flyway 表前缀、日志 `service` 字段都以此为锚点。
- **领域 API**：`common` 中 `IAuth*Api` 由 `core` 中 `Auth*Api` 实现；`client` 中 `IAuth*Proxy`
  继承同一接口并通过 Feign 调用远程服务。**目前仅 `role` 对外**——`group` 域只通过 admin HTTP
  暴露，无 Feign 接口。
- **方法级 Feign 路由**：所有 `IAuth*Api` 的方法上挂 `@GetMapping("/api/internal/auth/...")`
  / `@PostMapping`，接口类型上**不**放 `@RequestMapping`；`auth-api-internal` 的 Controller 直接
  `implements IAuth*Api`，路径自动继承——签名漂移可在编译期暴露。
- **管理端 vs 内部**：`/api/admin/auth/**` 仅由 `api-admin` 承载，`/api/internal/auth/**` 仅由
  `api-internal` 与 `api-public` 内的 `PermittedResourceController` 承载；两套前缀在网关层应
  分别路由。
- **跨服务种子数据**：`auth-sql` 的 `V1.0.0.0–V1.0.0.6` 是写入 `sys_*` 表的菜单 / 字典 / 缓存
  登记 / 参数 / i18n 文案——遵循"被写入方负责 DDL，写入方负责 INSERT"。

具体类名与边界以各子模块源码为准。

## 已知限制 / 后续工作

- ❗ **生效权限算法分散** — "用户 → 直接角色 ∪ 组继承角色"的合并逻辑同时存在于
  `RoleIdsByUserIdCache.computeEffectiveRoleIds` / `ResourceIdsByUserIdCache.computeEffectiveRoleIds` /
  `ResourceIdsByTenantIdAndUsernameCache.computeEffectiveRoleIds` 三处，代码完全一样——
  后续应下沉到 base 层 util，避免三处漂移
- ❗ **只有 `role` 对外开 Feign** — `group` 域只暴露 admin HTTP，跨服务想查"用户所在组" /
  "组的权限"必须走 admin 路径或自建 group Proxy
- ❗ **组层级 path 字段未文档化** — `auth_group.path` 是字符串祖先链（如 `/g1/g2/g3`），
  上级路径继承权限要靠 path LIKE 查询；README 提到"详见 path 字段"但没有具体格式说明
- ❗ **资源 / 用户主数据跨服务一致性** — auth 只持 id 不持快照；`sys_resource` / `user_account`
  被改名 / 删除时，auth 这边的 `auth_role_resource` / `auth_role_user` 会留死引用
- ❗ **`/api/admin/auth/**` 零 `@PreAuthorize`** — 修改角色权限的接口仅靠网关守护
- ❗ **fallback 只覆盖 `IAuthRoleApi`** — auth 客户端只有 1 个 Proxy 1 个 Fallback；group / 资源
  绑定关系等其他接口若新增 Proxy，需补对应 Fallback
