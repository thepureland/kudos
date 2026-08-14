# kudos-ability-cache

缓存能力主题——多级缓存框架 + 本地 / 远程 / 跨服务实现。

| 子目录 | 内容 |
|---|---|
| [`kudos-ability-cache-common`](kudos-ability-cache-common/README.md) | 核心：`MixCache` 二级缓存抽象、`@TenantCacheable` 注解 + 切面、跨节点失效广播 SPI |
| [`kudos-ability-cache-local`](kudos-ability-cache-local/README.md) | 本地缓存（Caffeine） |
| [`kudos-ability-cache-remote`](kudos-ability-cache-remote/README.md) | 远程缓存（Redis） |
| [`kudos-ability-cache-interservice`](kudos-ability-cache-interservice/README.md) | 跨服务缓存协作（Feign 协商缓存：client/provider/common 三件套） |

业务侧典型组合：`cache-common` + `cache-local-caffeine` + `cache-remote-redis`（开启
LOCAL_REMOTE 二级缓存）。

## 改进建议（自动分析 2026-06-11）

以下为本次深度审查中发现、但不宜直接修改（涉及行为/接口/设计决策）的事项，按维度分类。

### 功能缺陷 / 待补功能
- ✅ **TTL 随机化（雪崩防护）**（已补）：`createCache` 原先按配置固定 TTL，同一批写入的 key 会同时过期、回源流量拧成一个尖峰。现新增 `kudos.ability.cache.redis.ttl-jitter-percent`，每个**条目**的 TTL 从 `ttl * (1 ± p/100)` 均匀取值（包装 `RedisCacheConfiguration.ttlFunction`，所以显式配置的 TTL 和模块默认 900s 都覆盖到）。默认 `0` 即关闭、保持精确 TTL 不变；建议值 10，合法区间 0..50，越界启动期即失败。按条目而非按缓存抖动是关键——后者同缓存内的键仍会一起过期。
- **读路径无单飞（击穿防护）**：`kudos-ability-cache-common/src/io/kudos/ability/cache/common/core/keyvalue/MixCache.kt` 的 `mixGetOrLoad` 与 `batch/keyvalue/BatchCacheableAspect.kt` 的 `readCachedData`（源码中已有 TODO）在两级都未命中时直接回源，无 per-key 互斥；目前需业务自觉叠加 `@DistributedCacheGuard`。建议在 `mixGetOrLoad` 内提供可选的本地单飞（per-key `CompletableFuture` 合并）。
  注意 `Cache.get(key, valueLoader)` 的"loader 只调一次"契约因此**随策略配置而变**：SINGLE_LOCAL / REMOTE 委托原生实现（有 per-key 同步）成立，LOCAL_REMOTE 走 `mixGetOrLoad` 则不成立，调用方无从感知。
- ✅ **缓存 null 无法与未命中区分**（已修复）：`CacheValueWrapper` 改为真三态（`of(null)` 表示"命中且值为 null"，`empty()` 表示未命中），新增 `KeyValueCacheKit.getWrapper(...)`，`DistributedCacheGuardAspect` 改用它判定命中——命中的 null 不再每次加分布式锁回源。同时移除了 Redis 侧的 `disableCachingNullValues()`：它与 common 侧的负缓存设计冲突，会让 `@Cacheable` 方法返回 null 时 `RedisCache.put` 抛 `IllegalArgumentException` 并经 `MixCache.writeThrough` 冒进业务代码。

### 可扩展性 / 对外接口
- ✅ **`IKeyValueCacheManager` 命名契约不统一**（已修复）：契约已在接口 KDoc 中写明——`cacheName` 一律传**逻辑名**，版本前缀由实现内部处理；`pattern` 一律是**裸 key 模式**，不带版本前缀。两个实现与 `MixCacheManager` 的调用点均已统一，逐处补偿的代码已删除。修复过程中还发现 Caffeine 的 `evictByPattern` 把"缓存名前缀"函数错用在 key 模式上，一旦配置 `kudos.ability.cache.version` 就会静默匹配不到任何键（此前因发行 yml 里 version 为空而未暴露）。

### 可观测性
- **无命中率指标**：`MixCache` 的 get/put/evict 全链路无 Micrometer 埋点，Caffeine 的 `recordStats` 也未强制开启或暴露。线上无法回答"某缓存命中率多少、回源 QPS 多少"。建议在 `MixCache`（`kudos-ability-cache-common/.../core/keyvalue/MixCache.kt`）统一埋 hit/miss/load 计数器，并把 Caffeine/Redis 原生统计接入 `CacheMetrics`。
- **广播热路径重复扫描容器**：`MixCache.pushMsgRedis` 每次写都调 `SpringKit.getBeansOfType<ICacheMessageHandler>()` 全量扫描；`HashCacheKit` 已对 handler 做过惰性索引，建议同样 memoize（注意测试上下文重置）。

### 健壮性
- **`CacheOperatorVo.doNotify` 的兜底不完整**：`kudos-ability-cache-common/src/io/kudos/ability/cache/common/notify/CacheOperatorVo.kt` 只捕获 `NoSuchBeanDefinitionException`；若 `notifyTool.notify` 抛出其它异常，本地清理 fallback 不会执行且异常直接抛入业务线程，与 KDoc 声称的"发送失败则本地清理"不符。建议扩大 catch 范围并记录日志。

## 本轮修复（装配 / 契约 / 正确性）

在上述审查之后又发现并修复了以下问题，均补有回归测试：

- **`@Tenant*` 注解族此前完全不生效**：`TenantCacheable` / `TenantCachePut` / `TenantCacheEvict` 的元注解被注释掉（`//@Cacheable` 等）却保留了 `@AliasFor(annotation = ...)`，导致 Spring Cache 根本不识别它们，且读取注解时会抛 `AnnotationConfigurationException`；三个对应切面与 `tenantCacheKeyGenerator` bean 也从未注册。已恢复元注解、按 Spring `@GetMapping` 的惯例修正 alias 映射，并在 `LinkableCacheAutoConfiguration` 中补齐 bean 注册。
- **共享 bean 由三个配置类重复声明**：Caffeine / Redis 两个 AutoConfiguration 都继承 `BaseCacheConfiguration`，与 `LinkableCacheAutoConfiguration` 竞争同一批 bean，胜者取决于 classpath 扫描顺序；而 `BaseCacheConfiguration` 版的 `cacheItemsProperties` 缺 `@ConfigurationProperties`，它一旦胜出 `cache-items` 就完全不绑定、一个缓存都建不出来。现由 `LinkableCacheAutoConfiguration` 单一持有；`BaseCacheConfiguration` 保留为业务侧可选 mixin 并已补齐等价性。
- **启动预热与缓存初始化存在顺序竞态**：两者同为 `SmartInitializingSingleton`，而 Spring 按注册顺序回调、不遵循 `Ordered`；预热若先跑则缓存尚未创建，写入被静默丢弃。现由 `MixCacheInitializing` 在缓存就绪后显式触发。
- **Redis 存储层**：关闭了 `allowRuntimeCacheCreation`（此前 `existsKey` 探测一个未配置的名字就会种下一个绕过全部定制的"野生缓存"）；`createCache` 改为从注入的默认配置派生，修复"未配 ttl 即永不过期"以及 keyPrefix 与 `evictByPattern` 分叉的隐患；`RedisTemplate` 改为显式注入，不再静态查找到错误的 redis 实例或序列化器；`ScanClearRedisCache` 与 `DrainingCaffeineCache` 补上了遗漏的 `invalidate()` 覆写。
- **Hash 缓存集合查询返回不完整结果**：`getById` 按条回填本地，而集合查询"本地非空即返回"，于是回填一条后 `listAll` 只返回那一条——是错误结果而非陈旧结果。集合读取改为以远端为准（`findByIds` 例外，它能按 size 自验完整性）。
- **`CaffeineHashCache` 的否定操作符语义反转**：`NE` / `LG` / `NOT_IN` / `LIKE` 族落入兜底分支后执行的是**相等**判定，查"不等于 X"反而只返回 X。改为委托 `OperatorEnum.compare`。
- **`AbstractByIdCacheHandler` 的缓存键类型不一致**：`reloadAll` / `syncOnXxx` 用原始 `PK`，而 `doReload` / `getByIds` / `@BatchCacheable` 都是 String 键；非 String 主键下 `1L` 与 `"1"` 是两个条目。已统一为 String（`PK=String` 时行为不变）。
- **`MixCache` 两条日志的占位符从未被替换**：`ILog` 经 `MessageFormat` 格式化，而消息里的 `node's` 撇号会开启引用段，吞掉其后的 `{0} {1} {2}`。这是全仓通用陷阱，目前仅这两处踩中。
- **`DistributedCacheGuardAspect` 类初始化即依赖 Spring 容器**：伴生对象在类初始化期读取 `LockTool.lockProvider`，导致该类光被加载（类路径扫描、AOT、测试）就抛 `applicationContext is not initialized`。改为 `by lazy`。
- **新增装配完整性测试**：类路径扫描本模块所有 `@Aspect`，断言每个都有 `@Bean` 注册；并断言注解默认引用的各 keyGenerator bean 名真实存在。"注解发布了但 bean 没注册"这类病史（`DistributedCacheGuardAspect` 曾犯、租户切面又犯）由此被永久堵住。
- **`@BatchCacheable` 名副其实了**：`IKeyValueCacheManager` 新增 `multiGet`，Caffeine 走 `getAllPresent`、Redis 走一次 `MGET`（key 由 `RedisCache` 自身的推导逻辑产出，不复制前缀/转换代码），`MixCacheManager` 按策略分派并只为本地未命中的 key 访问远端。此前批量抽象底下是逐键读取，N 个 key 就是 N 次往返。不具备批量能力的实现会退化为逐键读取而非整批判未命中。
- **批量路径的命中判定改为看 key 是否存在**：旧实现用 `null` 同时表示"未命中"和"命中且值为 null"，导致负缓存在批量路径上完全失效。返回值仍然剔除 null（被注解方法声明的是值非空的 Map）。另新增 `@BatchCacheable(cacheNullForMissingKeys = true)` 可选开启"源端没有的 id 也缓存"，默认关闭。
- **不再靠切分字符串反推批量参数**：key 是按位置逐一生成的，直接按下标从原始参数取元素即可。旧实现按 `::` 切分 key 再做类型还原，参数值本身含 `::` 时会切错并把错误的值传给业务方法。

### 跨服务协商缓存（interservice）

- **cacheKey 现在区分被调服务**：`RequestInterceptor` 在 `Target.apply(template)` 之前运行，此时 `request.url()` 只有路径没有 host，而拼进 key 的 `applicationName` 是**调用方自己**的名字，同一 JVM 内所有 Feign client 都一样。于是 `serviceA.get("/config")` 与 `serviceB.get("/config")` 会算出同一个 key：一方的响应覆盖另一方，304 判定还会让 B 读到 A 的数据。现在把 `feignTarget()` 的服务名与 URL 纳入 key（读取方式是防御性的，Feign 若不再填充也不会影响调用）。顺带修掉了 `joinToString()` 用默认 `", "` 分隔、把分隔符常量当数组元素混在里面的拼接方式。
- **provider 端不再被下游 Filter 包装后静默失效**：原先用 `request !is CacheClientRequest` 判定，而 `ClientCacheWebFilter` 之后任何再包一层的 Filter（Spring Security 的 `SecurityContextHolderAwareRequestFilter` 必定会包）都会让该判定失败，整个服务端逻辑无声关闭、不报错也无日志。改用 `WebUtils.getNativeRequest` 沿包装链查找。同时把 `resolveReference(...) as HttpServletRequest` 改为 `as?`——请求已完成、异步派发或非 Servlet 环境下它会返回 null 或别的类型。
- **304 命中但本地条目已淘汰时改为显式失败**：`FEIGN-CACHE` 区是有界的（默认 `maximumSize=150`），请求往返期间条目被挤掉很常见；此时服务端因为我们上报了 uid 只回了空 body，既无 body 可解码也无本地副本。旧实现返回 null，而 Feign 接口在 Kotlin 里通常是不可空返回类型，这个 null 会流进业务代码在无关位置炸掉。现在抛带诊断信息的 `DecodeException`，且重试必定成功（本地无条目 → 下次不带 uid → 服务端回完整 body）。
- **指纹生成不再能把接口打成 500，退化指纹会被拒绝**：`genUid` 序列化任意返回类型，可能抛异常——一个纯优化的切面不该有能力让业务接口失败，现在捕获后跳过协商、原样返回。另外对"序列化不出任何内容"的类型直接拒绝生成指纹：否则该类型所有实例共享一个 UID，provider 会一直回 304、客户端持续读到陈旧数据且无任何信号。
  注意仍有一处**已知残留风险**（实测确认并写入 KDoc 与测试）：访问器抛异常时反射兜底会把属性置为 null 而非上抛，JSON 结构仍在，因此绕过上述守卫，同类型实例仍会共享 UID。不加"全字段为 null 即拒绝"的启发式是因为会误伤合法的全空 DTO；正确的规避方式是不要从 `@ClientCacheable` 接口返回访问器会抛异常的 DTO。
- **`KudosContextHolder.get()` 改为 `getOrNull()`**：Feign 跑在 HTTP 客户端 / `@Async` / 舱壁线程池上，`get()` 在无绑定时会新建上下文并 set 进 `InheritableThreadLocal`，既在池线程上留下永不回收的上下文（该类自己的 KDoc 就警告了这点），又让 `tenantId` 退化为空、把不同租户算到同一个 cacheKey 上。
- 每响应两条 INFO 日志降为一条 DEBUG。

### Hash 缓存

- **hash 缓存支持 TTL 了（本地 + 远端）**：`CacheConfig.ttl` 此前对 `hash = true` 的缓存项完全不起作用——只有容量上限、没有过期，一旦某次失效广播丢失（pub/sub 是 fire-and-forget），脏数据就永久驻留、没有任何东西能让它收敛。本地由 `CaffeineHashCache` 应用 `expireAfterWrite`；远端在 `IdEntitiesRedisHashDao` 上新增 `expireAll(dataKeyPrefix, ttl)`，由 `RedisHashCache` 在 save / saveBatch / refreshAll 之后调用。未配 ttl 时两侧都维持原先的不过期行为，零开销。

  远端这一项的关键是**必须覆盖该区域的全部 key**：一个 hash 缓存对应主数据 key 加上每个可筛选/可排序属性的 Set/ZSet 索引 key，而只有 DAO 知道这套布局。只给主 key 设过期会更糟——索引会活得更久并继续吐出实体已消失的 id，`listBySetIndex` 于是返回幻影成员。因此接口加在 DAO 层（它用 SCAN 枚举自己的索引 key），而不是让缓存模块去猜 key 格式；这与 Redis `MGET` 复用 `RedisCache.createCacheKey` 是同一个原则。

  配置 hash 缓存的 ttl 前需知道两点：TTL 在每次写入后重新施加，所以区域存活时间是"自**最后一次写入**起 ttl"，而非自首次填充起；且 hash 缓存**没有 load-on-miss 路径**，区域一旦过期，查询会返回空直到有东西重新填充（`writeOnBoot` 预热、显式 `reloadAll` 或普通写入）——ttl 限定的是陈旧上界，它不负责安排刷新。
- **二级索引的并发一致性**：`removeFromIndexes` 原先用 `entries.removeIf { ids.remove(id); ids.isEmpty() }`，谓词执行期间不持锁，另一线程刚重建并加入的 id 会随整个桶一起被删掉——实体还在主存里却从索引消失。改为逐桶 `computeIfPresent`，与 `compute` 形式的写入在同一把桶锁上互斥。另外给"清索引→写主存→重建索引"这组三步操作加了按 id 分条带的锁（64 条带），避免同一 id 的并发写交错后把实体同时挂在新旧两个属性值下。均有并发回归测试。
- **`clearLocal` 不再让并发写入凭空消失**：原先 `mainData.remove(cacheName)` 会摘掉实例，此刻已持有旧引用的写线程继续往一个没人再读的对象里写，随后 `main()` 重建空实例，这些写入无声消失。改为原地清空。
- **`entityClass` 参数真正生效了**：`findByIds` / `listAll` 里的 `as? E` / `as E` 因泛型擦除**完全不做运行时检查**，类型不符的值会被原样返回、在调用方某处炸成 `ClassCastException`；而 Redis 实现是按 `entityClass` 反序列化的，两级行为不一致。现在按类型校验，不符则记 WARN 并视为未命中（LOCAL_REMOTE 下会落到权威的远端）。
- **注解 KDoc 补充了写回门槛**：`@HashCacheableByPrimary` / `@HashCacheableBySecondary` 的 miss 写回被默认 `false` 的 `writeInTime` 闸住，与 Spring `@Cacheable` 的直觉相悖——"标了注解却每次都回源"通常就是漏配了这一项。

- **二级索引属性改为声明式**：`MixHashCache` 的索引属性集此前初始为空、由"最近一次写入"覆盖，索引结构因而成了调用时序的函数——只读过没写过的进程，回填出来的是没有二级索引的实体。现在由 `AbstractHashCacheHandler` 新增的 `exposedFilterableProperties()` / `exposedSortableProperties()`（与既有的 `exposedEntityClass()` 同一模式）在启动时提供基线，`MixHashCacheManager` 按 cacheName 汇总注入。

  运行期捕获保留为补充，但语义从"覆盖"改为"取并集"：`IHashCache` 的多个方法对这两个参数都默认空集，覆盖语义下一次窄集合的写入就会让缓存忘掉其他调用方仍在查询的索引。纯注解用法（无 handler）不受影响，退化为原先的从首次写入学习。

另外两处已随后修掉：

- **`decoderEnabled` 死字段已删除**：真正生效的是 `@ConditionalOnProperty` 直接读环境里的 `decoder-enabled`，而条件在任何 bean 存在之前就已求值，属性字段不可能左右它。字段在时它像个能用的开关——写 yml 有效（条件读的是同一个 key），用 Java API 设值却静默无效。开关本身通过 yml 继续可用。
- **新增 `requireFeignMarker`（默认 `false`）**：开启后，只有带内部 Feign 标记（`_feign_request`）的请求才参与协商，外部调用者拿不到响应指纹。**默认保持现状是刻意的**——该标记由 `kudos-ability-distributed-client-feign` 的 `GlobalHeaderRequestInterceptor` 写入，而本模块对它只有 compileOnly/test 依赖，运行时不保证存在；默认开启会在这类部署里静默关掉整个 provider 端。确认所有调用方都经过该拦截器后再开。

- **Feign decoder 改为复用 Spring Cloud 的解码链**：此前内层是裸 `JacksonDecoder`，等于把 Spring Cloud 的解码**整个换掉**而非装饰。由于本 bean 是 `@Primary` 且位于父上下文，Spring Cloud 自己那个 `@ConditionalOnMissingBean` 的 decoder 在任何 Feign client 子上下文里都不会被创建——也就是说只要 classpath 上有本模块，应用内**每一个** `@FeignClient` 的反序列化行为都变了：自定义 `HttpMessageConverter`（XML、protobuf、自定义 media type）失效，Spring 的 Jackson 配置与 `@JsonView` 也失效；该 decoder 还把 404 映射为 null 吞掉错误、`String` 硬编码 UTF-8 而忽略 `response.charset()`。现在内层换成 `SpringDecoder`，链路与 `FeignClientsConfiguration.feignDecoder` 完全一致，只有最外层的 `FeignCacheResponseInterceptor` 是本模块的。已无人使用的 `JacksonDecoder` 一并删除。

  注意本 bean 仍是**抑制**而非包装 Spring Cloud 的 decoder bean——两者按构造就是互斥的（`@ConditionalOnMissingBean` 会搜索祖先上下文），所以只能在这里搭出等价的链。另外 `SpringDecoder` 依赖的 `FeignHttpMessageConverters` 只声明在 Feign client 的子上下文里、父上下文取不到，因此本模块自行声明了该 bean（它从 customizer 自行构建转换器集，不是包装既有 bean，故结果等价）。这一点是被 provider 侧的端到端测试抓出来的。

interservice 侧的两项已处理：

- ✅ **热路径的 apr1 慢哈希**（已修复）：`Md5Crypt.apr1Crypt` 是为抗口令爆破设计的慢哈希，固定 1000 轮 MD5，且每轮参与哈希的明文包含完整请求体（1MB 的 POST 约等于 GB 量级哈希运算）。已改为 `DigestKit.getMD5` 一次性摘要、并按原始字节而非 `joinToString()` 拼装。注意这**改变了 cacheKey 取值**，升级后全集群既有本地协商缓存会同时失效、集体冷启动一次。
- ✅ **缺少 per-client 开关**（已修复）：拦截器注册为全局 `RequestInterceptor`，Spring Cloud 会应用到**每一个** Feign client，包括调用第三方外部 API 的那些。现在可用 `kudos.ability.cache.interservice.client.include-clients` / `exclude-clients` 按 `@FeignClient` 名筛选，`exclude` 优先；两者都为空时维持全量生效的旧行为。

以下一项**仍未处理**，属用法权衡而非代码缺陷：

- **provider 侧是净增开销**：每个 `@ClientCacheable` 请求都要把返回对象完整 JSON 序列化一次算指纹，未命中时 MVC 再序列化一次，且 Controller 方法始终完整执行（不像 ETag 能配合 `@Cacheable` 短路）。净收益只有回程 body 字节与客户端反序列化，服务端 CPU 纯增加——不适合用在计算昂贵但响应体很小的接口上。
