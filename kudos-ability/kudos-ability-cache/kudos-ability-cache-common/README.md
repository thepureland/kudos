# kudos-ability-cache-common

Kudos 缓存框架的核心模块（60 个源文件）。提供：

1. **两级缓存抽象**：`MixCache` / `MixCacheManager`——业务侧只看到 Spring `Cache` API，下面
   自动按策略走本地（Caffeine）/ 远程（Redis）/ 两级联动
2. **Hash 缓存抽象**：`MixHashCache` / `MixHashCacheManager`——为"按 id 拆 hash field"的
   带主键集合提供专用 API（不是 Spring `Cache` 接口）
3. **租户感知注解**：`@TenantCacheable` / `@TenantCacheEvict` / `@TenantCachePut` / `@TenantCaching`
   一套，自动按 `KudosContextHolder.tenantId` 拆 key
4. **分布式互斥**：`@DistributedCacheGuard` 注解 + Aspect，防止缓存穿透 stampede
5. **批量缓存**：`@BatchCacheable` 一次性查多个 key，未命中部分回源后合并写回
6. **跨节点失效广播**：`CacheMessage` + `ICacheMessageHandler` SPI（Redis pub/sub /
   MQ 各自子模块实现）
7. **配置驱动**：`kudos.ability.cache.cache-items[]` 字符串列表声明各缓存项的策略 / ttl
8. **版本隔离**：`CacheVersionConfig`——所有 key 自动加 `<version>:` 前缀，蓝绿 / 数据迁移可并存

## 设计要点

### 两级缓存读写规则（`MixCache`）

| 操作 | SINGLE_LOCAL | REMOTE | LOCAL_REMOTE |
|---|---|---|---|
| `get(key)` | local.get | remote.get | local 命中即返回；未命中查 remote 并回填 local |
| `put(k, v)` | local.put | remote.put | **先 remote 后 local**，再广播失效 |
| `evict(k)` | local.evict | remote.evict | 先 remote 后 local，再广播 |
| `putIfAbsent(k, v)` | local.pIA | remote.pIA | remote.pIA 成功 → 本地 pIA + 广播；已存在 → 本地 put（回填，不广播） |
| `clear()` | local.clear | remote.clear | 同 put，notifyKey = null |

`LOCAL_REMOTE` 的写入约定 **先远端后本地**：如果广播 finally 链路失败，本节点本地至少与
远端版本一致（不是本地新、远端老）；后者在分布式失败语义下更难解释。

### 防穿透 + 防 stampede

`@DistributedCacheGuard` 在缓存未命中走源时获取一把分布式锁（Redisson），其他线程 / 节点
等待持锁线程把结果写回缓存后**再读一次缓存**而不是各自回源。锁失败回退到正常回源（避免锁
故障让整个调用挂死）。

### 租户感知 key 生成

`TenantCacheKeyGenerator` / `ContextKeyGenerator` 把当前 `KudosContext.tenantId` 拼到 key
里——业务方法不需要手动加租户 ID。`TenantAdvancedCacheable` 在此之上提供按"租户 + 服务码 + 模式"
解析的高级形式，搭配 `kudos-ability-data-rdb-jdbc` 的动态数据源用。

### 配置驱动（`cache-items`）

```yaml
kudos:
  ability:
    cache:
      version: v2            # 缓存版本；所有 key 都会加 v2: 前缀
      enabled: true
      remoteStore: data      # 远程缓存所用的 redis 实例名
      cache-items:
        - name=USER_CACHE&strategy=LOCAL_REMOTE&ttl=900
        - name=DEMO&strategy=REMOTE&ttl=1800&writeOnBoot=true
```

字符串列表而非嵌套 yml object 是经过权衡的——长列表更紧凑、好 diff，代价是失去 IDE 字段
提示。解析见 `DefaultCacheConfigProvider.cacheItemToConfig`：按 `&` 拆 token、`=` 拆 kv、
反射 set 到 `CacheConfig` 字段。

### `CacheConfig.strategy` vs `strategyDictCode`

历史问题：`strategy` 来自 yml / 代码，`strategyDictCode` 来自 DB 字典码——读侧到处写
`config.strategy ?: config.strategyDictCode`。`resolvedStrategyCode` / `resolvedStrategy`
派生属性把兜底集中到一处。新代码必须用派生属性，不要再直接读两个原始字段。

### 跨节点失效消息

`MixCache.pushMsgRedis(...)` 调所有 `ICacheMessageHandler` bean 的 `sendMessage`。具体传输
方式在 `kudos-ability-cache-interservice-{redis, mq, ...}` 子模块；本模块只定义 SPI。

接收端流程：transport 层（Redis SubscribingMessageListener / MQ consumer）→ 调
`handler.receiveMessage(msg)` → handler 实现把 cacheName + key 委派给 `MixCacheManager.clearLocal(...)`。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/LinkableCacheAutoConfiguration` | 装配入口（cacheManager / aspect / keyGenerator 等 12+ bean） |
| `init/BaseCacheConfiguration` | 基础配置 mixin（业务可继承自定义） |
| `init/properties/CacheItemsProperties` | `cache-items[]` 字符串列表配置类 |
| `init/properties/CacheVersionConfig` | 缓存版本 + 失效广播 channel |
| `core/AbstractCacheHandler` | 业务自定义 handler 基类（含 `getSelf()` 自代理解决方案） |
| `core/keyvalue/MixCache` + `MixCacheManager` | 两级缓存核心 |
| `core/hash/MixHashCache` + `MixHashCacheManager` | Hash 形缓存核心 |
| `core/CacheDataInitializer` / `MixCacheInitializing` | 启动期数据预热 |
| `aop/keyvalue/Tenant*` | 4 个租户感知注解 + 对应 4 个 Aspect |
| `aop/keyvalue/DistributedCacheGuard` + Aspect | 缓存穿透防护（分布式锁） |
| `aop/hash/HashCacheableBy*` | Hash 缓存读注解 + Aspect |
| `batch/keyvalue/BatchCacheable` + Aspect + `IKeysGenerator` | 批量 K-V 缓存 |
| `batch/hash/HashBatchCacheableByPrimary` + Aspect + `DefaultHashBatchKeysGenerator` | 批量 Hash 缓存 |
| `kit/KeyValueCacheKit` / `HashCacheKit` / `TenantCacheTool` | 业务侧便捷调用入口 |
| `notify/CacheMessage` / `CacheNotifyListener` / `CacheOperatorVo` / `ICacheMessageHandler` | 跨节点失效广播抽象 |
| `support/CacheConfig` | 单个缓存项的配置 POJO |
| `support/CacheKey` | 自定义 key 注解（与 Spring `@Cacheable.key` 语义一致） |
| `support/SpelExpressionCache` | SpEL 解析结果进程级缓存（热路径优化） |
| `support/CacheValueWrapper` | "已查过但值是 null" vs "未查过"的表达 |
| `support/CacheCleanRegister` + `ICacheCleanListener` | 缓存清理回调注册 |
| `support/ContextKeyGenerator` / `TenantCacheKeyGenerator` | 上下文感知 key 生成器 |
| `support/DefaultCacheConfigProvider` + `ICacheConfigProvider` | `cache-items` 解析 + 按策略分组的快表 |
| `support/IHashCacheSync` | Hash 缓存"读未命中→回源→写回"模板接口 |
| `enums/CacheStrategy` / `CacheHandleType` | 策略 / 操作类型枚举 |

## 测试覆盖

- `SpelExpressionCacheTest`（4 case）——新增，纯单元测试，回归 SpEL 缓存命中 / 不同表达式
  独立 / 表达式求值正确性
- 其余 60 个源文件**当前没有专门测试**；下游 cache-local-caffeine / cache-remote-redis 模块
  的集成测试间接覆盖了 `MixCache` / `MixCacheManager` 大部分行为

补齐测试是后续重点工作之一；先把"哪里没测"在 README 列出来，方便有空时按优先级补：
- `MixCache` 各策略下 get / put / evict / putIfAbsent 的写入顺序与广播
- `DistributedCacheGuardAspect` 锁失败的回退路径
- `BatchCacheableAspect` / `HashBatchCacheableByPrimaryAspect` 的"半命中合并"
- `CriteriaRedisResolver` 同款的"组间 AND、组内 OR"在 `TenantCachingAspect` 的解析
- `CacheVersionConfig.getFinalCacheName` / `.getRealCacheName` 的空版本 / 已带前缀回填行为

## 已知限制 / 后续工作

- ❗ **测试覆盖率极低**：60 个源文件仅 1 个单测（本次补的）。Aspect 层 / Manager 层均
  无单测，依赖下游模块的集成测试间接覆盖
- ❗ `MixCache.pushMsgRedis` 在写路径上同步调用所有 `ICacheMessageHandler.sendMessage`；
  广播如果阻塞会拖累写延迟。源码里已用 `//TODO 异步?` 标过。需要改成 fire-and-forget 时
  应慎重处理失败补偿
- ❗ `CacheConfig.strategy` / `.strategyDictCode` 双字段并存是历史遗留，已用 `resolvedStrategy`
  派生属性集中，但两个原始字段仍可被代码直接读到。未来收口需要先排查所有调用方
- ❗ `cache-items` 用字符串列表 + 反射 setProperty 解析——失去 IDE 提示和编译期校验，
  错配字段名要等到启动时才发现
- ❗ `LOCAL_REMOTE` 策略下"写入失败但广播成功"的语义未定义；当前实现是"先 remote 后 local"，
  remote 失败时不会广播，local 失败时会继续广播——可能导致其他节点剔除本地但本节点
  仍有旧值
- ❗ `AbstractCacheHandler.selfProxy` 用 `SpringKit.getBean(this::class)`——子类如果有多个
  bean 实例（同型多 bean）会爆 NoUniqueBeanDefinitionException
- ❗ 文档大量集中在源码 kdoc 中（一些类的 kdoc 长达 50+ 行），适合配 KDoc 渲染工具
  生成静态站点；README 仅做导航

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify:kudos-ability-distributed-notify-common"))
api(libs.spring.context.support)
api(libs.spring.boot.starter.cache)

testImplementation(project(":kudos-test:kudos-test-container"))
```
