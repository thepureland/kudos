# kudos-ms-msg-sql

Msg 原子服务的 Flyway 迁移脚本——只放 `*.sql`，无 Kotlin 代码。

## 表结构（要点）

| 表 | 说明 |
|---|---|
| `msg_template` | 消息模板 |
| `msg_instance` | 模板渲染后的具体消息 |
| `msg_send` | 发送调度记录 |
| `msg_receive` | 一对一投递记录 |
| `msg_receiver_group` | 接收组 |
| `msg_unreceived` | 失败 / 未送达跟踪 |

## 约定

- 表前缀 `msg_`
- Flyway baseline + V_*_* / R_*_* 命名
- 内置模板（如系统欢迎消息）走 `R_*` 可重复脚本

## 依赖

无 Kotlin 依赖，纯资源模块。
