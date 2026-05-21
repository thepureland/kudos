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

## 测试覆盖

`test-src/.../RabbitMqTest.kt` 用 Testcontainers 起 RabbitMQ broker（`@EnabledIfDockerInstalled`），
跑两个集成测试：
- `sendAndReceiveMessageTest` —— send + 等待 consumer 收到，5s 超时报错
- `streamExceptionTest` —— 异常消息流程

```kotlin
SpringApplication.run(RabbitMqProducerApplication::class.java, ...)
RabbitMqTestContainer.startIfNeeded(registry)
```

无 docker 环境时整个测试类被跳过。

## 已知限制

- ℹ️ 模块自身仅装配类一个文件——本质上是 spring-cloud-starter-stream-rabbit 的 thin
  re-package + kudos yml 命名空间约定；业务能力来自 stream-common 和 Spring Cloud Stream Rabbit
- ✅ 已移除 build.gradle.kts 中与本模块无关的 commented-out PostgreSQL/Flyway 依赖
- ✅ 已启用 `@Import(StreamConsumerEnvironRegistrar::class)`，rabbit 模块会参与 kudos yml
  function.definition 自动聚合
- ✅ 默认 yml 的 RabbitMQ password 已改为 `${RABBITMQ_PASSWORD:guest}`，生产部署可直接通过
  环境变量覆盖；本地开发仍保留 guest 默认值

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common"))
api(libs.spring.cloud.starter.stream.rabbit)

testImplementation(project(":kudos-test:kudos-test-container"))
testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
testImplementation(libs.h2database.h2)
testImplementation(libs.spring.boot.starter.web)
```
