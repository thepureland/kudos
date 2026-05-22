# kudos-ability-web-springmvc

Spring MVC 适配层。给业务侧提供：

1. **统一的请求生命周期**：`WebContextInitFilter` 在每个请求前装配 `KudosContext`（session /
   cookie / header / IP / 浏览器 / OS / Locale / traceKey），请求结束 finally 清理
2. **统一的响应包装**：`GlobalResponseBodyHandler` 把控制器返回值统一封到 `ApiResponse`
3. **统一的异常映射**：`BadRequestExceptionHandler` + `GlobalExceptionHandler` 把 Spring MVC
   绑定 / Validation / 业务异常 / 未捕获异常翻译成同一套 ApiResponse
4. **可切换的内嵌容器**：`SwitchingServletWebServerFactory` 按 yml 切 Tomcat / Jetty
5. **CRUD Controller 基类**：`BaseReadOnlyController` / `BaseCrudController` 接通
   `IBaseReadOnlyService` / `IBaseCrudService` 的分页查询、详情、增删改 + 校验规则下发
6. **请求体安全护栏**：`MutableListSearchPayloadGuardAdvice` 拒绝以 `MutableListSearchPayload`
   作为外部请求体

## 设计要点

### 请求生命周期

```
servlet container
  → SessionRepositoryFilter (Spring Session)
  → WebContextInitFilter (order=DEFAULT+1) → KudosContextHolder.set(ctx)
     → DispatcherServlet
        → CorsHandlerInterceptor (头注入)
        → MutableListSearchPayloadGuardAdvice (拒绝可变 payload)
        → Controller
        → GlobalResponseBodyHandler (包成 ApiResponse + 回填 traceId)
        → BadRequestExceptionHandler / GlobalExceptionHandler (异常 → ApiResponse)
     ← (finally) KudosContextHolder.clear()
```

`KudosContextHolder.clear()` 在 filter 的 finally 中执行，保证线程池里的线程复用时不会
带着旧 context 跑下一个请求（这是 ThreadLocal 在 web 容器中最经典的内存泄漏 / 串台模式）。

### 容器切换

`kudos.ability.web.springmvc.server` 控制装配的工厂：
- `TOMCAT`（默认）—— 默认放宽 `relaxedPathChars` / `relaxedQueryChars` 收纳特殊字符，
  避免 GET 请求带 `"<>[]\^` `{|}` 时 Tomcat 直接 400
- `JETTY` —— 反射加载 `JettyServletWebServerFactory`，本模块 testImplementation 才有 Jetty
- `UNDERTOW` —— 已从枚举移除：Undertow 当前版本不支持 Servlet 6.1，被 Spring Boot 4 排除支持

`spring-boot-starter-web` 默认会带 Tomcat；切 Jetty 需自行排除 Tomcat starter 并加
`spring-boot-starter-jetty`。

### 异常处理双层

| 处理器 | 触发优先级 | 责任 |
|---|---|---|
| `BadRequestExceptionHandler` | `Ordered.HIGHEST_PRECEDENCE` | 4xx 类参数错（`@Valid`、`@RequestBody` JSON 解析、参数绑定、类型转换）→ 详细 `ErrorDetail` 列表 |
| `GlobalExceptionHandler` | 默认 | 业务异常 `ServiceException`、`require`/`check` 触发的 `IllegalArgumentException` / `IllegalStateException`、未捕获异常 → 通用 ApiResponse |

两个 advice 都标了 `@RestControllerAdvice`，但 `BadRequestExceptionHandler` 用 `@Order` 抢
先匹配——这样校验错误会带 `errors[]` 明细返回，而不是被 `GlobalExceptionHandler` 兜成"参数错误"
笼统响应。

### CORS

模块同时启用两套 CORS：
- `WebMvcConfigurer.addCorsMappings` —— Spring 框架级支持（含 preflight 处理、缓存）
- `CorsHandlerInterceptor` —— 朴素的"原样回显 Origin" 头注入

**生产部署应只保留前者并收紧到白名单 origin**。默认配置 `allowedOriginPatterns("*") +
allowCredentials(true)` 在 Spring 中合法（Spring 6+ 接受 pattern 形式），但语义上是
"反射任意来源 + 允许携带凭证"——等同开放门户。

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
由 `TerminalConstraintsCreator` 从 PO 注解反射生成。

**注意：基类用 `GenericKit.getSuperClassGenricClass(this::class, N)` 反射读父类泛型——
要求子类直接继承本基类。中间隔一层抽象基类时会失败。**

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/SpringMvcAutoConfiguration` | 装配入口（filters / handlers / interceptors / cors） |
| `init/SwitchingServletWebServerFactory` | Tomcat / Jetty 切换工厂 |
| `filter/WebContextInitFilter` + `IWebContextInitFilter` | 请求 → `KudosContext` 装配 + 清理 |
| `handler/GlobalResponseBodyHandler` | 返回值 → ApiResponse 包装 |
| `handler/BadRequestExceptionHandler` | 4xx 参数错统一响应 |
| `handler/GlobalExceptionHandler` | 业务 / 未捕获异常统一响应 |
| `handler/MutableListSearchPayloadGuardAdvice` | 拒绝可变 payload 作请求体 |
| `interceptor/CorsHandlerInterceptor` | CORS 头注入（旧式实现，与 Spring CORS 互补） |
| `controller/BaseController` / `BaseReadOnlyController` / `BaseCrudController` | CRUD 基类 |
| `support/XHttpServletRequest` | UA / IP / URL 解析扩展 |
| `support/enums/ServletServerEnum` | 容器枚举 |

## 配置示例

```yaml
kudos:
  ability:
    web:
      springmvc:
        server: TOMCAT   # TOMCAT | JETTY
      url:
        suffix: .html

server:
  port: 8080
  servlet:
    context-path: /api
  max-request-hold: 0    # 0 / 不配 = 不限制
  max-request-exclude:
    - /_monitor/**
    - /errors/**
```

## 测试覆盖

- `BadRequestExceptionHandlerTest` (4)、`GlobalExceptionHandlerTest` (9)、
  `GlobalResponseBodyHandlerTest` (7)、`MutableListSearchPayloadGuardAdviceTest` (4)
  —— MockMvc 集成测试，覆盖响应包装 / 异常映射 / 校验拒绝主要场景
- `TomcatServerTest` (2) / `JettyServerTest` (2) —— 容器装配启动 hello-world
- `UndertowServerTest` —— skipped，Undertow 不再支持
- `XHttpServletRequestTest` (9) —— 新增，纯单元，覆盖 UA / 终端 / URL 拼装；Edge → Chrome
  的当前误判用 case 锁定行为，调整时同步更新

未覆盖：`WebContextInitFilter`（依赖完整 Spring web 上下文）、`CorsHandlerInterceptor`
（拦截器本身行为简单，已由 CORS 集成测试间接覆盖）、`SwitchingServletWebServerFactory`
（由两个 ServerTest 间接覆盖）、`SpringMvcAutoConfiguration` bean 装配链路。

## 已知限制 / 后续工作

- ✅ 已移除未使用的 `commons-fileupload` 依赖；源码中无引用，multipart 处理继续沿用
  Spring Boot / Servlet 容器默认能力
- ❗ CORS 双套：`CorsHandlerInterceptor` 与 Spring `addCorsMappings` 重复处理同一 ResponseHeader，
  且 interceptor 用 `setHeader("Access-Control-Allow-Methods", "*")` 与 `addCorsMappings`
  的具体方法列表不一致。建议保留 Spring 框架版，废弃 interceptor
- ❗ 默认 CORS 配置过于开放（`allowedOriginPatterns("*") + allowCredentials(true)`），
  生产部署必须收紧；不要把 dev 默认配置直接搬到生产
- ❗ `BaseCrudController.getCreateValidationRule` 通过 `GenericKit.getSuperClassGenricClass(..., 6)`
  反射泛型——要求子类直接继承 BaseCrudController，不能中间多包一层
- ❗ `WebContextInitFilter` 把整个 session / 所有 cookie / 所有 header 都拷到 `KudosContext`，
  对大 session 不友好；高吞吐场景建议自定义 [IWebContextInitFilter] 只抓需要的字段
- ❗ `XHttpServletRequest.getBrowserInfo()` 中 Edge 检测顺序在 Chrome 之后——当前实现把 Edge UA
  误判为 Chrome；测试中已记录此行为，需要时调整 when 顺序
- ❗ `SpringMvcAutoConfiguration` 的 CORS 配置无 yml 开关；生产覆盖需要自定义 WebMvcConfigurer
- ❗ Undertow 已被 Spring Boot 4 排除（Servlet 6.1 不兼容）；如需 Undertow 需自行整合
  支持 Servlet 6.1 的非官方分支
- ❗ 内嵌容器未启用虚拟线程（Loom / Java 21+）。`spring.threads.virtual.enabled=true` 需配
  Tomcat 10.1+ / Jetty 12+

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-common"))
api(libs.spring.boot.starter.web)
api(libs.spring.session.core)
api(libs.spring.session.data.redis)

testImplementation(project(":kudos-test:kudos-test-common"))
testImplementation(libs.spring.boot.starter.webmvc.test)
testImplementation(libs.spring.boot.starter.jetty)
```
