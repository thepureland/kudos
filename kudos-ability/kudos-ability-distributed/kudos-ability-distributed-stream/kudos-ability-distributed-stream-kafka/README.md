# kudos-ability-distributed-stream-kafka

Kafka 作为 spring-cloud-stream broker 的接入模块。**实质上仅一个 AutoConfiguration**——
继承 `StreamCommonConfiguration`、绑定 `kudos-ability-distributed-stream-kafka.yml`，
具体行为来自 stream-common + spring-cloud-starter-stream-kafka。

## 配置示例

```yaml
spring:
  kafka:
    security:
      protocol: ${KAFKA_SECURITY_PROTOCOL:PLAINTEXT}
  cloud:
    stream:
      kafka:
        binder:
          brokers: ${KAFKA_BROKERS:localhost:9092}
          headers: ${KAFKA_BINDER_HEADERS:}
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

- ℹ️ 同 stream-rabbit：模块自身仅装配类——是 spring-cloud-starter-stream-kafka 的 thin
  re-package + kudos yml 命名空间约定；业务能力来自 stream-common 和 Spring Cloud Stream Kafka
- ✅ 已启用 `@Import(StreamConsumerEnvironRegistrar::class)`，Kafka 模块会参与 kudos yml
  function.definition 自动聚合
- ✅ 默认 yml 已暴露 `KAFKA_BINDER_HEADERS` → `spring.cloud.stream.kafka.binder.headers`，
  业务侧透传非标准 header 可直接通过环境变量覆盖
- ✅ 默认 yml 的 brokers 已改为 `${KAFKA_BROKERS:localhost:9092}`，生产部署可直接通过外部化
  配置覆盖；本地开发仍保留 localhost 默认值
- ✅ 默认 yml 已暴露 `KAFKA_SECURITY_PROTOCOL` → `spring.kafka.security.protocol`，缺省
  `PLAINTEXT`；SASL / SSL 的 credentials、truststore 等仍应走业务侧外部化密钥配置

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common"))
api(libs.spring.cloud.starter.stream.kafka)

testImplementation(project(":kudos-test:kudos-test-container"))
```
