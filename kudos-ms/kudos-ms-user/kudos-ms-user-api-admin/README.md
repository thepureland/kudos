# kudos-ms-user-api-admin

User 服务的**管理端 REST 控制器层**。路径 `/api/admin/user/...`。

## 内容

- `*Controller` 类——一个领域（account / login / org / 第三方账号）一个 controller
- 控制器调用 `user-core` 的 `IUser*Service`，不直接接触 DAO
- 返回类型直接用 `user-common` 的 VO

## 依赖

- `kudos-ms-user-core`
- `kudos-ability-web-springmvc`

## 部署形态

不是应用启动模块——只贡献 controller。启动入口在 `user-api-public` / `user-api-internal`。
