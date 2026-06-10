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
7. **配置驱动**：`kudos.ability.cache.cache-items[]` 字符串列表或
   `cache-item-configs[]` 结构化列表声明各缓存项的策略 / ttl
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
      cache-item-configs:
        - name: ORDER_CACHE
          strategy: LOCAL_REMOTE
          ttl: 900
```

`cache-items` 字符串列表保留用于兼容既有配置：长列表更紧凑、好 diff，但失去 IDE 字段提示。
新配置建议优先用 `cache-item-configs` 结构化对象，让 Spring Boot binder 直接绑定到
`CacheConfig` 字段。旧字符串解析会在启动期校验未知字段、缺 `name` / `strategy`、非法
`strategy` 和错误 token 格式，避免错配字段静默生效。

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
| `init/properties/CacheItemsProperties` | `cache-items[]` 字符串列表 + `cache-item-configs[]` 结构化配置类 |
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

## 常用接入示例

### 租户感知 K-V 缓存

```kotlin
@TenantCacheable(cacheNames = ["USER_CACHE"], suffix = "profile")
open fun getUserProfile(id: Long): UserProfile? {
    return userRepository.findProfile(id)
}
```

`TenantCacheable` 默认使用 `tenantCacheKeyGenerator`，会把当前 `KudosContext.tenantId`
纳入 key；业务方法不需要手动拼租户 ID。

### 批量 K-V 缓存

```kotlin
@BatchCacheable(cacheNames = ["USER_CACHE"], valueClass = UserProfile::class)
open fun listUserProfiles(ids: Collection<Long>): Map<String, UserProfile> {
    return userRepository.findProfiles(ids).associateBy { it.id.toString() }
}
```

返回值必须是 `Map`，key 为缓存 key；未命中部分回源后只写回新增结果。

### 缓存穿透保护

```kotlin
@DistributedCacheGuard
@TenantCacheable(cacheNames = ["USER_CACHE"])
open fun getUserProfileGuarded(id: Long): UserProfile? {
    return userRepository.findProfile(id)
}
```

缓存未命中时同 key 调用会先竞争分布式锁；未拿到锁的调用等待持锁方写回后再读一次缓存，
锁异常时回退为直接回源。

### Hash 缓存

```kotlin
@HashCacheableByPrimary(
    cacheNames = ["USER_HASH"],
    key = "#id",
    entityClass = UserEntity::class,
    filterableProperties = ["status"],
    sortableProperties = ["createdAt"]
)
open fun getUserEntity(id: Long): UserEntity? {
    return userRepository.findEntity(id)
}
```

对应缓存项必须配置 `hash=true`；主属性用于 `getById`，副属性用于等值筛选或范围 / 排序索引。

## 测试覆盖

- `SpelExpressionCacheTest`——SpEL 缓存命中 / 表达式隔离 / 求值正确性
- `DefaultCacheConfigProviderTest`——字符串配置、结构化 `cache-item-configs`、字段 / token /
  strategy 启动期校验、策略分组、hash 视图、默认值
- `CacheConfigTest` / `CacheVersionConfigTest`——派生策略和版本化 cache name 行为
- `MixCacheTest`——LOCAL_REMOTE 写入顺序、远端 / 本地 / 广播失败语义、`putIfAbsent`
- `DistributedCacheGuardAspectTest`——锁成功、锁失败回退、二次读缓存
- `BatchCacheableAspectTest`——批量缓存半命中、回源合并、空 key 处理
- `AbstractCacheHandlerTest`——`selfProxy` 默认按类型查找与 `selfBeanName()` 覆盖

仍需补齐的重点：
- `HashBatchCacheableByPrimaryAspect` / Hash cache 注解族的"半命中合并"
- `CriteriaRedisResolver` 同款的"组间 AND、组内 OR"在 `TenantCachingAspect` 的解析
- 租户感知 key 生成与 `TenantCacheable` / `TenantCachePut` / `TenantCacheEvict` 的组合行为

## 已知限制 / 后续工作

- ✅ **测试覆盖已提升**：本模块已有 8 个测试类，覆盖 Provider / Config / MixCache /
  DistributedCacheGuardAspect / BatchCacheableAspect / AbstractCacheHandler 等核心行为。
  Hash 注解族和租户组合场景仍需继续补
- ✅ `MixCache.pushMsgRedis` 已改为 fire-and-forget——每个 handler 的 `sendMessage`
  入队到模块级共享的 daemon 线程池（core=1 / max=cpu / queue=1024 / `CallerRunsPolicy`），
  发送失败仅 WARN 日志、不重试（重复广播比偶发丢一条更危险，下游收到则只是丢本地副本）。
  队列爆满会回退到 caller-runs 同步，宁可拖慢写也不丢消息
- ✅ `CacheConfig.strategy` / `.strategyDictCode` 双字段——所有读侧已收口到 `resolvedStrategy`/
  `resolvedStrategyCode` 派生属性（`DefaultCacheConfigProvider.initCacheConfig` 是最后一处
  raw reader，已迁移）。两个原始字段保留以支持 DB 反序列化 + yml 绑定两条写入路径
- ✅ `cache-items` 字符串列表已提供结构化替代：新增 `cache-item-configs[]`，可直接按
  `CacheConfig` 字段写 yml；旧字符串格式继续兼容，并在启动期显式校验未知字段、缺
  `name` / `strategy`、非法 `strategy` 和错误 token
- ✅ `LOCAL_REMOTE` 写失败语义已显式定义并测试：
  - **远端失败** → 异常上抛 / 不写本地 / 不广播（整网保持原值，一致）
  - **本地失败** → catch + WARN 日志 / 广播仍发出（远端是真相源；其他节点 invalidate
    本地副本会从远端读到新值；本节点本地降级，待下次 miss / TTL 自愈）
  - **广播失败** → fire-and-forget 异步，单 handler 失败不影响整体（同 [#1]）
- ✅ `AbstractCacheHandler.selfProxy` 支持 `selfBeanName()` override——多 bean 场景子类
  override 该方法返回显式 bean 名即可避开 `NoUniqueBeanDefinitionException`，默认仍按类型唯一查
- ✅ README 已补充配置、核心行为、测试范围和常用接入示例；源码 KDoc 仍保留更完整的参数
  细节，后续如需对外发布可再接入 KDoc 静态站点生成

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify:kudos-ability-distributed-notify-common"))
api(libs.spring.context.support)
api(libs.spring.boot.starter.cache)

testImplementation(project(":kudos-test:kudos-test-container"))
```

## 改进建议（自动分析 2026-06-11）

本次审查已直接修复：`CacheVersionConfig.getRealCacheName` 的 replace-all / 空版本误剥前缀 bug、
`MixCacheManager.evictByPattern` 的本地层前缀名 no-op 与 LOCAL_REMOTE 本节点本地不失效 bug、
`DefaultHashBatchKeysGenerator.generateKeys` 多参数路径的越界 / 集合 toString 拼 key bug。
以下为不宜直接修改的遗留项：

### 安全性
- **租户 ID 缺失时的命名空间合并**：`kit/TenantCacheTool.kt` 的 `getTenantKey` 在 `tenantId == null`
  时生成 `"null::key"` 前缀——所有无租户上下文的调用方共享同一命名空间，存在互相读到对方数据的隐患。
  建议 null 时 fail-fast 或使用独立保留命名空间。
- **SpEL 字符串拼接**：`support/TenantCacheKeyGenerator.kt` 用
  `"#tenantId.concat('::').concat($key)"` 字符串拼接构造 SpEL。`key`/`suffix` 来自注解（开发者可控），
  风险有限，但仍是表达式注入面；建议改为"先求值 suffix 表达式、再在 Kotlin 侧拼接前缀"的程序化方式。

### 一致性 / 可维护性
- **批量 key 生成器语义分裂**：`batch/keyvalue/DefaultKeysGenerator.kt` 是 positional-zip（等长校验、
  拒绝笛卡尔积），`batch/hash/DefaultHashBatchKeysGenerator.kt` 是笛卡尔积。两者实现同一个
  `IKeysGenerator` 接口但语义相反，业务侧在两类注解间迁移时极易踩坑。建议统一语义，或至少在两个注解的
  KDoc 中醒目标注差异。
- **`MixCacheManager` 配置缺失时的静默降级**：`loadLocalCacheConfig`/`loadMixCacheConfig` 中
  `localCacheManager.getCache(realKey)` 可能返回 null 并构造出 `MixCache(strategy, null, null)`，
  错误延迟到首次访问时才以 `requireNotNull` 暴露。建议装配期即校验并列出缺失项。

### 测试覆盖缺口
- `MixCacheManager.evictByPattern` / `clearLocal`（本次修复的两个分支均无回归测试）。
- `DefaultHashBatchKeysGenerator` 多参数笛卡尔积路径（本次修复，无任何测试）。
- `TenantCacheTool`（租户 key 拼接、clear 仅清本租户语义）与 `CacheNotifyListener`（策略闸门、双类型分发）。
