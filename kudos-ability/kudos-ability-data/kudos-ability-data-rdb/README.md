# kudos-ability-data-rdb

关系型数据库（RDB）接入集合。

| 子模块 | 角色 |
|---|---|
| [`kudos-ability-data-rdb-jdbc`](kudos-ability-data-rdb-jdbc/README.md) | JDBC 层基础设施：动态数据源、JDBC 元数据、Seata 兼容 |
| [`kudos-ability-data-rdb-ktorm`](kudos-ability-data-rdb-ktorm/README.md) | Ktorm ORM 适配 + CRUD DAO 基类 + Criteria 转换 |
| [`kudos-ability-data-rdb-flyway`](kudos-ability-data-rdb-flyway/README.md) | 多数据源 Flyway 迁移 |

典型用法：业务侧依赖 `rdb-ktorm`（间接拿到 `rdb-jdbc`），在启动初期通过 `rdb-flyway` 跑迁移。
