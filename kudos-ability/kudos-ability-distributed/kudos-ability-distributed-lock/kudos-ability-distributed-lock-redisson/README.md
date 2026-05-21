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

**异常处理**：业务方法抛出的任何 `Throwable` 会按原异常透传，避免破坏业务侧 typed
catch；释放锁阶段的异常仅 warn，不影响业务异常 / 返回值。

### `RedissonLockKit` 单例 + 工厂分离

`RedissonLockKit` 是静态工具入口；首次调用时通过 `SpringKit.getBean<RedissonLocker>()` 拿
bean 并缓存到字段。`@Synchronized` 保护初始化竞态。**key 前缀**统一加 `REDISSON::`。

无显式超时的 `RedissonLockKit.lock(lockKey)` / `RedissonLocker.lock(lockKey)` 不再调用
Redisson 的无限阻塞 `RLock.lock()`；默认最多等待 3 秒，拿到锁后租期 30 秒，拿不到返回 null。
需要业务自定义等待时间 / 租期时优先用 `tryLock`。

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
- `RedissonLockerTest` —— 纯 mock 单测覆盖无超时 `lock(lockKey)` 走 bounded `tryLock`，
  不再调用无限阻塞的 `RLock.lock()`
- `RedissonLockProviderTest` —— 纯 mock 单测覆盖 `unLock(Lock, key)` 的 RLock 分支会走
  `isHeldByCurrentThread` 守卫，非当前线程持有时不裸调 `unlock()`
- `DistributedLockAspectTest` —— 纯 mock 单测覆盖切面会按原异常透传业务 typed exception，
  且异常路径仍执行 finally 解锁

## 已知限制 / 后续工作

- ✅ `DistributedLockAspect.around` 已直接 rethrow 业务异常，不再包成 `RuntimeException`；
  业务侧 typed catch 可继续生效，并补单测锁住异常路径仍会解锁
- ❗ `DistributedLockAspect.around` 拿锁失败时直接返回 null——返回值类型为非 nullable 的业务
  方法会在调用方因 `NullPointerException` 崩溃。建议改成抛业务异常或显式 SpEL 模式声明默认值
- ❗ `RedissonLockKit` 是全局 static——同进程只支持一个 RedissonClient bean。多 redis 集群
  场景需自行包装
- ❗ key 前缀 `REDISSON::` 硬编码，业务不能更改；不同应用部署到同一 Redis 实例时如有命名冲突
  需要业务侧自己再加 namespace
- ❗ `RedissonLockProvider.unLock(Lock, key)` 受 `ILockProvider` 接口限制，仍无法用 key 校验
  传入 lock 是否同源；但 RLock 分支已改为走 `isHeldByCurrentThread` 守卫，避免裸 `unlock()`
- ✅ `RedissonLocker.lock(lockKey)` 已从无限阻塞 `RLock.lock()` 改为默认 bounded `tryLock`
  （最多等待 3 秒，租期 30 秒），超时 / 中断返回 null，并补单测锁住
- ❗ 删除了未使用的 `atom/AtomExecuteTask`（Thread 扩展类，全模块无引用），如果有外部反射依赖
  需要恢复

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-lock:kudos-ability-distributed-lock-common"))
api(libs.redisson)

testImplementation(project(":kudos-test:kudos-test-common"))
testImplementation(project(":kudos-test:kudos-test-container"))
```
