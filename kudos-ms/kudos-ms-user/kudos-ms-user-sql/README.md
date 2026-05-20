# kudos-ms-user-sql

User 原子服务的 Flyway 迁移脚本——只放 `*.sql`，无 Kotlin 代码。

## 表结构

| 表 | 说明 | 引入版本 |
|---|---|---|
| `user_account` | 用户账号主表（含 `login_password` / `security_password` / `authentication_key`、登录错误次数、最后登录信息、`session_key`、`authentication_key`、`org_id`、`supervisor_id`、内置位 `built_in` 等） | V1.0.0.20 |
| `user_account_third` | 第三方账号绑定（OAuth / SSO）；按 `(user_id, provider_code)` 唯一 | V1.0.0.21 |
| `user_login_remember_me` | 记住登录的长效 token | V1.0.0.22 |
| `user_log_login` | 登录日志（每次登录的审计记录） | V1.0.0.23 |
| `user_org` | 组织树（带 `path` 字段做祖先链） | V1.0.0.24 |
| `user_org_user` | 组织 ↔ 用户多对多 | V1.0.0.25 |
| `user_contact_way` | 用户联系方式（手机 / 邮箱，可多条） | V1.0.0.26 |
| `user_account_protection` | 账号保护：错误次数、冻结窗口等保护性字段 | V1.0.0.27 |

> 注意：表前缀历史命名是 **`user_*`**，不是 `sys_*`。早期文档曾误标为 `sys_*`，以 DDL 为准。

## 跨表关键 DDL 增量

| 版本 | 内容 |
|---|---|
| V1.0.0.0 – V1.0.0.6 | 初始化字典 / 微服务 / 缓存 / 资源 / 国际化等元数据 DML |
| V1.0.0.20 – V1.0.0.27 | 8 张业务表的初始化 DDL |
| **V1.0.0.28** | 给 `user_account` 补 6 列冻结字段：`freeze_type` / `freeze_time` / `freeze_start_time` / `freeze_end_time` / `freeze_title` / `freeze_content`——配合 `PassportService` 登录鉴权状态机 |

## 冻结字段语义（V1.0.0.28）

```
freeze_type        非空 = 存在一条冻结记录（manual / auto / admin / scheduled 字典码）
freeze_time        冻结记录写入时刻（审计）
freeze_start_time  null = 立即生效；非 null = 到点生效
freeze_end_time    null = 永久冻结；非 null = 到点自动解除
```

登录判定（在 `PassportService.login` 中）："当 `freeze_type IS NOT NULL` 且
`(freeze_start_time IS NULL OR now >= freeze_start_time)` 且 `(freeze_end_time IS NULL
OR now < freeze_end_time)`" 时视为"当前冻结"，登录被拒（返回 `ACCOUNT_FROZEN`）。

## 约定

- 表前缀 `user_`（注意：早期 user 服务文档曾使用 `sys_*` 命名，但实际 DDL 是 `user_*`）
- 与 sys 服务的表通过 schema / db 分离，不共表
- Flyway baseline + `V_*_*` / `R_*_*` 命名
- DML / DDL 用 `--region` 注释分块，便于阅读
- 用户密码字段经哈希存储（BCrypt，由 `user-core` 的 `PasswordKit` 维护）
- H2 方言脚本位于 `resources/sql/user/h2/`；其他方言按需增设同名目录（如 `resources/sql/user/mysql/`、`resources/sql/user/postgresql/`）

## 依赖

无 Kotlin 依赖，纯资源模块。运行期由 `kudos-ability-data-rdb-flyway` 在启动时扫描并 apply。

## 测试侧使用

`user-core` 的测试用例不直接走 Flyway，而是用 `test-resources/sql/h2/<domain>/<class>.sql`
做精确的小批量初始化（每个测试一份脚本，互不污染）。
