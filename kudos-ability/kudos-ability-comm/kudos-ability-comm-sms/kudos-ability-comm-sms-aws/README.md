# kudos-ability-comm-sms-aws

AWS SNS（Simple Notification Service）短信发送封装。基于 AWS SDK v2 异步客户端，给业务侧
`send(request, callback)` 最小化接口，内部走虚拟线程异步执行。

与同系列 `comm-sms-aliyun` 是镜像关系（同 SPI 风格 + WireMock 集成测试 + 异常兜底回调）；
区别是这个模块 **已启用** 并参与 build。

## 设计要点

### `send` 双签名

```kotlin
fun send(smsRequest: AwsSmsRequest)                                        // 不关心结果
fun send(smsRequest: AwsSmsRequest, callback: ((AwsSmsCallBackParam) -> Unit)?)  // 异步回调
```

实际是 `callback: ((...) -> Unit)? = null` 默认参数 + 显式 null。两条都立刻把 `doSend`
丢到虚拟线程。

### 凭证随请求传递（多租户友好）

`AwsSmsRequest` 自带 `region` / `accessKeyId` / `accessKeySecret`——**不通过全局 yml**。
多租户场景按 tenant 从 secret manager 临时填充。代价：**request 不能落日志**。

### 异常分层 + 安全兜底回调

```kotlin
catch AwsServiceException → cb.statusCode 来自 AWS, statusText 是 AWS errorCode/errorMessage
catch SdkServiceException → cb.statusCode 来自 SDK，statusText 是消息
catch Exception           → cb 保持 null
finally cb ?: AwsSmsCallBackParam(statusCode=599, statusText="client error")
```

`599` 不是 HTTP 标准码——内部约定的"本地客户端错误"标识，与 5xx 范围（服务端错误）
区分开。调用方拿到 599 通常意味着网络 / 序列化 / SDK 内部问题，重试可能没用。

### 共享 HTTP 客户端（仅代理模式启用时）

```kotlin
companion object {
    private var HTTP_CLIENT: SdkHttpClient? = null
}

@PostConstruct
private fun initApacheHttpClient() {
    if (proxyProperties.enable) {
        HTTP_CLIENT = ApacheHttpClient.builder()
            .proxyConfiguration(...)
            .build()
    }
}
```

启用代理时，全进程共享一个 `ApacheHttpClient`（含连接池）；每个 `send` 把它装到当次的
`SnsClient.builder()`。**关键限制**：
- 代理配置变更需重启进程（@PostConstruct 仅启动跑一次）
- 多租户若需不同代理出口要另行设计

**不启用代理时**：每次 `send` 都让 SDK 内部默认创建一份 HTTP 客户端 + 关闭——这是当前
模块的主要性能损耗点。高频发送应当无论是否走代理都注入一个共享 `SdkHttpClient`。

### 测试用 Endpoint 覆盖

`@Value("\${kudos.ability.comm.sms.aws.endpoint}")` 注入。`yml` 默认 `""`（空字符串）。
WireMock 测试通过 `@DynamicPropertySource` 覆盖：

```kotlin
@DynamicPropertySource
fun props(registry: DynamicPropertyRegistry) {
    val c = WireMockTestContainer.startIfNeeded(registry)
    registry.add("kudos.ability.comm.sms.aws.endpoint") {
        "http://${c.host}:${c.firstMappedPort}"
    }
}
```

handler 内 `endpointOverride.isNotBlank()` 时调 `builder.endpointOverride(URI.create(...))`
绕过 region 默认域名。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/AwsSmsAutoConfiguration` | 装配入口（handler + proxy properties） |
| `init/properties/SmsAwsProxyProperties` | 正向代理配置 |
| `handler/AwsSmsHandler` | 发送核心 + 异常分层 + 安全兜底回调 |
| `model/AwsSmsRequest` | 请求 POJO（含凭证） |
| `model/AwsSmsCallBackParam` | 响应 POJO |

## 配置示例

```yaml
kudos:
  ability:
    comm:
      sms:
        aws:
          endpoint: ""                 # 生产留空；测试覆盖
          proxy:
            enable: false              # 不需要走代理时 false 即可
            url: http://proxy:1234
            username: ${PROXY_USER}
            password: ${PROXY_PASS}
```

业务侧用法：

```kotlin
val req = AwsSmsRequest().apply {
    region = "us-east-1"
    accessKeyId = secrets.awsAk()
    accessKeySecret = secrets.awsSk()
    phoneNumber = "+15550100"
    message = "Your code is 1234"
}
awsSmsHandler.send(req) { cb ->
    when {
        cb.statusCode == 200 -> log.info("发送成功 messageId={}", cb.messageId)
        cb.statusCode in 500..598 -> retryQueue.enqueue(req)  // AWS 服务端错误
        cb.statusCode == 599 -> alertChannel.warn("AWS SDK 本地错误：${cb.statusText}")
        else -> failTracker.log(cb.statusCode, cb.statusText)
    }
}
```

## 测试覆盖

- `AwsSmsTest.send_sms_ok` —— WireMock 模拟 200 OK，断言 `statusCode=200, statusText="OK"`
- 还包含一个失败场景测试

依赖 `WireMockTestContainer`，需要 Docker。

## 已知限制 / 后续工作

- ❗ **每次 `send` 都现 new `SnsClient`**——未启用代理时 SDK 内部还要为每个 `SnsClient` 再
  造一份默认 HTTP 客户端，开销显著。生产高频发送场景应当：(a) 把 HTTP_CLIENT 改成无条件
  共享单例，(b) 把 `SnsClient` 也按 (region, AK) 缓存
- ❗ 没有显式 SDK 超时——AWS SDK 默认 `apiCallTimeout=null`（无限），需要时通过
  `ClientOverrideConfiguration.apiCallTimeout(...)` / `apiCallAttemptTimeout(...)` 设
- ❗ 没有重试 / 退避策略——AWS SDK 默认会自动重试 retryable 错误，但策略不可在本模块层
  调整。如要自定义重试需自己包一层
- ❗ `HTTP_CLIENT` 是进程级静态字段，代理配置变更需重启进程（@PostConstruct 仅运行一次）；
  多租户不同代理需要另行设计 client factory
- ❗ `HTTP_CLIENT` **从未显式 close**——JVM 退出时操作系统回收，但运行期重启容器会泄漏连接
  池（影响应用不大，影响监控指标）
- ❗ `AwsSmsRequest.accessKeySecret` 明文 + `Serializable`——序列化到日志 / 缓存时密钥外泄。
  生产场景需从 secret manager 临时填充
- ❗ 短信投递回执 / 退订处理未支持——AWS SNS 通过订阅 SQS 主题接收 delivery receipts，
  需要业务侧另开订阅链路

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-common"))
api(libs.amazon.awssdk.sns)
api(libs.amazon.awssdk.apache.client)

testImplementation(project(":kudos-test:kudos-test-container"))
```
