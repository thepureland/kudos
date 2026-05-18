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

## 已知限制

- ❗ 同 stream-rabbit：模块自身仅装配类——是 spring-cloud-starter-stream-kafka 的 thin
  re-package
- ❗ `@Import(StreamConsumerEnvironRegistrar::class)` 注释停用了；启用后才能自动聚合 multi-binding
  的 function.definition
- ❗ 没有自定义 Kafka 头序列化策略——业务侧若要透传非标准 header 需自行扩展

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common"))
api(libs.spring.cloud.starter.stream.kafka)

testImplementation(project(":kudos-test:kudos-test-container"))
```
