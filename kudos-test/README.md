# kudos-test

测试支持库集合——封装 testcontainers、Spring 测试上下文、契约测试基类、RDB 测试辅助。
**所有子模块的代码都只供测试期使用**（其他业务模块通过 `testImplementation` 引用）。

## 子模块

| 子模块 | 角色 |
|---|---|
| [`kudos-test-common`](kudos-test-common/README.md) | `@EnableKudosTest` 元注解 + `SpringKitTestContextListener`（修上下文缓存复用时 SpringKit 静态字段漂移的 bug） |
| [`kudos-test-container`](kudos-test-container/README.md) | Testcontainers 封装：PG / MySQL / H2 / Redis / MinIO / RabbitMQ / Kafka / RocketMQ / Nacos / Seata / SMTP / WireMock 等；按 Docker label 跨 JVM 复用容器 |
| [`kudos-test-rdb`](kudos-test-rdb/README.md) | `SqlTestBase` / `RdbTestBase` / `RdbAndRedisCacheTestBase`——SQL fixture 加载 + 容器编排 + 缓存重置三层基类 |
| [`kudos-test-api`](kudos-test-api/README.md) | 契约测试（Spring Cloud Contract 5.x，provider / consumer 双侧基类） |

## 模块依赖层次

```
kudos-test-common      ← @EnableKudosTest + SpringKit 同步
        ▲
        │ api 依赖
        │
kudos-test-container   ← TestContainerKit + 各 *TestContainer
        ▲
        │ api 依赖
        │
kudos-test-rdb         ← RDB / Redis 基类（继承 SqlTestBase）
                       
kudos-test-api          ← 契约测试（独立分支，只 api 依赖 kudos-test-common）
```

`kudos-test-common` 是所有子模块的基础——`@EnableKudosTest` 的语义和上下文同步逻辑由它
提供。`kudos-test-container` 在它之上加 Docker 容器编排，`kudos-test-rdb` 再叠 SQL
fixture / 缓存重置。契约测试这条分支独立，只用 common 不用 container（生成的测试用
MockMvc / WireMock，不直接拉 Docker）。

## 使用约定

```kotlin
@EnableKudosTest                // 装 Spring 上下文 + 同步 SpringKit
@EnabledIfDockerInstalled       // 本机无 Docker 则跳过（不让 CI 整批红）
class MyServiceTest {
    @Resource lateinit var myService: MyService

    @Test
    fun test() { ... }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            PostgresTestContainer.startIfNeeded(registry)
        }
    }
}
```

## 装载策略

`TestContainerKit` 维护一个"按 Docker label 查找运行中容器"的注册逻辑——多个测试类
**乃至多个 JVM 周期**共享同一份容器，避免每次反复启停。详见
[kudos-test-container/README](kudos-test-container/README.md#容器复用机制)。
