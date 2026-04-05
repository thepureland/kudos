# kudos-ms-msg-common

`msg` 原子服务共享契约层。包结构：**`io.kudos.ms.msg.common` 下先按业务模块再分 `api` / `consts` / `enums` / `vo`**；横切内容放在 **`platform`**（若存在）。

## 模块与 API

| 模块包 | API（`*.api`） |
|--------|----------------|
| `send` | `IMsgSendApi` |
| `instance` | `IMsgInstanceApi` |
| `receive` | `IMsgReceiveApi` |
| `receivergroup` | `IMsgReceiverGroupApi` |
| `template` | `IMsgTemplateApi` |

路径示例：`io.kudos.ms.msg.common.send.api`、`io.kudos.ms.msg.common.template.vo`。
