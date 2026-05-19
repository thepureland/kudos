# kudos-ms-sys-sql

## 定位

系统（`sys`）原子服务的 **数据库迁移脚本模块**：仅包含 Flyway 等工具使用的 **版本化 SQL 资源**，不包含 Kotlin 源码。`kudos-ms-sys-core` 通过依赖本模块在运行时加载迁移（与 `kudos-ability-data-rdb-flyway` 等能力配合）。

本模块 **Gradle 依赖为空**，职责单一，便于单独审阅 schema 变更与版本演进。

---

## 资源布局

脚本位于：

```
resources/sql/sys/h2/
```

当前迁移文件按版本号递增：

| 迁移文件 | 类型 | 概要 |
|----------|------|------|
| `V1.0.0.1__init_sys_system.sql` | DDL | 子系统（system） |
| `V1.0.0.2__init_sys_micro_service.sql` | DDL | 微服务 |
| `V1.0.0.3__init_sys_sub_system_micro_service.sql` | DDL | 子系统与微服务绑定 |
| `V1.0.0.4__init_sys_tenant.sql` | DDL | 租户 |
| `V1.0.0.5__init_sys_tenant_system.sql` | DDL | 租户与子系统 |
| `V1.0.0.6__init_sys_tenant_locale.sql` | DDL | 租户语言（早期表；后由 `V1.0.0.23` 引入的 `sys_locale` 主表取代承载语言主数据） |
| `V1.0.0.7__init_sys_data_source.sql` | DDL | 数据源 |
| `V1.0.0.8__init_sys_dict.sql` | DDL | 字典 |
| `V1.0.0.9__init_sys_dict_item.sql` | DDL | 字典项 |
| `V1.0.0.10__init_v_sys_dict_item.sql` | DDL（视图） | `v_sys_dict_item`：字典项 + 字典头联合查询 |
| `V1.0.0.11__init_sys_cache.sql` | DDL | 缓存配置元数据 |
| `V1.0.0.12__init_sys_resource.sql` | DDL | 资源（含菜单等） |
| `V1.0.0.13__init_sys_tenant_resource.sql` | DDL | 租户与资源 |
| `V1.0.0.14__init_sys_param.sql` | DDL | 参数 |
| `V1.0.0.15__init_sys_domain.sql` | DDL | 域 |
| `V1.0.0.16__init_sys_i18n.sql` | DDL | 国际化 |
| `V1.0.0.17__init_sys_access_rule.sql` | DDL | 访问规则 |
| `V1.0.0.18__init_sys_access_rule_ip.sql` | DDL | 访问规则 IP（含 IP 段 start/end） |
| `V1.0.0.19__init_v_sys_access_rule_ip.sql` | DDL（视图） | `v_sys_access_rule_ip`：规则 + IP 联合视图（对应 `VSysAccessRuleWithIpDao`） |
| `V1.0.0.20__add_missing_fk_indexes.sql` | DDL（refinement） | 补建关键外键索引：`sys_access_rule_ip.parent_rule_id`、`sys_dict_item.dict_id` / `parent_id` |
| `V1.0.0.21__add_platform_access_rule_uniq.sql` | DDL（refinement） | 删除旧约束并以 `nulls not distinct` 重建 `uq_sys_access_rule` 唯一索引，使平台级（`tenant_id` 为 NULL）规则也参与唯一性 |
| `V1.0.0.22__init_sys_out_line.sql` | DDL | 大纲（OutLine）：按 (子系统,租户) 维度可缓存的轮廓数据 |
| `V1.0.0.23__init_sys_locale.sql` | DDL | 语言/区域主数据，配合 `LocaleByCodeCache` |
| `V1.0.0.24__seed_sys_locale_cache.sql` | DML（seed） | 写入默认语言数据，保证 cache 启动可用 |
| `V1.0.0.25__seed_sys_out_line_cache.sql` | DML（seed） | 写入默认大纲数据 |

> 目录名为 `h2`，与测试/本地常用 H2 方言一致；当前生产/CI 也复用此目录，未维护独立的 PostgreSQL/MySQL 方言版本。脚本中部分语法（如 `nulls not distinct`、`comment on column`）依赖 H2 兼容层或目标 RDB 同名特性，新增脚本需在目标库回归验证。
>
> 与代码的双向关系：
> - 命名 `Vx.y.z.n__init_<snake_case_table>.sql` 与 `core` 中 `Sys<PascalCase>Table` / `Sys<PascalCase>Dao` 严格一一对应（视图前缀 `v_` ↔ `VSys*Dao`）。
> - DML（`seed_*`）保证启动后缓存对应表至少有一行兜底数据，对应 `core/*/cache/*Cache` 的 `loadXxx` 行为。
> - 重命名或修列的 refinement 脚本需保持向前兼容（`drop constraint if exists`、`create index if not exists`），方便在已升级环境再次执行不报错。

---

## 与其他子模块的关系

| 模块 | 关系 |
|------|------|
| **kudos-ms-sys-core** | `api` 依赖本模块，运行迁移并基于表结构实现 DAO/Service |
| **kudos-ms-sys-common** | 无直接依赖；VO 与表字段在设计上对应 |

---

## 维护注意

- 新增表或变更列：新增 **更高版本号** 的迁移文件，避免修改已发布版本脚本（除非团队约定可重建环境）。已发布脚本（V1.0.0.1–V1.0.0.19）的列错误一律通过 V1.0.0.20+ 的 **refinement 迁移** 修正。
- 视图（`v_*`）与业务查询强相关，变更时需同步 `core` 中 `VSys*Dao` 与 `common` 中 VO。
- **平台级数据**（`tenant_id` 为 NULL）唯一约束需使用 `nulls not distinct`（参见 V1.0.0.21）；普通 `UNIQUE` 在多数 RDB 上视 NULL 互不相等，会让平台级行重复插入而不报错。
- **缓存依赖的 seed 数据**：新增以 `*Cache` 在启动期 `doReload` 的表时，配套提交 `seed_*` 迁移，避免空库时缓存为 null 引起 NPE。
- **外键 / 高频过滤列必须建索引**：参考 V1.0.0.20。Ktorm 不会自动建索引；缺索引在多租户大表上会快速劣化。
