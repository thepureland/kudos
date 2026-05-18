# kudos-ability-distributed-stream-rabbit

RabbitMQ 作为 spring-cloud-stream broker 的接入模块。**实质上仅一个 AutoConfiguration**——
继承 `StreamCommonConfiguration`、绑定 `kudos-ability-distributed-stream-rabbit.yml`，
具体行为都来自 stream-common + spring-cloud-starter-stream-rabbit。

## 配置示例

```yaml
spring:
  cloud:
    stream:
      rabbit:
        bindings:
          <bindingName>:
            consumer:
              auto-bind-dlq: true
              dlq-ttl: 600000
            producer:
              transacted: false
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: ${RABBIT_PASS}
```

实际 producer / consumer 装配走 `StreamCommonConfiguration` 父类的所有 bean——含
`@MqProducer` 切面、`StreamProducerHelper`、失败消息持久化、binding 启动校验。

## 已知限制

- ❗ 模块自身仅装配类一个文件——本质上是 spring-cloud-starter-stream-rabbit 的 thin
  re-package + kudos yml 命名空间约定
- ❗ build.gradle.kts 有一行 commented-out 注释依赖（flyway-database-postgresql）；与本模块
  实际无关，可移除
- ❗ `@Import(StreamConsumerEnvironRegistrar::class)` 注释停用了——意味着该模块下的
  function.definition 不会被自动聚合。要启用 multi-consumer aggregation 需取消注释

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common"))
api(libs.spring.cloud.starter.stream.rabbit)

testImplementation(project(":kudos-test:kudos-test-container"))
testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
testImplementation(libs.h2database.h2)
testImplementation(libs.spring.boot.starter.web)
```
