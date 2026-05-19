# kudos-ms-user-api-public

User 服务**面向终端用户**的 Spring Boot 进程。承载浏览器 / 移动端发起的登录、登出、改密
等 passport 流，并把 Servlet Session 桥接到 `KudosContext`。

## 启动入口

- `UserApiWebApplication`（`@EnableKudos`）—— main 类
- `UserApiWebAutoConfiguration` —— 装配入口

```bash
./gradlew :kudos-ms:kudos-ms-user:kudos-ms-user-api-public:bootRun
```

## 暴露内容

| 类 | 说明 |
|---|---|
| `PassportPublicController` | `/api/public/user/passport/*`——login / logout / verify / changePassword 等"用户态"端点 |
| `UserContextWebFilter` | 把 `HttpSession[SESSION_KEY_USER]` 的 `SessionUserPrincipal` 写入 `KudosContext.user`；优先级 `Ordered.LOWEST_PRECEDENCE - 100`（在 `WebContextInitFilter` 之后） |

> **不暴露**管理端（`/api/admin/user/*`）端点——build 不依赖 `kudos-ms-user-api-admin`，
> 那是另一个进程的事。
>
> **不暴露** Feign provider 接口——那是 `api-internal` 的职责。

## 与 api-internal 的区别

| 维度 | api-public | api-internal |
|---|---|---|
| 面向 | 终端用户（浏览器 / 移动端） | 其他微服务（Feign 调用） |
| Session | 有（`UserContextWebFilter` 桥接 HttpSession → KudosContext） | 无——上游调用方自己带身份 |
| Nacos | 不依赖 | 依赖 discovery + config（作为 provider 注册） |
| 端点风格 | `/api/public/user/...` REST | 直接由 `IUser*Api` 接口注解决定路径 |
| 暴露的 IUserApi | 仅 passport 子集 | passport + account + org |

## 依赖

- `kudos-ms-user-core`（业务实现）
- `kudos-ability-web-springmvc`（MVC + 异常 / 参数 / 响应包装）

> 注意：当前 build 不依赖 `kudos-ms-user-api-admin`。早期文档曾声称"装配 user-core +
> user-api-admin"，与实际 `build.gradle.kts` 不符；如要把 admin 控制器并到本进程跑，
> 需在 `build.gradle.kts` 显式加 `api(project(":kudos-ms:kudos-ms-user:kudos-ms-user-api-admin"))`。

## 已知限制

- ❗ `UserContextWebFilter` 顺序依赖 `WebContextInitFilter` 先跑——若上游过滤器链改动
  优先级，需同步调整 `@Order` 值（兜底：`KudosContextHolder.get()` 会创建空 context，
  但其他 filter 拿到的就是部分填充态）
- ❗ Session 失效策略只在 `PassportPublicController.logout` 显式调用——客户端崩溃 /
  浏览器关闭不会主动清，依赖容器 session timeout
