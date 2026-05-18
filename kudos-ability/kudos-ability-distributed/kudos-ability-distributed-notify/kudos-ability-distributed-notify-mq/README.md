# kudos-ability-distributed-notify-mq

`kudos-ability-distributed-notify-common` 的 MQ 投递实现。基于 spring-cloud-stream，发出走
`mqNotify-out-0` binding，消费走 `mqNotify-in-0` binding。

## 设计要点

### `NotifyMqProducer.notify` 的 "AOP 占位"模式

```kotlin
@MqProducer(topic = "mqNotify", bindingName = "mqNotify-out-0")
override fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean {
    if (messageVo.notifyType.isBlank()) { ...; return false }
    return true
}
```

**方法体的 `return true` 是占位**——真正的 MQ 发送由 `@MqProducer` 切面（在 stream-common 模块）
拦截 `messageVo` 完成。同款模式见 `kudos-ability-log-audit-mq.MqAuditService`。

**陷阱**：如果应用没引入 `kudos-ability-distributed-stream-*` 的 producer 切面，本方法是
no-op，通知静默丢失。

### `mqNotify` 消费者

`NotifyMqAutoConfiguration.mqNotify()` 注册一个 spring-cloud-stream consumer：
- 从 `mqNotify-in-0` binding 收 `Message<StreamMessageVo<JSONObject>>`
- payload 反序列化为 `NotifyMessageVo`
- 按 `notifyType` 在 `NotifyListenerItem` 查找 listener
- 先查"本应用 namespace"，没命中再查 default namespace
- 找到就调 `notifyProcess`；没找到只打 info 日志（不报错）

### binding 启动期校验

`notifyMqProducerBindingVerifier` 在 InitializingBean 阶段查 `BindingServiceProperties`：
找不到 `mqNotify-out-0` binding 时打 warn 提示运维。**不 fail-fast**——只是友好提醒。

## 配置示例

```yaml
spring:
  cloud:
    stream:
      bindings:
        mqNotify-out-0:
          destination: NOTIFY_TOPIC
        mqNotify-in-0:
          destination: NOTIFY_TOPIC
          group: ${spring.application.name}   # consumer group——多实例 competing consume
      function:
        definition: mqNotify
```

## 已知限制

- ❗ 同 log-audit-mq："AOP 占位"模式对新人不友好；切面未装载时通知静默丢失。运维只能从
  `notifyMqProducerBindingVerifier` 的 warn 日志判断。建议加 health check 检查 binding 状态
- ❗ consumer 反序列化失败只打 error 日志不抛错——丢失的消息无补偿
- ❗ `NotifyListenerItem.get(notifyType)`（fallback to default namespace）在多个 namespace
  各自注册了同 type listener 时会派发到 default 那个，可能不符合业务预期
- ❗ binding 名 `mqNotify-out-0` / `mqNotify-in-0` 硬编码

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify:kudos-ability-distributed-notify-common"))
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common"))
api(libs.alibaba.fastjson2)

testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-rocketmq"))
testImplementation(project(":kudos-test:kudos-test-container"))
testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
testImplementation(libs.h2database.h2)
testImplementation(libs.spring.boot.starter.web)
```
