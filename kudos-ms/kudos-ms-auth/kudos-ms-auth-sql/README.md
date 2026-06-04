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
