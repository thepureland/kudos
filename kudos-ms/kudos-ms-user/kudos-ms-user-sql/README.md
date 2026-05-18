# kudos-ms-user-sql

User 原子服务的 Flyway 迁移脚本——只放 `*.sql`，无 Kotlin 代码。

## 表结构（要点）

| 表 | 说明 |
|---|---|
| `sys_user` | 用户账号主数据 |
| `sys_user_third` | 第三方账号绑定（OAuth / SSO） |
| `sys_user_login_remember` | 记住登录的长效 token |
| `sys_user_log_login` | 登录日志 |
| `sys_org` | 组织树（带 path 字段） |
| `sys_org_user` | 组织 ↔ 用户多对多 |

## 约定

- 表前缀 `sys_`（user 服务历史命名沿用 `sys_*`，与 sys 服务的表通过 schema / db 分离）
- Flyway baseline + V_*_* / R_*_* 命名
- 用户密码字段经哈希存储（具体算法由 user-core 层维护）

## 依赖

无 Kotlin 依赖，纯资源模块。
