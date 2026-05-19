# kudos-ms-msg-api-internal

Msg 服务 **对内 Provider 进程**的启动入口、自动配置与 `/api/internal/msg/...` 控制器。
是 `msg-client` 中各 `IMsg*Proxy` 的对端实现——服务间 Feign 调用最终落到这里。

## 内容

- `MsgApiProviderApplication` —— Spring Boot main，挂 `@EnableKudos`
- `MsgApiProviderAutoConfiguration` —— `@ComponentScan("io.kudos.ms.msg.api.internal")` +
  `IComponentInitializer.getComponentName() = "kudos-ms-msg-api-internal"`
- 4 个 thin controller，每个 `implements IMsg*Api`（来自 common），方法体单行
  转发到 core 的 `MsgXxxApi` 实现

## 控制器清单

| Controller | 实现契约 | 委托对象（来自 msg-core） |
|------------|----------|---------------------------|
| `MsgSendInternalController` | `IMsgSendApi` | `MsgSendApi` |
| `MsgTemplateInternalController` | `IMsgTemplateApi` | `MsgTemplateApi` |
| `MsgReceiveInternalController` | `IMsgReceiveApi` | `MsgReceiveApi` |
| `MsgInstanceInternalController` | `IMsgInstanceApi` | `MsgInstanceApi` |

**没有 `IMsgReceiverGroupApi` 的控制器**——因为 common 里这个接口是空的（见 msg-common
README "已知限制"）。

实现模式：

```kotlin
@RestController
class MsgReceiveInternalController(
    private val msgReceiveApi: MsgReceiveApi,
) : IMsgReceiveApi {

    override fun getReceivesByUserId(receiverId: String) =
        msgReceiveApi.getReceivesByUserId(receiverId)

    override fun markRead(id: String) =
        msgReceiveApi.markRead(id)
    // ...
}
```

> ⚠️ controller 类**没有** `@RequestMapping("/api/internal/...")` —— 路径全部继承自
> common 接口的方法级 `@GetMapping` / `@PostMapping`。看到 controller 想知道 URL 必须
> 跳进 `IMsg*Api`；IDE 里 Spring inspect 可以 resolve 出来，但裸读代码会迷路。

## 部署形态对比

| 维度 | api-internal | api-public |
|------|--------------|------------|
| 路径前缀 | `/api/internal/msg/...` | `/api/admin/msg/...`（见已知限制） |
| 受众 | 内部微服务（通过 Feign proxy） | 浏览器 / 控制台 |
| 服务发现 | **必须注册到 Nacos** | 通常不强求 |
| 配置中心 | Nacos config | 文件 / env |
| 跨服务缓存 | `cache-interservice-provider` 暴露本服务缓存接口 | 无 |

## 依赖（实际 `build.gradle.kts`）

```
api(":kudos-ms-msg-core")
api(":kudos-ability-cache-interservice-provider")    ← 独有
api(":kudos-ability-distributed-discovery-nacos")    ← 独有
api(":kudos-ability-distributed-config-nacos")       ← 独有
api(":kudos-ability-web-springmvc")
```

比 api-public 多出 **3 个分布式相关依赖**：
- **`discovery-nacos`**：进程启动时往 Nacos 注册服务，msg-client 的 `@FeignClient(name = "msg-...")`
  通过 Nacos 解析 IP
- **`config-nacos`**：拉远端配置，让运维不用滚动重启就能改 yml
- **`cache-interservice-provider`**：把 msg 服务的本地缓存暴露成跨服务可读，供其他服务
  通过 `cache-interservice-consumer` 直接拿数据（不是 Feign 调用，更轻）

## 装配链

```
MsgApiProviderApplication(@EnableKudos)
  ↓ boot
ComponentInitializationDispatcher 扫到所有 classpath 上的 IComponentInitializer：
  - MsgAutoConfiguration                  (msg-core)
  - MsgApiProviderAutoConfiguration       (本模块, scan io.kudos.ms.msg.api.internal)
  - 各 ability 模块的 *AutoConfiguration   (cache / ktorm / nacos / springmvc / ...)
  ↓
四个 internal controller 被注册成 bean，分别绑定到 IMsg*Api 注解的路径
```

## 已知限制 / 后续工作

- ❗ **api-internal 与 api-public 的边界由 yml 决定，代码层未硬分隔**——内部 controller
  的路径前缀 `/api/internal/...` 是契约（来自 common 接口注解），如果运维把 internal
  jar 部署到公网端口、没靠网关 ACL 拦截，外部就能直接调 `publish`。建议在 yml 里
  配 servlet path-based filter 或加 spring-security 全局拦截
- ❗ **缺 `IMsgReceiverGroupInternalController`**——等 common 的 `IMsgReceiverGroupApi`
  补完方法后同步补
- ❗ **rate limit / circuit breaker 全靠基础设施**——controller 层无任何 Resilience4j
  / Sentinel 装配；远端 burst 流量直冲 core service，要靠 ability-distributed-* 兜底
- ❗ **无 trace id propagation 显式装配**——指望 `@EnableKudos` 拉的 starter 处理；
  排障时需要确认链路追踪 starter 实际在 classpath
- ❗ **无 yml**——本模块零配置文件，nacos endpoint / serverPort / spring profile 都
  靠运行环境注入。本地起进程必须先设 `NACOS_SERVER_ADDR` 等环境变量
- ❗ **`MsgApiProviderApplication` 和 `MsgApiAdminApplication` 命名不一致**：
  Web / Provider / Admin 三套命名混用；建议统一成 Web / Provider 或全部 Application
