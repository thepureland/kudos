# kudos-ms-auth-sql

## 定位

鉴权（`auth`）原子服务的**数据库迁移脚本模块**：仅包含 Flyway 使用的**版本化 SQL 资源**，
不含 Kotlin 源码。`kudos-ms-auth-core` 通过依赖本模块在运行时加载迁移（与
`kudos-ability-data-rdb-flyway` 等能力配合）。

本模块 **Gradle 依赖为空**，职责单一，便于单独审阅 schema 变更与版本演进。

---

## 资源布局

脚本位于：

```
resources/sql/auth/h2/
```

> 目录名为 `h2`，与测试 / 本地常用 H2 方言一致；若生产使用其他数据库，可能另有同结构的方言
> 适配脚本（以本仓库实际目录为准）。

---

## 迁移文件清单

迁移分两段：**`V1.0.0.0`–`V1.0.0.6`** 是写入 `sys` 库的**鉴权域种子数据**（菜单、字典、参数、
缓存配置、i18n 文案等——这些表的 DDL 属于 `kudos-ms-sys-sql`，本模块只负责"auth 模块占用的
那批行"），**`V1.0.0.20+`** 才是 auth 自有表的 DDL：

| 迁移文件 | 概要 |
|----------|------|
| `V1.0.0.0__insert_sys_micro_service.sql` | 在 `sys_micro_service` 中注册 auth 微服务条目 |
| `V1.0.0.1__insert_sys_dict.sql` | 注册 auth 模块需要的字典头 |
| `V1.0.0.2__insert_sys_dict_item.sql` | 上述字典头的项 |
| `V1.0.0.3__insert_sys_cache.sql` | 在 `sys_cache` 中登记 auth 各级缓存（与 core 的 HashCache / KeyValueCache 1:1 对齐） |
| `V1.0.0.4__insert_sys_resource.sql` | 注册 auth 管理后台菜单 / 按钮资源 |
| `V1.0.0.5__insert_sys_param.sql` | 注册 auth 模块的系统参数 |
| `V1.0.0.6__insert_sys_i18n.sql` | 注册 auth 模块的 i18n 多语言文案 |
| `V1.0.0.20__init_auth_role.sql` | 角色主表 |
| `V1.0.0.21__init_auth_role_user.sql` | 角色 ↔ 用户关联（用户 id 指向 `user.sys_user`） |
| `V1.0.0.22__init_auth_role_resource.sql` | 角色 ↔ 资源关联（资源 id 指向 `sys.sys_resource`） |
| `V1.0.0.23__init_auth_group.sql` | 用户组主表，含层级 `path` 字段 |
| `V1.0.0.24__init_auth_group_user.sql` | 组 ↔ 用户关联 |
| `V1.0.0.25__init_auth_group_role.sql` | 组 ↔ 角色关联（"组里所有人"自动持有该角色） |
| `V1.0.0.36__init_auth_identity_provider.sql` | Provider 模板与租户身份提供方实例；仅保存 `client_secret_ref`，并种入 Google、LINE、通用 OIDC 模板 |
| `V1.0.0.37__init_auth_external_identity_invitation.sql` | 一次性外部身份邀请；token/预期邮箱/消费 subject 仅保存 SHA-256，支持有效期、吊销和原子使用计数 |
| `V1.0.0.38__init_auth_identity_provider_jit_config.sql` | Provider 一对一 JIT 配置；保存用户名策略、verified-email/域名准入和账号业务默认值及变更原因 |
| `V1.0.0.39__extend_identity_provider_management.sql` | Provider 实例增加创建/更新操作者与原因；新增一对一类型化 claim path 映射表 |
| `V1.0.0.40__init_auth_refresh_token.sql` | Hash-only Refresh Token family、父子轮换、消费/撤销状态及签发 token epoch |
| `V1.0.0.41__init_auth_password_history.sql` | 登录/安全密码的退役哈希历史，按租户、账号和用途隔离 |
| `V1.0.0.42__init_auth_recovery_code.sql` | MFA 恢复码组；仅保存作用域 SHA-256，支持单次消费与整组撤销 |
| `V1.0.0.43__init_auth_tenant_mfa_policy.sql` | 租户一对一 MFA 模式、注册宽限期、允许方法、条件选择器及变更审计 |
| `V1.0.0.44__init_auth_webauthn_credential.sql` | WebAuthn 公钥凭证、认证器属性、软撤销状态及签名计数 CAS 版本 |
| `V1.0.0.45__init_auth_webauthn_attestation_policy.sql` | 租户一对一 WebAuthn attestation format、AAGUID 准入及可信证明要求策略 |
| `V1.0.0.46__init_auth_webauthn_authenticator_risk_policy.sql` | 租户一对一 WebAuthn 认证器风险阻断级别及变更审计 |
| `V1.0.0.47__init_auth_security_event.sql` | 认证安全事件；按租户/类型/去重键/UTC 窗口唯一聚合，保留指纹、风险摘要及首末时间 |
| `V1.0.0.48__extend_auth_security_event_workflow.sql` | 安全事件 OPEN/ACKNOWLEDGED/CLOSED 状态、处置分类、操作人/原因/时间及乐观并发版本 |
| `V1.0.0.49__extend_auth_security_event_assignment_sla.sql` | 安全事件当前负责人/分派审计、SLA 截止时间、字段一致性约束及负责人/超时队列索引 |
| `V1.0.0.50__extend_auth_security_event_escalation_outbox.sql` | 安全事件升级级别/下次升级时间，以及支持租约领取、退避重试和死信状态的通知 outbox |
| `V1.0.0.51__extend_auth_security_event_notification_replay.sql` | 通知重放次数、租户死信索引，以及保留操作者/原因/UTC 时间的追加式重放审计表 |
| `V1.0.0.52__init_auth_security_event_notification_route.sql` | 租户级通知路由规则（按租户/类型/投递形态唯一）、变更前后快照审计表，以及路由缓存的 `sys_cache` 注册 |
| `V1.0.0.53__init_auth_security_event_oncall_roster.sql` | 租户值班表与显式 UTC 班次窗口、值班表变更审计、路由规则的 `responder_roster_code` 列，以及值班表缓存的 `sys_cache` 注册 |
| `V1.0.0.54__init_auth_security_event_notification_channel.sql` | 通知的逐渠道终态账本（按通知+渠道唯一），使重试不再重发已交付渠道 |
| `V1.0.0.55__init_auth_mfa_enrollment_exemption.sql` | 逐用户 MFA 注册临时豁免；每次授予为新行，撤销字段全有或全无，窗口必须有效 |
| `V1.0.0.56__init_auth_login_event.sql` | 认证结果审计事件；按事务唯一，标识只存 SHA-256，成功/失败证据互不借用 |
| `V1.0.0.57__init_auth_credential_revocation.sql` | 管理员发起的凭证吊销追加式记录；含操作者、原因、可选安全事件关联及吊销后是否已无因子 |

> **版本号分段约定**：`V1.0.0.0` 起步号段留给"种子 / 引导"型脚本，业务表 DDL 从 `V1.0.0.20`
> 开始；这样新增 `insert_*` 种子无需挤占表 DDL 段，反之亦然。

---

## 命名与组织约定

- **表前缀 `auth_`**：与 sys / user / msg 服务隔离；同库部署时按前缀也能快速圈定本服务的表。
- **`V_*_*` 版本化脚本**：一个文件一条迁移；不在一个文件里混多个 DDL/DML。
- **内置数据（如系统角色 / 默认管理员）走 `R_*` 可重复脚本**：当前仓库中尚无 `R_*`，按
  约定后续可以补充。
- **种子数据写入 `sys_*` 表**：跨服务种子由"被写入方负责 DDL，写入方负责 INSERT"——例如
  `V1.0.0.4__insert_sys_resource.sql` 是 auth 服务向 sys 库注册自己的菜单条目。

---

## 与其他子模块的关系

| 模块 | 关系 |
|------|------|
| **kudos-ms-auth-core** | 依赖本模块 → 运行迁移并基于表结构实现 DAO / Service |
| **kudos-ms-auth-common** | 无直接依赖；VO 字段与表列在设计上对应 |

---

## 维护注意

- **新增表或变更列**：新增**更高版本号**的迁移文件，避免修改已发布版本脚本（除非团队约定
  可重建环境）。
- **跨服务种子**：写入 `sys_*` 表的 INSERT 必须保证幂等（建议 `INSERT ... ON CONFLICT
  DO NOTHING` 或先 `DELETE` 再 `INSERT`），否则多次 Flyway 验签会失败。
- **视图（`v_*`）与业务查询强相关**：本模块当前无视图；若后续加，需同步 `core` 的 DAO /
  实体与 `common` 的 VO。

## 已知限制 / 后续工作

- ❗ **仅 H2 方言** — `resources/sql/auth/h2/` 是唯一目录；MySQL / PG 移植需要业务方手动复制 +
  按方言差异适配
- ❗ **跨服务种子未做幂等保护** — `V1.0.0.0~V1.0.0.6` 向 `sys_*` 表 INSERT 时如果重新 baseline，
  Flyway 校验会跳过但 INSERT 不会重做；多次部署到不同库时容易因为种子 id 冲突翻车
- ❗ **`auth_group.path` 字符串祖先链未做长度上限** — 组层级超深时 `path` 列可能超出 varchar 长度，
  需在 DDL 上设合理上限并在 service 层校验
- ❗ **缺少级联删除约束** — 删除角色时 `auth_role_user` / `auth_role_resource` 中的引用不会
  自动清理；目前靠 service 层先删关联再删主体，绕过 service 会留死数据
- ❗ **没有索引** — `auth_role_user(role_id)` / `auth_role_resource(role_id)` 等高频过滤列
  当前缺索引；用户量 / 权限规模上来后查询性能会快速劣化
- ❗ **缺 `R_*_*` repeatable 脚本** — 当前全是 V_*；系统角色 seed 数据建议改 R_*
  以支持反复 apply
- ❗ **`V1.0.0.59` 跨模块读写 `user_account`** — 登录密码迁入 `auth_credential` 的回填脚本直接
  `SELECT` / `UPDATE` user 域的表，依赖 sys → user → auth 的同库 Flyway 执行顺序。auth 与 user
  分库部署时该脚本回填不到任何数据（也不会报错），需要改走应用层迁移把两边的密码对齐
