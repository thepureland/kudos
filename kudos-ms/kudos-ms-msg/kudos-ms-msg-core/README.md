# kudos-ms-msg-core

Msg 原子服务的**领域实现层**：DAO / 业务 Service / 多级缓存 / `IMsg*Api` 实现。
**不含 HTTP 控制器**（控制器在 `msg-api-admin`）。

## 分层

```
io.kudos.ms.msg.core
├── init/MsgAutoConfiguration        Spring 装配入口
├── model
│   ├── table                        Ktorm 表对象
│   └── po                           PO / 行对象
├── dao                              DAO 类
├── service
│   ├── iservice
│   └── impl
├── cache                            模板 / 接收组等缓存
└── api                              IMsg*Api 实现
```

## 业务流程在 core 里的体现

- `IMsgTemplateApi.findByCode(...)` → 缓存命中模板蓝本
- `IMsgSendApi.send(templateCode, params, receivers)` → 渲染 + 落 instance + 拆 send
- 投递失败 → `MsgUnreceived` 表 + 触发审计 / 重试调度

`MsgSendService` 通过 `comm-email` / `comm-sms-*` 等下层模块完成具体投递通道。

## 装配

`MsgAutoConfiguration`：
- `@ComponentScan("io.kudos.ms.msg.core")`
- `@AutoConfigureAfter(KtormAutoConfiguration::class)`
- `IComponentInitializer.getComponentName() = "kudos-ms-msg-core"`

## 依赖

- `kudos-ms-msg-common`、`kudos-ms-msg-sql`
- `kudos-ability-data-rdb-ktorm`、`kudos-ability-cache-common`
- `kudos-ms-user-client`（receiver-id → 用户具体联系方式）
- `kudos-ability-comm-email` / `kudos-ability-comm-sms-aws`（投递通道）
