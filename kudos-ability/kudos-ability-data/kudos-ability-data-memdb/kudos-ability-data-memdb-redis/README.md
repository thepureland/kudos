# kudos-ability-data-memdb-redis

基于 Spring Data Redis + Lettuce 的多实例 Redis 集成。

1. **多实例 RedisTemplate** —— 按 `kudos.ability.data.redis.redis-map.<name>` 配置任意多个 redis
   实例（默认 / cache / session 等），通过 `RedisTemplates` 容器按 name 索引
2. **可配置序列化器** —— STRING / JDK / FASTJSON 三种字面值，或直接配置任意 `RedisSerializer`
   实现类的全限定名，分别用于 key / hashKey / value / hashValue
3. **哨兵 / 集群 / 单机透明** —— 按 `sentinel.nodes` → `cluster.nodes` → 单机的优先级自动选择
   （与 Spring Boot 一致）
4. **方法级限流** —— `@RateLimiter` 注解 + AOP + lua 脚本，按 DEFAULT / USER / IP 维度
5. **Hash 全表 DAO** —— `IdEntitiesRedisHashDao` 提供 Hash 主存 + Set/ZSet 二级索引 + 分页 + 排序；
   `BoundIdEntitiesRedisHashDao` 是按实体绑定描述符的类型安全门面

## 设计要点

### 多 RedisTemplate 装配与生命周期

`RedisAutoConfiguration` 启动时遍历 `kudos.ability.data.redis.redis-map`，每个 name 都装一份
`LettuceConnectionFactory + RedisTemplate`，最终汇总到 `RedisTemplates`。`default-redis` 字段
指向其中一份作为 `defaultRedisTemplate`，业务 `@Autowired RedisTemplate` 时默认拿到它。

`RedisTemplates` 同时持有每个实例的 `LettuceConnectionFactory`：

- 实现 `DisposableBean`，context 关闭时统一 `destroy()`（连接池 + Lettuce 线程随应用退出释放）
- `getConnectionFactory(name)` 供需要原生 `RedisConnectionFactory` 的基础设施复用
  （消息监听容器、Spring Session、健康检查等）
- `getRequiredRedisTemplate(name)` 在名字配错时抛出带可用实例列表的异常，
  优先于 `getRedisTemplate(name)!!`

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
            timeout: 3000            # 命令超时（Lettuce commandTimeout）
            connect-timeout: 1000    # TCP 建连超时
            read-from: master        # 读偏好；默认 replicaPreferred
            ssl:
              enabled: false
            max-active: 200          # 负数=不限制（commons-pool2 语义）
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
- `timeout` → Lettuce `commandTimeout`（不配则 Lettuce 默认 60s）；`connect-timeout` → 建连超时
- `read-from` 可按实例配置读偏好，默认 `replicaPreferred`（主从下读可能观察到复制延迟，
  需要强一致读的实例配 `master`）
- 集群模式支持 `username`（Redis 6+ ACL）；`ssl.enabled=true` 或配置 `ssl.bundle` 时调用
  `LettucePoolingClientConfiguration.useSsl()` 启用 SSL 传输
- 集群模式额外：60s 周期 topology 刷新、`ASK_REDIRECT` / `UNKNOWN_NODE` 自适应触发刷新、
  关闭 `validateClusterNodeMembership`（云上做了 NAT 的环境必须关掉）

哨兵模式配置（注意两套凭据不要混）：

```yaml
redis-map:
  master:
    database: 0
    username: dataUser        # 数据节点（master/replica）的凭据
    password: ${REDIS_PASSWORD}
    sentinel:
      master: mymaster
      nodes:
        - 127.0.0.1:26379
        - 127.0.0.1:26380
      username: sentinelUser  # 哨兵节点自身的凭据
      password: ${SENTINEL_PASSWORD}
```

### `IdEntitiesRedisHashDao` 索引模型

主数据用 Hash（id → 对象 JSON），二级索引按属性建：
- `filterableProperties` → `idx:set:<property>:<value>`（Set），用于等值 / IN 查询
- `sortableProperties` → `idx:zset:<property>`（ZSet，score = 数值），用于范围 / 排序 / 分页；
  非数值属性值**不会**写入索引（写哨兵 score 会静默破坏排序），跳过并 WARN

**更新时清理旧索引**：save / saveBatch 在覆盖前先读出旧实体，属性值变化时把 id 从旧值的
Set 索引里移除（ZSet 按成员覆盖 score，仅新值不再产生 score 时移除成员）。否则 `type` 从
A 改成 B 后，`idx:set:type:A` 会永远残留该 id，等值查询返回幻影行。

**refreshAll 按 key 原子切换**：主数据和全部索引先 pipeline 写入 tmp key，再逐 key `RENAME`
覆盖（RENAME 原子替换目标），最后删除新数据集不再产生的旧索引 key。刷新期间任一 key 上的
查询要么看到完整旧索引、要么看到完整新索引，不存在"索引被清空重建中"的窗口
（跨 key 不原子，跨多个索引 key 的查询可能短暂看到新旧混合）。

`CriteriaRedisResolver` 把 `Criteria` 拆成对索引的查询：
- 组间 AND（结果集求交集）
- 组内数组 OR（结果集求并集）
- 索引选择：范围操作符 → ZSet（只有它能做范围）；等值 → Set（`filterableProperties` 的所在地），
  数值 value 若没有对应的 Set 索引 key，则回退到 ZSet

**不能只按"值像不像数字"来选索引**：那样 `type = 1` 会被送到 ZSet 索引，而把 `type` 只声明为
`filterableProperties`（数值型枚举列最自然的声明方式）时根本没有 `zset:type`，查询会静默返回空。

**ZSet score 边界**：
- 下界必须用 `-Double.MAX_VALUE`（Java 的 `Double.MIN_VALUE` 是最小**正**数）
- 严格开区间（GT/LT/NOT_BETWEEN）用 `Math.nextUp` / `Math.nextDown` 移动一个 ULP，
  任何量级下都精确。**不能用固定 epsilon**：毫秒时间戳量级（1e12）的 ULP ≈ 2.4e-4，
  比它小的 epsilon 会被舍入吃掉，GT 静默退化成 GE

**无排序分页**：`list()` 不带 Order 时对 id 做字典序排序后再分页——HKEYS / SMEMBERS
的返回顺序是未定义的，直接分页会出现翻页重复 / 漏行。

**带排序分页在 Redis 内完成**（不再把整个 ZSet 拉回 JVM 过滤）：
- 无条件 → 直接 `ZRANGE` / `ZREVRANGE` 排序索引取一页
- 有条件 → 候选 id 暂存临时 Set，与排序索引 `ZINTERSTORE`（权重 `(1, 0)`，score 只来自排序索引，
  否则 Set 成员的默认 score 1 会整体偏移），再取一页；临时 key 在 `finally` 中必删

传输量因此与页大小成正比，而不是与表大小成正比。**集群注意**：交集是多 key 操作，
Redis Cluster 下要求这些 key 落在同一 slot——给 `dataKeyPrefix` 加 hash tag（如 `{myTable}:rows`）。
`CriteriaRedisResolver` 的 `IN` 并集本来就有同样要求。

### 按实体绑定的 DAO（`BoundIdEntitiesRedisHashDao`）

`IdEntitiesRedisHashDao` 每次调用都要重复传 key 前缀 / 实体类 / 两个索引属性集，而 save 与
deleteById 传的属性集**必须一致**，否则删除会漏清索引——类型系统抓不到这种错配。
`RedisHashEntitySpec` 把这些绑定一次：

```kotlin
val dao = BoundIdEntitiesRedisHashDao(
    redisTemplates,
    RedisHashEntitySpec(
        dataKeyPrefix = "sys:dict",
        entityClass = Dict::class,
        filterableProperties = setOf("type", "active"),
        sortableProperties = setOf("updateTime"),
    )
)
dao.save(dict)
dao.deleteById("d1")                    // 与 save 用同一套索引定义，不会错配
dao.list(criteria, 1, 20, Order.desc("updateTime"))
```

采用组合而非继承：未绑定的 API 只能通过你自己传入的 delegate 访问，调用方无法在这个对象上
绕过 spec；需要非默认 redis 实例时，把重写了 `getRedisTemplate()` 的子类作为 delegate 传入即可。

### 限流（`@RateLimiter`）

切面在方法执行前调一次 `limit.lua` 脚本，**固定窗口**计数：

- 窗口从首次放行的请求开始计时，`EXPIRE` 只在计数器创建时设置一次，
  后续放行**不刷新**过期时间（否则持续流量会让计数器永不过期，限流远比声明严格）
- 固定窗口在边界两侧最多可通过 2×count 次调用；对突刺敏感的场景用更小的窗口
- 计数 key 为 `rate.limit:[<id>:]<class>-<method>`，遵循 `CacheKey` 的 `:` 分隔约定
- `LimitType.DEFAULT` —— 按"声明类.方法名"全局共享计数
- `LimitType.USER` —— 加上当前 `user.id`（要求 `KudosContextHolder.user` 已设置）
- `LimitType.IP` —— 加上当前客户端 IP（要求 `KudosContextHolder.clientInfo.ip` 已设置）

计数实例与失败策略按注解配置：

```kotlin
// 计数放在专用的 rateLimit 实例；该实例挂了也不影响业务可用性
@RateLimiter(time = 1, count = 50, redisName = "rateLimit", failOpen = true)
fun search(...) { ... }
```

- `redisName` —— `redis-map` 中的实例名，留空用默认实例；名字配错**立即报错**并列出可用实例名
- `failOpen` —— 仅影响**基础设施故障**（Redis 挂了 / 超时）：`false`（默认）拒绝请求 fail-closed，
  `true` 放行并打 WARN。超过阈值始终拒绝；`redisName` 配错属于部署错误，不会被 failOpen 掩盖

切面由 `RedisAutoConfiguration` 以 `@Bean` 注册（构造注入，不依赖宿主应用的包扫描范围，
也不再用 `@Lazy` 字段注入——那需要对具体类做 CGLIB 懒代理，曾导致启动失败）。

### `PrefixedStringRedisSerializer`

`serializer/PrefixedStringRedisSerializer.kt`（原名 `StringRedisSerializer`，为避免与
Spring 自带类混淆已改名）按指定 prefix 自动给所有 key 加 / 去 namespace。装到 RedisTemplate
`keySerializer` 上即可。prefix 不允许为空白；null key 按空 key 处理（接口要求非空返回值），
不会再被渲染成字面量 `"null"`。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/RedisAutoConfiguration` | 装配入口，绑定 `kudos.ability.data.redis.*` 配置，注册限流切面 |
| `init/properties/RedisProperties` | 顶层容器（`defaultRedis` + `redisMap`） |
| `init/properties/RedisExtProperties` | 单实例配置（Spring Boot `DataRedisProperties` + 序列化器 + 连接池 + read-from） |
| `RedisConnectFactory` | Lettuce 连接工厂构造（哨兵 / 集群 / 单机分支） |
| `RedisTemplates` | name → RedisTemplate / ConnectionFactory 容器，负责关停 |
| `RedisSerializerEnum` | 序列化器字典（STRING / JDK / FASTJSON；非枚举字面值按 FQCN 解析） |
| `consts/CacheKey` | key 拼装工具（`:` / `,` 分隔；旧拼写常量已 @Deprecated 保留） |
| `serializer/PrefixedStringRedisSerializer` | 自动前缀 String 序列化器 |
| `aop/RateLimiter` + `aop/LimitType` + `aop/RateLimiterAspect` | 限流注解 + 切面 |
| `dao/IdEntitiesRedisHashDao` | Hash 主存 + Set/ZSet 二级索引 DAO |
| `dao/BoundIdEntitiesRedisHashDao` | 按实体绑定 spec 的类型安全门面 |
| `dao/support/RedisHashEntitySpec` | 实体的 key / 类型 / 索引属性描述符 |
| `dao/support/CriteriaRedisResolver` | `Criteria` → Redis 索引查询解析器 |

## 测试覆盖

- `RedisTemplateTest`、`IdEntitiesRedisHashDaoTest`、`BoundIdEntitiesRedisHashDaoTest`、
  `CriteriaRedisResolverTest`、`RateLimiterAspectTest` —— 基于 RedisTestContainer 的端到端集成
- `CacheKeyTest`、`RedisSerializerEnumTest`、`RedisConnectFactoryTest`、`RedisTemplatesTest`、
  `RedisExtPropertiesTest`、`RedisAutoConfigurationTest`、`PrefixedStringRedisSerializerTest`
  —— 纯单元测试，无 Docker 依赖

## 已知限制 / 后续工作

- ✅ `ssl.enabled` / `ssl.bundle` 已接入 Lettuce SSL 开关；但 `ssl.bundle` 目前仅作为启用
  SSL 的信号，未把自定义证书材料注入 Lettuce，双向 TLS / 私有 CA 场景仍需扩展连接工厂
- ❗ `IdEntitiesRedisHashDao.list` 仅第一个 Order 参与 Redis 排序，多字段排序需应用层兜底
- ❗ 二级索引的多 key 操作（`IN` 并集、带条件排序分页的 `ZINTERSTORE`）要求相关 key 同 slot；
  Redis Cluster 下必须给 `dataKeyPrefix` 加 hash tag
- ❗ 主数据与索引的写入跨多条命令，无 lua / 事务级原子性；进程在两者之间崩溃仍可能短暂不一致
  （refreshAll 已按 key 原子化，增量 save 未覆盖）
- ❗ 带排序的查询只返回排序索引里有的行：非数值属性值不入索引，这类行不会出现在排序结果中
  （无排序查询仍可见）
- ❗ 连接工厂虽由 `RedisTemplates` 负责关停，但未注册为独立 Spring bean，Actuator 的
  Redis 健康检查不会自动生效；需要时从 `getConnectionFactory(name)` 自行装配 HealthIndicator
- ❗ 集群 topology 刷新周期（60s）暂不可配置
- ❗ 哨兵模式已支持配置装配，但缺少端到端集成测试（测试容器未起哨兵拓扑），仅覆盖到配置对象层面

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
