# kudos-ms-user-common

`user` 原子服务共享契约层。包结构：**`io.kudos.ms.user.common` 下先按业务模块再分 `api` / `consts` / `enums` / `vo`**；横切内容放在 **`platform`**。

## 模块与 `IUser*Api` 对应关系

| 模块包 | 内容 |
|--------|------|
| `account` | `IUserAccountApi`、`IUserAccountThirdApi`、`IUserAccountProtectionApi`；账号、第三方账号、账号保护、组织用户相关 VO/枚举（原 `user` / `protection` / `orguser` 合并） |
| `login` | `IUserLoginRememberMeApi`；记住登录与登录日志相关 VO/枚举（原 `loginremember` / `loglogin` 合并） |
| `contact` | `IUserContactWayApi` |
| `org` | `IUserOrgApi` |

路径示例：`io.kudos.ms.user.common.account.api`、`io.kudos.ms.user.common.org.vo.request`。
