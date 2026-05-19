# kudos-ability-comm

通信能力主题——邮件 / 短信 / WebSocket。

| 子目录 | 实现 |
|---|---|
| [`kudos-ability-comm-common`](kudos-ability-comm-common/README.md) | 共享 base（占位） |
| [`kudos-ability-comm-email`](kudos-ability-comm-email/README.md) | SMTP 邮件发送（基于 spring-boot-starter-mail） |
| [`kudos-ability-comm-sms`](kudos-ability-comm-sms/README.md) | 短信发送（阿里云 / AWS） |
| [`kudos-ability-comm-websocket`](kudos-ability-comm-websocket/README.md) | WebSocket 业务封装（占位） |

所有通信类模块都走"虚拟线程 + 回调"模式：业务调 `handler.send(req) { result -> ... }`，
立即返回，结果通过 callback 异步通知。
