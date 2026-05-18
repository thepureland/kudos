# kudos-ms-msg-api-admin

Msg 服务的**管理端 REST 控制器层**。路径 `/api/admin/msg/...`。

## 内容

- `*Controller` 类——一个领域（template / instance / send / receive / receiver-group）一个 controller
- 控制器调用 `msg-core` 的 `IMsg*Service`，不直接接触 DAO
- 返回类型直接用 `msg-common` 的 VO

## 依赖

- `kudos-ms-msg-core`
- `kudos-ability-web-springmvc`

## 部署形态

不是应用启动模块——启动入口在 `msg-api-public` / `msg-api-internal`。
