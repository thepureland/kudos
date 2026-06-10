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
- 拿不到锁 → 不执行业务方法 + 触发 `IDistributedLockCallback.doLockFail`；默认抛
  `DistributedLockAcquireException`，显式 `throwOnFailure=false` 时兼容旧行为返回 null
- 拿到锁 → finally 释放 + 触发 `doLockSuccess`
- `lockerBeanName = "..."` → 指定 RedissonLocker bean，支持同进程多 RedissonClient 场景

**异常处理**：业务方法抛出的任何 `Throwable` 会按原异常透传，避免破坏业务侧 typed
catch；释放锁阶段的异常仅 warn，不影响业务异常 / 返回值。

### `RedissonLockKit` 单例 + 工厂分离

`RedissonLockKit` 是静态工具入口；默认通过 `SpringKit.getBean("redissonLocker")` 拿 bean
并按 beanName 缓存。需要多 Redis / 多 RedissonClient 时，可声明多个 `RedissonLocker` bean，
并通过 `lockerBeanName` 或 `RedissonLockKit.*(..., lockerBeanName)` 选择。

**key 前缀**默认 `REDISSON::`，可通过
`kudos.ability.distributed.lock.redisson.lockKeyPrefix` 配置，或调用
`RedissonLockKit.setLockKeyPrefix(...)` 调整；传空字符串表示不加前缀。

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
          lockKeyPrefix: "REDISSON::"
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
  key/name 校验与 `isHeldByCurrentThread` 守卫，key 不匹配或非当前线程持有时不裸调 `unlock()`
- `DistributedLockAspectTest` —— 纯 mock 单测覆盖切面会按原异常透传业务 typed exception，
  异常路径仍执行 finally 解锁，拿锁失败默认抛 `DistributedLockAcquireException` 且可显式兼容
  返回 null
- `RedissonLockKitTest` —— 纯 mock 单测覆盖可配置 key 前缀、命名 locker 入口

## 已知限制 / 后续工作

- ✅ `DistributedLockAspect.around` 已直接 rethrow 业务异常，不再包成 `RuntimeException`；
  业务侧 typed catch 可继续生效，并补单测锁住异常路径仍会解锁
- ✅ `DistributedLockAspect.around` 拿锁失败默认抛 `DistributedLockAcquireException`，避免非
  nullable 返回值在调用方延迟 NPE；旧调用方可显式 `throwOnFailure=false` 返回 null
- ✅ `RedissonLockKit` 已支持按 locker beanName 缓存 / 调用，`@DistributedLock.lockerBeanName`
  也可选择指定 locker；多 RedissonClient 场景可声明多个 `RedissonLocker` bean 后按名使用
- ✅ key 前缀已从硬编码改为可配置：默认 `REDISSON::`，配置项
  `kudos.ability.distributed.lock.redisson.lockKeyPrefix`，也可通过 `RedissonLockKit.setLockKeyPrefix`
  调整
- ✅ `RedissonLockProvider.unLock(Lock, key)` 的 RLock 分支已校验 `RLock.name` 与传入 key
  是否匹配，匹配后才走 `isHeldByCurrentThread` 守卫解锁；非 RLock 仍按标准 `Lock.unlock()`
  处理
- ✅ `RedissonLocker.lock(lockKey)` 已从无限阻塞 `RLock.lock()` 改为默认 bounded `tryLock`
  （最多等待 3 秒，租期 30 秒），超时 / 中断返回 null，并补单测锁住
- ✅ `atom/AtomExecuteTask` 已恢复并标记 `@Deprecated`，仅为历史外部反射 / 二进制兼容保留；
  模块内部仍无引用

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-lock:kudos-ability-distributed-lock-common"))
api(libs.redisson)

testImplementation(project(":kudos-test:kudos-test-common"))
testImplementation(project(":kudos-test:kudos-test-container"))
```

## 改进建议（自动分析 2026-06-11）

- **【功能】缺少看门狗/自动续期支持**：
  `src/io/kudos/ability/distributed/lock/redisson/annotations/DistributedLockAspect.kt`
  `@DistributedLock` 默认 `leaseTime=20` 秒固定租期，业务执行超过租期后锁自动释放、临界区裸奔。
  Redisson 在 `tryLock(wait, -1, unit)` 时会启用 watchdog 自动续期，但注解层未显式声明/测试
  `-1` 语义。建议把"`leaseTime=-1` 启用看门狗"作为受支持的契约写入注解 KDoc，并补集成测试。
- **【安全】SpEL 生成的锁键未净化**：
  `src/io/kudos/ability/distributed/lock/redisson/annotations/DistributedLockAspect.kt`
  `genLockKey` 把 SpEL 结果直接拼成 `tenantId::<result>`：用户可控参数中含 `::` 或超长字符串会
  污染键空间（与其他业务键碰撞）；`tenantId` 为 null 时前缀字面量为 `"null"`，多个无租户调用方
  会共享同一隔离段。建议对 SpEL 结果做字符白名单过滤或哈希，并对 null 租户给出独立哨兵值。
- **【可观测性】无锁等待/持有时长与成功率指标**：
  `src/io/kudos/ability/distributed/lock/redisson/annotations/DistributedLockAspect.kt`
  仅有 debug/warn 日志，没有 Micrometer Timer（等待耗时、持有耗时）和 Counter（成功/失败次数，
  按 lockKey 前缀维度）。锁竞争问题在生产几乎只能靠指标定位，建议补埋点；"拿锁失败"日志也应
  带上 `waitTime`/`leaseTime` 便于排查。
- **【可维护性】`redissonClient` Bean 在 disabled 时返回 null**：
  `src/io/kudos/ability/distributed/lock/redisson/init/RedissonLockAutoConfiguration.kt`
  `enabled=false` 时 `@Bean` 方法返回 null——Spring 虽容忍 null bean，但下游 `@Autowired` 报错
  信息晦涩。建议改用 `@ConditionalOnProperty("kudos.ability.distributed.lock.redisson.enabled")`
  控制整个 bean 的注册（需回归验证依赖方）。
- **【扩展性】切面静态绑定 `RedissonLockKit`，绕过 `ILocker` SPI**：
  `src/io/kudos/ability/distributed/lock/redisson/annotations/DistributedLockAspect.kt`
  common 模块定义了 `ILocker` 抽象，但注解切面直接调用 Redisson 专属的 `RedissonLockKit`，
  未来接 Curator/Etcd 实现时切面无法复用。建议长期把切面下沉到 lock-common 并依赖 `ILocker`。
- **【测试】SpEL 键生成与回调触发无单测**：
  `genLockKey` 的两种策略（自动键、SpEL 键）以及 `IDistributedLockCallback` 的
  success/fail 回调触发路径目前零覆盖，均为低成本纯逻辑单测。
