# kudos-ms-user-client

User 服务的 **Feign 客户端代理 + 降级**——其他微服务通过 Kotlin 接口调用远端 user 服务。

## 代理 / 降级清单

| Feign name | Proxy（继承 `IUser*Api`） | Fallback |
|---|---|---|
| `user-account` | `IUserAccountProxy` | `UserAccountFallback` |
| `user-account-protection` | `IUserAccountProtectionProxy` | `UserAccountProtectionFallback` |
| `user-account-third` | `IUserAccountThirdProxy` | `UserAccountThirdFallback` |
| `user-contact-way` | `IUserContactWayProxy` | `UserContactWayFallback` |
| `user-org` | `IUserOrgProxy` | `UserOrgFallback` |
| `user-login-remember-me` | `IUserLoginRememberMeProxy` | `UserLoginRememberMeFallback` |
| `user-passport` | `IPassportProxy` | `PassportFallback` |

> `kudos-ms-auth` 主要消费 `user-passport`、`user-account`、`user-account-third`、
> `user-org` 这几条线（登录 / 用户元数据 / 第三方绑定 / 组织归属）。

## 接口签名约定

- `*Proxy` 接口继承 `user-common` 中的 `IUser*Api` —— Feign 通过反射读到的方法签名与
  server 端实现完全一致，避免双份维护
- `*Fallback` 都继承 `AbstractFeignFallbackSupport(name)`，命名传 Fallback 类名字符串供
  日志 / 监控识别；方法层面默认返回安全值（空列表 / null / 默认枚举），具体语义见各
  Fallback 实现
- 注意：Feign client 的 `name` 用的是 **`user-<domain>` 短名**，不是 micro-service
  `user`——靠 Feign 名做按域路由，便于在配置中分别打开 / 关闭 / 设熔断阈值

## 业务侧引入

```kotlin
implementation(project(":kudos-ms:kudos-ms-user:kudos-ms-user-client"))
```

```kotlin
@Autowired
private val userAccountProxy: IUserAccountProxy

@Autowired
private val passportProxy: IPassportProxy
```

## 依赖

- `kudos-ms-user-common`（契约 / VO / 枚举）
- `kudos-ability-distributed-client-feign`（Feign + Hystrix/Resilience 包装、`AbstractFeignFallbackSupport` 在此）

**不依赖 user-core**——纯远端调用包装，保持 client 模块轻量、不拉入持久层 / 缓存。

## 降级策略要点

- Fallback 一律 `open class`——Feign 通过 cglib 代理需要可继承
- 任何 fallback 返回值**必须能让上游调用方不抛 NPE**：列表返回 `emptyList()`，单对象
  返回 `null` 或安全默认值；登录类返回明确的失败枚举（`PassportLoginStatusEnum.SERVICE_UNAVAILABLE`
  之类）
- 不在 fallback 里做"重试"——重试是上游 / `kudos-ability-distributed-client-feign` 配置项的事
