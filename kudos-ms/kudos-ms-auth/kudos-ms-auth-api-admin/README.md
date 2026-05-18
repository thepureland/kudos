# kudos-ms-auth-api-admin

Auth 服务的**管理端 REST 控制器层**。映射到 `/api/admin/auth/...` 等路径，对接前端管理后台。

## 内容

- `*Controller` 类——一个领域（role / group / group-user / role-user / role-resource）一个 controller
- 控制器调用 `auth-core` 的 `IAuth*Service`，不直接接触 DAO
- 返回类型直接用 `auth-common` 的 VO

## 依赖

- `kudos-ms-auth-core`（含 Service 接口与实现）
- `kudos-ability-web-springmvc`（基础 controller 能力）

## 部署形态

`api-admin` 自身**不是**应用启动模块——它只贡献 controller。实际启动入口在
`auth-api-public`（web 形态）/ `auth-api-internal`（provider 形态）。
