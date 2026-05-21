# kudos-ability-distributed-stream-common

Spring Cloud Stream 之上的通用封装。给上层（`notify-mq` / `log-audit-mq` / 业务侧）提供：

1. **`@MqProducer` + `@MqConsumer` 注解 + 切面** —— 不必直接 `streamBridge.send`
2. **失败消息持久化** —— 发送失败的消息可入 `SysMqFailMsg` 表 / 文件，等待人工补救
3. **多 binding 函数定义聚合** —— `StreamConsumerEnvironRegistrar` 自动合并 yml 里散落的
   `spring.cloud.function.definition`
4. **统一异常处理** —— `StreamGlobalExceptionHandler` / `StreamProducerExceptionHandler`
5. **同步 / 异步发送 + binding 启动校验**

## 设计要点

### `@MqProducer` 的 AOP 模式

```kotlin
@MqProducer(topic = "USER_EVENTS", bindingName = "userEvents-out-0")
fun publishUserEvent(event: UserEvent): Boolean = true
```

切面 `MqProducerAspect` 在 `@AfterReturning` 阶段：
- 检查方法返回 `Boolean false` 且 `cancelOnFalse=true` → 跳过发送（业务侧的"取消发送"语义）
- 取 `payloadParameterIndex` 指定的方法参数作为消息体，默认 `0`
- 调 `StreamProducerHelper.sendMessage(bindingName, data)`

业务侧典型用法：方法体把"发送前校验"逻辑放上，校验失败返回 false 让切面跳过。
**方法返回值的真实含义是"是否决定要发"，不是"是否发送成功"**。
多参数 producer 方法必须显式指定 `payloadParameterIndex`。

### `StreamProducerHelper.doRealSend` 异步语义

```
streamBridge.send() 立即返回 true (异步入队)
       ↓
真实 MQ flush 失败 → 触发 IStreamFailHandler.CHANNEL_BEN_NAME 错误通道
       ↓
StreamProducerFailHandlerProcessor 写 sys_mq_fail_msg 表 / 触发自定义 handler
```

**`sendMessage` 返回 true ≠ 消息已抵达 broker**——只代表入了 StreamBridge 的本地队列。
真正的失败感知通过 error channel 异步回调。这是 spring-cloud-stream 的固有特性，本模块
封装了错误通道的接线。

### 失败消息持久化

```
SysMqFailMsg (表 sys_mq_fail_msg)
  ├─ bindingName
  ├─ topic
  ├─ messageBody (JSON)
  ├─ retryCount
  └─ status (waiting / processing / done)
```

`SysMqFailMsgService` 提供失败消息的查询 / 重试 / 删除 API；下游 `cache-remote-redis` 等
模块用得到。

### `StreamConsumerEnvironRegistrar` 自动聚合 function definition

Spring Cloud Function 要求所有 consumer 通过 `spring.cloud.function.definition` 注册，
形如 `"foo;bar;baz"`。多个模块各自有自己的 yml 时手动合并易错。

`StreamConsumerEnvironRegistrar` 是 `ImportBeanDefinitionRegistrar`，在装配前：
- 扫描所有 `kudos.*` yml 文件里的 `spring.cloud.function.definition` 值
- 用 `[;,\\s]+` 拆分 / 去重
- 合并写回 Environment 最前面（addFirst）→ 覆盖原配置

### binding 启动校验

```yaml
kudos:
  ability:
    distributed:
      stream:
        binding-verify:
          enabled: true
          fail-on-missing: true
          required-producer-bindings:
            - userEvents-out-0
            - notifyMq-out-0
        producer-limit:
          enabled: true
          max-in-flight: 1024
          acquire-timeout-millis: 50
```

`InitializingBean` 在装配末期跑 — 必需的 binding 缺失时按 `failOnMissing` 决定 warn 或抛错。

## 模块入口

| 路径 | 角色 |
|---|---|
| `annotations/MqProducer` | 生产者注解（`bindingName` + `topic`） |
| `annotations/MqConsumer` | 消费者注解（注：返回类型必须是 `Consumer<Message<...>>`） |
| `annotations/MqProducerAspect` | 生产者切面，@AfterReturning 拦截 |
| `support/StreamProducerHelper` | `sendMessage` / `asyncSendMessage` / `doRealSend` |
| `support/StreamMessageConverter` | `application/x-kudos-stream` content-type 转换 |
| `support/StreamProducerFailHandlerProcessor` | 错误通道入口 → 失败消息持久化 |
| `handler/StreamGlobalExceptionHandler` | spring-cloud-stream 全局异常入口 |
| `handler/StreamProducerExceptionHandler` | producer 端异常处理 |
| `handler/IStreamFailHandler` / `StreamFailHandlerItem` | 失败处理 SPI + 注册表 |
| `biz/SysMqFailMsgService` + `ISysMqFailMsgService` | 失败消息业务 service |
| `dao/StreamExceptionMsgDao` | `sys_mq_fail_msg` 表 DAO |
| `model/po/SysMqFailMsg` + `model/table/SysMqFailMsgs` | 失败消息表实体 + Ktorm 表 |
| `model/vo/StreamMessageVo` / `StreamHeader` / `StreamProducerMsgVo` | 消息载体 / 头 / 生产 VO |
| `init/StreamCommonConfiguration` | 装配入口（11+ bean） |
| `init/StreamConsumerEnvironRegistrar` | function.definition 聚合注册器 |
| `init/properties/StreamAsyncSendExecutorProperties` | 异步发送线程池配置 |
| `init/properties/StreamBindingVerifyProperties` | binding 启动校验配置 |
| `init/properties/StreamProducerLimitProperties` | producer 侧本地 in-flight 限流配置 |

## 已修复（本轮 8 维度审计）

- ✅ `StreamHeader.toContextParam` 把 `headers[USER_ID_KEY]` 错赋给 `tenantId`，`dataSourceId`
  类型转换不一致——属于"零 callsite 的预埋 bug"，已修
- ✅ `StreamGlobalExceptionHandler.isFromConsumer` 用 `Locale.getDefault()` 做 `lowercase`——
  Turkish locale 下 `"kafka_..."` 大小写映射会误判，改为 `Locale.ROOT`
- ✅ `IHeaderTopic.kt` 全仓零实现，删除（旧版 README 还列在模块入口里）
- ✅ `StreamHeader._datasourceTenantId` 的 Java 风格 getter / setter 包装移除（Kotlin property
  足够）；下划线 property name **保留**——`BeanKit.extract` 按 property name 投到 header，
  改名会断 wire 兼容

## 已知限制

- ✅ `@MqProducer` 方法返回值"`false` = 取消发送"已通过注解参数
  `cancelOnFalse` 显式化，默认保持历史行为；业务代码可直接从注解读到语义
- ℹ️ `StreamProducerHelper.sendMessage` 返回 true 仍表示"已提交给 StreamBridge"，不等价于
  broker ack；这是 spring-cloud-stream 异步发送模型。真实失败仍通过 error channel 异步触发，
  本模块负责接线和失败持久化
- ✅ `MqProducerAspect.afterReturning` 已支持 `payloadParameterIndex`，多参数业务方法可以显式
  指定要发送的参数；未指定且存在多参数时会打 warn
- ✅ `StreamProducerExceptionHandler.processFailedData` 已在失败数据里保存 `msgBodyClassName`，
  重试时优先按原 className 反序列化；无 serializer 时再尝试用 primary constructor 从 JSON Map
  恢复，最后才回退为 Map / List 动态结构
- ℹ️ `SysMqFailMsg` 表用 Ktorm 写是当前默认实现；`StreamCommonConfiguration.streamExceptionMsgDao`
  已是 `@ConditionalOnMissingBean`，非 RDB / 非 Ktorm 部署可覆盖 DAO / service bean
- ℹ️ `StreamConsumerEnvironRegistrar` 只扫描 `kudos.*` yml 是模块自动聚合范围；业务方自有 yml
  仍按 Spring 默认机制读取，跨模块自动聚合需放在 kudos 命名资源下
- ✅ 已增加 producer-side 本地 in-flight 限流：
  `kudos.ability.distributed.stream.producer-limit.enabled=true`，可配置
  `max-in-flight` 和 `acquire-timeout-millis`，默认关闭保持兼容
- ✅ 已补基础单测：`StreamMessageConverter` 序列化往返、`StreamHeader.toContextParam`
  header 还原、`StreamFailHandlerItem` 精确匹配 / 默认 fallback、`@MqProducer`
  payload 参数选择、失败重试 payload 类型恢复
- ℹ️ `StreamGlobalExceptionHandler` 三入口 / function.definition 聚合仍依赖集成验证；本轮补了
  producer 切面与失败重试的无容器单测，后续可继续把 error channel 路由和 registrar 聚合抽成纯函数测试

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
api(libs.spring.cloud.stream)
api(libs.alibaba.fastjson2)
```
