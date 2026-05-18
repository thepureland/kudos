# kudos-ability-distributed-stream-rocketmq

RocketMQ 作为 spring-cloud-stream broker 的接入模块。

比 stream-rabbit / stream-kafka 多两个模块特有文件：
- `RocketMqProperties` —— 暴露 `nameSrvAddr` + `saveException` 给 `RocketMqBatchConsumer` 使用
- `RocketMqBatchConsumer` —— 基于 `DefaultLitePullConsumer` 的批量拉取消费者

## 设计要点

### 标准 spring-cloud-stream 路径

`RocketMqAutoConfiguration` 继承 `StreamCommonConfiguration`——与 rabbit / kafka 同款。
业务侧用 `@MqProducer` / `@MqConsumer` 注解即可，broker 选 RocketMQ 时不必改业务代码。

### `RocketMqBatchConsumer` 批量拉取消费

跟 spring-cloud-stream consumer 平行的"原生 RocketMQ 客户端"封装，业务侧需要更细粒度
（batch size + 提交时机）控制时用。

```kotlin
val consumer = RocketMqBatchConsumer<MyEvent>(
    groupName = "my-group",
    topic = "MY_TOPIC",
    batchProcessSize = 1000,
    pullTime = 5_000L,
    saveException = true
)
consumer.start { batch ->
    batch?.forEach { item -> /* 业务处理 */ }
}
```

特性：
- 守护线程持续 `consumer.poll()`；消息累积达 `batchProcessSize` 或距上次处理超 `pullTime` 触发
- 业务处理成功 → `consumer.commit()` 提交消费位点
- 业务处理失败 + `saveException=true` → 失败消息入 `sys_mq_fail_msg` 表 + 也提交位点（避免 MQ 堵塞）
- 业务处理失败 + `saveException=false` → 不提交，下次重试

**已知 CPU 问题（仍未修）**：busy-loop —— `poll()` 返回空时立即再次 poll，没有 sleep / backoff，
低流量场景会空转。修复需要在 `daemonThread` 循环里加 `Thread.sleep(100)`-ish 退避。

## 配置示例

```yaml
spring:
  cloud:
    stream:
      rocketmq:
        binder:
          name-server: localhost:9876
        bindings:
          <bindingName>:
            consumer:
              push:
                threadCount: 4
              messageModel: CLUSTERING

kudos:
  ability:
    distributed:
      stream:
        save-exception: true
```

## 已知限制 / 后续工作

- ❗ `RocketMqBatchConsumer.start` 的循环没有 sleep / backoff——空轮询时 CPU 占用偏高。
  典型修法：`if (poll == null || poll.isEmpty()) Thread.sleep(pollIdleSleepMs)`，
  `pollIdleSleepMs` 可配置（建议默认 100ms）
- ❗ `RocketMqBatchConsumer.toProcessBizData` 反序列化用 JDK `ObjectInputStream`——要求消息
  类实现 `Serializable`，且对反序列化漏洞链敏感。生产环境建议改成显式 JSON / protobuf
- ❗ `RocketMqBatchConsumer.start` 入参 `bizBatchProcess: (MutableList<BatchConsumerItem<T?>?>?) -> Unit`
  —— 嵌套可空类型对业务侧不友好；过多 `?` 来源是 Java 互操作历史。可改成 `(List<BatchConsumerItem<T>>) -> Unit`
- ❗ `RocketMqBatchConsumer.destroy()` 已修复：除了 `isRunning=false` 也调用 `consumer.shutdown()`
  释放 RocketMQ 客户端的 netty / 消费位点资源
- ❗ `@Import(StreamConsumerEnvironRegistrar::class)` 注释停用——同 rabbit / kafka 模块

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common"))
api(libs.spring.cloud.starter.stream.rocketmq)

testImplementation(project(":kudos-test:kudos-test-container"))
```
