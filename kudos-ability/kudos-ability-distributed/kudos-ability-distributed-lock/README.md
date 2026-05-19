# kudos-ability-distributed-lock

分布式锁。

| 子模块 | 实现 |
|---|---|
| [`kudos-ability-distributed-lock-common`](kudos-ability-distributed-lock-common/README.md) | SPI：`@DistributedLock` 注解 + `ILocker<T>` |
| [`kudos-ability-distributed-lock-redisson`](kudos-ability-distributed-lock-redisson/README.md) | Redisson 实现 + 切面装配 |
