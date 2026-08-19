# kudos-ability-web-springmvc

Spring MVC 适配层。给业务侧提供：

1. **统一的请求生命周期**：`WebContextInitFilter` 在每个请求前装配 `KudosContext`（session /
   cookie / header / IP / 浏览器 / OS / Locale / traceKey），请求结束 finally 清理
2. **统一的响应包装**：`GlobalResponseBodyHandler` 把控制器返回值统一封到 `ApiResponse`
3. **统一的异常映射**：`BadRequestExceptionHandler` + `GlobalExceptionHandler` 把 Spring MVC
   绑定 / Validation / 业务异常 / 未捕获异常翻译成同一套 ApiResponse
4. **可选的容器强制**：`kudos.ability.web.springmvc.server` 在多容器共存时指定用 Tomcat / Jetty
5. **CRUD Controller 基类**：`BaseReadOnlyController` / `BaseCrudController` 接通
   `IBaseReadOnlyService` / `IBaseCrudService` 的分页查询、详情、增删改 + 校验规则下发
6. **请求体安全护栏**：`MutableListSearchPayloadGuardAdvice` 拒绝以 `MutableListSearchPayload`
   作为外部请求体

## 设计要点

### 请求生命周期

```
servlet container
  → SessionRepositoryFilter (Spring Session，若应用启用)
  → WebContextInitFilter (order=DEFAULT+1) → KudosContextHolder.set(ctx) + 回写 X-Trace-Id
     → DispatcherServlet
        → Spring CORS 支持（addCorsMappings，含 preflight）
        → MutableListSearchPayloadGuardAdvice (拒绝可变 payload)
        → Controller
        → GlobalResponseBodyHandler (包成 ApiResponse + 回填 traceId)
        → BadRequestExceptionHandler / GlobalExceptionHandler (异常 → ApiResponse)
     ← (finally) KudosContextHolder.clear()
```

`KudosContextHolder.clear()` 在 filter 的 finally 中执行，保证线程池里的线程复用时不会
带着旧 context 跑下一个请求（这是 ThreadLocal 在 web 容器中最经典的内存泄漏 / 串台模式）。

注意 `clear()` 在**排除路径上也会执行**：`KudosContextHolder.get()` 是"读时创建"语义，
只要下游读过 holder 就会留下一个 context，跳过清理会把优化变成泄漏。

### traceKey

**两个头,两类调用方**：

| 头 | 谁在用 | 方向 |
|---|---|---|
| `X-Trace-Id`（`context.trace-id-header`） | 外部调用方、浏览器；ktor runtime 读的也是它 | **请求 + 响应** |
| `_UUID`（`Consts.RequestHeader.TRACE_KEY`） | 内部 Feign 服务间调用 | 仅请求 |

解析顺序：先 `X-Trace-Id`,再 `context.additional-trace-key-headers`（默认 `[_UUID]`）；
都没有或都不合法则生成 UUID。某个头的值不合法只是跳过,不会丢弃下一个头里的合法值。

> **为什么公开头优先**：它是响应里对外宣告的那个名字。这样"改名"时读写两侧**由构造保证**同步移动,
> 而不是靠两个配置项碰巧取了相同的值——读写不对称正是这里修掉的问题,修法本身不能留下再次跑偏的口子。
>
> **`_UUID` 必须保留**：`KudosContextRequestInterceptor` 在每个出向 Feign 调用上设置它,
> `FeignContextSignatureVerifier` 还把它纳入请求签名计算。所有既有内部调用的解析结果与改动前完全一致；
> 公开头只是把同样的能力延伸给外部调用方——不能指望他们使用带下划线前缀的内部头名。

- **校验后才采信**：长度上限 `context.max-trace-key-length`（默认 128），字符集限定为
  字母数字与 `-_.`。这不是洁癖——该值会被拼进日志行、并回写进响应头，未校验的值可以用
  CR/LF 伪造日志条目或做 HTTP 响应头拆分；无长度上限还能撑爆按 traceId 建索引的日志 / 指标后端。
  规则实现在 `kudos-ability-web-common` 的 `TraceKeys`，与 ktor runtime 共用同一份
- 回写响应头**在 chain 之前**写入，因此提前提交的响应、`@IgnoreApiResponseWrap` 端点、
  以及短路的错误响应都能拿到。`trace-id-header` 置空串则整个停用公开头：不回写,入站也只读 `_UUID`

### 容器装配

`kudos.ability.web.springmvc.server` **默认不设置**。不设置时本模块不贡献任何
`ServletWebServerFactory`，由 Spring Boot 按 classpath 选容器——这是 Boot 原生受支持的路径，
所有 `server.*` 配置与容器专属 customizer 都照常生效。**生产环境请保持不设置。**

显式设置（`TOMCAT` / `JETTY`）只用于多容器依赖共存、需要确定性地让某一个胜出的场景，
本模块自己的 Tomcat / Jetty 测试矩阵就是这个用途。

> **为什么不再有 `SwitchingServletWebServerFactory`**：它自己实现 `ServletWebServerFactory`
> 并手工从 Environment 读 `server.port` 和 `server.servlet.context-path`。但 Boot 通过
> `WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>` 施加配置，而这个包装类
> **不是** `ConfigurableServletWebServerFactory`，泛型不匹配 → customizer 被跳过。结果是
> `server.ssl.*`、`server.compression.*`、`server.servlet.session.*`、`server.http2`、
> `server.shutdown`、`server.address`、`server.server-header` 等**全部被静默丢弃**，且没有
> 任何报错。现在返回的是 Boot 自己的 `TomcatServletWebServerFactory` /
> `JettyServletWebServerFactory`，配置链路完整。

Tomcat 的 `relaxedPathChars` / `relaxedQueryChars` 放宽（避免 GET 带 `"<>[]\^` `{|}` 时
直接 400）以 `TomcatConnectorCustomizer` **bean** 的形式提供，因此无论工厂是 Boot 建的还是
本模块建的都会生效。

Undertow 已从枚举移除：当前版本不支持 Servlet 6.1，被 Spring Boot 4 排除。

### 装配条件的写法约定

本模块所有 `@ConditionalOnMissingBean` 都指名**具体类型**或**具体 bean 名**，不用宽泛的框架接口。
这不是风格问题：该注解按工厂方法**返回类型**匹配，所以

- 返回 `FilterRegistrationBean<*>` 的方法，会在应用注册**任意**一个自己的 Filter 时退避
- 返回 `HandlerInterceptor` 的方法，会在应用注册**任意**一个拦截器时退避

两者都会让本模块的装配静默失效，且日志里没有任何线索说明 KudosContext 为什么不再被填充。

另外：本模块由 `ComponentInitializerSelector`（普通 `ImportSelector`，非 deferred）导入，
bean 定义注册在自动配置**之前**。这个顺序让这里的条件能压过 Boot 的同名条件；反过来也意味着
这里的条件**不能可靠地看到**应用自己 `@Configuration` 里声明的 bean。因此凡是"允许业务覆盖"
的地方，覆盖点都是接口 bean（`IWebContextInitFilter`、`ObjectMapper`），而不是注册包装类。

### 消息转换器

`extendMessageConverters` 在 Spring 7 已标记 `forRemoval`，现改用
`configureMessageConverters(HttpMessageConverters.ServerBuilder)` + `withJsonConverter(...)`。
由框架保证 JSON 转换器**留在原槽位**——这一点是有实际后果的：JSON 转换器一旦排到
`StringHttpMessageConverter` 前面，就会连 `String` 返回值一起接管并加上 JSON 引号，
悄悄破坏所有返回裸字符串的端点。

### 异常处理与 HTTP 状态码约定

| 处理器 | 触发优先级 | 责任 |
|---|---|---|
| `BadRequestExceptionHandler` | `Ordered.HIGHEST_PRECEDENCE` | 4xx 类参数错（`@Valid`、`@RequestBody` JSON 解析、参数绑定、类型转换）→ 详细 `ErrorDetail` 列表 |
| `GlobalExceptionHandler` | 默认 | 业务异常、记录不存在、断言失败、未捕获异常 |

**状态码策略**（每个 handler 都用 `@ResponseStatus` 显式声明）：

| 结果 | HTTP | 理由 |
|---|---|---|
| `ServiceException` | **200** | 业务规则拒绝，调用本身是成功的。属正常流量，判成 4xx/5xx 会把网关与 APM 的告警淹掉。结果由 body 里的 `code` 承载 |
| `ObjectNotFoundException` | **404** | 目标记录不存在 |
| 参数绑定 / Bean Validation / `ConstraintViolationException` / `require` / `check` | **400** | 调用方传错了 |
| 其它未捕获异常 | **500** | 服务端故障 |

> **两处已修正的旧行为**，写在这里以免被改回去：
>
> 1. **未捕获异常原本返回 HTTP 200**，`"500"` 只藏在 body 里。负载均衡、网关、APM 都只看状态行、
>    不会解析 body，所以真正的服务端错误对平台上**每一条告警链路都是不可见的**。
> 2. **`GlobalExceptionHandler` 里有 5 个 handler 与 `BadRequestExceptionHandler` 完全重复**
>    （`MethodArgumentNotValidException` / `BindException` /
>    `MissingServletRequestParameterException` / `MethodArgumentTypeMismatchException` /
>    `HttpMessageNotReadableException`）。因为后者带 `@Order(HIGHEST_PRECEDENCE)` 总是先命中，
>    这些副本在生产中不可达；但在只装配了它一个的窄上下文（比如某些测试）里又会给出**不同的答案**
>    ——HTTP 200、且没有 `errors[]` 明细。现在一种异常只有一个 handler、一个答案。

`ObjectNotFoundException` 此前会掉进兜底分支，产生 ERROR 级日志 + `SYSTEM_ERROR`。由于 CRUD
基类的 `getDetail` / `getEdit` 查不到就抛它，**前端查一个失效 id 这种最普通的客户端错误
会触发一条系统错误告警**。

### 响应包装

- `String` 返回值走 `StringHttpMessageConverter`，手工序列化成 JSON 后**同时把 Content-Type
  置为 `application/json`**。否则响应声明的类型与内容自相矛盾；而且该转换器是**按 Content-Type
  推导字符集**的，不声明 JSON 就会退回自己的默认值 ISO-8859-1，把所有非 ASCII 字符打乱
- 成功响应的 `message` 为空串。**这个修在源头**：`IErrorCodeEnum.displayText` 在配了前缀时
  返回的是 i18n **key**（`sys.error-msg.default.200`）而非文案，且这条路径上没人解析它。
  原先 `ApiResponse.success()` 把这个 key 塞进 message，再由本 handler 在出站时识别并擦除——
  只治了 HTTP 调用方的症状，`ApiResponse.success()` 的其它使用方照样拿到裸 key。现在
  `success()` 不再产生占位符，出站擦除逻辑随之删除（它还会误伤业务刻意设置的同名 message）

### CORS

只保留 Spring 框架级支持（`addCorsMappings`），由 `kudos.ability.web.springmvc.cors.*` 驱动。

原先并存的 `CorsHandlerInterceptor` 已删除：它把请求的 `Origin` 原样回显、并把
`Access-Control-Allow-Methods` 硬写成 `*`，覆盖掉 `addCorsMappings` 刚协商好的结果——
两个写方对同一批响应头各执一词。Spring 自己的实现已正确处理 preflight、`Vary` 头和凭证规则。

默认值仍然宽松（`allowed-origin-patterns=[*]` + `allow-credentials=true`）以免挡住开发，
但**这个组合等价于"任意站点都可以用已登录用户的身份调用本 API"**。现在有两点改进：

- 可以纯用 yml 收紧，不必再自定义 `WebMvcConfigurer`
- 该组合仍然生效时，启动日志会打 WARN，任何忘记收紧的环境都能在日志里看见

收紧只需设 `cors.allowed-origins`（非空时自动忽略 `allowed-origin-patterns`）。

### IP / UA 解析

`XHttpServletRequest.getRemoteIp()` 信任 `x-forwarded-for` / `Proxy-Client-IP` /
`WL-Proxy-Client-IP` 三个代理头：**只能在已被可信反向代理（Nginx / ALB / CDN）兜底过滤
后使用**。直接对外暴露的服务会被攻击者通过伪造头任意"修改"自身 IP。

User-Agent 解析（`getBrowserInfo` / `getOsInfo` / `getClientTerminal`）是简单的 contains
启发式，覆盖主流浏览器 / OS / 终端，不追求 ua-parser 库的精度。命中顺序敏感（Edge UA
含 "Chrome"，当前实现优先匹配 Chrome——见 `XHttpServletRequestTest` 中已记录的回归 case）。

### CRUD Controller 基类

```kotlin
class UserController : BaseCrudController<
    Long,                  // PK
    UserService,           // B: 业务 Service
    UserSearchPayload,     // S: 列表查询 VO
    UserListVO,            // R: 列表项响应 VO
    UserDetailVO,          // D: 详情响应 VO
    UserEditVO,            // E: 编辑响应 VO
    UserCreateForm,        // CF: 新增请求 VO
    UserUpdateForm         // UF: 编辑请求 VO
>()
```

基类提供 `/pagingSearch` / `/getDetail` / `/getEdit` / `/save` / `/update` / `/delete` /
`/batchDelete` / `/getCreateValidationRule` / `/getUpdateValidationRule`。前端表单校验规则
由 `TerminalConstraintsCreator` 从注解反射生成。

**VO 类型的两条获取通道**：

1. 默认按子类的类型实参反射推断。`GenericKit.getSuperClassGenricClass` 读直接父类上绑定的实参，
   父类自己没绑时继续向上走。因此：
   - 直接继承基类 —— 可以
   - 经过一层**非泛型**夹层（夹层自己把 8 个实参全绑死）—— **也可以**，向上走能找到
   - 经过一层**泛型**夹层（夹层留着类型参数没绑）—— **不行**：此时叶子类的直接父类携带的是
     夹层的类型参数，下标与基类声明对不上
2. 泛型夹层确有必要时，把 VO 类显式传给构造器，反射就完全不参与：

```kotlin
abstract class TenantScopedController<D : Any>(detail: KClass<D>) :
    BaseReadOnlyController<Long, SomeService, SomeQuery, SomeRow, D>(detail)

class UserController : TenantScopedController<UserDetail>(UserDetail::class)
```

推断失败时抛出的 `IllegalStateException` 会指名是哪个类型参数、并说明改用构造器传入——
底层反射原本只会报下标越界或悄悄返回 `Any`，都指不到"夹层类"这个真正的病因。

分页上限无需在此另设护栏：`ListSearchPayload` 已内建 `getMaxPageSize()`（默认 100）与
`isUnpagedSearchAllowed()`（默认 false）。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/SpringMvcAutoConfiguration` | 装配入口（filters / handlers / cors / 容器强制） |
| `init/SpringMvcProperties` | `kudos.ability.web.springmvc.*` 配置绑定 |
| `filter/WebContextInitFilter` + `IWebContextInitFilter` | 请求 → `KudosContext` 装配 + 清理 + traceId 回写 |
| `handler/GlobalResponseBodyHandler` | 返回值 → ApiResponse 包装 |
| `handler/BadRequestExceptionHandler` | 4xx 参数错统一响应 |
| `handler/GlobalExceptionHandler` | 业务 / 未捕获异常统一响应 |
| `handler/MutableListSearchPayloadGuardAdvice` | 拒绝可变 payload 作请求体 |
| `controller/BaseController` / `BaseReadOnlyController` / `BaseCrudController` | CRUD 基类 |
| `support/XHttpServletRequest` | UA / IP / URL 解析扩展 |
| `support/enums/ServletServerEnum` | 容器枚举 |

## 配置示例

```yaml
kudos:
  ability:
    web:
      springmvc:
        # 生产请勿设置 server：不设置 = 由 classpath 决定（Boot 原生路径）
        # server: TOMCAT        # TOMCAT | JETTY，仅用于多容器共存时强制指定
        cors:
          enabled: true
          allowed-origins:      # 非空时忽略 allowed-origin-patterns
            - https://admin.example.com
          allow-credentials: true
          max-age: 86400
        context:
          trace-id-header: X-Trace-Id   # 公开头：读写同名；置空串则整个停用
          additional-trace-key-headers: # 追加读取的头（按序），默认内部 Feign 头
            - _UUID
          max-trace-key-length: 128
          exclude-path-patterns:        # 跳过 context 装配的路径
            - /_monitor/**
            - /errors/**
        tomcat:
          relaxed-path-chars: "\"<>[\\]^`{|}"
          relaxed-query-chars: "\"<>[\\]^`{|}"
      url:
        suffix: .html

server:
  port: 8080
  servlet:
    context-path: /api
```

## 测试覆盖

- `BadRequestExceptionHandlerTest`、`GlobalExceptionHandlerTest`（**两个 advice 同时注册**，
  断言的是调用方真正收到的答案，而不是单个 advice 孤立时的答案）、
  `GlobalResponseBodyHandlerTest`、`MutableListSearchPayloadGuardAdviceTest`
- `WebContextInitFilterTest` —— session / cookie / header 拷贝、traceKey 校验（CR/LF 注入、
  超长、空格一律拒绝）、traceId 回写、排除路径、非 HTTP 请求穿透、异常时清理
- `SpringMvcAutoConfigurationTest` —— bean 装配、消息转换器替换与顺序、CORS 属性绑定
- `TomcatServerTest` / `JettyServerTest` —— 容器装配启动 hello-world，**并断言实际启动的
  就是配置指定的那个容器**（两种容器都在测试 classpath 上，Boot 平局时偏向 Tomcat，
  没有这条断言的话 Jetty 用例会在 Tomcat 上跑通、什么也没验证）
- `BaseCrudControllerTest` —— CRUD 委派 + 夹层基类下显式构造通道与推断失败信息
- `XHttpServletRequestTest` —— UA / 终端 / URL 拼装
- `UndertowServerTest` —— skipped，Undertow 不再支持

## 已知限制 / 后续工作

- ❗ 默认 CORS 配置仍然开放（`allowed-origin-patterns=[*]` + `allow-credentials=true`），
  生产部署必须收紧；现在可纯 yml 收紧，且启动会打 WARN
- ❗ **失败响应的 message 仍然是未解析的 i18n key**。`ApiResponse.fail(errorCode)` 用的是
  `IErrorCodeEnum.displayText`，配了前缀时返回的是 `sys.error-msg.default.4002` 这样的 key。
  成功响应已在源头修掉，失败侧要真正解析需要接入 `MessageSource`，属新增能力而非修复，
  留作后续
- ❗ `WebContextInitFilter` 把整个 session / 所有 cookie / 所有 header 都拷到 `KudosContext`，
  对大 session 不友好；现在可用 `context.exclude-path-patterns` 跳过无谓路径，需要字段级
  裁剪则自定义 `IWebContextInitFilter`
- ❗ `XHttpServletRequest.getBrowserInfo()` 中 Edge 检测顺序在 Chrome 之后——当前实现把 Edge UA
  误判为 Chrome；测试中已记录此行为，需要时调整 when 顺序
- ❗ 模块组内仍无请求限流能力（无 RateLimiter / bucket4j / resilience4j 集成点）。
  原先 yml 里的 `server.max-request-hold` / `server.max-request-exclude` **已删除**：
  全仓库没有任何读取方，等于对外宣传了一个不存在的限流护栏
- ❗ 缺访问日志与慢请求日志；`WebContextInitFilter` 已掌握请求起止点，是天然的接入位置
- ❗ traceKey 未与 W3C `traceparent` 打通；后续若接 Micrometer Tracing，对接点在
  `WebContextInitFilter.resolveTraceKey`
- ❗ Undertow 已被 Spring Boot 4 排除（Servlet 6.1 不兼容）
- ❗ 内嵌容器未启用虚拟线程（Loom / Java 21+）。`spring.threads.virtual.enabled=true` 需配
  Tomcat 10.1+ / Jetty 12+

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-common"))
api(libs.spring.boot.starter.web)
api(libs.jackson.module.kotlin)

// 只为 SessionRepositoryFilter.DEFAULT_ORDER 这一个常量，公开面不暴露 Spring Session 类型
implementation(libs.spring.session.core)

// Jetty 只在编译期需要（表达 JettyConfiguration 的 bean 签名），运行期由 @ConditionalOnClass 兜底
compileOnly(libs.spring.boot.starter.jetty)
```

> `spring-session-data-redis` 原本以 `api` 挂在这里。已移除：全仓库没有任何代码或配置选择过
> session store，它等于把整套 Redis session 栈强加给每个依赖本模块的应用却不换来任何东西。
> 需要 Redis session 的应用应自己声明这个意图，连同必须配套的 store 配置一起。
