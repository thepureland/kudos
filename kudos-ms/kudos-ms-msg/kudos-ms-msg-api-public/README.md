# kudos-ms-msg-api-public

Msg 服务 **对外 Web 进程**的启动入口与自动配置。生产部署 msg 的"公开 HTTP 面"（管理端 /
用户控制台调用）就跑这个 jar。

## 内容

- `MsgApiWebApplication` —— Spring Boot `main`，加 `@EnableKudos` 触发 kudos 框架
  自动装配
- `MsgApiWebAutoConfiguration` —— `@ComponentScan("io.kudos.ms.msg.api.public")` +
  `IComponentInitializer.getComponentName() = "kudos-ms-msg-api-public"`

## 启动

```bash
./gradlew :kudos-ms:kudos-ms-msg:kudos-ms-msg-api-public:bootRun
```

## 依赖（实际 `build.gradle.kts`）

```
api(project(":kudos-ms:kudos-ms-msg:kudos-ms-msg-core"))
api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
```

只有 `msg-core` + `web-springmvc`。**没有** `msg-api-admin` / `msg-api-internal` 的依赖
——所有控制器都不在 classpath 上。

## 与 api-internal 的区别

| 维度 | api-public | api-internal |
|------|------------|--------------|
| 启动类 | `MsgApiWebApplication` | `MsgApiProviderApplication` |
| 包路径 | `io.kudos.ms.msg.api.public` | `io.kudos.ms.msg.api.internal` |
| 路径规约 | `/api/admin/msg/...`（管理端） | `/api/internal/msg/...`（Feign provider） |
| 额外依赖 | 无 | `discovery-nacos` / `config-nacos` / `cache-interservice-provider` |
| 受众 | 浏览器 / 控制台 | 其他微服务（通过 msg-client 的 Feign proxy） |

代码层差异极小——主要靠运行期 yml（端口 / actuator / 网络可见性）分离。所以同一份
`msg-core` 业务实现在两个进程都跑一份。

## 已知限制 / 后续工作

- ❗ **`build.gradle.kts` 没有 `msg-api-admin` 依赖**，但 README 历来说"装配
  msg-core + msg-api-admin + 完整 web 栈"——**配置与文档对不上**。两种解释：
  (a) 漏配 admin 依赖，导致 public 进程实际上没有任何 `/api/admin/msg/...` 控制器
  注册（boot 起来但没业务路由）；
  (b) 设计意图就是把 admin 单独部署，public 只是 core + web 的 boot shell——但那样
  又解释不通为什么会拉 `web-springmvc`。
  **需要决定**：要么把 admin 加进依赖，要么把 README 改成"纯 core boot shell"并解释清楚
- ❗ **缺 `application.yml`**——repo 内没有任何 `application*.yml` / `application*.properties`，
  配置完全靠运行环境注入（容器 env、Spring Cloud Config）。CI 集成测试 / 本地调试
  缺示例文件，需要先翻 sibling 服务的 yml 模板
- ❗ **`MsgApiWebAutoConfiguration` `@ComponentScan` 只扫 `io.kudos.ms.msg.api.public`**
  ——只能拉到本模块的 controller / 配置；其他模块（admin / internal）的 controller 即使
  classpath 上有，也得靠各自的 `IComponentInitializer` 被框架 dispatcher 拉起。如果想
  让 public 同时挂 admin 路由，需要在依赖里加 admin 模块（dispatcher 会自动拉 admin 的
  `MsgApiAdminAutoConfiguration`）
- ❗ **无 actuator / observability 显式装配**——靠 `@EnableKudos` 透传，具体暴露哪些
  endpoint 看 yml；本模块代码层零控制
- ❗ **安全过滤器链未配置**——controller 层无 `@PreAuthorize`，public 进程的鉴权完全
  托管给外部网关；如果绕过网关直连本进程端口，所有 admin endpoint（前提是 admin 依赖
  补上后）都是裸的
