# kudos-ability-distributed-stream-kafka

Kafka 作为 spring-cloud-stream broker 的接入模块。**实质上仅一个 AutoConfiguration**——
继承 `StreamCommonConfiguration`、绑定 `kudos-ability-distributed-stream-kafka.yml`，
具体行为来自 stream-common + spring-cloud-starter-stream-kafka。

## 配置示例

```yaml
spring:
  cloud:
    stream:
      kafka:
        binder:
          brokers: localhost:9092
          replication-factor: 1
          auto-create-topics: true
        bindings:
          <bindingName>:
            consumer:
              ack-mode: MANUAL_IMMEDIATE
              start-offset: latest
            producer:
              compression-type: lz4
              sync: false
```

producer / consumer 装配通过 `StreamCommonConfiguration` 父类：含 `@MqProducer` 切面、
`StreamProducerHelper`、失败消息持久化、binding 启动校验。

## 测试覆盖

`test-src/.../KafkaTest.kt` 用 Testcontainers 启动 Kafka broker（`@EnabledIfDockerInstalled`），
跑两个集成测试：
- `sendAndReceiveMessageTest` —— 发送 + consumer 5s 内收到，超时报错
- `streamExceptionTest` —— consumer 端抛异常，触发 `StreamGlobalExceptionHandler` 路径

```kotlin
kafkaContainer = KafkaTestContainer.startIfNeeded(registry)
SpringApplication.run(KafkaProducerApplication::class.java,
    "--spring.cloud.stream.kafka.binder.brokers=${...}")
```

无 docker 环境时跳过整个测试类。

## 已知限制

- ❗ 同 stream-rabbit：模块自身仅装配类——是 spring-cloud-starter-stream-kafka 的 thin
  re-package
- ✅ 已启用 `@Import(StreamConsumerEnvironRegistrar::class)`，Kafka 模块会参与 kudos yml
  function.definition 自动聚合
- ❗ 没有自定义 Kafka header 透传策略——业务侧透传非标准 header 需自行配置
  `spring.cloud.stream.kafka.binder.headers`
- ❗ 默认 yml `brokers: localhost:9092` 仅本地开发可用；生产部署必须通过外部化配置覆盖
- ❗ 默认 yml 未启用 SASL / SSL——生产场景需自行配置 `spring.kafka.security.protocol` 等

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common"))
api(libs.spring.cloud.starter.stream.kafka)

testImplementation(project(":kudos-test:kudos-test-container"))
```
