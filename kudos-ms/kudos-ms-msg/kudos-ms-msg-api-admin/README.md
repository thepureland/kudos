# kudos-ms-msg-api-admin

Msg 服务的 **管理端 REST 控制器层**——路径前缀 `/api/admin/msg/...`，面向运营 / 控制台
界面。控制器调 `msg-core` 的 `IMsg*Service`，**不直接接触 DAO**。

> 注：本模块 **既能作为库被嵌入** `msg-api-public` / `msg-api-internal`，也带有自己的
> `MsgApiAdminApplication`（`@EnableKudos` + `main`）可单独 boot 起来调试，但**生产
> 部署不走这个入口**——靠 public / internal 启动并通过 `@ComponentScan` 把 admin 的
> controller 拉进来。

## 控制器清单

| Controller | 路径前缀 | 委托 service | 模式 |
|------------|----------|--------------|------|
| `MsgTemplateAdminController` | `/api/admin/msg/template` | `IMsgTemplateService` | `BaseCrudController` + `@WebAudit`（create/update/delete 落审计日志） |
| `MsgInstanceAdminController` | `/api/admin/msg/instance` | `IMsgInstanceService` | `BaseCrudController` |
| `MsgSendAdminController` | `/api/admin/msg/send` | `IMsgSendService` | `BaseCrudController` |
| `MsgReceiveAdminController` | `/api/admin/msg/receive` | `IMsgReceiveService` | `BaseCrudController` |
| `MsgReceiverGroupAdminController` | `/api/admin/msg/receiverGroup` | `IMsgReceiverGroupService` | `BaseCrudController` |
| `MsgUnreceivedAdminController` | `/api/admin/msg/unreceived` | `IMsgUnreceivedService` | **手写**（非 CRUD） |

### BaseCrudController 路由（标准 9 件套）

5 个 controller 继承 `BaseCrudController<ID, Service, Query, Row, Detail, Edit, FormCreate, FormUpdate>`，
自动暴露：

```
POST   /pagingSearch               → PagingSearchResult<Row>
GET    /getDetail                  → Detail
GET    /getEdit                    → Edit
GET    /getCreateValidationRule    → Map<String, ValidationRule>
GET    /getUpdateValidationRule    → Map<String, ValidationRule>
POST   /save                       → ID
PUT    /update                     → Boolean
DELETE /delete                     → Boolean
POST   /batchDelete                → Boolean
```

例：

```kotlin
@RestController
@RequestMapping("/api/admin/msg/send")
class MsgSendAdminController :
    BaseCrudController<String, IMsgSendService, MsgSendQuery,
                       MsgSendRow, MsgSendDetail, MsgSendEdit,
                       MsgSendFormCreate, MsgSendFormUpdate>()
```

——一行 class 声明 = 9 个 endpoint。VO 类型链 `Row / Detail / Edit / FormCreate / FormUpdate`
全来自 `msg-common`。

### MsgUnreceivedAdminController（特例）

故意不继承 `BaseCrudController`，因为 `msg_unreceived` 表是 **listener 驱动写入**
（投递失败时由 channel listener 落表），admin 端没有"手工新增一条失败记录"的合理场景。
只暴露三个运营操作：

| 方法 | 路径 | 入参 | 返回 |
|------|------|------|------|
| `listUnresolvedBySend` | `GET /api/admin/msg/unreceived/listUnresolvedBySend` | `@RequestParam sendId` | `List<MsgUnreceivedRow>` |
| `resolve` | `POST /api/admin/msg/unreceived/resolve` | `@RequestParam id` | `Boolean` |
| `bumpRetry` | `POST /api/admin/msg/unreceived/bumpRetry` | `@RequestParam id` | `Boolean` |

> ⚠️ `bumpRetry` **只累加计数 / 推进时间戳，不实际重发**——业务侧需要自己重新调
> `IMsgSendApi.publish`，再回调 `bumpRetry` 记账。这是 README of `MsgUnreceivedAdminController`
> 中显式说明的设计取舍。

## 装配

`MsgApiAdminAutoConfiguration`：

```kotlin
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.msg.api.admin"])
open class MsgApiAdminAutoConfiguration : IComponentInitializer {
    override fun getComponentName() = "kudos-ms-msg-api-admin"
}
```

通过 `IComponentInitializer` SPI（被 `ComponentInitializationDispatcher` 在 boot 时
统一拉起）暴露给框架。**不是** Spring Boot 标准的 `AutoConfiguration.imports` 机制——
kudos 自带的 dispatcher 替代了 Spring Boot 的 auto-config SPI，新增 controller 包不
需要改 imports 文件，只要被 `@ComponentScan` 覆盖即可。

## 依赖

```
api(project(":kudos-ms:kudos-ms-msg:kudos-ms-msg-core"))
api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-common"))
testImplementation(project(":kudos-test:kudos-test-container"))
```

core + web-springmvc + log-audit-common（仅为 `@WebAudit` 注解；审计存储由部署侧选
log-audit-rdb / mongo / mq）——没有任何 distributed / cache / discovery 依赖；这些都是
public / internal 启动模块的职责，让 admin 保持纯 controller 层。

## 与 api-public / api-internal 的边界

```
                ┌──────────────────────┐
                │ msg-api-public       │  ← 启动入口（管理端 HTTP / 用户面向）
                │ MsgApiWebApplication │
                └──────────┬───────────┘
                           │ @ComponentScan 拉取
                ┌──────────▼───────────┐
                │ msg-api-admin        │  ← 本模块（库形态嵌入）
                │ /api/admin/msg/...   │
                └──────────────────────┘

                ┌──────────────────────┐
                │ msg-api-internal     │  ← 启动入口（服务间 Feign provider）
                │ MsgApiProviderApp    │
                └──────────┬───────────┘
                           │ @ComponentScan 拉取（路径不同：internal/...）
                           ▼
                ┌──────────────────────┐
                │ msg-api-internal     │
                │ /api/internal/msg/...│ ← 不复用 admin，独立一套 thin facade
                └──────────────────────┘
```

注意：**api-public 嵌入 admin**，所以 admin 的 `/api/admin/msg/...` 路由在 public
进程上可见；**api-internal 不嵌入 admin**，它有自己的 `/api/internal/msg/...` 控制器
直接实现 `IMsg*Api`。也就是说同一个 controller 不会同时挂两套路径——admin 与 internal
是平行而非分层关系。

## 已知限制 / 后续工作

- ⚙️ **无 `@PreAuthorize`（刻意，按全项目约定）**：method-level `@PreAuthorize` 在整个 kudos
  代码库零先例——授权统一靠网关 / `EnableKudos` 框架级过滤器。admin 沿用此约定。部署时务必确保
  网关对 `/api/admin/**`、`batchDelete` 等敏感 endpoint 配了访问控制，否则绕过网关会直接暴露
- ✅ **模板 CRUD 审计**（已实现）：`MsgTemplateAdminController` create/update/delete/batchDelete
  用 `@WebAudit` 标注，由 `WebLogAuditAspect` 落审计日志（需部署侧接入审计存储才真正写库）。
  其余 admin 控制器如需审计，照此加注解即可
- ❗ **路由由 `BaseCrudController` 反射生成**——没有显式 enum 路由列表，IDE 跳转 +
  OpenAPI 生成都依赖反射 introspection，外部接入文档难写
- ❗ **错误响应靠全局 handler**——controller 不显式声明 `@ResponseStatus` / problem+json，
  错误结构由 `kudos-ability-web-springmvc` 兜底；前端如果想差异化处理，需要看全局
  handler 的实现而非接口签名
- ❗ **无 idempotency 头**：`POST /save` 重试时会创建多条记录；与 `msg-core` 的"重复
  发送幂等性缺失"同源
- ❗ **`MsgApiAdminApplication` 是误导**：模块里挂了 `main` 但生产不走这个入口；删除
  + 让 admin 维持纯库形态会更清晰，避免误启
- ❗ **`MsgUnreceivedAdminController.bumpRetry` 与 publish 解耦**——运营在 UI 上点
  "重试"必须分两步（先重发再 bump），UX 容易漏；要么 admin 端封装一个 `retry` 接口
  把 publish + bump 串起来
