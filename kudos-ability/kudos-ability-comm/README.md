# kudos-ability-comm

通信能力主题——邮件 / 短信 / WebSocket。

| 子目录 | 实现 |
|---|---|
| [`kudos-ability-comm-common`](kudos-ability-comm-common/README.md) | 共享 base（占位） |
| [`kudos-ability-comm-email`](kudos-ability-comm-email/README.md) | SMTP 邮件发送（基于 spring-boot-starter-mail） |
| [`kudos-ability-comm-sms`](kudos-ability-comm-sms/README.md) | 短信发送（阿里云 / AWS） |
| [`kudos-ability-comm-websocket`](kudos-ability-comm-websocket/README.md) | WebSocket 业务封装（会话注册 / 广播 / 分布式投递，基于 Ktor） |

所有通信类模块都走"虚拟线程 + 回调"模式：业务调 `handler.send(req) { result -> ... }`，
立即返回，结果通过 callback 异步通知。

## 改进建议（自动分析 2026-06-11）

以下为本次深度审查中**不宜直接修改**（涉及 public API 变更 / 新功能 / 设计决策）的发现，
按维度分类。已直接修复的问题（阿里云 endpoint 解析 bug、邮件头注入校验、路由日志归属、
若干 README 过时陈述）不在此列。

### 对外接口（public API）

- **阿里云回调泄漏第三方 SDK 类型**：`kudos-ability-comm-sms-aliyun/src/io/kudos/ability/comm/sms/aliyun/handler/AliyunSmsHandler.kt`
  的 `send` 回调参数是阿里云 SDK 的 `SendSmsResponseBody`，而 AWS 模块回调的是自有的
  `AwsSmsCallBackParam`。建议补一个 `AliyunSmsCallBackParam` 包装层，避免 SDK 升级直接破坏
  业务侧回调代码，并与 AWS 模块风格统一（属于 API 变更，需排期处理）。
- **请求模型可变 POJO + 全 nullable 字段**：`EmailRequest` / `AliyunSmsRequest` / `AwsSmsRequest`
  均为 var-POJO，必填项（如 `serverPort`、`region`）只能运行期 `requireNotNull` 兜底。长期可
  考虑提供带必填参数的构造函数或 builder，让编译期就能拦住缺参（API 变更，不宜本次直接做）。

### 可扩展性

- **缺统一短信发送 SPI**：`AliyunSmsHandler` 与 `AwsSmsHandler` 无公共接口，请求/回调模型也
  不同，业务侧无法多态切换厂商，新增厂商（腾讯云 / Twilio）后调用代码需逐处适配。建议在
  `kudos-ability-comm-common` 定义 `ISmsSender` + 统一的请求/回执抽象（这也能让 comm-common
  摆脱"空占位"状态）。
- **AWS 共享 HTTP 客户端为进程级静态字段**：`kudos-ability-comm-sms-aws/.../handler/AwsSmsHandler.kt`
  的 `HTTP_CLIENT` 是 companion 静态 var，多租户不同代理出口、测试隔离都受限；建议改为实例
  字段或注入式 client factory（README 已注明，此处归档为待办）。

### 功能缺陷 / 值得补充

- **重试机制缺失（三个发送模块共性）**：`EmailHandler` / `AliyunSmsHandler` / `AwsSmsHandler`
  对 transient 错误（网络抖动、SMTP 4xx 临时拒绝）一律直接 FAIL，调用方各自造轮子。建议在
  comm-common 提供统一的"重试 + 退避"装饰器或回调辅助。
- **限流缺失**：所有 handler 都可被业务无限调用，SMTP 封号 / 云厂商限流（阿里云
  `isv.BUSINESS_LIMIT_CONTROL`）只能事后发现。建议提供可选的客户端速率限制 SPI。
- **邮件附件不支持**：`EmailHandler` 未暴露 `MimeMessageHelper.addAttachment`，业务需要附件时
  只能绕开本模块。
- **短信发送回执未支持**：AWS SNS 的 delivery receipt（经 SQS 订阅）、阿里云的回执消息
  （SmsReport）均无对接点；建议至少在 README 给出对接指引或预留回执回调 SPI。
- **客户端不复用**：email 每次 new `JavaMailSenderImpl`、aliyun 每次 new `AsyncClient`、aws 每次
  new `SnsClient`（各自 README 已注明）。高频场景建议按 (host, account) / (region, AK) 缓存。

### 安全性

- **凭证明文 + Serializable**：`EmailRequest.senderPassword`、`AliyunSmsRequest.accessKeySecret`、
  `AwsSmsRequest.accessKeySecret` 均为明文 String 且类实现 `Serializable`，对象一旦被序列化到
  日志/缓存即泄密（KDoc 已警告）。建议长期改为 `CharArray` 或引入 `CredentialsProvider` SPI，
  调用时临时取值。
- **WebSocket 无内置鉴权拦截**：`kudos-ability-comm-websocket-ktor/.../routing/KudosWebSocketRouting.kt`
  只能靠业务在 `sessionFactory` / `onConnect` 里手工鉴权，容易遗漏。建议抽出
  `WebSocketInterceptor` 链（README 已注明，归档为待办）。
- **广播无 backpressure**：`WebSocketBroadcaster` 对慢 session 的发送队列无容量上限，极端情况
  会堆积至 OOM（README 已注明）。

### 可观测性

- **无发送指标**：三个发送 handler 均无 Micrometer 埋点（成功率 / 耗时 / 失败分类），生产排障
  只能翻日志。建议提供可选的 metrics 装饰器，命名如 `kudos.comm.email.send.duration`。
- **日志缺关联上下文**：`EmailHandler` 的 `log.info("Starting to send email...")`、
  `AliyunSmsHandler` 的 `[aliyun] Starting to send SMS...` 均无关联 ID / 目标计数（脱敏后），
  并发场景下日志无法对上请求。
- **Redis 通道无生命周期管理**：`RedisWebSocketBroadcastChannel` 内部的 `CoroutineScope` 永不
  cancel、监听器不注销，Spring 容器刷新/关闭时泄漏。建议实现 `DisposableBean` 或暴露 `close()`。

### 测试覆盖

- **EmailHandler 失败路径无测试**：`kudos-ability-comm-email/test-src/.../EmailTest.kt` 只有
  2 个成功 case；参数缺失 → FAIL 回调、partial-send（部分地址非法）、CRLF 注入拦截均未覆盖
  （前两者可不依赖 Docker 纯单测）。
- **阿里云 endpoint 解析无单测**：本次修复的 `buildClient` host:port / 纯域名解析 bug 正是因
  缺单测而长期存在；解析逻辑可抽为 internal 纯函数后补参数化单测。
- **AWS 代理模式无测试**：`SmsAwsProxyProperties.enable=true` 分支（`initApacheHttpClient`）
  无任何测试。
- **WebSocketBroadcaster 无独立单测**：仅经由 `DistributedWebSocketBroadcasterTest` 间接覆盖；
  单 session 失败返回计数、空列表短路等语义值得直接固化。

### 可维护性

- **魔法值 `"local-test"`**：`AliyunSmsHandler.doSend` 异常兜底响应体的
  `requestId("local-test")` 在生产异常路径同样出现，字面义误导（像测试残留）；建议改为
  `"n/a"` 之类中性值（回调方可能依赖该值，故未直接改）。
- **重复 sessionId 注册残留旧索引**：`KudosWebSocketRegistry.register` 对同 sessionId 重复注册
  时旧 userId/tenantId 桶不清理（已有测试固化此行为）；建议 register 内先 unregister 同 id。
- **comm-common 空置决策**：`CommThreadPoolProperties` 无人装配（README 已注明）；若采纳上文
  "统一短信 SPI"建议，可顺势把公共抽象落到该模块，否则应删除。
