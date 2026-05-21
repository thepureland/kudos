# kudos-ability-distributed-lock-common

分布式锁 SPI + 注解。具体实现在 `kudos-ability-distributed-lock-redisson`。

## 设计要点

| 文件 | 角色 |
|---|---|
| `annotations/DistributedLock` | 方法注解：`@DistributedLock(key, waitTime, leaseTime)`——切面在 redisson 模块 |
| `locker/ILocker` | 锁 SPI：`getLock` / `lock` / `tryLock` / `unlock` |
| `locker/IDistributedLockCallback` | 锁成功/失败回调 SPI |
| `locker/DistributedLockContext` | `IDistributedLockCallback` 的 ThreadLocal 持有者 |

业务侧典型用法：

```kotlin
@DistributedLock(key = "#userId", leaseTime = 30)
fun consumeQuota(userId: String) { ... }
```

`#userId` 是 SpEL（在 redisson 模块切面解析）。`leaseTime` 控制锁的最长持有时间——超过自动
释放，防持锁线程挂掉的永久死锁。

## 测试覆盖

- `DistributedLockContextTest` —— 纯单测覆盖当前线程 set/get、`clear()` 清理、子线程不继承
  父线程 callback 的约束

## 已知限制

- ❗ 模块只定义契约，具体能力（含 SpEL key 解析、Redisson `RLock` 装配等）全在 lock-redisson
- ✅ `DistributedLockContext` 用 `InheritableThreadLocal` 但 `childValue=null`——子线程不继承，
  并已补单测锁住；线程池场景仍要 finally 中 `clear()` 避免污染
- ❗ `ILocker.lock(lockKey: String)`（无超时签名）等价于无限阻塞——避免在生产业务路径使用

## 依赖

```kotlin
api(project(":kudos-context"))
```

无具体实现依赖；纯 SPI 模块。
