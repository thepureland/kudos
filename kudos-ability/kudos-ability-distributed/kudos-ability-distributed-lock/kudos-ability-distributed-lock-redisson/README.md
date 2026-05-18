# kudos-ability-distributed-lock-redisson

`kudos-ability-distributed-lock-common` 的 Redisson 实现。提供 `@DistributedLock` 注解切面 +
`ILocker<RLock>` 实现 + `RedissonClient` bean 装配。

## 设计要点

### 注解切面（`DistributedLockAspect`）

```kotlin
@DistributedLock(key = "#userId", waitTime = 0, leaseTime = 30)
fun consumeQuota(userId: String) { ... }
```

- `key = ""` → 自动生成 `serviceCode::tenantId::className::methodName::paramTypes`
- `key = "..."` → SpEL 表达式，结果拼成 `tenantId::<spel result>`（**租户隔离**自动包含）
- 拿不到锁 → 不执行业务方法，直接返回 null + 触发 `IDistributedLockCallback.doLockFail`
- 拿到锁 → finally 释放 + 触发 `doLockSuccess`

**异常处理**：业务方法抛出的任何 `Throwable` 会被包成 `RuntimeException` 抛出（栈跟踪保留为
cause）；释放锁阶段的异常仅 warn 不影响业务返回值。

### `RedissonLockKit` 单例 + 工厂分离

`RedissonLockKit` 是静态工具入口；首次调用时通过 `SpringKit.getBean<RedissonLocker>()` 拿
bean 并缓存到字段。`@Synchronized` 保护初始化竞态。**key 前缀**统一加 `REDISSON::`。

### `unlock` 的 `isHeldByCurrentThread` 守卫

Redisson `RLock.unlock()` 在线程没持有锁时抛 `IllegalMonitorStateException`。
[RedissonLocker.unlock] 和 [RedissonLockKit.unlock] 都加了 `isHeldByCurrentThread` 守卫，
让"误调 unlock"成为 no-op 而不是抛错。

### Redisson 4.0+ 的 password 位置变化

旧版本 password 设在 `BaseConfig` 上；4.0 起改到 `Config` 上。本模块已迁移；
`initBaseConfig` 里相应 setter 调用已删除——见源码注释。

## 配置示例

```yaml
kudos:
  ability:
    distributed:
      lock:
        redisson:
          enabled: true
          mode: single   # single | cluster
          config:
            nettyThreads: 32
            threads: 16
            transportMode: NIO
          baseConfig:
            password: ${REDIS_PASS:}
            timeout: 3000
            retryAttempts: 3
            clientName: my-app
          singleServerConfig:
            address: redis://localhost:6379
            connectionPoolSize: 32
            database: 0
```

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/RedissonLockAutoConfiguration` | 装配入口（aspect + locker + provider + client + properties） |
| `init/properties/Redisson*Properties` | 5 个配置 POJO |
| `annotations/DistributedLockAspect` | `@DistributedLock` 切面 |
| `locker/RedissonLocker` | `ILocker<RLock>` 实现 |
| `bean/RedissonLockProvider` | `ILockProvider<RLock>` 实现（kudos-context SPI 桥接） |
| `kit/RedissonLockKit` | 业务侧 `RedissonLockKit.lock(...)` 静态入口 |

## 测试覆盖

- `RedissonLockSingleTest` —— 单机 Redisson 集成测试（依赖 Redis testcontainer）

## 已知限制 / 后续工作

- ❗ `DistributedLockAspect.around` 包业务异常成 `RuntimeException` 抛出——业务侧的 typed
  catch 会失效，需要时考虑 `Throwable` 直接 rethrow 或区分 checked / unchecked
- ❗ `DistributedLockAspect.around` 拿锁失败时直接返回 null——返回值类型为非 nullable 的业务
  方法会在调用方因 `NullPointerException` 崩溃。建议改成抛业务异常或显式 SpEL 模式声明默认值
- ❗ `RedissonLockKit` 是全局 static——同进程只支持一个 RedissonClient bean。多 redis 集群
  场景需自行包装
- ❗ key 前缀 `REDISSON::` 硬编码，业务不能更改；不同应用部署到同一 Redis 实例时如有命名冲突
  需要业务侧自己再加 namespace
- ❗ `RedissonLockProvider.unLock(Lock, key)` 忽略 key 参数——直接 `lock.unlock()`，依赖调用方
  保证传入的 lock 对应 key
- ❗ 删除了未使用的 `atom/AtomExecuteTask`（Thread 扩展类，全模块无引用），如果有外部反射依赖
  需要恢复

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-lock:kudos-ability-distributed-lock-common"))
api(libs.redisson)

testImplementation(project(":kudos-test:kudos-test-container"))
```
