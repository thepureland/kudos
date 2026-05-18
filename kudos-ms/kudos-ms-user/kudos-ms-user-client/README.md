# kudos-ms-user-client

User 服务的 **Feign 客户端代理 + 降级**——其他微服务通过 Kotlin 接口调用远端 user 服务。

## 内容

- `*Proxy : IUser*Api` —— Feign 接口，签名与 server 端实现一致
- `*Fallback` —— 调用失败时的降级（继承 `AbstractFeignFallbackSupport`）

业务侧引入：

```kotlin
implementation(project(":kudos-ms:kudos-ms-user:kudos-ms-user-client"))
```

然后 `@Autowired private val userAccountProxy: IUserAccountProxy`。

## 依赖

- `kudos-ms-user-common`
- `kudos-ability-distributed-client-feign`

**不依赖 user-core**——纯远端调用包装。
