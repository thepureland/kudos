# kudos-context

**定位**：kudos 工程的**运行时上下文层**。介于 [`kudos-base`](../kudos-base/README.md)（纯 Kotlin / 无 Spring）与上层（`kudos-ability` / `kudos-ms`）之间，提供需要 Spring 容器的共用设施：Spring 集成、上下文传播、分布式锁、失败数据重试、跨模块校验集成等。

**与 kudos-base 的边界**：本模块**可以依赖 Spring**，上游 `kudos-base` 不允许。`kudos-base` 提供的纯工具（如 `ValidationKit` / `LockTool` 的概念基类）在此与 Spring Bean 容器对接。

**本模块仅提供契约与缺省实现，不含分布式实现**：

| 提供 | 不提供（在哪里） |
|---|---|
| `ILockProvider` / `ILeaseLockProvider` / `IReentrantLockProvider` 接口、`NormalLockService` 单机内存实现 | `RedissonLockProvider` → `kudos-ability-distributed-lock-redisson` |
| `IFailedDataHandler` 接口、`AbstractFailedDataHandler` 文件落盘骨架 | 具体 handler 实现（如 Kafka / Redis producer 重试）→ 业务模块自行实现 |
| `IConstraintValidatorProviderBean` 接口、`CustomConstraintValidatorFactory` 装配 | 实际的 `ConstraintValidator` → 业务模块（如 `kudos-ms-*-core`）注册 |
| `IConfigDataFinder` 接口（`ServiceLoader` SPI） | Nacos / Apollo 实现 → 配置中心适配模块 |
| `KudosContext` / `KudosContextHolder` / `KudosContextElement` | 实际写入上下文的 Filter / Interceptor / Ktor plugin → `kudos-ability-web-*` |

> **运维含义**：仅依赖 `kudos-context` 而 **不依赖** `kudos-ability-distributed-lock-*` 的多实例部署，会让 `LockTool` 静默走 `NormalLockService` 本地锁——分布式锁失效（见下文 [8-dim 审计补遗 B](#b-已知限制--安全考量)）。

---

## 子系统索引

| 子系统 | 关键类型 | 说明 |
|---|---|---|
| `core` | [`KudosContext`](src/io/kudos/context/core/KudosContext.kt), [`KudosContextHolder`](src/io/kudos/context/core/KudosContextHolder.kt), [`KudosContextElement`](src/io/kudos/context/core/KudosContextElement.kt), [`ClientInfo`](src/io/kudos/context/core/ClientInfo.kt) | 业务请求级上下文。`KudosContextHolder` 用 `InheritableThreadLocal`；`KudosContextElement` 是协程传播支持（与 ThreadLocal **并行**的两套存储，见下文） |
| `kit` | [`SpringKit`](src/io/kudos/context/kit/SpringKit.kt), [`TransactionTool`](src/io/kudos/context/kit/TransactionTool.kt), [`ProxyKit`](src/io/kudos/context/kit/ProxyKit.kt) | Spring Bean / 属性查找、事务同步、AOP 代理解包（JDK 动态代理 + CGLIB 反射拆包） |
| `spring` | [`SpringContextInitializer`](src/io/kudos/context/spring/SpringContextInitializer.kt) | `ApplicationContextInitializer` 实现，把 `applicationContext` 注入 `SpringKit` |
| `init` | [`ContextAutoConfiguration`](src/io/kudos/context/init/ContextAutoConfiguration.kt), [`ValidatorAutoConfiguration`](src/io/kudos/context/init/ValidatorAutoConfiguration.kt), [`ComponentInitializationDispatcher`](src/io/kudos/context/init/ComponentInitializationDispatcher.kt), [`IComponentInitializer`](src/io/kudos/context/init/IComponentInitializer.kt), [`@EnableKudos`](src/io/kudos/context/init/EnableKudos.kt), [`ComponentInitializerSelector`](src/io/kudos/context/init/ComponentInitializerSelector.kt) | Spring 自动装配 + Kudos 组件初始化协调（`beforeInit` / `afterInit` lifecycle、`@AutoConfigureAfter`/`Before` 编排） |
| `lock` | [`ILockProvider` / `ILeaseLockProvider` / `IReentrantLockProvider`](src/io/kudos/context/lock/ILockProvider.kt), [`NormalLockService`](src/io/kudos/context/lock/NormalLockService.kt), [`LockTool`](src/io/kudos/context/lock/LockTool.kt) | 锁框架。三个接口同文件：`ILockProvider` 是组合 marker，`ILeaseLockProvider` 是租约锁 (`tryLock`/`lockExecute`)，`IReentrantLockProvider` 是可重入锁 (`lock`)；`LockTool` 是全局门面 |
| `retry` | [`IFailedDataHandler`](src/io/kudos/context/retry/IFailedDataHandler.kt), [`AbstractFailedDataHandler`](src/io/kudos/context/retry/AbstractFailedDataHandler.kt), [`FailedDataRetryScanner`](src/io/kudos/context/retry/FailedDataRetryScanner.kt), [`RetryConfig`](src/io/kudos/context/retry/RetryConfig.kt) | 失败数据落盘 + 定时重试。`RetryConfig` 集中路径解析，跨平台 |
| `validation` | [`CustomConstraintValidatorFactory`](src/io/kudos/context/validation/CustomConstraintValidatorFactory.kt), [`IConstraintValidatorProviderBean`](src/io/kudos/context/validation/IConstraintValidatorProviderBean.kt) | 把 Spring Bean 注册为 JSR-380 自定义约束验证器；桥接 `kudos-base` 的 `ValidationContext` |
| `config` | [`YamlPropertySourceFactory`](src/io/kudos/context/config/YamlPropertySourceFactory.kt), [`OrderProperties`](src/io/kudos/context/config/OrderProperties.kt), [`IConfigDataFinder`](src/io/kudos/context/config/IConfigDataFinder.kt) | YAML 加载、顺序属性源、配置中心 SPI（`ServiceLoader` 加载 `IConfigDataFinder` 实现，如 Nacos） |
| `support` | [`Consts`](src/io/kudos/context/support/Consts.kt) | 模块常量（缓存分隔符、默认门户/子系统/微服务编码、Feign / Notify 等 Header key） |

---

## `KudosContext` 字段语义

中心数据类型——所有"业务请求级"信息都挂在此处。各字段的消费方分散在 `kudos-ability-*`，下表整理实际语义与典型消费点（防止读者反向猜测）：

| 字段 | 类型 | 语义 | 谁消费 |
|---|---|---|---|
| `portalCode` | `String?` | 门户编码（最外层多门户隔离层） | 业务侧极少用，预留 |
| `subSystemCode` | `String?` | 子系统编码（同一门户下的逻辑分区，如 admin / ops / open） | `GlobalHeaderRequestInterceptor` 透传 Feign 头 `_sub_sys_code`；`FeignContextWebFilter` 解出回写到 `context.subSystemCode`；`@HashCacheableBySecondary` SpEL `#subSystemCode` 拼缓存 key；`sys-core.AccessRuleIpsBySubSysAndTenantIdCache` 维度键 |
| `microServiceCode` | `String?` | 微服务编码（部署单元粒度） | 预留 |
| `atomicServiceCode` | `String?` | **原子服务编码**——本工程里 microservice 拆得更细的最小自治单元 | [`RetryConfig.pathFor`](src/io/kudos/context/retry/RetryConfig.kt) 失败数据子目录；`StreamProducerExceptionHandler` 失败落盘；`DistributedLockAspect` 锁 key 一部分 |
| `tenantId` | `String?` | 租户 ID。null = 平台级；空白 = 视场景；具体值 = 业务租户 | Stream `StreamHeader.tenantId` 透传；Feign 头 `_tenant_id`；多租户路由；缓存维度键（sys/auth 模块大量用，详见 sys-core 的 "租户键归一化"） |
| `dataSourceId` | `String?` | 主库数据源 ID，null = 走路由策略 | Feign 头 `Consts.RequestHeader.DATASOURCE_ID` 透传；jdbc 模块按此切数据源；Stream `StreamHeader.dataSourceId`（注意：`StreamHeader` 字段名为 `Int?`，实际反序列化按 `String?`——历史 wire format 不一致，已在 `StreamHeader` 内有注释钉住） |
| `readOnlyDataSourceId` | `String?` | 备库 / 只读库数据源 ID | jdbc 模块用于读写分离 |
| `_datasourceTenantId` | `String?` | **下划线前缀的 wire format 历史约定**——同名出现在 `StreamHeader._datasourceTenantId` / jdbc 模块。下划线表示"跨进程透传字段、不要随便改名"，等同于 `Consts.RequestHeader.*` 那一组 underscore-prefixed header key | Stream + jdbc 协作 |
| `user` | `IIdEntity<String>?` | 当前用户（仅 ID + 实体契约，详情不在此处保存） | 业务代码 `KudosContextHolder.get().user?.id` |
| `traceKey` | `String?` | 分布式调用链 trace key | Feign 头 `Consts.RequestHeader.TRACE_KEY` 透传；`FeignContextWebFilter` 回写；日志 MDC |
| `clientInfo` | [`ClientInfo`](src/io/kudos/context/core/ClientInfo.kt)`?` | 客户端信息（IP / 浏览器 / locale / timezone 等） | HTTP 入口 Filter 写入 |
| `sessionAttributes` / `cookieAttributes` / `headerAttributes` | `MutableMap?` | 三类 Web 协议元数据，**按协议层归类**（不是按业务） | Web 入口 Filter 一次性塞，业务代码读 |
| `otherInfos` | `MutableMap<String, Any?>?` | 业务自定义字段。**优先用具名字段**——`otherInfos` 是 escape hatch，常用 key 在 [`KudosContext.OTHER_INFO_KEY_*`](src/io/kudos/context/core/KudosContext.kt) 提供常量（`_DATA_SOURCE_` / `_DATABASE_` / `_VERIFY_CODE_`） |

**字段惯例**：
- 编码字段（`portalCode` / `subSystemCode` / `microServiceCode` / `atomicServiceCode`）由**粗到细**形成层级。绝大多数业务只用 `subSystemCode` + `tenantId` 两层。
- 下划线前缀（`_datasourceTenantId` / `_DATA_SOURCE_` / `Consts.RequestHeader._sub_sys_code` ...）是**跨进程 wire format**——序列化到 Feign header / Stream header / RPC，**不可随意改名**，否则跨服务不兼容。
- 所有 `Mutable*` 字段都是 **lazy-init**（首次 `addXxx` 才分配 map），减少未使用场景的内存开销；读时记得 null check 或走 `addXxx` 链式 API。

---

## 关键设计约定

### 1. 上下文持有：`KudosContextHolder` 的两种取法

```kotlin
KudosContextHolder.get()       // 未初始化时自动创建 KudosContext 并写入 ThreadLocal
KudosContextHolder.getOrNull() // 未初始化返回 null，不创建
```

**何时用哪个**：HTTP 请求线程通常已由 Filter / Interceptor 注入上下文，用 `get()` 顺手；定时任务 / 协程 / 线程池工作线程则应优先 `getOrNull()` —— 避免隐式创建带来的"幽灵上下文"。

`InheritableThreadLocal` 在线程池场景下**已有的 worker 线程**不会自动看到 worker 创建后才 `set` 的上下文。跨线程池传递必须显式拷贝。

#### 协程世界的并行存储：`KudosContextElement`

`KudosContextHolder` 的 `InheritableThreadLocal` 在 **挂起函数** 跨线程恢复时不可靠（协程恢复点可能在另一个 worker 上）。所以 `kudos-context` 在协程侧提供 [`KudosContextElement`](src/io/kudos/context/core/KudosContextElement.kt) 作为 `CoroutineContext.Element` —— 这是与 ThreadLocal **并行**的存储，**不互通**。

```kotlin
// 同步代码：从 ThreadLocal 拿
val ctx = KudosContextHolder.get()

// 协程内：从 CoroutineContext 拿
suspend fun foo() {
    val ctx = currentKudosContext()  // 协程里没有 → error()
}

// 显式覆盖
withKudosContext(KudosContext()) { /* coroutine body */ }

// 只在缺失时注入（HTTP 入口推荐用 IfAbsent 版）
withKudosContextIfAbsent { /* coroutine body */ }

// 在 CoroutineScope 上启协程时显式附带
scope.launchWithKudos(ctx) { ... }
```

**桥接责任**：Ktor / WebFlux 等入口插件需在创建协程时同时把 `KudosContext` 写入两边（`KudosContextHolder.set` + `withContext(KudosContextElement(ctx))`）。**库本身不做自动桥接** —— 同步代码读 ThreadLocal、协程代码读 Element，互相不感知。

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

### 4. 启动期分两阶段：`SpringContextInitializer` 早于自动装配

```
Spring Boot 启动
  ├─ ① Initializer 阶段（极早，BeanFactory 还没启动）
  │     └─ resources/META-INF/spring.factories 里注册的
  │        org.springframework.context.ApplicationContextInitializer
  │             → SpringContextInitializer.initialize(ctx)
  │                  → SpringKit.applicationContext = ctx  ✅
  │
  ├─ ② BeanDefinitionRegistryPostProcessor 阶段
  │     └─ ComponentInitializationDispatcher.postProcessBeanDefinitionRegistry
  │          ↑ 翻译 @AutoConfigureAfter/Before → dependsOn
  │
  ├─ ③ Bean 实例化 + before/afterInit 钩子（见下节）
  │
  └─ ④ 应用业务代码运行（此时 SpringKit / LockTool / TransactionTool 全部就绪）
```

**为什么 `LockTool` 必须用 `by lazy`**：本模块里的 `object`（`SpringKit` / `LockTool` / `TransactionTool`）可能被其它 `object` 的 companion init 引用，那一刻 JVM 会触发其类加载——如果发生在 ① 之前，`SpringKit.applicationContext` 还是 null。`by lazy` 把 `getBeansOfType` 延后到第一次 **业务调用**，那时一定已经过 ①。

**`SpringContextInitializer` 是本模块唯一通过 `META-INF/spring.factories` 暴露的 SPI**（其它 `Auto*Configuration` 类通过 `@EnableKudos → ComponentInitializerSelector → ScanKit` 的扫描发现，**不走 Spring Boot 标准的 `AutoConfiguration.imports` 机制**）——所以 `@AutoConfigureAfter` 在本工程里不是 no-op，由 `ComponentInitializationDispatcher` 自行翻译为 `BeanDefinition.dependsOn`，详见下节。

### 5. 上下文初始化协调：`IComponentInitializer`

任何标 `@Configuration` 且实现 [`IComponentInitializer`](src/io/kudos/context/init/IComponentInitializer.kt) 的配置类会被 [`ComponentInitializationDispatcher`](src/io/kudos/context/init/ComponentInitializationDispatcher.kt) 接管。完整生命周期：

```
@EnableKudos (主类)
    └─ @ImportAutoConfiguration(ComponentInitializerSelector)
           └─ ScanKit.findImplementations 扫描 io.kudos.context|ability|ms
                  └─ 注入所有 IComponentInitializer 实现（除 @EnableKudos.exclusions 列出的）

启动期：ComponentInitializationDispatcher 作为 BeanDefinitionRegistryPostProcessor + SmartInstantiationAwareBeanPostProcessor：

  ① postProcessBeanDefinitionRegistry  ← Ordered.HIGHEST_PRECEDENCE
      └─ 扫描所有候选配置类，将 @AutoConfigureAfter(X) 翻译为 BeanDefinition.dependsOn=[X 的 beanName]
                              @AutoConfigureBefore(Y) 翻译为 Y 的 BeanDefinition.dependsOn += 当前 beanName（反向）

  ② setBeanFactory
      └─ 对每个 IComponentInitializer compName：
          收集 factoryBeanName == compName 的所有 child bean + compName 自身 → remainingCount[compName] = size

  ③ Bean 创建循环
      ├─ postProcessBeforeInitialization(bean=IComponentInitializer)
      │       └─ initializerInstances[beanName] = bean; bean.beforeInit()  (幂等：beforeCalled set)
      │
      ├─ postProcessAfterInitialization(bean)
      │       └─ 对所有 componentBeanNames：如果 beanName ∈ children
      │            remainingCount[comp] -= 1
      │            if reaches 0: initializerInstances[comp].afterInit()
      │
      └─ getEarlyBeanReference(bean) (循环依赖/CGLIB 早期暴露)
              └─ 当前实现转发到 postProcessAfterInitialization —— 让代理也参与计数
                 但同一 beanName 若两条路径都走过会**双扣减**，afterInit 可能提前触发
```

`@EnableKudos` 注解在应用主类上启用此机制，扫描路径目前硬编码为 `io.kudos.context` / `io.kudos.ability` / `io.kudos.ms` —— 业务方自己的包名不会被扫到（见 [已知约束 4](#已知约束与陷阱)）。`@EnableKudos(exclusions = [Foo::class, ...])` 可在启动时按 `KClass` 排除特定初始化器。

### 6. JSR-380 自定义校验器：Spring 注入

`CustomConstraintValidatorFactory` 桥接 `kudos-base.ValidationContext.validator` 与 Spring `LocalValidatorFactoryBean`，让自定义 `ConstraintValidator` 可以 `@Autowired` Spring Bean。注册方式：实现 `IConstraintValidatorProviderBean` Bean，工厂启动时扫描所有此类型 Bean 并把它们的 `provide()` 结果注册到 Hibernate Validator。

`ValidatorAutoConfiguration` 通过 `ValidationContextBridge.@PostConstruct` 把 Spring 创建的 validator 桥接到 `kudos-base.ValidationContext.validator`——让容器外的工具（`ValidationKit` 等）也能拿到 Spring 注入版本。

### 7. 并发原语统一：`ConcurrentHashMap.computeIfAbsent`

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

## 配置键参考

| 键 | 类型 | 默认 | 作用 | 引用点 |
|---|---|---|---|---|
| `kudos.retry.failed-data-path` | system property | `${java.io.tmpdir}/kudos-failed-data` | 失败数据持久化根目录 | [`RetryConfig.SYS_PROP_BASE_PATH`](src/io/kudos/context/retry/RetryConfig.kt) |
| `KUDOS_RETRY_FAILED_DATA_PATH` | env var | 同上 | 同上（system property 优先） | [`RetryConfig.ENV_VAR_BASE_PATH`](src/io/kudos/context/retry/RetryConfig.kt) |

> 本模块**目前只读以上两个外部键**。其它 Kudos 行为（锁实现选择、validator 配置、组件初始化扫描路径等）全部走 Spring Bean 注入或扫描路径硬编码，不暴露 property。

`spring.factories` 注册（唯一一项）：

```properties
# resources/META-INF/spring.factories
org.springframework.context.ApplicationContextInitializer=io.kudos.context.spring.SpringContextInitializer
```

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

---

## 8-dim 审计补遗

### A. 默认 Bean / 自动装配契约

| Bean | 来源 | 覆盖方式 |
|---|---|---|
| `failDataTaskScheduler` (TaskScheduler) | [`ContextAutoConfiguration`](src/io/kudos/context/init/ContextAutoConfiguration.kt) 单线程 `ThreadPoolTaskScheduler`，`daemon=true`、`waitForTasksToCompleteOnShutdown=false` | `@ConditionalOnMissingBean(name = "failDataTaskScheduler")` —— 应用声明同名 Bean 即覆盖 |
| `defaultValidator` (LocalValidatorFactoryBean) | [`ValidatorAutoConfiguration`](src/io/kudos/context/init/ValidatorAutoConfiguration.kt) `@Primary` `CustomConstraintValidatorFactory` | 不要再用 `@Bean("mvcValidator")` 占名——Spring Boot 4 默认禁 bean override；MVC 槽位由 `kudos-ability-web-springmvc.SpringMvcAutoConfiguration.getValidator()` 拉回此 Bean |
| `methodValidationPostProcessor` | 同上，`@Primary` | 替换默认 MVP，确保走 `CustomConstraintValidatorFactory` |
| `validationContextBridge` | 同上 | 内部 `@PostConstruct` 把 validator 写入 `kudos-base.ValidationContext.validator`。**副作用专用 bean**，重构动因：避免在 `defaultValidator()` Bean 工厂方法里写副作用 |
| `ComponentInitializationDispatcher` | [`ContextAutoConfiguration`](src/io/kudos/context/init/ContextAutoConfiguration.kt) `@Import` | `Ordered.HIGHEST_PRECEDENCE`——确保比任何业务 Bean 都早处理 BeanDefinition |

### B. 已知限制 / 安全考量

- ❗ **`LockTool` fallback 是本地 `NormalLockService`**：[`LockTool.LOCK_SERVICE`](src/io/kudos/context/lock/LockTool.kt) `by lazy` 时若 Spring 容器里没有任何 `ILockProvider` Bean，会 **静默** new 一个 `NormalLockService()`。结果是**多实例部署下分布式锁失效**——所有节点都用各自的内存锁。生产环境必须明确依赖 `kudos-ability-distributed-redisson` 等模块注册 Redisson 锁。**没有启动期断言**提示这一点。
- ✅ **`FailedDataRetryScanner.scheduleAll` 已改用 `KudosContextHolder.getOrNull()`**：`@PostConstruct`
  线程里不会再为了取 `atomicServiceCode` 隐式创建空 `KudosContext`。若启动线程没有上下文，
  锁 key 仍使用 null 维度（`failed-data-retry-${businessType}_null`）；多原子服务部署到同一进程时，
  仍建议业务侧在调度入口提供明确的原子服务维度或拆分 handler。
- ❗ **`FailedDataRetryScanner.lockRetry` 锁租约 600s 写死**：批量处理超过 10 分钟时其它实例可并行获取，造成重复处理。`IFailedDataHandler` 必须自身**幂等**或自己额外加去重；当前接口契约没强调这一点。
- ❗ **`KudosContextHolder.clear()` 无自动调用点**：模块只提供 API，调用责任完全在使用方（Filter / Interceptor / 协程入口）。线程池复用 + 漏 `clear()` → 上下文污染 + 用户/租户串号。建议在 `kudos-ability-web-*` 模块的请求结束钩子里 `finally { KudosContextHolder.clear() }`。
- ❗ **`YamlPropertySourceFactory.SOURCE_MAP` 用普通 `HashMap`**：启动期写、运行时只读还安全，但若运行时还有动态 reload propertySource 的扩展，会出竞态。当前内部读 API 已包了 `Collections.unmodifiableMap`，外部读安全。
- ❗ **`SpringKit.applicationContext` getter `error()`，setter 接受 null**：未初始化时调任意 `getBean*` 都抛 `IllegalStateException`，但属性 setter 允许重新写 null —— 测试代码可能借此 reset，但生产代码意外 `SpringKit.applicationContext = null` 后再读会让全模块挂掉。没有 `final` 锁定写入。
- ❗ **`ProxyKit` 用反射读 CGLIB 字段 `CGLIB$CALLBACK_0` / JDK proxy `h`**：跨 JVM / 跨 Spring 大版本可能失效。Spring 5 → 6 → Boot 4 升级需回归测试 [`ProxyKit.getTargetClass`](src/io/kudos/context/kit/ProxyKit.kt)。当前 **无对应单测**。
- ❗ **`ComponentInitializationDispatcher.getEarlyBeanReference` 与 `postProcessAfterInitialization` 重叠**：循环依赖场景下同一 beanName 可能两路径都进入，导致 `remainingCount` 双扣减、`afterInit` 提前触发。模块内已知未修复（见 [已知约束 5](#已知约束与陷阱)），**集成测试覆盖待补**。

### C. 测试覆盖审计

| 模块 | 测试 | 覆盖盲点 |
|---|---|---|
| `core` | `KudosContextHolderTest`（ThreadLocal / get vs getOrNull / 线程隔离） | ❌ `KudosContextElement` 协程传播无测试；`withKudosContext*` 全套未覆盖 |
| `kit` | `SpringKitTest`、`TransactionToolTest` | ❌ `ProxyKit` 完全无测试（反射拆 CGLIB / JDK proxy）|
| `lock` | `NormalLockServiceTest`（45 用例中占大头：租约/可重入两套机制、过期清理、并发竞态） | ❌ `LockTool` fallback 到 `NormalLockService` 的路径无测试 |
| `retry` | `RetryConfigTest`、`FailedDataRetryScannerTest` | ❌ `AbstractFailedDataHandler.persistFailedData` 与 `readDataFromFile` 序列化往返无测试；`scheduleAll` 与 cron 触发无测试。✅ 已覆盖调度锁维度读取不会隐式创建 `KudosContext` |
| `init` | （无） | ❌ `ComponentInitializationDispatcher` 全套未测试——`@AutoConfigureAfter` / `Before` 翻译、`beforeInit` / `afterInit` 时序、`getEarlyBeanReference` 双计数都是裸跑 |
| `validation` | （无） | ❌ `CustomConstraintValidatorFactory` 注入路径、`IConstraintValidatorProviderBean` 扫描装配未测试 |
| `config` | （无） | ❌ `YamlPropertySourceFactory` Nacos / 本地合并、`OrderProperties` 顺序保持未测试 |

### D. Kotlin / Spring 互操作 idioms

本模块多次踩过 Kotlin × Spring 的"语义错位"，沉淀的 idiom：

- **`object` 单例 + `by lazy { SpringKit.getBeansOfType(...) }`**：见 [`LockTool.LOCK_SERVICE`](src/io/kudos/context/lock/LockTool.kt)。之前用 `lateinit var + init{}` 在 `object` 加载时就读 Spring 容器——如果 `LockTool` 比 `SpringContextInitializer` 早加载（其它 `object` 的 companion init 引用 LockTool 即可触发），就 `error("applicationContext not initialized")`。改 `by lazy` 后，单线程 SYNCHRONIZED 安全 + 强制延迟到 Spring 上下文就绪。
- **`@Volatile` 在 Kotlin 顶层 `object` 属性上**：见 [`SpringKit.applicationContext`](src/io/kudos/context/kit/SpringKit.kt)。写发生在 Spring 启动线程，读发生在任意业务线程；不加 `@Volatile` 会出现 "明明 init 完成、跨线程读到 null" 的诡异 race。
- **`internal var lockProviderSupplier`**：见 [`FailedDataRetryScanner`](src/io/kudos/context/retry/FailedDataRetryScanner.kt)。Kotlin `internal` 在测试源集可见，作为"轻量级 visible-for-testing"——比 `@VisibleForTesting` 注解或 protected 拆类都简单。
- **`open class` + CGLIB 代理**：Kudos 所有 `@Configuration` / 切面切到的 Bean 都标 `open`（Kotlin 默认 final，CGLIB 子类生成失败）。已在 Gradle 配置 `kotlin("plugin.spring")` 自动 open，但**自定义注解**触发的代理仍需手动 `open`。
- **`@PostConstruct` 副作用 vs Bean 工厂方法副作用**：见 [`ValidatorAutoConfiguration.ValidationContextBridge`](src/io/kudos/context/init/ValidatorAutoConfiguration.kt)。Bean 工厂方法只该 `return new instance`，写入静态全局应放到独立 `@PostConstruct` bean 里——便于销毁/重建时副作用不重复触发。
- **Kotlin 私有字段重名父类私有字段的退路**：见 [`CustomConstraintValidatorFactory.applicationContext`](src/io/kudos/context/validation/CustomConstraintValidatorFactory.kt)。Spring 父类 `LocalValidatorFactoryBean.applicationContext` 是 `private`，Kotlin 没法访问、子类 `protected` 拿不到；为了在 `postProcessConfiguration` 里用，子类直接维护一份私有副本并 override `setApplicationContext` 同步写入。看似冗余，是 Kotlin/Spring 互操作的最小代价。
