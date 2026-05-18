# kudos-ms-msg

**定位**：**消息（`msg`）原子服务**的 Gradle 聚合模块——承载消息模板、消息实例、消息发送、
消息接收、接收组等领域能力。是平台级"系统通知 / 推送"的统一出口。

**在工程中的角色**：`SysConsts.ATOMIC_SERVICE_NAME = "msg"`；管理端 HTTP + 服务间 Feign。

---

## 子模块文档索引

| 子模块 | 说明 |
|--------|------|
| [kudos-ms-msg-common](kudos-ms-msg-common/README.md) | 跨模块共享契约 |
| kudos-ms-msg-sql | Flyway 迁移脚本 |
| kudos-ms-msg-core | DAO / Service / 缓存 / API 实现 |
| kudos-ms-msg-api-admin | 管理端 REST 控制器 |
| kudos-ms-msg-api-public | 对外 Web 启动入口 |
| kudos-ms-msg-api-internal | 对内 Provider 启动入口 |
| kudos-ms-msg-client | Feign 代理 + 降级 |

---

## 依赖关系（概念）

```
                    ┌──────────────────┐
                    │ kudos-ms-msg-    │
                    │ common           │
                    └────────┬─────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   │
┌─────────────────┐  ┌─────────────────┐         │
│ kudos-ms-msg-   │  │ kudos-ms-msg-   │         │
│ sql             │  │ client          │         │
└────────┬────────┘  └────────┬────────┘         │
         │                    │ only common      │
         └──────────┬─────────┘                  │
                    ▼                            │
            ┌───────────────┐                    │
            │ kudos-ms-msg- │                    │
            │ core          │◄───────────────────┘
            └───────┬───────┘
                    │
    ┌───────────────┼───────────────┐
    ▼               ▼               ▼
┌────────┐   ┌────────────┐   ┌────────────┐
│ api-   │   │ api-       │   │ api-       │
│ admin  │   │ public     │   │ internal   │
└────────┘   └────────────┘   └────────────┘
```

---

## 关键概念

- **消息模板（`MsgTemplate`）**：参数化的消息蓝本，业务侧按模板 id + 参数发送
- **消息实例（`MsgInstance`）**：模板渲染后的具体消息记录
- **消息发送（`MsgSend`）**：调度 / 投递的状态机
- **消息接收（`MsgReceive`）**：单一接收者的投递记录（含已读 / 未读 / 失败状态）
- **接收组（`MsgReceiverGroup`）**：消息接收者的逻辑分组，避免一次性挑用户
- **MsgUnreceived**：失败 / 未送达跟踪表（最近补的 feature batch 4）

## 业务流程

```
template (蓝本) + params
        ↓ 渲染
instance (实例)
        ↓ 调度
send (按 receiver 拆分)
        ↓ 投递
receive (一对一记录) → MsgUnreceived (失败补偿)
```

## 与其他服务的依赖

- 调 `kudos-ms-user-client` 把 receiver-id 列表换成具体用户（邮箱 / 手机号 / 推送 token）
- 调 `kudos-ability-comm-email` / `comm-sms-*` 走具体投递通道
- 失败入 `MsgUnreceived` 表 + 通过 `kudos-ability-log-audit-mq` 异步审计
