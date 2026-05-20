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

`auth` 域 admin 当前仅有两个 Controller（与 `common` 的两个领域包对齐），每个 Controller
承担"主资源 + 它的所有关联关系"：

| 控制器 | 基础路径 | 职责概要 |
|--------|----------|----------|
| `AuthRoleAdminController` | `/api/admin/auth/role` | 角色 CRUD + 启用状态、角色 ↔ 用户、角色 ↔ 资源 |
| `AuthGroupAdminController` | `/api/admin/auth/group` | 用户组 CRUD、组 ↔ 用户、组 ↔ 角色 |

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

- ❗ **零 `@PreAuthorize`**：所有 admin 端点都依赖**网关 / 外部鉴权过滤器**做访问控制。
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
