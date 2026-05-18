# kudos-ability-data-memdb-redis

基于 Spring Data Redis + Lettuce 的多实例 Redis 集成。
 
1. **多实例 RedisTemplate** —— 按 `kudos.ability.data.redis.redis-map.<name>` 配置任意多个 redis
   实例（默认 / cache / session 等），通过 `RedisTemplates` 容器按 name 索引
2. **可配置序列化器** —— STRING / JDK / FASTJSON 三种，分别用于 key / hashKey / value / hashValue
3. **集群 / 单机透明** —— 配 `cluster.nodes` 自动走 `RedisClusterConfiguration`，否则单机
4. **方法级限流** —— `@RateLimiter` 注解 + AOP + lua 脚本，按 DEFAULT / USER / IP 维度
5. **Hash 全表 DAO** —— `IdEntitiesRedisHashDao` 提供 Hash 主存 + Set/ZSet 二级索引 + 分页 + 排序

## 设计要点

### 多 RedisTemplate 装配

`RedisAutoConfiguration` 启动时遍历 `kudos.ability.data.redis.redis-map`，每个 name 都装一份
`LettuceConnectionFactory + RedisTemplate`，最终汇总到 `RedisTemplates`。`default-redis` 字段
指向其中一份作为 `defaultRedisTemplate`，业务 `@Autowired RedisTemplate` 时默认拿到它。

```yaml
kudos:
  ability:
    data:
      redis:
        default-redis: master
        redis-map:
          master:
            host: localhost
            port: 6379
            database: 0
            password: ${REDIS_PASSWORD}
            key-serializer: string
            value-serializer: fastjson
            hashkey-serializer: string
            hashvalue-serializer: fastjson
            max-active: 200
            max-idle: 20
            min-idle: 5
            max-wait: 200ms
          cache:
            host: replica
            port: 6379
            database: 1
            key-serializer: string
            value-serializer: jdk
```

### Lettuce 连接策略

`RedisConnectFactory` 装配 `LettuceConnectionFactory` 时统一打开：
- `autoReconnect=true`、`keepAlive=true`、`disconnectedBehavior=REJECT_COMMANDS`
- `readFrom=REPLICA_PREFERRED`（读优先打从节点；单机模式无影响）
- 集群模式额外：60s 周期 topology 刷新、`ASK_REDIRECT` / `UNKNOWN_NODE` 自适应触发刷新、
  关闭 `validateClusterNodeMembership`（云上做了 NAT 的环境必须关掉）

SSL 暂未接入；如有需要在 `RedisConnectFactory.newLettuceConnectionFactory` 内的
`LettucePoolingClientConfiguration.builder()` 上加 `.useSsl()`。

### `IdEntitiesRedisHashDao` 索引模型

主数据用 Hash（id → 对象 JSON），二级索引按属性建：
- `filterableProperties` → `idx:set:<property>:<value>`（Set），用于等值 / IN 查询
- `sortableProperties` → `idx:zset:<property>`（ZSet，score = 数值），用于范围 / 排序 / 分页

`CriteriaRedisResolver` 把 `Criteria` 拆成对索引的查询：
- 组间 AND（结果集求交集）
- 组内数组 OR（结果集求并集）
- 数值型 value 或范围操作符 → ZSet；其余 → Set

**ZSet score 下界陷阱**：Java 的 `Double.MIN_VALUE` 是最小**正**数；
`LT`/`LE`/`NOT_BETWEEN` 的下界必须用 `-Double.MAX_VALUE`，否则负 score 成员会被全部漏掉。
本模块已修正。

### 限流（`@RateLimiter`）

切面在方法执行前调一次 `limit.lua` 脚本：`INCR + EXPIRE`，超阈值返回 0。

- `LimitType.DEFAULT` —— 按"声明类.方法名"全局共享计数
- `LimitType.USER` —— 加上当前 `user.id`（要求 `KudosContextHolder.user` 已设置）
- `LimitType.IP` —— 加上当前客户端 IP（要求 `KudosContextHolder.clientInfo.ip` 已设置）

注：限流计数走 `defaultRedisTemplate`，不支持多实例分别限流。如需配独立限流 redis，
当前需自行实现 Aspect。

### 自定义 prefixed StringRedisSerializer

`serializer/StringRedisSerializer.kt`（注意：与 Spring 自带类同名不同包）按指定 prefix
自动给所有 key 加 / 去 namespace。装到 RedisTemplate `keySerializer` 上即可。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/RedisAutoConfiguration` | 装配入口，绑定 `kudos.ability.data.redis.*` 配置 |
| `init/properties/RedisProperties` | 顶层容器（`defaultRedis` + `redisMap`） |
| `init/properties/RedisExtProperties` | 单实例配置（Spring Boot `DataRedisProperties` + 序列化器 + 连接池） |
| `RedisConnectFactory` | Lettuce 连接工厂构造（集群 vs 单机分支） |
| `RedisTemplates` | name → RedisTemplate 容器 |
| `RedisSerializerEnum` | 序列化器字典（STRING / JDK / FASTJSON） |
| `consts/CacheKey` | key 拼装工具（`:` / `,` 分隔） |
| `serializer/StringRedisSerializer` | 自动前缀 String 序列化器 |
| `aop/RateLimiter` + `aop/LimitType` + `aop/RateLimiterAspect` | 限流注解 + 切面 |
| `dao/IdEntitiesRedisHashDao` | Hash 主存 + Set/ZSet 二级索引 DAO |
| `dao/support/CriteriaRedisResolver` | `Criteria` → Redis 索引查询解析器 |

## 测试覆盖

- `RedisTemplateTest`、`IdEntitiesRedisHashDaoTest` —— 基于 RedisTestContainer 的端到端集成，
  覆盖 RedisTemplate 用法 + Hash DAO 全部公共方法
- `CacheKeyTest`、`RedisSerializerEnumTest` —— 新增，纯单元测试，无 Docker 依赖

未单测但被集成测试覆盖的：`CriteriaRedisResolver`、`RedisExtProperties.getSerializerByType`。
仍未覆盖：`RateLimiterAspect`（依赖 Spring AOP + Redis 全量装配，未来加专门的集成测试）。

## 已知限制 / 后续工作

- ❗ 仅 `defaultRedisTemplate` 参与限流；多 redis 实例分别限流需自行扩展 Aspect
- ❗ SSL 未实现，仅在源码中预留注释。需 SSL 时改 `RedisConnectFactory`
- ❗ `IdEntitiesRedisHashDao.refreshAll` 重建索引时**逐条** `save()`，没有 pipeline——
  全量刷新 1w 条以上时会偏慢。可优化为 pipeline + scoreBatch
- ❗ `IdEntitiesRedisHashDao.list` 仅第一个 Order 参与 Redis 排序；多字段排序需应用层兜底
- ❗ `IdEntitiesRedisHashDao.getPropertyValue` 走 Java 反射，对 Kotlin data class 的字段
  访问性能不如 KProperty.get；热路径建议自定义 DAO 覆盖
- ❗ `RedisSerializerEnum` 是 hardcoded 枚举，添加新序列化器（如 Kryo）需改源码 + 枚举
- ❗ `RedisAutoConfiguration` 的 `redisProperties()` bean 名固定为 `redisProperties`，可能与
  Spring Boot `RedisProperties` bean 冲突（实际通过 `@ConditionalOnMissingBean` 兼容，但
  名字带歧义）

## 依赖

```kotlin
api(project(":kudos-context"))
api(project(":kudos-base"))
api(libs.spring.boot.starter.data.redis)
api(libs.alibaba.fastjson2)
api(libs.alibaba.fastjson2.spring)
api(libs.apache.commons.pool2)

testImplementation(project(":kudos-test:kudos-test-container"))
```

Lettuce 版本由 `spring-boot-starter-data-redis` BOM 管理（避免 reactive API 不兼容的
`NoSuchMethodError`）。
