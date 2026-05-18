# kudos-ability-distributed-config-nacos

Nacos 作为配置中心的接入封装。**90% 的能力来自 spring-cloud-alibaba 的
`alibaba.cloud.nacos.config` starter**——本模块只补充：

1. **`NacosConfigDataFinder`**：把 spring-cloud-alibaba 的 `NacosPropertySourceRepository`
   适配成 kudos 的 `IConfigDataFinder` SPI（**当前未启用 / dead code**——见下方"已知限制"）
2. **`NacosConfigServiceListener`**：业务侧 `addListener` / `removeListener` 的薄封装，
   不必直接接触 nacos `ConfigService`
3. **`AbstractConfigChangeListener`**：透传 nacos SDK 的 `AbstractListener`——预留扩展点，
   将来想挂埋点 / 重试 / 上下文透传时业务代码不必改

业务侧大部分时候**直接用 `@Value` / `@ConfigurationProperties` 就够了**——配置中心拉
yml 由 spring-cloud-alibaba 自动完成，本模块的"显式监听" API 仅在"需要在配置变更时
执行回调逻辑"时使用。

## 设计要点

### `NacosConfigServiceListener` 的进程级单例陷阱

```kotlin
companion object {
    @Volatile
    private var configService: ConfigService? = null
}
```

- `ConfigService` 是 nacos SDK 的重型对象（包含 HTTP / gRPC 客户端、调度线程等）——按设计应当全
  进程复用一份
- 当前实现：**第一个被实例化的 `NacosConfigServiceListener` 创建 `ConfigService`，后续实例
  的 `serverAddr` / Properties 被忽略**——同一个进程内不能给两个不同的 Nacos 集群挂监听
- 旧实现还有并发竞争（两线程同时通过 `if (configService == null)` 各 new 一份）—— 已用
  双重检查 + `synchronized(NacosConfigServiceListener::class.java)` 修正

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
| `NacosConfigDataFinder` | spring-cloud-alibaba ↔ kudos IConfigDataFinder 适配（未启用） |
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

- ❗ **`NacosConfigDataFinder` 实际是 dead code**：模块下没有
  `META-INF/services/io.kudos.context.config.IConfigDataFinder` 注册文件，
  `YamlPropertySourceFactory` 里的 `ServiceLoader.load(IConfigDataFinder::class.java)`
  找不到本类。启用方式：
  ```
  echo io.kudos.ability.distributed.config.nacos.NacosConfigDataFinder \
      > resources/META-INF/services/io.kudos.context.config.IConfigDataFinder
  ```
  当前不主动加这个文件是为了不擅自改变现有装配行为——业务方按需启用
- ❗ `NacosConfigServiceListener.configService` 进程级单例 + 首次写入胜出——**多 Nacos 集群
  场景不工作**。需要时业务侧绕开本封装，直接 `NacosFactory.createConfigService(...)` 自管
  生命周期
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
