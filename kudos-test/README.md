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

## 改进建议（自动分析 2026-06-11）

跨子模块的共性问题（各子模块 README 末尾有同日的详细清单）：

1. **安全：固定宿主端口绑定 0.0.0.0 + 默认弱凭据**——`bindingPort` 未指定 host IP，H2/MySQL/
   Nacos（auth 关闭）/Seata/RocketMQ 在测试期间对局域网可达；在共享 CI runner 或办公网上跑
   测试时存在被外部写入的风险。详见 kudos-test-container/README。
2. **日志统一**：`TestContainerKit`、`SqlTestBase` 等核心路径用 `println` 而非 slf4j，CI 上无法
   按级别过滤。建议一次性统一替换（行为级别等价，可安全批量做）。
3. **模板重复**：14 个 `*TestContainer` 共享约 60 行复制模板，已经实际产生过 label 复制粘贴
   bug（RocketMQ `getRunningContainer` 返回 H2 容器，2026-06-11 已修复）。建议抽象公共基类，
   这是该模块组性价比最高的一项重构。
4. **自测试缺口**：kudos-test-container 完全没有测试；kudos-test-common 的测试是空壳。锁/租约
   等纯逻辑部分可不依赖 Docker 直接单测。
