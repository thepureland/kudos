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

## 已知限制 / 后续工作

- ❗ **Fallback 静默吞错** — 当 Feign 调用失败回落到 fallback 时，业务侧从返回值无法区分"远端真返回空"
  和"服务不可达"两种语义。需结合 `kudos-ability-distributed-client-feign` 的统一监控埋点观察
- ❗ **`PassportFallback.login` 返回 `SERVICE_UNAVAILABLE` 后无重试** — 上游业务（auth 网关）
  需自行做指数退避，否则瞬时抖动会被用户感知为"登录失败"
- ❗ **多 Feign client name 没有统一开关** — `user-account` / `user-org` 等 7 个 client name
  各自独立配置熔断 / 超时；想统一禁用 user 服务调用需逐个 name 配置
- ❗ **缺少接口契约测试** — 当前 client 与 `user-core` 的方法签名仅靠 `IUser*Api` 同源保证；
  签名漂移（如参数顺序变动）需依赖编译期发现。建议接入 Spring Cloud Contract（`kudos-test-api-contract`）
- ❗ **`AbstractFeignFallbackSupport.name` 字符串硬编码** — 各 Fallback 在 ctor 里塞自己的类名字符串，
  类被重命名时易遗漏；可改为反射 `this::class.simpleName`

## 改进建议（自动分析 2026-06-11）

- ❗ **`PassportFallback.login` 借用 `LOCKED` 表示降级**（`src/io/kudos/ms/user/client/passport/fallback/PassportFallback.kt`）：
  `PassportLoginStatusEnum.LOCKED` 的语义是"错误次数超限被锁定"，Feign 降级时返回它会让终端用户
  被误导为"账号被锁"。本 README 上文"降级策略要点"提到的 `SERVICE_UNAVAILABLE` 枚举值实际**并不存在**
  （文档与代码不符）。建议在 `PassportLoginStatusEnum` 新增该值后改写（枚举属公共契约，未直接修改）。
- **降级日志级别不对称**：`PassportFallback` 用 `errorWrite`、`UserAccountFallback` 用 `warnRead`——
  读 / 写语义区分合理，但同为"远端不可达"事件，监控告警需同时盯两个级别；建议在
  `AbstractFeignFallbackSupport` 层统一埋 metrics。
