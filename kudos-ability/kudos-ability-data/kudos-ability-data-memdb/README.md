# kudos-ability-data-memdb

内存数据库（in-memory DB）接入集合。

| 子模块 | 实现 |
|---|---|
| [`kudos-ability-data-memdb-redis`](kudos-ability-data-memdb-redis/README.md) | Redis——多实例 RedisTemplate + 限流注解 + Hash DAO |

注：本目录是"内存数据库"分类，区别于 [`kudos-ability-data-rdb`](../kudos-ability-data-rdb/)
（关系型数据库）。未来如有 Memcached / Hazelcast 等其他内存 DB 接入，应同放本目录下。
