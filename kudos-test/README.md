# kudos-test

测试支持库集合——封装 testcontainers、Spring 测试上下文、契约测试基类、RDB 测试辅助。
**所有子模块的代码都只供测试期使用**（其他业务模块通过 `testImplementation` 引用）。

## 子模块

| 子模块 | 角色 |
|---|---|
| [`kudos-test-common`](kudos-test-common/README.md) | `@EnableKudosTest` 注解 + Spring 测试上下文装配 |
| [`kudos-test-container`](kudos-test-container/README.md) | Testcontainers 封装：PostgreSQL / MySQL / H2 / Redis / MinIO / RabbitMQ / Kafka / RocketMQ / Nacos / Seata / SMTP / WireMock 等 |
| [`kudos-test-rdb`](kudos-test-rdb/README.md) | RDB 测试基类（`RdbTestBase` / `SqlTestBase` / `RdbAndRedisCacheTestBase` / `CacheTestResetSupport`） |
| [`kudos-test-api`](kudos-test-api/README.md) | 契约测试（provider / consumer 双侧基类） |

## 使用约定

```kotlin
@EnableKudosTest  // 自动装配 kudos 上下文 + Spring test context
@EnabledIfDockerInstalled  // 仅在本机有 Docker 时跑（避免 CI 没 Docker 时整批挂）
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

`TestContainerKit` 维护一个进程级单例的"容器复用"逻辑——多个测试类共享同一份 Docker
容器，避免每个测试反复启停。
