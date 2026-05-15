# kudos-context

**定位**：kudos 工程的**运行时上下文层**。介于 [`kudos-base`](../kudos-base/README.md)（纯 Kotlin / 无 Spring）与上层（`kudos-ability` / `kudos-ms`）之间，提供需要 Spring 容器的共用设施：Spring 集成、上下文传播、分布式锁、失败数据重试、跨模块校验集成等。

**与 kudos-base 的边界**：本模块**可以依赖 Spring**，上游 `kudos-base` 不允许。`kudos-base` 提供的纯工具（如 `ValidationKit` / `LockTool` 的概念基类）在此与 Spring Bean 容器对接。

---

## 子系统索引

| 子系统 | 关键类型 | 说明 |
|---|---|---|
| `core` | [`KudosContext`](src/io/kudos/context/core/KudosContext.kt), [`KudosContextHolder`](src/io/kudos/context/core/KudosContextHolder.kt), [`KudosContextElement`](src/io/kudos/context/core/KudosContextElement.kt), `ClientInfo` | 业务请求级上下文。`KudosContextHolder` 用 `InheritableThreadLocal`；`KudosContextElement` 是协程传播支持 |
| `kit` | [`SpringKit`](src/io/kudos/context/kit/SpringKit.kt), [`TransactionTool`](src/io/kudos/context/kit/TransactionTool.kt), `ProxyKit` | Spring Bean / 属性查找、事务同步、AOP 代理解包 |
| `spring` | [`SpringContextInitializer`](src/io/kudos/context/spring/SpringContextInitializer.kt) | `ApplicationContextInitializer` 实现，把 `applicationContext` 注入 `SpringKit` |
| `init` | [`ContextAutoConfiguration`](src/io/kudos/context/init/ContextAutoConfiguration.kt), [`ValidatorAutoConfiguration`](src/io/kudos/context/init/ValidatorAutoConfiguration.kt), [`ComponentInitializationDispatcher`](src/io/kudos/context/init/ComponentInitializationDispatcher.kt), `IComponentInitializer`, `@EnableKudos`, `ComponentInitializerSelector` | Spring 自动装配 + Kudos 组件初始化协调（`beforeInit` / `afterInit` lifecycle、`@AutoConfigureAfter`/`Before` 编排） |
| `lock` | [`ILockProvider`](src/io/kudos/context/lock/ILockProvider.kt), [`ILeaseLockProvider`](src/io/kudos/context/lock/ILockProvider.kt), [`IReentrantLockProvider`](src/io/kudos/context/lock/ILockProvider.kt), [`NormalLockService`](src/io/kudos/context/lock/NormalLockService.kt), [`LockTool`](src/io/kudos/context/lock/LockTool.kt) | 锁框架。租约锁 (`tryLock`/`lockExecute`) 与可重入锁 (`lock`) 拆分为两个接口；`LockTool` 是全局门面 |
| `retry` | [`IFailedDataHandler`](src/io/kudos/context/retry/IFailedDataHandler.kt), [`AbstractFailedDataHandler`](src/io/kudos/context/retry/AbstractFailedDataHandler.kt), [`FailedDataRetryScanner`](src/io/kudos/context/retry/FailedDataRetryScanner.kt), [`RetryConfig`](src/io/kudos/context/retry/RetryConfig.kt) | 失败数据落盘 + 定时重试。`RetryConfig` 集中路径解析，跨平台 |
| `validation` | [`CustomConstraintValidatorFactory`](src/io/kudos/context/validation/CustomConstraintValidatorFactory.kt), `IConstraintValidatorProviderBean` | 把 Spring Bean 注册为 JSR-380 自定义约束验证器；桥接 `kudos-base` 的 `ValidationContext` |
| `config` | `YamlPropertySourceFactory`, `OrderProperties`, `IConfigDataFinder` | YAML 加载、顺序属性源 |
| `support` | `Consts` | 模块常量 |

---

## 关键设计约定

### 1. 上下文持有：`KudosContextHolder` 的两种取法

```kotlin
KudosContextHolder.get()       // 未初始化时自动创建 KudosContext 并写入 ThreadLocal
KudosContextHolder.getOrNull() // 未初始化返回 null，不创建
```

**何时用哪个**：HTTP 请求线程通常已由 Filter / Interceptor 注入上下文，用 `get()` 顺手；定时任务 / 协程 / 线程池工作线程则应优先 `getOrNull()` —— 避免隐式创建带来的"幽灵上下文"。

`InheritableThreadLocal` 在线程池场景下**已有的 worker 线程**不会自动看到 worker 创建后才 `set` 的上下文。跨线程池传递必须显式拷贝。

### 2. 锁框架：两套语义清晰拆分

```kotlin
// 租约锁：tryLock 超时后自动过期，常用 lockExecute 自动 try-finally 释放
LockTool.lockExecute(
    lockKey = "user:42:checkout",
    supplier = { performCheckout() },
    second = 30,
    errorCode = MyErrors.LOCK_TIMEOUT
)

// 可重入锁：lock() 拿 Lock 实例，必须手动 unLock(lock, key) 释放
val lock = LockTool.lockProvider.lock("cache:warm-up") ?: return null
try {
    return computeAndCache()
} finally {
    LockTool.lockProvider.unLock(lock, "cache:warm-up")
}
```

**关键**：两套机制底层走不同的数据结构（租约锁 `cacheKeyMap + DelayQueue`、可重入锁 `KeyLockRegistry<String>`）。**不要混用**——拿了租约锁后用 `unLock(lock, key)` 释放或反过来都会失败/泄漏。接口已拆分为 `ILeaseLockProvider` 与 `IReentrantLockProvider`，新代码应明确依赖较窄的那个。

### 3. 失败数据重试路径配置

失败数据持久化目录解析优先级（[`RetryConfig`](src/io/kudos/context/retry/RetryConfig.kt)）：

1. 系统属性 `kudos.retry.failed-data-path`
2. 环境变量 `KUDOS_RETRY_FAILED_DATA_PATH`
3. 默认 `${java.io.tmpdir}/kudos-failed-data`（跨平台安全）

最终路径 = `{base}/{atomicServiceCode 或 default}`，原子服务编码取自 `KudosContextHolder.getOrNull()?.atomicServiceCode`。

### 4. 上下文初始化协调：`IComponentInitializer`

任何标 `@Configuration` 且实现 [`IComponentInitializer`](src/io/kudos/context/init/IComponentInitializer.kt) 的配置类会被 [`ComponentInitializationDispatcher`](src/io/kudos/context/init/ComponentInitializationDispatcher.kt) 接管：

- 配置类自身初始化**前** → `beforeInit()`
- 配置类及其所有 `@Bean` 全部初始化**完毕** → `afterInit()`
- 标 `@AutoConfigureAfter` / `@AutoConfigureBefore` 会被翻译成 `BeanDefinition.dependsOn`

`@EnableKudos` 注解在应用主类上启用此机制，扫描路径目前硬编码为 `io.kudos.context` / `io.kudos.ability` / `io.kudos.ms`。

### 5. JSR-380 自定义校验器：Spring 注入

`CustomConstraintValidatorFactory` 桥接 `kudos-base.ValidationContext.validator` 与 Spring `LocalValidatorFactoryBean`，让自定义 `ConstraintValidator` 可以 `@Autowired` Spring Bean。注册方式：实现 `IConstraintValidatorProviderBean` Bean，工厂启动时扫描所有此类型 Bean 并把它们的 `provide()` 结果注册到 Hibernate Validator。

`ValidatorAutoConfiguration` 通过 `ValidationContextBridge.@PostConstruct` 把 Spring 创建的 validator 桥接到 `kudos-base.ValidationContext.validator`——让容器外的工具（`ValidationKit` 等）也能拿到 Spring 注入版本。

### 6. 并发原语统一：`ConcurrentHashMap.computeIfAbsent`

各处缓存（`SpringKit.applicationContext` 标 `@Volatile`、`Registry` / `KeyLockRegistry` / `descriptorAccessorCache` 等）一律 per-key 锁，不用 `synchronized(map)` 的粗粒度。

---

## 已知约束与陷阱

> 每条都有对应测试钉住，"修复"前请先看代价

1. **`KudosContextHolder.get()` 自动创建**：未初始化时返回新实例并写入 ThreadLocal。这是有意行为（避免每个 caller 写 null check），但代价是"上下文未注入"会被静默掩盖。需明确区分场景的代码请用 `getOrNull()`。
2. **`InheritableThreadLocal` 不传给线程池里已存在的 worker**：只有"父线程创建子线程"时拷贝当前快照。线程池 warmup 后的 worker 看不到主线程后来 `set` 的上下文。
3. **`LockTool.hasKeyLock` 命名反直觉**（已 `@Deprecated`）：返回 `!tryLock(...)`，"拿不到锁时为 true"。直接用 `!lockProvider.tryLock(...)`。
4. **`ComponentInitializerSelector` 扫描路径硬编码**：只看 `io.kudos.context` / `io.kudos.ability` / `io.kudos.ms`。自定义业务包不会被自动发现。
5. **`ComponentInitializationDispatcher.getEarlyBeanReference`**：循环依赖 / CGLIB 代理场景下可能造成 `remainingCount` 双扣减、`afterInit` 触发时机偏移。集成测试覆盖待补。
6. **`CustomConstraintValidatorFactory` 维护一份私有 `applicationContext`**：Spring 父类把同名字段标 `private`，Kotlin 属性解析撞名导致无法直接读父类版本——所以子类自己开一份。看起来像反模式，实际是 Kotlin/Spring 互操作下的最小冗余。
7. **失败数据重试锁超时 vs 处理时长**：`FailedDataRetryScanner.lockRetry` 用 600 秒租约，若实际处理超过会被其它实例并行获取。处理短或重试幂等的场景安全；批量耗时场景需配置更长租约。

---

## 测试约定

- 目录：`test-src/`，与 `src/` 一一对应
- 单元测试用 JUnit 5 + `kotlin.test`，**不**启动完整 Spring 容器；需要 Spring 静态 API 时用 `TransactionSynchronizationManager.initSynchronization()` 这类自带可测 API
- 跑全套：`./gradlew :kudos-context:test`
- 当前覆盖（45 用例）：
  - `SpringKitTest`：Bean 查找
  - `KudosContextHolderTest`：`InheritableThreadLocal` 行为、`get` vs `getOrNull`、跨线程隔离
  - `NormalLockServiceTest`：租约锁 / 可重入锁两套机制、过期清理、并发竞态
  - `TransactionToolTest`：事务同步、异常吞噬、独立 sync 对象
  - `RetryConfigTest`:三级 fallback、跨平台默认值
  - `FailedDataRetryScannerTest`：文件扫描各分支、锁交互 try-finally

### 测试时的可测性钩子

- `FailedDataRetryScanner.lockProviderSupplier`：注入本地锁服务，绕开 `LockTool` 对 Spring 容器的依赖
- `FailedDataRetryScanner.retry()` / `lockRetry()`：标 `internal`，可在 Kotlin 测试中直接调用
- `RetryConfig.resolveBasePath()`：标 `internal`，每次调用都重新读优先级（区别于 `by lazy` 的 `baseFailedDataPath`）

---

## 与 kudos-base 的依赖清单

```
io.kudos.base.bean.validation.support.ValidationContext  → 桥接 Spring validator 给 kudos-base
io.kudos.base.data.json.JsonKit                          → 失败数据序列化
io.kudos.base.enums.ienums.IErrorCodeEnum                → 错误码
io.kudos.base.error.ServiceException                    → 业务异常
io.kudos.base.io.ScanKit                                 → classpath 扫描（ComponentInitializerSelector）
io.kudos.base.lang.GenericKit                            → 泛型反射
io.kudos.base.logger.LogFactory                          → 日志
io.kudos.base.model.contract.entity.IIdEntity            → 实体契约
io.kudos.base.support.KeyLockRegistry                    → 可重入锁底层结构
```

依赖**单向**：`kudos-context → kudos-base`，不反向。
