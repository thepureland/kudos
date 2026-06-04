# kudos-ms-auth-common

## 定位

鉴权（`auth`）原子服务的**共享契约层**：不包含持久化、缓存实现或 HTTP 控制器，只提供跨
`core`、`client`、各 `api-*` 模块复用的 **类型与接口**。依赖面刻意保持精简：仅 `kudos-ms-user-common`
（用 `UserAccountCacheEntry` 拼装"角色 → 用户"展示信息）与 `spring-web` 的 `compileOnly`
注解能力（让 Feign 代理识别方法级 `@GetMapping/@PostMapping`），便于其他微服务仅引用本模块即可
编译通过远程接口与 DTO。

---

## 包结构（`io.kudos.ms.auth.common`）

**第一层按业务模块划分**：

| 模块包 | 内容 |
|--------|------|
| `group` | 用户组及组-用户、组-角色关联的 `vo` / `enums`（原 `groupuser` / `grouprole` 已并入） |
| `role`  | 角色契约与 `vo` / `enums`（原 `roleresource` / `roleuser` 已并入） |
| `platform` | 跨模块横切契约 |

**第二层**按类型分子包（若模块暂无该类内容则省略目录）：

| 子包 | 内容 |
|------|------|
| `api` | 该模块对应的 `IAuth*Api` 契约（若有） |
| `enums` | 错误码枚举 `*ErrorCodeEnum` |
| `vo` | 值对象：`vo/request`、`vo/response`，及根下的 `*CacheEntry` |

路径示例：`io.kudos.ms.auth.common.role.api`、`io.kudos.ms.auth.common.group.vo.response`。

---

## 接口一览

本模块当前仅暴露一个领域 `Api` 与一个 platform 契约——**`group` 不对外开放 Feign 接口，由
`auth-api-admin` 直接调用 core Service**：

| 接口 | 包 | 职责概要 |
|------|----|----------|
| `IAuthRoleApi` | `role.api` | 角色 / 角色-用户 / 角色-资源 全部对外能力（按 id / code 查、用户↔角色绑定、角色↔资源绑定、`hasRole`、`getUserResourceIds` 等） |
| `IPermittedResource` | `platform.api` | "当前登录用户可访问资源"——返回菜单树，封装"用户 → 组 → 角色 → 资源"的完整链路 |

> **方法级路由约定**：与 `kudos-ms-sys-common` / `kudos-ms-user-common` 同模式——`IAuth*Api`
> 在**方法级**挂 `@GetMapping("/api/internal/auth/...")`，**不**在接口类型上放 `@RequestMapping`；
> 这样 Feign 代理与服务端 Controller 共享同一份路径定义，签名漂移可在编译期暴露。

---

## VO 命名约定

| 后缀 | 用途 |
|------|------|
| `*Row` | 列表/表格行（紧凑投影） |
| `*Detail` | 只读详情（含关联展开，如 `AuthRoleUserDetail` / `AuthRoleResourceDetail`） |
| `*Edit` | 表单回填 |
| `*CacheEntry` | 缓存条目（与 core 多级缓存 1:1 对齐） |
| `*FormCreate` / `*FormUpdate` | 写入入参 |
| `I*FormBase` | Create / Update 表单共享字段的 Kotlin 接口 |
| `*Query` | 列表筛选入参 |

`role/vo` 与 `group/vo` 各自包含完整的一套（Row / Detail / Edit / CacheEntry / FormCreate /
FormUpdate / Query），关系型 VO（`AuthRoleUserDetail` / `AuthRoleResourceDetail` /
`AuthGroupUserDetail`）放在主资源模块的 `vo/response` 下，**不**单独建 `roleuser` / `groupuser`
子包。

---

## 依赖关系

- **直接依赖**：`kudos-ms-user-common`（仅为引用 `UserAccountCacheEntry`，让"角色 → 用户"
  展示链路可在契约层直接表达，避免回环依赖）。
- **`compileOnly`**：`spring-web`（方法级 Feign 路由注解）。
- **被依赖方**：`kudos-ms-auth-core`、`kudos-ms-auth-client`；其他微服务也可仅依赖本模块即可
  使用 `IAuthRoleApi` 与全套 VO。

---

## 与其他子模块的关系

| 模块 | 关系 |
|------|------|
| **core** | 实现 `IAuthRoleApi` / `IPermittedResource`，并大量使用本模块的 `vo` / `enums` |
| **client** | `IAuth*Proxy : IAuth*Api`，序列化同一套 VO |
| **api-admin** | Controller 入参 / 出参直接使用本模块 VO |
| **api-internal** | Controller `implements IAuth*Api`，路径完全继承自接口方法级注解 |

---

## 扩展建议

- 新增契约：在对应业务模块下扩展 **`api` / `vo` / `enums`**；若属于横切能力（如另一种
  "当前用户视图"），再考虑放入 **`platform`**。
- 若 `group` 后续需要对外暴露 Feign 接口，按 `role` 的形式补 `group.api.IAuthGroupApi`，
  并在 `auth-client` / `auth-api-internal` 同步加 Proxy / Controller。
- 保持本模块**无数据库类型与 Spring 运行期注解**——除已存在的方法级路由 `compileOnly`
  外，不应引入 `@Component` / `@Service` / Jackson 配置等。

## 已知限制 / 后续工作

- ❗ **`group` 域无对外契约** — `IAuthGroupApi` 未定义，跨服务想读"用户所在组"必须走 admin HTTP
  或自建 Proxy；建议按 role 模式补全契约
- ❗ **`spring-web` 注解依赖 compileOnly** — 业务方把本模块用作纯 SDK（不依赖 spring-web）时，
  `IAuth*Api` 上的 `@*Mapping` 反射元数据不可见，Feign 拼路径会失败
- ❗ **依赖 `kudos-ms-user-common`** — `UserAccountCacheEntry` 用作"角色 → 用户"展示载体；
  user-common 改字段会编译期串到 auth-common 所有调用方
- ❗ **`*CacheEntry` 与 core 的多级缓存类型耦合** — common 直接持有 `*CacheEntry`，但实际产生
  这些条目的是 core 的 hash cache；core 改 entry 字段时 common 必须同步，破坏分层
- ❗ **方法级路由约定无静态检查** — 任何人在 `IAuth*Api` 类型上加 `@RequestMapping` 不会被发现，
  会让 Feign 拼路径出错
