# kudos-ms-msg-client

Msg 服务的 **Feign 客户端代理 + 降级**——其他微服务通过 Kotlin 接口调用远端 msg 服务。

## 内容

- `*Proxy : IMsg*Api` —— Feign 接口
- `*Fallback` —— 调用失败时的降级（继承 `AbstractFeignFallbackSupport`）

业务侧引入：

```kotlin
implementation(project(":kudos-ms:kudos-ms-msg:kudos-ms-msg-client"))
```

业务侧典型用法：

```kotlin
@Autowired private val msgSendProxy: IMsgSendProxy
msgSendProxy.send(templateCode = "WELCOME", params = mapOf(...), receivers = ...)
```

## 依赖

- `kudos-ms-msg-common`
- `kudos-ability-distributed-client-feign`

**不依赖 msg-core**——纯远端调用包装。
