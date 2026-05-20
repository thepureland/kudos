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

**CPU busy-loop 已修**：低流量场景 `poll()` 返回空集合时，循环原本立即重 poll → CPU 100%。
现在加 `IDLE_POLL_SLEEP_MS = 100L` 空轮询休眠 + `InterruptedException` 处理。位点提交逻辑
不受影响——pullTime 触发的强制 commit 仍每 5s 跑一次。

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

## 测试覆盖

`test-src/.../RocketMqTest.kt` 用 Testcontainers 启动 NameServer + Broker（`@EnabledIfDockerInstalled`）
跑 send / receive / 异常路径 3 个集成测试。无 docker 环境跳过。

`RocketMqBatchConsumer` 本身**没有单元测试**——批量阈值 / 时间窗 / 异常持久化都是
集成验证。建议补 fake `DefaultLitePullConsumer` 单测，覆盖：
- 数量阈值触发处理（batchProcessSize 命中）
- 时间窗触发处理（pullTime 超时）
- saveException=true 时失败仍 commit + 入表
- saveException=false 时失败不 commit
- 空轮询 idle backoff

## 已修复（本轮 8 维度审计）

- ✅ `RocketMqBatchConsumer.start` CPU busy-loop——`poll()` 返回空时 `Thread.sleep(IDLE_POLL_SLEEP_MS)`
  让出 CPU，并正确处理 `InterruptedException`（旧实现完全没有 sleep）
- ✅ `RocketMqBatchConsumer.destroy()` 同时调 `consumer.shutdown()` 释放 netty / 位点资源
  （旧实现只置 isRunning=false 让循环退出）

## 已知限制 / 后续工作

- ❗ `RocketMqBatchConsumer.toProcessBizData` 反序列化用 JDK `ObjectInputStream`——要求消息
  类实现 `Serializable`，且对反序列化漏洞链敏感。生产环境建议改成显式 JSON / protobuf。
  改动是**wire-breaking**——需所有 producer + consumer 同步切换
- ❗ `RocketMqBatchConsumer.start` 入参 `bizBatchProcess: (MutableList<BatchConsumerItem<T?>?>?) -> Unit`
  —— 嵌套可空类型对业务侧不友好；过多 `?` 来源是 Java 互操作历史。可改成
  `(List<BatchConsumerItem<T>>) -> Unit`，但需全部 callsite 配合
- ❗ `RocketMqProperties.instance` 用 `SpringKit.getBean` 静态访问 bean——单测难替换，
  耦合 Spring 容器。改成 ctor 注入更好测
- ❗ `RocketMqBatchConsumer` 业务异常时只调 `consumer.commit()`——没有死信队列概念，靠
  `sys_mq_fail_msg` 表替代 DLQ
- ✅ 已启用 `@Import(StreamConsumerEnvironRegistrar::class)`，RocketMQ 模块会参与 kudos yml
  function.definition 自动聚合
- ✅ 默认 yml 的 `name-server` 已改为 `${ROCKETMQ_NAME_SERVER:localhost:9876}`，生产部署
  可直接通过环境变量覆盖；本地开发仍保留 localhost 默认值

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common"))
api(libs.spring.cloud.starter.stream.rocketmq)

testImplementation(project(":kudos-test:kudos-test-container"))
```
