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

### `AbstractConfigChangeListener` 作为预留扩展点

```kotlin
abstract class AbstractConfigChangeListener : AbstractListener()
```

**没有添加任何行为**——纯转发 nacos SDK 的 `AbstractListener`。存在价值是给业务代码一个
"kudos 命名空间下的基类"，**未来本模块要加埋点 / 上下文透传 / 重试时业务调用方不需要改动**。
现在删了它没有损失；保留它是 API 稳定性投资。

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

依赖 `NacosTestContainer`（启 nacos-server docker image）。**当前在本机环境 nacos
testcontainer 启动可能不稳定**——非本模块代码问题，是 nacos server image 启动延迟敏感。
预先 `docker pull nacos/nacos-server:v3.1.1-slim` + 跑几次预热可缓解。

## 已知限制 / 后续工作

- ✅ **`NacosConfigDataFinder` 已注册并启用**：模块 classpath 上有
  `META-INF/services/io.kudos.context.config.IConfigDataFinder` 文件，
  `YamlPropertySourceFactory.loadFromConfigCenter` 会把 Nacos 拉到的 PropertySource
  叠加在本地 yml 之上。要退出请在业务侧覆写 SPI 或排除本模块
- ✅ `NacosConfigServiceListener` 已改为按 `(serverAddr, namespace)` 分桶缓存
  `ConfigService`——多 Nacos 集群可并存，同集群同 namespace 仍只创建一份重型对象
- ❗ 模块自身**几乎不做任何事**——本质是 spring-cloud-alibaba 的 thin façade。如果业务方
  愿意直接用 spring-cloud-alibaba 的 starter + nacos SDK，本模块可以删
- ❗ `AbstractConfigChangeListener` 现在仅是空转发。要在不破坏业务调用的前提下加埋点 /
  上下文透传 / 重试，本模块要先把这个 hook 利用起来。当前是占坑
- ❗ 测试依赖 `NacosTestContainer`，本机 / CI 偶发启动失败——nacos server 启动慢 (~30s)
  + 健康探测严格。需要时可以调大 `startupTimeout` 或换镜像
- ❗ 没有"配置加密 / 解密 hook"——nacos 自身支持 KMS 加密，但本封装没有对应桥接，业务方
  自行处理

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.alibaba.cloud.nacos.config)

testImplementation(project(":kudos-test:kudos-test-container"))
```
