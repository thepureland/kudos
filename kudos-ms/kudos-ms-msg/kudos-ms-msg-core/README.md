# kudos-ms-msg-core

Msg 原子服务的**领域实现层**：模板渲染 / 发送 / 接收追踪 / 投递失败补偿。
**不含 HTTP 控制器**（控制器在 `msg-api-admin` / `msg-api-internal` / `msg-api-public`）。

## 业务对象

| 表/PO | 角色 |
|---|---|
| `msg_template` | 消息模板蓝本（含 title / content / defaultTitle / defaultContent） |
| `msg_instance` | 一次发送的"消息实例"——某次 send 调用拼出的 title + body 落库 |
| `msg_receive` | 一次实例对一个收件人的"接收记录"——含 read / unread 状态 |
| `msg_unreceived` | 投递失败 / 离线收件人的暂存表（重试补偿用） |
| `msg_receiver_group` | 收件组定义 |

## 模板渲染（`template/render/MsgTemplateRenderer`）

```
MsgTemplateCacheEntry + Map<String, Any> params + nowProvider
                              ↓
            title := template.title ?: template.defaultTitle  (.ifBlank 回退)
            content := template.content ?: template.defaultContent
                              ↓
        merged := autoParams(now)  ⊕  params       (params 同名覆盖)
                              ↓
        RenderedMessage(title, content, paramsUsed = merged)
```

- 自动参数：`time` / `date` / `year` / `month` / `day`（来自 `nowProvider()`，测试可注入）
- 占位符语法 `${name}`——来自 `kudos-base` 的 `fillTemplateByObjectMap`
- 业务参数同名时**优先生效**：业务方传入的 `time` 会覆盖自动注入的当前时间

## 接收状态机（`MsgReceiveStatusEnum`）

```
UNREAD ── markRead ──► READ
   │                    │
   └────── DELETED ◄────┘
```

`MsgReceiveService.markRead` 跳过已经 READ / DELETED 的记录——避免重复审计副作用。
`markAllReadByUserId` 用 `batchUpdateProperties` 单条 UPDATE，不触发逐条事件。

## 跨服务依赖

| Client | 用途 |
|---|---|
| `kudos-ms-user-client` | receiverId → 用户具体联系方式（邮箱 / 手机号） |
| `kudos-ability-comm-email` | SMTP 投递通道 |
| `kudos-ability-comm-sms-aws` | AWS SNS SMS 投递通道 |

## 投递渠道（`send/channel/`）

发布与消费解耦：`MsgPublishService` 只把渲染结果按 `publishMethod` 投到 MQ，真正的发送在各渠道
监听器里完成。渠道路由是**运行时**的——每个监听器是一个 `INotifyListener`，框架按
`notifyType = MsgPublishMethodEnum.listenerType` 自动分发。

```
AbstractMsgChannelDispatchListener   ← 模板方法：状态流转 / 联系方式批量查询 / 无联系方式(NO_CONTACT)
        ▲          ▲                    与渠道拒收(CHANNEL_REJECT)记账 / MsgReceive 落库 / 成功失败聚合
        │          │
  Email 渠道    SMS 渠道               ← 子类只实现：contactWayDictCode + doDispatch(实际发送)
 (contact 201) (contact 101)
```

| 渠道 | 类 | 联系方式码 | 生效条件 | 底层 |
|---|---|---|---|---|
| Email | `email/MsgEmailDispatchListener` | `201` | `kudos.msg.email.server-host` | `comm-email`（一次批量发，回调给成功/失败邮箱列表） |
| SMS | `sms/MsgSmsDispatchListener` | `101` | `kudos.msg.sms.aws.access-key-id` | `comm-sms-aws`（按号码逐条异步发，N 个回调用 countdown 聚合） |

**新增渠道**：
1. 在 `MsgPublishMethodEnum` 加渠道项（含 listenerType），并同步 `publish_method` 字典
2. 新建 `XxxDispatchListener : AbstractMsgChannelDispatchListener`，实现 `publishMethod` /
   `contactWayDictCode` / `doDispatch`，用 `@ConditionalOnProperty` 控制生效
3. 不需要改发布侧、其它渠道，也不需要重新部署它们

## 装配

`MsgAutoConfiguration`：
- `@ComponentScan("io.kudos.ms.msg.core")`
- `@AutoConfigureAfter(KtormAutoConfiguration::class)` —— 等 Ktorm Database 就绪
- `IComponentInitializer.getComponentName() = "kudos-ms-msg-core"`

## 测试覆盖

- 模板渲染单测覆盖：占位符替换 / 自动参数 / 业务参数优先 / 空模板回退 defaultTitle/Content
- 接收 / 实例 / 模板的 DAO / Service 集成测试均用 h2

`MsgTemplateRenderer.render(nowProvider = ...)` 暴露 ctor-less 时钟注入——单测可注入
固定 `LocalDateTime` 验证 `time` / `date` / `year` 字段。

## 已知限制 / 后续工作

- ✅ **重复发送幂等性**（已实现）：`MsgPublishRequest.idempotencyKey` 传业务请求唯一标识
  （如业务侧 request id）后，相同 `(tenantId, idempotencyKey)` 的重复 publish 直接返回已有
  `MsgSend.id`，不再产生重复 msg_instance / msg_send。落库由 `msg_send.idempotency_key` +
  唯一索引 `uq_msg_send__tenant_idempotency` 保证；并发竞态下后到的事务因唯一索引失败，重试时
  命中短路分支。**不传 / 传空** 则保持旧行为（每次都新建记录）。注意短路是在模板渲染 / 落库
  *之前* 做一次 select 预检，幂等命中不会渲染模板
- ✅ **投递通道运行时路由**（已实现）：发布侧 `MsgPublishService` 按 `request.publishMethod` 把
  事件投到 MQ（notifyType = `publishMethod.listenerType`，如 `msg.dispatch.email`）；消费侧每个渠道
  是一个 `INotifyListener` bean，由 notify 框架按 notifyType 自动路由——**加渠道 = 加一个 bean，
  无需改其它渠道或发布侧、无需重新部署**。共用的派发骨架（状态流转 / 联系方式查询 / 无联系方式与
  渠道拒收记账 / 成功失败聚合）抽到 `AbstractMsgChannelDispatchListener`，渠道子类只实现两点：
  联系方式字典码 + 实际发送。详见下方「投递渠道」
- ❗ **`MsgUnreceived` 重试无调度器**：失败消息入暂存表后没有自动重试 cron——需业务自建
  定时任务从 `msg_unreceived` 拉数据走 re-send 路径
- ⚙️ **控制器无 `@PreAuthorize`（刻意，按全项目约定）**：method-level `@PreAuthorize` 在整个
  kudos 代码库零先例——授权统一由网关 / 外部鉴权过滤器做。msg 沿用此约定，不引入 method security。
  部署时务必确保网关对 `/api/admin/**`、`bulkSend` 等敏感 endpoint 配了访问控制，否则会直接暴露
- ✅ **模板内容大小上限**（form 层已实现）：`IMsgTemplateFormBase` 各字段加了 `@MaxLength`
  约束（与 `msg_template` DDL 列宽对齐；`content` / `defaultContent` 无界列封顶
  `CONTENT_MAX_LENGTH=65535`，对齐 MySQL `TEXT`），由 BaseCrudController save/update 的
  `@Valid` 强制，挡住恶意大模板导致的渲染 OOM。**仍待办（属 msg-sql）**：`msg_template.content`
  在 SQL 层仍是无界 `varchar`/CLOB，跨方言移植 mysql/pg 时建议显式给列上限
- ✅ **模板 CRUD 审计日志**（已实现，在 `msg-api-admin`）：`MsgTemplateAdminController` 的
  create / update / delete / batchDelete 用 `@WebAudit`（opType=CREATE/UPDATE/DELETE，
  moduleCode=`msg-template`）标注，由 `WebLogAuditAspect` 拦截落审计日志。注解是被动的——
  需部署侧接入审计存储（log-audit-rdb / mongo / mq 之一）才会真正写库，未接入时注解无副作用

## 依赖

- `kudos-ms-msg-common` / `kudos-ms-msg-sql`
- `kudos-ability-data-rdb-ktorm` / `kudos-ability-cache-common`
- `kudos-ms-user-client`
- `kudos-ability-comm-email` / `kudos-ability-comm-sms-aws`（投递通道）
