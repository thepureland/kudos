# kudos-ms-msg

**定位**：**消息（`msg`）原子服务**的 Gradle 聚合模块——承载消息模板、消息实例、消息发送、
消息接收、接收组等领域能力，是平台级"系统通知 / 推送"的统一出口。

**在工程中的角色**：`SysConsts.ATOMIC_SERVICE_NAME = "msg"`；管理端 HTTP + 服务间 Feign。

---

## 子模块文档索引

| 子模块 | 形态 | 一句话 |
|--------|------|--------|
| [kudos-ms-msg-common](kudos-ms-msg-common/README.md) | 契约库 | 4 个 `IMsg*Api` + 状态机枚举 + VO；`spring-web` 只 compileOnly |
| [kudos-ms-msg-sql](kudos-ms-msg-sql/README.md) | 资源（纯 SQL） | Flyway 迁移，**当前只有 h2 方言** |
| [kudos-ms-msg-core](kudos-ms-msg-core/README.md) | 业务实现库 | DAO / Service / 模板渲染 / 接收状态机 / 投递通道 listener |
| [kudos-ms-msg-client](kudos-ms-msg-client/README.md) | Feign 客户端库 | 4 对 Proxy + Fallback，**不依赖 core** |
| [kudos-ms-msg-api-admin](kudos-ms-msg-api-admin/README.md) | 管理端 controller 库 | `/api/admin/msg/...` 路由，5 个 BaseCrudController + 1 个手写 |
| [kudos-ms-msg-api-public](kudos-ms-msg-api-public/README.md) | 启动入口 | 对外 Web 进程 |
| [kudos-ms-msg-api-internal](kudos-ms-msg-api-internal/README.md) | 启动入口 | 对内 Feign Provider 进程，独有 Nacos / interservice-cache 依赖 |

---

## 依赖关系（实际）

```
                    ┌──────────────────┐
                    │ msg-common       │ ── compileOnly spring-web
                    └────────┬─────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   │
┌─────────────────┐  ┌─────────────────┐         │
│ msg-sql         │  │ msg-client      │         │
│ (Flyway 资源)   │  │ Proxy+Fallback  │         │
└────────┬────────┘  └─────────────────┘         │
         │                                       │
         └──────────┬────────────────────────────┘
                    ▼
            ┌───────────────┐
            │ msg-core      │
            │ Service+DAO   │
            └───────┬───────┘
                    │
    ┌───────────────┼─────────────────────────┐
    ▼               ▼                         ▼
┌──────────┐   ┌──────────┐               ┌──────────────┐
│ api-     │◄──│ api-     │               │ api-internal │
│ admin    │   │ public   │               │ +nacos       │
│ (库)     │   │ (启动)   │               │ +interservice│
└──────────┘   └──────────┘               └──────────────┘
```

> ⚠️ **api-internal 不 import api-admin**——内部 Feign provider 进程只挂 `/api/internal/msg/**`，
> 不挂管理端 controller。api-public 已通过依赖 api-admin 同时承载 `/api/admin/msg/**`
> （见 `kudos-ms-msg-api-public/build.gradle.kts`）。

---

## 关键概念

- **消息模板（`MsgTemplate`）**：参数化的消息蓝本，业务侧按 (tenantId, eventType, msgType, locale)
  4 元组查询，按模板 + params 发送
- **消息实例（`MsgInstance`）**：模板渲染后的具体消息记录（title + content + paramsUsed）
- **消息发送（`MsgSend`）**：调度 / 投递的状态机；一次 publish 一条 send 记录
- **消息接收（`MsgReceive`）**：单一接收者的投递记录（含 RECEIVED / UNREAD / READ / DELETED 状态）
- **接收组（`MsgReceiverGroup`）**：消息接收者的逻辑分组（按部门 / 角色 / tag / 在线状态等
  11 种 type），避免一次性穷举用户
- **未送达（`MsgUnreceived`）**：失败 / 未送达跟踪表，含 retry_count / resolved 字段

## 业务流程

```
business code
       ↓
IMsgSendApi.publish(MsgPublishRequest)         ── tenantId + eventType + msgType + locale
       ↓ Publish service
template lookup → 渲染 → 落 msg_instance        ── MsgTemplateRenderer
       ↓
落 msg_send（status=PENDING）→ 投 MQ
       ↓                          ↓ MQ 失败
status=SENT_TO_MQ              status=FAILED_TO_SEND_TO_MQ
       ↓ consumer 拉到
status=CONSUMED_FROM_MQ
       ↓ 各 channel listener（email / sms / siteMsg）发布
拆分到每个 receiverId → 落 msg_receive
       ↓ 投递失败
                              → 落 msg_unreceived (含 reason)
       ↓
最终 status = SUCCESS / SUCCESS_PARTIAL / FAILED_FINAL
```

## 与其他服务的依赖

- **`kudos-ms-user-core`**：`msg-core` 直接依赖（不是通过 Feign）——把 receiverId 换成
  具体联系方式（邮箱 / 手机号 / 推送 token）
- **`kudos-ability-comm-email`**：SMTP 投递通道（`MsgEmailDispatchListener`）
- **`kudos-ability-comm-sms-aws`**：AWS SNS SMS 投递通道（msg-core README 提及）
- **`kudos-ability-distributed-notify-common`**：channel listener 的事件分发抽象
  （`@EventListener` 接收 `MsgDispatchEvent`）

## 跨模块约定

- **字典码 = 枚举耦合**：`send_status` / `receive_status` / `publish_method` 等字典项
  的 `item_code` 必须等于 Kotlin 枚举的 `dictCode`。改动需要同步 `V1.0.0.2__insert_sys_dict_item.sql`
  与对应 enum 类
- **路径前缀约定**：管理端走 `/api/admin/msg/...`，服务间走 `/api/internal/msg/...`；
  internal 路径直接挂在 common 的 `IMsg*Api` 方法注解上，让 Feign proxy 与服务端
  controller 共享路径定义
- **VO 命名链**：`{Domain}{Row / Detail / Edit / FormCreate / FormUpdate / Query}`，
  全部放在 common 的对应业务子包 `vo/request|response/`

## 整体已知限制

详见各子模块 README。综合性问题：

- ✅ **SQL 方言已补 PG + MySQL**——`resources/sql/msg/{h2,postgresql,mysql}/` 三套并存，13 个迁移文件按 RdbTypeEnum 自动选择；DML 用 PG `on conflict do nothing` / MySQL `insert ignore` 保证幂等；`msg_template` 缺的 `idx_msg_template__tenant_event` 索引已补在 PG/MySQL 版本上（h2 暂未补，sys 表用 dict_code 做点查不依赖此索引）
- ✅ **fallback 已区分 4xx / 5xx**——5 个 Fallback 改为 `FallbackFactory<*>` + `AbstractFeignFallbackSupport` 重载，日志按 `client-error-4xx` / `server-error-5xx` / `unreachable` 分类
- ✅ **错误码占位已补**——5 个 ErrorCodeEnum（send/template/instance/receive/receiverGroup）按代码实际失败路径定义了核心码（NOT_FOUND / DUPLICATE / 业务条件）；UNSPECIFIED 保留作兜底

---

## 改进建议（自动分析 2026-06-11）

以下为本轮深度审查发现、但因涉及 public API 签名变更 / 行为变更 / 需业务确认而**未直接修改**的事项。
已直接修复的：email 渠道同邮箱多用户记账丢失 bug（`MsgEmailDispatchListener`）、
`MsgPublishService` 魔法值 `"user"` 常量化、以及 sql / client / common / api-internal 四份 README 的过期内容。

### 功能缺陷 / 待补功能

1. **发送频率限制缺失** —— `kudos-ms-msg-core/src/io/kudos/ms/msg/core/send/service/impl/MsgPublishService.kt`：
   publish 入口无任何每租户 / 每收件人频控，bug 或恶意调用可瞬间打爆 SMTP / AWS SNS 配额并骚扰用户。
   建议在 publish 前加可配置的令牌桶（按 tenantId + publishMethod 维度，可基于 redis）。
2. **模板变量校验缺失** —— `kudos-ms-msg-core/src/io/kudos/ms/msg/core/template/service/impl/MsgTemplateService.kt`
   与 `template/render/MsgTemplateRenderer.kt`：保存模板时不解析 `${}` 占位符集合；publish 时缺参仅把
   `${foo}` 原样输出给终端用户（渲染测试明确固化了该行为）。建议保存时提取占位符存元数据，publish 时
   校验 params 覆盖度，缺参按策略告警或拒发。
3. **渲染回退疑似漏判 `defaultActive`** —— `MsgTemplateRenderer.kt` 第 45-46 行：`MsgTemplate.defaultActive`
   语义是"是否启用默认值"，但渲染回退 defaultTitle/defaultContent 前未检查该开关，开关形同虚设。
   属行为变更，需业务确认后修复并同步单测 `falls_back_to_defaults_when_main_blank`。
4. **模板匹配非确定性** —— `MsgTemplateService.kt` `getTemplateByEvent` 用 `searchAs(criteria).firstOrNull()`，
   多条命中（尤其 localeDictCode=null 时）取哪条取决于 DB 返回顺序。建议加确定性排序（locale 精确匹配优先），
   或对 (tenant, event, msgType, locale) 建唯一约束。
5. **失败重试无调度器**（核心 README 已记录，此处汇总）—— `msg_unreceived` 落表后无自动重试 cron，
   `bumpRetry` 只记账不重发；建议补一个可选的重试调度组件（读 unresolved + 走 publish + bump）。

### 安全性

6. **markRead 无归属校验（越权标记）** —— `kudos-ms-msg-common/src/io/kudos/ms/msg/common/receiver/api/IMsgReceiveApi.kt`
   `markRead(id)` / core `MsgReceiveService.markRead`：仅凭 receive 主键即可置已读，未校验该记录属于哪个
   receiverId，任何内部调用方（或绕过网关的外部流量）可批量篡改他人已读状态。建议契约加 receiverId 参数并在
   service 层校验归属（涉及 public API 签名，未直接改）。
7. **publish 收件人集合无上限** —— `kudos-ms-msg-common/src/io/kudos/ms/msg/common/send/vo/request/MsgPublishRequest.kt`：
   `receiverIds` 无 size 上限校验，超大集合会单事件投 MQ、email 单批塞全部地址。建议加上限常量（如 1000）
   并在 `MsgPublishService.publish` 开头拒绝超限请求（返回 `MsgSendErrorCodeEnum` 新码）。
8. **收件箱查询无分页** —— `IMsgReceiveApi.getReceivesByUserId` / `MsgReceiveService.getReceivesByUserId`：
   一次性返回用户全部接收记录，老用户收件箱大时内存 / 带宽放大，可被用作放大攻击。建议契约加分页参数
   （API 签名变更，未直接改）。
9. **HTML 邮件参数未转义** —— `MsgEmailDispatchListener`（`htmlFormat=true` 默认开）+ `MsgTemplateRenderer`：
   业务 params 原样替换进 HTML 邮件正文；若 params 含用户可控内容（昵称等），存在 HTML / 链接注入（钓鱼）风险。
   建议对 email 渠道渲染结果做 HTML escape，或在渲染器按渠道注入转义策略。
10. **fallback 日志可能泄露消息参数** —— `kudos-ms-msg-client/src/io/kudos/ms/msg/client/send/fallback/MsgSendFallback.kt`：
    `errorWrite("publish", cause, request)` 把整个 `MsgPublishRequest`（含 params，可能携带验证码等敏感值）
    打进 ERROR 日志。建议只记 tenantId / eventType / receiver 数量等脱敏摘要。

### 测试覆盖

11. **渠道派发骨架无单测** —— `AbstractMsgChannelDispatchListener` 的 NO_CONTACT 记账、SUCCESS_PARTIAL
    聚合分支、`MsgSmsDispatchListener` 的 countdown 回调聚合均无测试；依赖均为接口，可 mock 后纯 JVM 测试。
12. **MsgReceiverGroupService 无测试** —— core `test-src` 缺 `MsgReceiverGroupServiceTest`
    （`fetchActiveReceiverGroups` 的 active + type 过滤逻辑）。
13. **client 缺契约测试**（client README 已记录）—— 建议 WireMock 验证 proxy 路径 / body 与 common 注解一致。

### 可观测性

14. **无 metrics** —— 全模块只有日志，没有发送成功率 / 渠道耗时指标。建议在
    `AbstractMsgChannelDispatchListener.finalizeDispatch` 与 `MsgPublishService.publish` 接入 Micrometer
    Counter/Timer（tag：channel、tenantId、status），否则"发送成功率"只能靠扫 msg_send 表统计。
15. **finishSend 读改写竞态** —— `kudos-ms-msg-core/src/io/kudos/ms/msg/core/send/service/impl/MsgSendService.kt`
    `finishSend`：先 get 再 updateProperties，无乐观锁也非原子自增；并发完成回调时计数可能互相覆盖。
    建议改为原子 `UPDATE ... SET success_count = success_count + ?` 或加 version 列。

### API contract

16. **markRead 返回值语义混叠** —— `IMsgReceiveApi.markRead` 返回 `false` 同时表示"记录不存在"与
    "已是 READ/DELETED 态"，且 Feign 降级也返回 `false`，调用方无法区分三种情况。建议未来版本返回
    结果枚举或抛带 `MsgReceiveErrorCodeEnum` 的异常。
17. **错误码已定义但未贯通** —— `MsgSendErrorCodeEnum` 等已补核心码，但 `MsgPublishService` 失败路径仍是
    "返回 null + 日志"，调用方拿不到 errorCode；建议 publish 改为抛业务异常或返回带码的结果对象
    （public API 行为变更，未直接改）。

### 可扩展性 / 可维护性（轻微）

18. **邮件 / 短信单租户单账号** —— `MsgEmailProperties` / `MsgSmsProperties` KDoc 已注明；多租户凭据
    需要一个 per-tenant 凭据解析 SPI，建议与频控（第 1 条）一起设计。
19. **MsgUnreceivedService.recordFailures 逐条 insert** —— 大批量失败时 N 次往返；BaseCrudDao 如有
    batchInsert 可替换（需确认批量插入对 id 生成 / 事件钩子的行为一致，故未直接改）。
