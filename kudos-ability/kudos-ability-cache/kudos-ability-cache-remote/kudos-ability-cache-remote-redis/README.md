# kudos-ability-cache-remote-redis

Redis 作为远程缓存接入 kudos cache 框架。提供：

1. **K-V 远程缓存**：`RedisKeyValueCacheManager` 在 `MixCacheManager` 装配链中担任
   `remoteCacheManager`
2. **Hash 远程缓存**：`RedisHashCache` 实现 `IHashCache`，作为带 id 集合 Hash 缓存的远端存储
3. **跨节点失效广播**：`RedisCacheMessageHandler` 实现 `ICacheMessageHandler` SPI，
   走 Redis pub/sub 把失效消息扩散到其它节点
4. **Hash 远程读写处理器**：`RedisRemoteCacheProcessor` 给 `TenantAdvancedCacheableAspect`
   等 Aspect 用作"租户级 hash 缓存"的存储后端

## 设计要点

### 装配顺序

`@AutoConfigureAfter(RedisAutoConfiguration::class)` 保证 `RedisTemplates` 已就绪；
`@AutoConfigureBefore(LinkableCacheAutoConfiguration::class)` 保证 `MixCacheManager` 装配
时已能取到 `remoteCacheManager` bean。两个注解都被 kudos 自定义的
`ComponentInitializationDispatcher` 识别，**不是** Spring Boot 默认 SPI 的 no-op 用法。

### Redis 实例选择

```yaml
kudos:
  ability:
    cache:
      enabled: true
      remoteStore: data    # 指向 kudos.ability.data.redis.redis-map.<name>
      version: v2          # 全局缓存版本前缀
```

`remoteCacheManager` bean 按 `remoteStore` 从 `RedisTemplates` 取对应实例；
未配置时回退到 `defaultRedis`；两者都没配启动期抛错——不让缓存在配错的情况下静默退化。

### 跨节点失效广播

```
Node A:  MixCache.evict(key)
         → remote.evict(key)               # 删 Redis
         → local.evict(key)                # 删本地
         → pushMsgRedis(name, key)         # 调所有 ICacheMessageHandler
            ↓
         RedisCacheMessageHandler.sendMessage
         → redisTemplate.convertAndSend("v2:cache:local-remote:channel", msg)
            ↓
Node B:  RedisMessageListenerContainer 收到消息
         → RedisCacheMessageHandler.onMessage
         → receiveMessage(msg)
         → if (msg.nodeId != myNodeId)
              → kv: mixCacheManager.clearLocal(name, key)
              → hash: 找 IHashCacheSync bean.evictLocal / clearLocal
         → 触发 CacheCleanRegister 注册的监听器
```

**关键点**：
- 每个节点启动期生成一个 `cacheNodeId`（UUID）；发送时塞进消息体，接收端比对 nodeId
  决定要不要清理本地——避免本节点回环清理自己刚清完的 key。
- 反序列化失败 → **抬到 error 级日志**（旧版本 silent，事故消息会消失在 jul 日志）。
  反序列化失败实际是一致性事故：本节点收不到通知 → 本地缓存可能长期持有脏数据。
- 监听异常被 catch 不上抛——pub/sub listener 由 Spring 单线程串行投递，抛出会被容器吞
  但中断对当前消息的处理；后续消息会被误判为"全部失败"。

### `RedisHashCache` 与 `MixHashCache` 的职责分离

**已修历史 bug**：之前 `RedisHashCache` 在每个写方法里 push 一条 pub/sub 通知，但：
1. 通知带版本前缀，本地 `CaffeineHashCache` 用逻辑名存——前缀名查 mainData 总是空，
   通知实际是 no-op
2. `MixHashCache` 在 LOCAL_REMOTE 模式下也会发一条（用逻辑名），导致一次写发了 2 条 pub/sub

现在职责划分：
- `RedisHashCache` —— **只管存**，没有广播
- `MixHashCache` —— 统一负责发广播（用逻辑名）

业务侧总是通过 `HashCacheKit` 拿到 `MixHashCache`，不直接持有 `RedisHashCache`。

### `RedisRemoteCacheProcessor` vs `RedisHashCache`

两者都是"Hash 形 redis 缓存"，但用途不同：
- `RedisRemoteCacheProcessor` —— 给 `TenantAdvancedCacheableAspect` / `BatchCacheableAspect` 等
  用，结构是 `cacheKey → Hash<dataKey, value>`，dataKey 由业务侧（如租户码）决定
- `RedisHashCache` —— 给"带 id 实体集合"用，结构是 `cacheName → Hash<id, Entity> + Set/ZSet 索引`

两者并存且不冲突；前者是租户级 K-V，后者是 id 集合 + 索引查询。

### `existsKey` 实现的演变

旧版本反射调 `RedisCache.createAndConvertCacheKey`（包可见方法）。JPMS 模块系统下反射
访问包可见成员可能失败。改走公开 `Cache.get(key)` API，配合 `disableCachingNullValues()`
保证语义清晰（不存 null value → `get != null` 等价于"存在"）。

### 虚拟线程支持

`redisMessageListenerContainer` 在 `spring.threads.virtual.enabled=true` 时改用虚拟线程
执行器（`SimpleAsyncTaskExecutor` + `setVirtualThreads(true)`），pub/sub 监听不再占用
平台线程。需要 JDK 21+。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/RedisCacheAutoConfiguration` | 装配入口；7 个 bean（远程 manager / 监听容器 / processor / nodeId / messageHandler / hash cache 等） |
| `RedisKeyValueCacheManager` | K-V 远程缓存管理器 |
| `RedisHashCache` | Hash 远程缓存存储（无广播） |
| `notice/RedisCacheMessageHandler` | pub/sub 收发 + 反序列化错误的可见日志 |
| `support/RedisRemoteCacheProcessor` | 租户 Hash K-V 缓存的远程读写实现 |

## 配置示例

```yaml
kudos:
  ability:
    cache:
      enabled: true
      version: v2
      remoteStore: data
      redis:
        node-id: ${HOSTNAME:}   # 可选；为空时启动期自动生成 UUID
      cache-items:
        - name=USER_CACHE&strategy=LOCAL_REMOTE&ttl=900
        - name=DEMO&strategy=REMOTE&ttl=1800

  ability:
    data:
      redis:
        default-redis: data
        redis-map:
          data:
            host: localhost
            port: 6379
            value-serializer: fastjson    # 跨服务共享缓存数据时推荐
```

## 测试覆盖

- `RemoteCacheTest` / `BatchCacheableTest` / `LocalRemoteCacheTest` / `NoCacheTest` —— K-V
- `RemoteHashCacheTest` / `HashBatchCacheableTest` / `LocalRemoteHashCacheTest` /
  `LocalRemoteHashBatchCacheableTest` / `NoHashCacheTest` —— Hash
- 测试依赖 `RedisTestContainer`（testcontainers），需要 Docker 运行环境

## 已知限制 / 后续工作

- ❗ `RedisKeyValueCacheManager.loadCaches()` 返回 `caches` 直接引用，且 Spring
  `afterPropertiesSet()` 后会内部拷贝——此后 `addCache` 加进来的实例不会被识别。运行期
  动态加 cache 暂不支持
- ❗ pub/sub 走 Redis 单连接订阅，连接抖动 / 网络中断时**消息会丢**——Redis pub/sub 是
  fire-and-forget，没有重传。需要严格一致性的场景应改用 Redis streams 或 MQ
- ✅ `cacheNodeId` 支持通过 `kudos.ability.cache.redis.node-id` 配置稳定节点标识；未配置或空白
  时仍回退启动期 UUID
- ❗ `RedisRemoteCacheProcessor.writeCacheData` 设的过期时间应用在整个 Hash key 上，
  不是单个 field——同 Hash 下多 field 共享 TTL，互相覆盖会改动其他 field 的过期
- ❗ `RedisCacheMessageHandler` 对反序列化失败只 log error，不主动清空本地——目前的判断是
  反序列化失败已经是事故，自动 clear 可能放大问题。需要时业务侧应自行加监控告警
- ❗ pub/sub channel 名只按 `cacheVersion` 区分（`<version>:cache:local-remote:channel`）；
  同一 redis 多个应用共享时建议各自配独立 `cacheVersion` 避免互相收到对方的失效消息

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-memdb:kudos-ability-data-memdb-redis"))

testImplementation(project(":kudos-test:kudos-test-container"))
testImplementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine"))
```

`cache-local-caffeine` 仅 testImplementation——本模块自身不依赖本地缓存，但
LOCAL_REMOTE 测试需要本地实现配合验证"远端写 → 本地清"链路。
