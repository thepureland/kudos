# kudos-ms-msg-common

`msg` 原子服务的**共享契约层**——所有跨进程契约（Feign API、错误码、VO、缓存条目）落在这里，
core / client / api-* 三侧都靠它对齐。

**依赖姿态**（见 `build.gradle.kts`）：
- `api(project(":kudos-base"))`
- `compileOnly(platform(libs.spring.boot.bom))` + `compileOnly("org.springframework:spring-web")`

`spring-web` 用 `compileOnly` ——只是为了在 `IMsg*Api` 上挂方法级 `@GetMapping` /
`@PostMapping`，让 Feign 端识别路由；运行期不强制把 spring-web 拉进 client / consumer。
与 sys-common / user-common / auth-common 同模式。

## 包结构（先按业务再分 layer）

```
io.kudos.ms.msg.common.
├── send/
│   ├── api/                ── IMsgSendApi
│   ├── enums/              ── MsgSendStatusEnum、MsgPublishMethodEnum、MsgSendErrorCodeEnum
│   └── vo/
│       ├── MsgDispatchEvent          (Spring ApplicationEvent payload，给 channel listener)
│       ├── MsgSendCacheEntry
│       ├── request/                  (MsgPublishRequest、MsgSendForm*、MsgSendQuery)
│       └── response/                 (MsgSendRow / Edit / Detail)
├── instance/
│   ├── api/                ── IMsgInstanceApi
│   ├── enums/              ── MsgInstanceErrorCodeEnum
│   └── vo/                 ── MsgInstanceCacheEntry + request/response
├── receiver/                          ← 注意是 receiver 不是 receive
│   ├── api/                ── IMsgReceiveApi、IMsgReceiverGroupApi
│   ├── enums/              ── MsgReceiveStatusEnum、MsgUnreceivedReasonEnum、
│   │                          MsgReceive/ReceiverGroupErrorCodeEnum
│   └── vo/                 ── MsgReceiveCacheEntry、MsgReceiverGroupCacheEntry + request/response
└── template/
    ├── api/                ── IMsgTemplateApi
    ├── enums/              ── MsgTemplateErrorCodeEnum
    └── vo/
        ├── RenderedMessage           (渲染产物：title + content + paramsUsed)
        ├── MsgTemplateCacheEntry
        ├── request/                  (IMsgTemplateFormBase / Create / Update / Query)
        └── response/                 (MsgTemplateRow / Edit / Detail)
```

旧的 `receive` / `receivergroup` 已合并到 **`receiver`** 单一业务包下，但 API 接口仍
分成两个：`IMsgReceiveApi`（单条收件）与 `IMsgReceiverGroupApi`（接收组定义）。

## Feign 路径约定

所有对外 API 路径都走 `/api/internal/msg/{domain}/{op}`，并且：
- **方法级注解**——`@GetMapping` / `@PostMapping` 直接挂在接口方法上
- **接口本身不带 `@RequestMapping` 前缀**——Feign 通过完整路径解析，省一层拼接
- 服务端控制器 `implements IMsg*Api`，自动继承同样的路径，**契约 = 路由**

| 接口 | 方法 | 路径 |
|------|------|------|
| `IMsgSendApi` | `publish(MsgPublishRequest)` | `POST /api/internal/msg/send/publish` |
| `IMsgTemplateApi` | `getTemplateById(id)` | `GET /api/internal/msg/template/getTemplateById` |
| `IMsgTemplateApi` | `getTemplateByEvent(tenantId, eventType, msgType, locale?)` | `GET /api/internal/msg/template/getTemplateByEvent` |
| `IMsgInstanceApi` | `getInstanceById(id)` | `GET /api/internal/msg/instance/getInstanceById` |
| `IMsgReceiveApi` | `getReceivesByUserId(receiverId)` | `GET /api/internal/msg/receive/getReceivesByUserId` |
| `IMsgReceiveApi` | `getUnreadCountByUserId(receiverId)` | `GET /api/internal/msg/receive/getUnreadCountByUserId` |
| `IMsgReceiveApi` | `markRead(id)` | `POST /api/internal/msg/receive/markRead` |
| `IMsgReceiveApi` | `markAllReadByUserId(receiverId)` | `POST /api/internal/msg/receive/markAllReadByUserId` |
| `IMsgReceiverGroupApi` | `getReceiverGroupById(id)` | `GET /api/internal/msg/receiverGroup/getReceiverGroupById` |
| `IMsgReceiverGroupApi` | `listActiveReceiverGroups(receiverGroupTypeDictCode?)` | `GET /api/internal/msg/receiverGroup/listActiveReceiverGroups` |

> `IMsgReceiverGroupApi` 当前只暴露只读 lookup，不提供跨服务创建 / 修改接收组；写操作仍走
> admin 端 `MsgReceiverGroupAdminController`。

## 状态机

### MsgSendStatusEnum（`msg_send.send_status_dict_code`）

```
PENDING (00)
   ↓ Publish service 投 MQ 成功
SENT_TO_MQ (11) ─→ 投 MQ 失败时直接置 FAILED_TO_SEND_TO_MQ (21)
   ↓ Consumer 拉到消息
CONSUMED_FROM_MQ (31)
   ↓ Consumer 完成发送
SUCCESS (33) / SUCCESS_PARTIAL (32) / FAILED_FINAL (22)

旁路：CANCELLED (01)  —— admin 主动止损
```

### MsgReceiveStatusEnum（`msg_receive.receive_status_dict_code`）

```
RECEIVED (11) ─┐
               ├── 都算"未读"，并入 UNREAD_CODES = {11, 01}
UNREAD (01) ───┘
        ↓ 点开
READ (12)
        ↓ 收件箱删除（保留行做审计）
DELETED (21)
```

> `RECEIVED` 与 `UNREAD` 的区别：`RECEIVED` 是落库初始态（接收方还没拉过），`UNREAD`
> 是拉过但未点开。`getUnreadCountByUserId` 用 `UNREAD_CODES` 同时算两者。

### MsgUnreceivedReasonEnum

`NO_CONTACT` / `CHANNEL_REJECT` / `TIMEOUT` / `LISTENER_ERROR` / `EMPTY_RECEIVERS` /
`UNKNOWN`——写入 `msg_unreceived.fail_reason` 前缀，便于运营按原因分桶处理。

## 关键 VO

- **`MsgPublishRequest`**：4-tuple lookup（`tenantId` + `eventTypeDictCode` +
  `msgTypeDictCode` + `localeDictCode?`）+ `receiverIds` + `params` + `publishMethod`。
  Publish 服务用前 4 个字段去 `getTemplateByEvent` 拿模板
- **`MsgDispatchEvent`**：Spring `ApplicationEvent` payload，由 publish service 发布、
  各 channel listener（email / sms / siteMsg）订阅。包含已渲染的 `renderedTitle` /
  `renderedContent` —— **模板不再传**，避免 listener 重复渲染
- **`RenderedMessage`**：渲染产物三元组（`title` / `content` / `paramsUsed`）。
  `paramsUsed` 是审计字段——记录这次渲染实际替换了哪些占位符的值
- **`MsgTemplateCacheEntry`**：含 `defaultActive` / `defaultTitle` / `defaultContent`——
  渲染时若 `title.isBlank()` 回退到默认值，逻辑见 `MsgTemplateRenderer`
- **`MsgReceiverGroupCacheEntry`**：含 `defineTable` + `nameColumn`，暗示按"动态表 + 取
  哪一列做姓名"的方式解析组成员；解析逻辑在 core 而非 common

## 错误码

`MsgSendErrorCodeEnum` / `MsgTemplateErrorCodeEnum` / `MsgInstanceErrorCodeEnum` /
`MsgReceiveErrorCodeEnum` / `MsgReceiverGroupErrorCodeEnum`——已按代码实际失败路径定义
核心码（如 send 侧 `RECEIVER_IDS_EMPTY` / `TEMPLATE_NOT_FOUND` / `MQ_PUBLISH_FAILED`），
`UNSPECIFIED` 保留作兜底。新增时注意与 `kudos-base` 的 `IErrorCodeEnum` 约定的
code 段落不冲突，i18n 前缀为 `msg.error-msg.{domain}`。

## 与其他模块的关系

```
                    base(kudos-base)
                          ▲
                          │ api
                  ┌───────┴───────┐
                  │ msg-common    │  ←—— compileOnly: spring-web (仅注解可见)
                  └───────┬───────┘
            ┌─────────────┼─────────────┐
            ▼             ▼             ▼
        msg-core      msg-client     msg-api-* (通过 core 间接拉)
        (实现 IMsg*Api)  (Feign + Fallback)
```

common **不依赖任何运行时框架**——纯契约，能被任何 JVM 服务消费。这是为什么 spring-web
要用 `compileOnly`：注解只在编译期看到，运行时由消费方决定要不要拉。

## 已知限制 / 后续工作

- ✅ `IMsgReceiverGroupApi` 已补最小查询契约：按 id 获取接收者群组、按类型列出启用群组；
  `msg-core` / `api-internal` / `msg-client` 已同步实现
- ✅ **错误码已按失败路径补核心码**——5 个 ErrorCodeEnum 不再只有 `UNSPECIFIED`；
  但 core 侧多数失败路径仍以"返回 null + 日志"而非抛带 errorCode 的异常收尾，
  调用方还吃不到这些码（见组级 README 改进建议）
- ✅ `MsgPublishMethodEnum` 已补 `ALL_USER("all_user")`，并修正 SQL 中 `all_user`
  字典项归属到 `publish_method`
- ❗ **DTO 校验注解不全**——`IMsgTemplateFormBase` 已挂 `@MaxLength`（对齐 DDL 列宽），
  但 `MsgPublishRequest` 仍无 `receiverIds` 大小上限 / `params` 体积约束，其余 Form
  也缺 `@NotNull` 类注解；校验逻辑部分仍在 core 侧手写
- ❗ **`MsgSendCacheEntry.jobId` 隐含批量调度概念**，但 common 里没有 Job 相关类型；
  调度抽象目前在 core 内部，跨服务可见度为零
- ❗ **VO 命名混用**：`MsgSendRow` vs `MsgInstanceRow` vs `MsgReceiveRow` 命名同模式 OK，
  但 `MsgReceiverGroupDetail` 中含 `audit timestamps`，其他 Detail VO 不含——契约不齐
