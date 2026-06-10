# kudos-ability-data

数据存储能力主题。

| 子目录 | 内容 |
|---|---|
| [`kudos-ability-data-rdb`](kudos-ability-data-rdb/README.md) | 关系型数据库：JDBC / Ktorm / Flyway |
| [`kudos-ability-data-memdb`](kudos-ability-data-memdb/README.md) | 内存数据库：Redis |
| [`kudos-ability-data-docdb`](kudos-ability-data-docdb/README.md) | 文档数据库：MongoDB |
| [`kudos-ability-data-tsdb`](kudos-ability-data-tsdb/README.md) | 时间序列数据库：InfluxDB |

业务侧典型组合：`data-rdb-ktorm`（间接拿到 jdbc）+ `data-rdb-flyway`（迁移）+ `data-memdb-redis`
（Redis 模板）。监控/IoT 时序数据按需引入 `data-tsdb-influxdb`。

## 改进建议（自动分析 2026-06-11）

以下为本次深度审查中发现、但因涉及行为/公开 API 变更而**未直接修改**的问题，按维度分类。
各子模块 README 已有的"已知限制 / 后续工作"条目不在此重复。

### 安全性

1. **ORDER BY 注入面仅做了单引号过滤** —— `kudos-ability-data-rdb-jdbc/src/io/kudos/ability/data/rdb/jdbc/kit/RdbKit.kt`
   的 `getOrderSql()` 只排除了含 `'` 的属性名，属性名仍被原样拼进 SQL（如 `name; DROP ...` 不含引号也能通过）。
   建议改为白名单正则（如 `^[A-Za-z0-9_.]+$`）校验，不符合即丢弃并 WARN。因可能影响现有调用方
   （合法但含特殊字符的列名），未直接修改。
2. **Redis value 默认 JDK 序列化** —— `kudos-ability-data-memdb-redis/.../init/properties/RedisExtProperties.kt`
   中 `valueSerializer` / `hashvalueSerializer` 默认 `jdk`（`JdkSerializationRedisSerializer`）。若同一 Redis
   可被低信任方写入，JDK 反序列化存在 gadget 链风险；建议默认改为 `fastjson`（safeMode）或 `string`，
   并在 README 强调。属默认值行为变更，未直接修改。
3. **前缀序列化器对 null key 的处理** —— `kudos-ability-data-memdb-redis/.../serializer/StringRedisSerializer.kt`
   的 `serialize(null)` 会写出字面量 `"<prefix>:null"` key。建议显式 `require(key != null)` 快速失败。

### 功能缺陷 / 值得补充

4. **Redis Hash DAO 无序分页不稳定** —— `kudos-ability-data-memdb-redis/.../dao/IdEntitiesRedisHashDao.kt`
   的 `list(criteria, pageNo, pageSize)` 在未传 `orders` 时直接对 `Set`/`HKEYS` 结果 `toList()` 后切页，
   集合无序导致翻页可能重复/漏行。建议无 order 时按 id 字典序排序后再分页。
5. **读写分离策略硬编码** —— `kudos-ability-data-memdb-redis/.../RedisConnectFactory.kt` 将
   `ReadFrom.REPLICA_PREFERRED`、池兜底 `maxTotal=200`、集群拓扑刷新 `60s` 写死。建议全部提为
   `RedisExtProperties` 配置项（特别是 read-from 策略，强一致读场景需要 `MASTER`）。
6. **限流注解的失败开关** —— `kudos-ability-data-memdb-redis/.../aop/RateLimiterAspect.kt` 在 Redis
   故障时把异常包装后向上抛（fail-closed）。建议增加 `failOpen` 注解属性让业务选择"Redis 不可用时放行"。

### 可扩展性

7. **新增 RDB 类型需改枚举源码** —— `kudos-ability-data-rdb-jdbc/.../metadata/RdbTypeEnum.kt` +
   `RdbKit.determinRdbTypeByUrl()`：未知 url 前缀直接 `valueOf` 抛 `IllegalArgumentException`，报错信息
   不指向配置项；也无 SPI 注册新数据库类型的口子。建议捕获后给出"受支持类型列表"的明确错误，并考虑
   SPI 化。
8. **`determinRdbTypeByUrl` 拼写错误（缺 e）** —— 同上文件。public API 不能直接改名；建议新增
   `determineRdbTypeByUrl` 并将旧名 `@Deprecated(ReplaceWith(...))` 过渡。

### 可观测性

9. **Redis 二级索引查询无慢查询日志** —— `IdEntitiesRedisHashDao` / `CriteriaRedisResolver` 的交并集
   在应用层完成，一次 `list()` 可能产生 N 次 Redis 往返，但无任何耗时埋点。建议加"超过阈值打 WARN"
   的耗时日志或 Micrometer Timer。
10. **数据源路由无指标** —— `kudos-ability-data-rdb-jdbc/.../aop/DynamicDataSourceAspect.kt` 仅 debug
    日志。建议发布 Micrometer counter（路由命中缓存、动态创建数据源次数、失败次数），与已有的
    Hikari 连接池指标（`HikariDataSourceMeterInitEvent`）配套。

### 可维护性 / 对外 API

11. **`searchByPayload` 用 `List<Any>` 按下标传递三元结果** —— `kudos-ability-data-rdb-ktorm/.../support/BaseReadOnlyDao.kt`
    （`objects[0] as Query` / `objects[1] as Set<String>` / `objects[2] as Map<...>`），类型不安全且易错位。
    建议改为私有 data class（私有方法，重构无 API 影响，但涉及多处调用点，留待专门提交）。
12. **`search(listSearchPayload)` 返回 `List<*>` 三态语义** —— 同上文件：结果元素可能是实体 / 单列值 /
    Map，靠文档约定区分。建议拆分为命名明确的方法（如 `searchEntities` / `searchColumn` / `searchMaps`），
    旧方法过渡期保留。
13. **`RedisTemplates` 暴露可变状态** —— `defaultRedisTemplate` 为 `var`，`getRedisTemplateMap()` 返回
    内部 `MutableMap`。建议改为 `val` + 只读视图（属 public API 变更，未直接修改）。
14. **KDoc 中英文混杂** —— 如 `BaseCrudDao.updateByCriteria` / `batchUpdateByCriteria` 为中文 KDoc，
    同文件其余为英文；`CriteriaConverter.convertCriterion` 同样。建议统一为一种语言。

### 测试覆盖

15. **Mongo 开关关闭分支未测** —— `kudos-ability-data-docdb-mongo`：`big-integer-as-string=false` 时
    `MongoCustomConversions` bean 不注册的分支缺 `ApplicationContextRunner` 测试（influxdb 模块已有同款
    条件矩阵测试可参照）。
16. **`RdbMetadataKit` 索引统计行分支** —— 本次已修复 `getIndexInfo` 返回 `COLUMN_NAME=null` 统计行
    导致的 NPE（见 `_getColumnsByTableName`），但 H2 难以复现该场景，建议用 mock `DatabaseMetaData`
    补一条单测锁定行为。
17. **`BaseCrudDao` 审计字段 map 路径** —— 本次已修复 `superclasses.contains(IAuditable)` 漏判间接实现者
    的 bug（改为 `isSubclassOf`），建议补一条"实体经 `IManagedDbEntity` 间接实现 `IAuditable` 时
    `updateProperties` 自动填充 `updateTime`"的回归测试。
