# kudos-ms-auth-common

`auth` 原子服务共享契约层。包结构：**`io.kudos.ms.auth.common` 下先按业务模块再分 `api` / `consts` / `enums` / `vo`**；横切内容放在 **`platform`**。

## 模块

| 模块包 | 内容 |
|--------|------|
| `group` / `role` / `roleresource` / `roleuser` / `groupuser` | 各模块 `vo`、`enums` |
| `role` | `IAuthRoleApi` |
| `platform` | `IPermittedResource`（`platform.api`）等跨模块契约 |

路径示例：`io.kudos.ms.auth.common.role.api`、`io.kudos.ms.auth.common.group.vo`。
