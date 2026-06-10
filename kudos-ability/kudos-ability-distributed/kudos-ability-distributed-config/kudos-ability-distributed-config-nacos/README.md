# kudos-ability-distributed-config-nacos

Nacos 作为配置中心的接入封装。**90% 的能力来自 spring-cloud-alibaba 的
`alibaba.cloud.nacos.config` starter**——本模块只补充：

1. **`NacosConfigDataFinder`**：把 spring-cloud-alibaba 的 `NacosPropertySourceRepository`
   适配成 kudos 的 `IConfigDataFinder` SPI，已通过
   `resources/META-INF/services/io.kudos.context.config.IConfigDataFinder` 注册
2. **`NacosConfigServiceListener`**：业务侧 `addListener` / `removeListener` 的薄封装，
   不必直接接触 nacos `ConfigService`
3. **`AbstractConfigChangeListener`**：透传 nacos SDK 的 `AbstractListener`——预留扩展点，
   将来想挂埋点 / 重试 / 上下文透传时业务代码不必改

业务侧大部分时候**直接用 `@Value` / `@ConfigurationProperties` 就够了**——配置中心拉
yml 由 spring-cloud-alibaba 自动完成，本模块的"显式监听" API 仅在"需要在配置变更时
执行回调逻辑"时使用。

## 设计要点

### `NacosConfigServiceListener` 按 (serverAddr, namespace) 分桶缓存

```kotlin
companion object {
    private val SERVICE_CACHE = ConcurrentHashMap<CacheKey, ConfigService>()
    private fun obtainConfigService(properties): ConfigService =
        SERVICE_CACHE.computeIfAbsent(CacheKey.of(properties)) { NacosFactory.createConfigService(properties) }
}
```

- `ConfigService` 是 nacos SDK 的重型对象（含 HTTP / gRPC 客户端、调度线程等）——同集群同
  namespace 应当全进程复用一份
- **多集群 / 多 namespace 支持**：按 `(serverAddr, namespace)` 分桶，不同集群独立 ConfigService
  实例，互不污染
- `ConcurrentHashMap.computeIfAbsent` 保证同 key 只 new 一次，无需双重检查 + synchronized
- 历史背景：旧实现是 `@Volatile var configService` 单例 + 双重检查，首次胜出，后续 properties
  被静默忽略——同进程内挂两个 Nacos 集群直接错乱。本次修复改为分桶

### `AbstractConfigChangeListener` 作为监听 hook

```kotlin
abstract class AbstractConfigChangeListener : AbstractListener() {
    override fun receiveConfigInfo(configInfo: String?) {
        beforeConfigChanged(configInfo)
        runCatching { onConfigChanged(configInfo) }
            .onSuccess { afterConfigChanged(configInfo, null) }
            .onFailure { afterConfigChanged(configInfo, it); throw it }
    }
}
```

业务侧推荐覆盖 `onConfigChanged(configInfo)`；如需埋点 / 重试 / 上下文透传，可覆盖
`beforeConfigChanged` / `afterConfigChanged`。为了兼容旧代码，仍可直接覆盖
`receiveConfigInfo`，但这样会绕过基类 hook。

### 配置值解密 hook

`NacosConfigDataFinder` 支持通过 `ServiceLoader<NacosConfigValueDecryptor>` 处理配置值：

```kotlin
class KmsDecryptor : NacosConfigValueDecryptor {
    override fun supports(value: String) = value.startsWith("ENC(")
    override fun decrypt(value: String): String = ...
}
```

模块不内置具体 KMS / 密钥管理实现，避免把云厂商和密钥生命周期强耦合到基础封装里。

### `NacosConfigAutoConfiguration` 不注册任何 bean

```kotlin
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class NacosConfigAutoConfiguration : IComponentInitializer {
    override fun getComponentName() = "kudos-ability-distributed-config-nacos"
}
```

类体内就一个 `getComponentName()`。装配 Nacos 客户端的真正工作交给
`alibaba.cloud.nacos.config` starter；本类的存在仅为：
- 让 kudos 自定义 SPI 调度器 `ComponentInitializerSelector` 识别本模块
- 通过 `@AutoConfigureAfter(ContextAutoConfiguration::class)` 声明 kudos 上下文先就绪

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/NacosConfigAutoConfiguration` | 装配入口（不注册 bean） |
| `NacosConfigDataFinder` | spring-cloud-alibaba ↔ kudos IConfigDataFinder 适配（已通过 ServiceLoader 注册） |
| `listener/NacosConfigServiceListener` | `ConfigService.addListener` / `removeListener` 薄封装 |
| `listener/AbstractConfigChangeListener` | `nacos AbstractListener` 转发基类（预留扩展点） |

## 配置示例

依赖 spring-cloud-alibaba 的标准 Nacos 配置：

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: localhost:8848
        namespace: ${NACOS_NAMESPACE:public}
        group: DEFAULT_GROUP
        file-extension: yaml
  config:
    import:
      - "nacos:application.yaml"      # 等同 Spring Boot 4 标准格式
```

## 测试覆盖

- `NacosConfigTest.testPublishAndRead` —— 发布配置 + 客户端读取
- `NacosConfigTest.testListener` —— 注册 listener + 接收变更回调
- `NacosConfigDataFinderTest` —— 纯单测覆盖 SPI 注册、按 dataId 查找、解密 hook
- `AbstractConfigChangeListenerTest` —— 纯单测覆盖配置变更前后 hook 与异常路径

依赖 `NacosTestContainer`（启 nacos-server docker image）。**当前在本机环境 nacos
testcontainer 启动可能不稳定**——非本模块代码问题，是 nacos server image 启动延迟敏感；
测试容器已改为探测 readiness API，并把 startup timeout 放宽到 90 秒。

## 已知限制 / 后续工作

- ✅ **`NacosConfigDataFinder` 已注册并启用**：模块 classpath 上有
  `META-INF/services/io.kudos.context.config.IConfigDataFinder` 文件，
  `YamlPropertySourceFactory.loadFromConfigCenter` 会把 Nacos 拉到的 PropertySource
  叠加在本地 yml 之上。要退出请在业务侧覆写 SPI 或排除本模块
- ✅ `NacosConfigServiceListener` 已改为按 `(serverAddr, namespace)` 分桶缓存
  `ConfigService`——多 Nacos 集群可并存，同集群同 namespace 仍只创建一份重型对象
- ℹ️ 模块自身**仍是 thin façade**——核心配置中心能力来自 spring-cloud-alibaba。如果业务方
  愿意直接用 spring-cloud-alibaba 的 starter + nacos SDK，本模块可以删
- ✅ `AbstractConfigChangeListener` 已提供 `beforeConfigChanged` / `onConfigChanged` /
  `afterConfigChanged` hook，业务侧可在不包 listener 的情况下挂埋点 / 上下文透传 / 重试
- ✅ `NacosTestContainer` 已从页面探测改为 readiness API 探测，并把 startup timeout 放宽到
  90 秒，降低 nacos server 镜像启动慢导致的 CI 抖动
- ✅ 已提供 `NacosConfigValueDecryptor` 解密 SPI；`NacosConfigDataFinder` 返回配置时会对
  String 值逐项调用匹配的 decryptor，具体 KMS / 密钥实现由业务侧通过 ServiceLoader 接入

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.alibaba.cloud.nacos.config)

testImplementation(project(":kudos-test:kudos-test-container"))
```

## 改进建议（自动分析 2026-06-11）

- **【功能】缺少"配置热更新 → kudos 侧自动生效"链路**：
  `src/io/kudos/ability/distributed/config/nacos/NacosConfigDataFinder.kt`
  只在启动时把 Nacos PropertySource 适配给 `YamlPropertySourceFactory`；配置变更后 kudos 叠加的
  PropertySource 不会刷新，也没有桥接 Spring 的 `EnvironmentChangeEvent` / `@RefreshScope`。
  业务要感知变更只能手工 `NacosConfigServiceListener.addListener`。建议提供"变更 → 重建
  PropertySource → 发布刷新事件"的开箱回调机制。
- **【安全】`ConfigService` 缓存 key 不含凭证指纹**：
  `src/io/kudos/ability/distributed/config/nacos/listener/NacosConfigServiceListener.kt`
  `SERVICE_CACHE` 按 `(serverAddr, namespace)` 分桶（有意不把 password/accessKey 折进 key），
  但副作用是：同地址同 namespace、**不同凭证**的第二次构造会静默复用第一份连接，第二份凭证被
  忽略。建议把凭证摘要（如 SHA-256 截断）纳入 key，或检测到凭证不一致时 warn。
- **【可观测性】解密 hook 无日志且异常会中断整个配置装载**：
  `src/io/kudos/ability/distributed/config/nacos/NacosConfigDataFinder.kt`
  `decryptIfNecessary` 中某个 decryptor `decrypt` 抛异常会向上传播、整个 PropertySource 加载失败，
  且无法定位是哪个 key；解密成功/跳过也无 debug 日志。建议逐 key 捕获并在异常信息中带上
  属性名（不带值）。
- **【扩展性】decryptor 只支持 `ServiceLoader`，拿不到 Spring 依赖**：
  `src/io/kudos/ability/distributed/config/nacos/decrypt/NacosConfigValueDecryptor.kt`
  ServiceLoader 实例化无法注入容器 bean（典型如 KMS client）。鉴于该 finder 在容器就绪前运行，
  可考虑支持"静态注册"入口（`NacosConfigDataFinder.registerDecryptor(...)`）作为补充。
- **【测试】多集群分桶缓存逻辑无单测**：
  `src/io/kudos/ability/distributed/config/nacos/listener/NacosConfigServiceListener.kt`
  `CacheKey.of` 的提取逻辑与"同 key 复用、异 key 隔离"语义目前只有 README 描述、无测试锁定
  （`NacosConfigTest` 走的是单集群集成路径）。
