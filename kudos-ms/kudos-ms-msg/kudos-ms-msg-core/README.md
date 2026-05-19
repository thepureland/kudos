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

- ❗ **重复发送幂等性缺失**：`send(templateCode, params, receivers)` 没有 idempotencyKey
  参数——同一业务请求重试会产生多条 msg_instance / msg_receive。建议接入业务侧
  request id 或自实现幂等表
- ❗ **投递通道选择硬编码**：`MsgSendService` 走具体 `comm-email` / `comm-sms-aws` 实现——
  切换通道需重新部署。未实现 Strategy / Factory 模式让运行时路由
- ❗ **`MsgUnreceived` 重试无调度器**：失败消息入暂存表后没有自动重试 cron——需业务自建
  定时任务从 `msg_unreceived` 拉数据走 re-send 路径
- ❗ **控制器无 `@PreAuthorize`**：admin 控制器靠网关 / 外部鉴权过滤器做访问控制；漏
  配置时 `bulkSend` 等敏感 endpoint 直接暴露
- ❗ **模板内容大小无上限**：`msg_template.content` 通常是 TEXT 列；恶意大模板可导致
  渲染 OOM。建议在 form 校验加 maxLength
- ❗ **没有审计日志**：模板创建 / 修改 / 删除关键操作不写 AuditLog——合规审计场景需自接入

## 依赖

- `kudos-ms-msg-common` / `kudos-ms-msg-sql`
- `kudos-ability-data-rdb-ktorm` / `kudos-ability-cache-common`
- `kudos-ms-user-client`
- `kudos-ability-comm-email` / `kudos-ability-comm-sms-aws`（投递通道）
