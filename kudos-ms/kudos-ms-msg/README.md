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
│ api-     │   │ api-     │               │ api-internal │
│ admin    │   │ public   │               │ +nacos       │
│ (库)     │   │ (启动)   │               │ +interservice│
└──────────┘   └──────────┘               └──────────────┘
```

> ⚠️ **api-public / api-internal 并不 import api-admin**——三个 api-* 模块各自独立依赖
> `msg-core`，互不交叉。这意味着 api-public 进程上**没有** admin controller 注册
> （除非补依赖）。详见 `kudos-ms-msg-api-public/README.md` 的"已知限制"。

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

- ❗ **api-public 没有 admin 依赖**——三个 api-* 各自独立 boot，但 README 与 gradle
  config 不一致，需要决定是补依赖还是改文档
- ❗ **`IMsgReceiverGroupApi` 接口体空**——common / client / api-internal 三处都缺
  接收组的跨服务读写
- ❗ **SQL 只有 h2 方言**——生产 mysql / pg 移植尚未做；msg_template 索引也缺
- ❗ **fallback 不区分 4xx / 5xx**——调用方拿到 null 时分不清是参数错还是对端挂
- ❗ **错误码全是 `UNSPECIFIED`** —— 4 个 ErrorCodeEnum 占位待填
