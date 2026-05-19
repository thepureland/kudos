# kudos-ability-data

数据存储能力主题。

| 子目录 | 内容 |
|---|---|
| [`kudos-ability-data-rdb`](kudos-ability-data-rdb/README.md) | 关系型数据库：JDBC / Ktorm / Flyway |
| [`kudos-ability-data-memdb`](kudos-ability-data-memdb/README.md) | 内存数据库：Redis |

业务侧典型组合：`data-rdb-ktorm`（间接拿到 jdbc）+ `data-rdb-flyway`（迁移）+ `data-memdb-redis`
（Redis 模板）。
