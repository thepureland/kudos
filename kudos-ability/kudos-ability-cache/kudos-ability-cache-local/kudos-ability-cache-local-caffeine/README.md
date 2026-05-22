# kudos-ability-cache-local-caffeine

Caffeine 作为本地缓存实现接入 kudos cache 框架。提供：

1. **K-V 本地缓存**：`CaffeineKeyValueCacheManager` 在 `MixCacheManager` 装配链中担任
   `localCacheManager`，跟远程 Redis 配合做两级缓存
2. **Hash 本地缓存**：`CaffeineHashCache` 实现 `IHashCache` + `IHashCacheSync` SPI，
   作为带 id 集合 Hash 缓存的本地副本
3. **同步 invalidate 行为**：`DrainingCaffeineCache` 在 `evict` / `clear` 后立即
   `cleanUp()`，消除"清完还能短暂读到"的异步生效问题
4. **spec 兜底**：`ensureSizeBound` 在缺 `maximumSize/maximumWeight` 时自动加默认上限，
   防止业务漏配把本地缓存跑成无界 `LinkedHashMap`

## 设计要点

### 装配顺序

`@AutoConfigureBefore(LinkableCacheAutoConfiguration::class)` 让本模块的 bean 先于
`MixCacheManager` 注入——后者按 bean name `localCacheManager` 取本地实现。这个注解被
kudos 自定义的 SPI 调度器 `ComponentInitializationDispatcher` 识别，**不是** Spring Boot
默认 SPI 的 no-op 用法。

### `DrainingCaffeineCache`：为什么需要

Caffeine 的 `invalidate(key)` 是**同步**生效（立即 `asMap().remove(key)`），但
`invalidateAll()` 把所有清理标记排进队列，由后续 maintenance cycle 异步执行。**结果**：
紧跟 `clear()` 之后的 `get(key)` 还可能命中刚被标记失效的副本。这在 `@Cacheable.get`
路径上特别明显（直接走 `nativeCache.asMap()`）。

解决：覆盖 `evict` / `clear`，在 super 调用后立刻 `native.cleanUp()` 强制刷队列。这套
逻辑由 0f645e8e 提交里的 `ResourceIdsByTenantIdAndGroupCodeCacheTest.syncOnRoleResourceChange`
失败用例反向推出来的——参见源码注释引用。

### spec 兜底

`spring.cache.caffeine.spec` 由 yml 配置：

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=150,expireAfterAccess=3600s,recordStats
```

`CaffeineKeyValueCacheManager.ensureSizeBound` 在 `createCache` 前用正则查 spec 里有没有
`maximumSize=` / `maximumWeight=`；缺则自动补 `maximumSize=10000` 并 warn 日志。**生产
看到 warn 请立即补全 spec 而不是接受兜底值**——10000 不是科学数字，只是比无界好。

### `CaffeineHashCache` 索引模型

仿 Redis Hash + Set + ZSet 的结构本地化：

```
mainData[cacheName][id]                  → Entity
setIndex[cacheName][set:prop:value]      → Set<idStr>
zsetIndex[cacheName][zset:prop]          → Map<idStr, score(Double)>
```

API 与 `IdEntitiesRedisHashDao` 同形：`save` / `saveBatch` / `getById` / `findByIds` /
`listBySetIndex` / `listPageByZSetIndex` / `list(criteria, ...)` / `refreshAll` /
`clear(cacheName)` / `evictLocal(cacheName, id)`。

### ZSet score 下界陷阱

`toDouble(value)` 的回退值用 `-Double.MAX_VALUE` 而非 `Double.MIN_VALUE`——后者是最小**正**
数，会把负值排成"比 fallback 还大"，排序错位。这个坑跟 `memdb-redis` 模块同款，已修。

## 配置示例

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=150,expireAfterAccess=3600s,recordStats

kudos:
  ability:
    cache:
      local:
        caffeine:
          hash:
            maximum-size: 10000
```

`expireAfterWrite` 由每个 cache item 单独的 `ttl` 决定，spec 里的 `expireAfterAccess` /
`expireAfterWrite` **会被覆盖**——这是有意的：单缓存级 TTL 才是业务侧最常调整的旋钮。

`kudos.ability.cache.local.caffeine.hash.maximum-size` 控制单个 Hash cacheName 下的本地
实体上限，默认 10000。主数据达到上限被 Caffeine 驱逐时，会同步清理 Set/ZSet 二级索引，
避免索引返回已不存在的 id。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/CaffeineCacheAutoConfiguration` | 装配入口，注册 `localCacheManager` + `caffeineIdEntitiesHashCache` |
| `CaffeineKeyValueCacheManager` | K-V 缓存管理器（被 `MixCacheManager` 取作本地层） |
| `DrainingCaffeineCache` | `CaffeineCache` 薄包装，保证 evict/clear 立即生效 |
| `CaffeineHashCache` | Hash 缓存本地实现 + IHashCacheSync 回调 |

## 测试覆盖

- `LocalHashCacheTest`（25 case）+ `HashBatchCacheableTest`（4）+ `NoHashCacheTest`
- `LocalCacheTest`（2）+ `BatchCacheableTest`（1）+ `NoCacheTest`
- 34 测试全绿；覆盖了 `MixCacheManager` 在本地 + caffeine 装配下的端到端行为

## 已知限制 / 后续工作

- ✅ `CaffeineHashCache` 主数据已改为按 cacheName 分桶的 Caffeine cache，并通过
  `maximum-size` 配置兜底；驱逐、删除、覆盖写入都会清理 Set/ZSet 二级索引
- ❗ `CaffeineHashCache.getPropertyValue` 用 Java 反射，对 Kotlin data class 性能不如
  KProperty.get；与 `IdEntitiesRedisHashDao.getPropertyValue` 重复实现——属于值得提到
  cache-common 的候选
- ❗ `CaffeineHashCache.list` 仅第一个 `Order` 参与排序；多字段排序需应用层处理（与
  Redis 实现保持一致）
- ❗ `CaffeineHashCache.matchesCriterion` 用 `toString()` 比较 EQ，丢失类型保真度
  （Long 1 vs String "1" 会被判等）；目前业务侧依赖这个宽松行为，但要谨慎
- ❗ `ensureSizeBound` 的默认 10000 不科学，只是兜底；生产 warn 出现请立即补全 spec
- ❗ `evictByPattern` 全表扫描 + 正则匹配——缓存条目数大时性能差，需要时考虑维护辅助索引

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
api(libs.github.caffeine)

testImplementation(project(":kudos-test:kudos-test-common"))
```
