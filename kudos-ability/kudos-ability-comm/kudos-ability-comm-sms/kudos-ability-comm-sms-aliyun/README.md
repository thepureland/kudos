# kudos-ability-comm-sms-aliyun

阿里云短信发送封装（基于阿里云 dysmsapi 异步 SDK）。给业务侧一个 `send(request, callback)`
最小化接口，内部走虚拟线程异步执行。

## 当前状态：已启用

模块在 `settings.gradle.kts` 中参与构建。源码完整，包含 handler + auto-configuration +
request 模型 + 2 个 WireMock 集成测试（需要 Docker 可用）。业务侧需要时直接添加
`api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-sms:kudos-ability-comm-sms-aliyun"))`。

## 设计要点

### 与 comm-email 模块的一致接口

```kotlin
fun send(smsRequest: AliyunSmsRequest, callback: (SendSmsResponseBody) -> Unit) {
    Thread.ofVirtual().start { doSend(smsRequest, callback) }
}
```

发送 / 回调 / 虚拟线程的契约与 `EmailHandler` 完全对齐——业务侧的"通信能力"代码可以
统一风格使用。

### 凭证随请求传递（多租户友好）

`AliyunSmsRequest` 自带 `region` / `accessKeyId` / `accessKeySecret`——**不通过全局
yml 配置**。这种设计的初衷是支持"每租户一套阿里云 AK"：业务侧从配置中心 / 密钥管理
按租户读出 AK 填到 request，本模块按它发送。代价是模型类自带凭证，**不要把整个
request 实例写日志**。

### 安全回调（异常路径也有响应体）

发送异常时 `doSend` 在 finally 里构造"安全响应体"：

```kotlin
SendSmsResponseBody.builder()
    .code("EXCEPTION")
    .message(lastError?.message ?: "unknown error")
    .requestId("local-test")
    .build()
```

这样调用方 callback 永远收到一个非 null 的响应体——避免业务侧再做 null 检查。

### 测试用 endpoint 覆盖（WireMock）

`AliyunSmsHandler.endpointOverrideStr` 通过 `@Value` 注入
`kudos.ability.comm.sms.aliyun.endpoint`。**生产留空** → SDK 走官方域名；**测试填**
WireMock 完整 URI（`http://localhost:8080`，**勿省略 scheme**）。

```kotlin
// 测试代码示例
@DynamicPropertySource
fun registerProperties(registry: DynamicPropertyRegistry) {
    val container = WireMockTestContainer.startIfNeeded(registry)
    registry.add("kudos.ability.comm.sms.aliyun.endpoint") {
        "http://${container.host}:${container.firstMappedPort}"
    }
}
```

`buildClient` 内部处理三种格式：完整 URI（`http://host:port`）/ 主机端口（`host:port`）/
纯域名（`host`）。无 scheme 时默认 `https`。

### `AliyunSmsAutoConfiguration` 装配

仅一个 bean：`aliyunSmsHandler`。`@AutoConfigureAfter(ContextAutoConfiguration::class)`
确保 kudos 上下文先初始化。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/AliyunSmsAutoConfiguration` | 装配入口（仅 `aliyunSmsHandler` bean） |
| `handler/AliyunSmsHandler` | 发送核心：客户端构造 + 请求转换 + 异常兜底回调 |
| `model/AliyunSmsRequest` | 请求 POJO（含凭证 + 模板参数） |

业务侧不需要持有 `AsyncClient`——所有 SDK 调用在 handler 内部完成。

## 配置示例

```yaml
kudos:
  ability:
    comm:
      sms:
        aliyun:
          endpoint:    # 生产留空；测试填 http://wiremock:port
```

业务侧使用：

```kotlin
val req = AliyunSmsRequest().apply {
    region = "cn-hangzhou"
    accessKeyId = secrets.aliyunAk()    // 从密钥管理拿
    accessKeySecret = secrets.aliyunSk()
    phoneNumbers = "13800000000"
    signName = "公司签名"
    templateCode = "SMS_123456"
    templateParam = """{"code":"1234"}"""
}
aliyunSmsHandler.send(req) { resp ->
    when (resp.code) {
        "OK" -> log.info("发送成功 requestId={}", resp.requestId)
        "isv.BUSINESS_LIMIT_CONTROL" -> rateLimiter.markThrottled(req.phoneNumbers)
        "EXCEPTION" -> retryQueue.enqueue(req)  // 本地异常的兜底 code
        else -> failTracker.log(resp.code, resp.message)
    }
}
```

## 测试覆盖

- `AliyunSmsTest.send_sms_ok` —— 正常发送返回 OK
- `AliyunSmsTest.send_sms_rate_limited` —— 命中限流返回 `isv.BUSINESS_LIMIT_CONTROL`

两个测试都基于 `WireMockTestContainer`，需要 Docker。

## 已知限制 / 后续工作

- ❗ 每次 `send` 都现 new `AsyncClient` + 关闭——内部含 HTTP 客户端 / 线程池，单次开销
  不小。高频发送场景应当按 (region, accessKey) 缓存客户端
- ❗ 没有显式 SDK 调用超时——依赖阿里云 SDK 默认值（通常 30s+），慢调用会拖住虚拟线程。
  需要时通过 `ClientOverrideConfiguration` 设
- ❗ 没有重试机制——transient 错误（5xx / 网络抖动）直接返回错误码；调用方需自己重试
- ❗ 没有失败地址区分——阿里云 SDK 单次调用对应"一个手机号"或"一组手机号"，单组失败
  时没有 partial-success 概念（与邮件不同）
- ❗ `AliyunSmsRequest.accessKeySecret` 明文 + `Serializable`——序列化到日志 / 缓存时密钥外泄。
  生产场景应在调用前从 secret manager 临时填充
- ❗ 没有发送速率限制——阿里云对手机号 / 模板有调用频次限制，业务侧需自行管理
- ❗ 只支持 `dysmsapi20170525`（阿里云海外 / 国内统一短信 API）。如需用阿里云"通信号码池"
  等其他产品，需新开模块

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-common"))
api(libs.alibaba.cloud.dysmsapi)

testImplementation(project(":kudos-test:kudos-test-container"))
```
