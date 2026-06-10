# kudos-ability-distributed-lock-common

分布式锁 SPI + 注解。具体实现在 `kudos-ability-distributed-lock-redisson`。

## 设计要点

| 文件 | 角色 |
|---|---|
| `annotations/DistributedLock` | 方法注解：`@DistributedLock(key, waitTime, leaseTime, throwOnFailure, lockerBeanName)`——切面在 redisson 模块 |
| `locker/ILocker` | 锁 SPI：`getLock` / `lock` / `tryLock` / `unlock` |
| `locker/IDistributedLockCallback` | 锁成功/失败回调 SPI |
| `locker/DistributedLockContext` | `IDistributedLockCallback` 的 ThreadLocal 持有者 |

业务侧典型用法：

```kotlin
@DistributedLock(key = "#userId", leaseTime = 30)
fun consumeQuota(userId: String) { ... }
```

`#userId` 是 SpEL（在 redisson 模块切面解析）。`leaseTime` 控制锁的最长持有时间——超过自动
释放，防持锁线程挂掉的永久死锁。`throwOnFailure` 默认 true，拿不到锁时抛
`DistributedLockAcquireException`；需要兼容旧的返回 null 语义时显式设为 false。
`lockerBeanName` 用于多 RedissonClient 场景下选择具体 locker bean。

## 测试覆盖

- `DistributedLockContextTest` —— 纯单测覆盖当前线程 set/get、`clear()` 清理、子线程不继承
  父线程 callback 的约束

## 状态 / 后续工作

- ℹ️ 模块只定义契约，具体能力（含 SpEL key 解析、Redisson `RLock` 装配等）全在 lock-redisson
- ✅ `DistributedLockContext` 用 `InheritableThreadLocal` 但 `childValue=null`——子线程不继承，
  并已补单测锁住；线程池场景仍要 finally 中 `clear()` 避免污染
- ✅ `ILocker.lock(lockKey: String)` 已约定为默认等待窗口内获取锁，拿不到返回 null；
  Redisson 实现默认最多等待 3 秒、租期 30 秒，不再无限阻塞
- ✅ `@DistributedLock` 已支持 `throwOnFailure` 和 `lockerBeanName`，分别用于拿锁失败语义
  和多 locker 选择

## 依赖

```kotlin
api(project(":kudos-context"))
```

无具体实现依赖；纯 SPI 模块。

## 改进建议（自动分析 2026-06-11）

- **【对外接口】`ILocker<T : Lock?>` 泛型上界可空**：
  `src/io/kudos/ability/distributed/lock/common/locker/ILocker.kt`
  上界写成 `Lock?` 导致 `unlock(lock: T)` 理论上可传 null、实现侧被迫处理可空分支。
  属 public API 不宜现在改签名；建议下个不兼容版本改为 `T : Lock`。
- **【对外接口/文档】`waitTime=0` 的语义依赖实现**：
  `src/io/kudos/ability/distributed/lock/common/annotations/DistributedLock.kt`
  注解本身不约束实现方对 `waitTime=0`（单次尝试）与负值的处理，目前语义只由 redisson 实现
  事实定义。建议在 SPI 层（`ILocker.tryLock` KDoc）统一约定非法值（负数）的行为。
