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
- 先查"本应用 namespace"；只有显式开启
  `kudos.ability.distributed.notify.fallback-to-default-namespace=true` 时，未命中才继续查 default
  namespace
- 找到就调 `notifyProcess`；没找到只打 info 日志（不报错）

### binding 启动期校验

`notifyMqProducerBindingVerifier` 在 InitializingBean 阶段查 `BindingServiceProperties`：
找不到 `mqNotify-out-0` binding 时默认打 warn 提示运维；可配置
`kudos.ability.distributed.notify.mq.fail-on-missing-producer-binding=true` 改成启动失败。

### 消费失败策略

默认保持历史行为：反序列化失败或 listener 抛异常时记录日志并返回，消息会被底层 binder 当作
已消费处理。生产环境建议打开：

```yaml
kudos:
  ability:
    distributed:
      notify:
        mq:
          rethrow-consumer-exception: true
```

打开后异常会重新抛给 spring-cloud-stream binder，由 binder / MQ 的 retry、DLQ 或重投配置接管。

## 配置示例

```yaml
kudos:
  ability:
    distributed:
      notify:
        mq:
          fail-on-missing-producer-binding: true
          rethrow-consumer-exception: true
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

- ✅ 同 log-audit-mq："AOP 占位"模式对新人不友好；切面 / binding 未装载时可能 no-op。
  `notifyMqProducerBindingVerifier` 已支持
  `kudos.ability.distributed.notify.mq.fail-on-missing-producer-binding=true` fail-fast；
  另外可配合 stream-common 的 `kudos.ability.distributed.stream.binding-verify.*` 做统一 binding
  自检
- ✅ consumer 反序列化失败或 listener 抛异常时，已支持
  `kudos.ability.distributed.notify.mq.rethrow-consumer-exception=true` 重新抛给 binder，由 MQ
  retry / DLQ 接管；默认仍保持兼容模式，仅记录日志
- ✅ `NotifyListenerItem.get(notifyType)` 的 default namespace fallback 已改为显式开关，
  默认不跨 namespace 派发，避免多个 namespace 同 type listener 时误投
- ℹ️ binding 名 `mqNotify-out-0` / `mqNotify-in-0` 是 Spring Cloud Function 约定的一部分，
  代码中已统一收敛到 `NotifyMqBindings` 常量；如需改实际 topic，用
  `spring.cloud.stream.bindings.*.destination`，如需改 binding 别名，用 Spring Cloud Stream
  的 function binding alias 配置，而不是改 kudos 代码

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
