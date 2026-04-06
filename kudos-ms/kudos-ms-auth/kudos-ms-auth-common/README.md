# kudos-ms-auth-common

`auth` 原子服务共享契约层。包结构：**`io.kudos.ms.auth.common` 下先按业务模块再分 `api` / `consts` / `enums` / `vo`**；横切内容放在 **`platform`**。

## 模块

| 模块包 | 内容 |
|--------|------|
| `group` | 用户组及组-用户关联等 `vo`、`enums`（原 `groupuser` 已并入） |
| `role` | `IAuthRoleApi`；角色及角色-资源、角色-用户关联等 `vo`、`enums`（原 `roleresource` / `roleuser` 已并入） |
| `platform` | `IPermittedResource`（`platform.api`）等跨模块契约 |

路径示例：`io.kudos.ms.auth.common.role.api`、`io.kudos.ms.auth.common.group.vo`。
