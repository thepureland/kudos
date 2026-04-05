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

当前迁移文件按版本号递增（示例，以仓库内实际文件为准）：

| 迁移文件 | 概要 |
|----------|------|
| `V1.0.0.1__init_sys_system.sql` | 子系统（system） |
| `V1.0.0.2__init_sys_micro_service.sql` | 微服务 |
| `V1.0.0.3__init_sys_sub_system_micro_service.sql` | 子系统与微服务绑定 |
| `V1.0.0.4__init_sys_tenant.sql` | 租户 |
| `V1.0.0.5__init_sys_tenant_system.sql` | 租户与子系统 |
| `V1.0.0.6__init_sys_tenant_locale.sql` | 租户语言 |
| `V1.0.0.7__init_sys_data_source.sql` | 数据源 |
| `V1.0.0.8__init_sys_dict.sql` | 字典 |
| `V1.0.0.9__init_sys_dict_item.sql` | 字典项 |
| `V1.0.0.10__init_v_sys_dict_item.sql` | 字典项视图 |
| `V1.0.0.11__init_sys_cache.sql` | 缓存配置 |
| `V1.0.0.12__init_sys_resource.sql` | 资源（含菜单等） |
| `V1.0.0.13__init_sys_tenant_resource.sql` | 租户与资源 |
| `V1.0.0.14__init_sys_param.sql` | 参数 |
| `V1.0.0.15__init_sys_domain.sql` | 域 |
| `V1.0.0.16__init_sys_i18n.sql` | 国际化 |
| `V1.0.0.17__init_sys_access_rule.sql` | 访问规则 |
| `V1.0.0.18__init_sys_access_rule_ip.sql` | 访问规则 IP |
| `V1.0.0.19__init_v_sys_access_rule_ip.sql` | 访问规则与 IP 联合视图 |

> 目录名为 `h2`，与测试/本地常用 H2 方言一致；若生产使用其他数据库，可能另有同结构或方言适配脚本（以本仓库实际目录为准）。

---

## 与其他子模块的关系

| 模块 | 关系 |
|------|------|
| **kudos-ms-sys-core** | `api` 依赖本模块，运行迁移并基于表结构实现 DAO/Service |
| **kudos-ms-sys-common** | 无直接依赖；VO 与表字段在设计上对应 |

---

## 维护注意

- 新增表或变更列：新增 **更高版本号** 的迁移文件，避免修改已发布版本脚本（除非团队约定可重建环境）。
- 视图（`v_*`）与业务查询强相关，变更时需同步 `core` 中 DAO/实体与 `common` 中 VO。
