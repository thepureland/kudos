# kudos-test-container

Testcontainers 的统一封装——给业务测试提供"按需启动 + 跨测试复用"的容器实例。

## 内容

### 注解
- `@EnabledIfDockerInstalled` —— 本机没 Docker 时跳过测试，不让整批 CI 挂掉

### 工具
- `kit/TestContainerKit` —— 进程级容器注册表，复用同一份容器
- `kit/XGenericContainer` —— 加强版 `GenericContainer`（带 host / port 解析）
- `kit/DockerKit` —— Docker daemon 探测

### 容器封装（一容器一文件）

| 容器 | 用途 |
|---|---|
| `PostgresTestContainer` / `MySqlTestContainer` / `H2TestContainer` | RDB |
| `RedisTestContainer` | Redis |
| `MinioTestContainer` | 对象存储 |
| `SmtpTestContainer` | 邮件 |
| `RabbitMqTestContainer` / `KafkaTestContainer` / `RocketMqTestContainer` | MQ |
| `NacosTestContainer` | 注册 / 配置中心 |
| `SeataTestContainer` | 分布式事务 |
| `WireMockTestContainer` | HTTP Mock |

## 使用模式

```kotlin
@EnableKudosTest
@EnabledIfDockerInstalled
class MyDbTest {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            PostgresTestContainer.startIfNeeded(registry)
        }
    }
}
```

`startIfNeeded(registry)`：
- 同进程多次调用复用同一容器
- 自动把 `host` / `port` / `username` / `password` 注册到 Spring `DynamicPropertyRegistry`

## 已知限制

- ❗ 容器版本 hardcoded 在各 `*TestContainer` 文件——升级镜像需要同步改代码
- ❗ 某些镜像在本地拉取慢 / 启动慢（如 Nacos / Seata），测试期偶发超时
