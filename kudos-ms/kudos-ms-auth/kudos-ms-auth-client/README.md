# kudos-ms-auth-client

Auth 服务的 **Feign 客户端代理 + 降级处理**——让其他微服务（user / sys / msg / 业务）通过
统一 Kotlin 接口调用远端 auth。**只依赖 `auth-common`**，不打包 core，避免调用方意外加载
业务实现。

## 内容

- `*Proxy : IAuth*Api` —— Feign 接口；继承自 common 的契约接口，签名与 server 端实现一致
- `*Fallback` —— 调用失败时的降级实现（继承 `AbstractFeignFallbackSupport`：读接口 warn +
  安全默认值；写接口 error + 失败值）

业务侧引入：

```kotlin
dependencies {
    implementation(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-client"))
}
```

然后 `@Autowired private val authRoleProxy: IAuthRoleProxy`。

## 依赖

- `kudos-ms-auth-common`（契约与 VO 序列化对齐）
- `kudos-ability-distributed-client-feign`（Feign 注解 + 上下文透传 + 降级基类）

不依赖 auth-core——`auth-client` 是"远端调用对端"的薄包装，本地没有任何业务逻辑。
