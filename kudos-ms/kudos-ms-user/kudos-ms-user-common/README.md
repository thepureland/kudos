# kudos-ms-user-common

`user` 原子服务共享契约层。包结构：**`io.kudos.ms.user.common` 下先按业务模块再分 `api` / `consts` / `enums` / `vo`**；横切内容放在 **`platform`**。

## 模块与 `IUser*Api` 对应关系

| 模块包 | 内容 |
|--------|------|
| `user` | `IUserAccountApi`、`IUserAccountThirdApi`；账号与第三方账号 VO/枚举 |
| `protection` | `IUserAccountProtectionApi` |
| `contact` | `IUserContactWayApi` |
| `org` | `IUserOrgApi` |
| `loginremember` | `IUserLoginRememberMeApi` |
| `loglogin` | 登录日志等 VO（以 `vo` 为准） |
| `orguser` | 组织用户等 VO |

路径示例：`io.kudos.ms.user.common.user.api`、`io.kudos.ms.user.common.org.vo.request`。
