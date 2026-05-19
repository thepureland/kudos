# kudos-ms-user-api-admin

User 服务的**管理端 REST 控制器层**。路径前缀 `/api/admin/user/...`。

## 控制器清单

| 控制器 | 路由前缀 | 委托 Service |
|---|---|---|
| `UserAccountAdminController` | `/api/admin/user/account` | `IUserAccountService`——账号 CRUD、改密、冻结/解冻、激活态、内置位 |
| `UserAccountThirdAdminController` | `/api/admin/user/accountThird` | `IUserAccountThirdService`——第三方账号绑定/解绑（OAuth / SSO） |
| `UserAccountProtectionAdminController` | `/api/admin/user/accountProtection` | `IUserAccountProtectionService`——错误次数 / 冻结策略查询 |
| `UserContactWayAdminController` | `/api/admin/user/contactWay` | `IUserContactWayService`——联系方式（手机 / 邮箱）多条管理 |
| `UserOrgAdminController` | `/api/admin/user/org` | `IUserOrgService`——组织树 + 组织 ↔ 用户绑定 |
| `UserLoginRememberMeAdminController` | `/api/admin/user/rememberMe` | `IUserLoginRememberMeService`——记住登录 token 失效 / 列表 |

> 通行证（`IPassportApi` 登录 / 登出 / 校验 / 改密）**没有 Admin 控制器**——属于用户态行为，
> 通过 `kudos-ms-user-client` 的 `IPassportProxy` 暴露给 auth 服务等内部调用方。

## 设计约束

- 控制器只调用 `user-core` 的 `IUser*Service`，**不直接接触 DAO** / 缓存层
- 返回类型直接用 `user-common` 的 VO（`*Detail` / `*Row` / `*Edit`），不在 controller 层
  做额外组装
- 入参用 `user-common` 的 `*FormCreate` / `*FormUpdate` / `*Query`，由 `kudos-ability-web-springmvc`
  的统一参数解析 / 校验器处理

## 依赖

- `kudos-ms-user-core`
- `kudos-ability-web-springmvc`（Spring MVC + 统一异常 / 参数校验 / 响应包装）

## 部署形态

本模块**自身就是一个 Spring Boot 应用**——`UserApiAdminApplication` 是 `@EnableKudos`
main 类，可独立 `bootRun`，专门跑管理端 HTTP（与 `api-public` / `api-internal` 是三套
对等的启动入口，各起一个进程）。

```bash
./gradlew :kudos-ms:kudos-ms-user:kudos-ms-user-api-admin:bootRun
```

`UserApiAdminAutoConfiguration` 负责把本模块的 controller 扫描注入 Spring 上下文，
配合 `user-core` 的 `UserAutoConfiguration` 完成装配。

> 当前 build 中**没有其他模块依赖 `kudos-ms-user-api-admin`**——它既不向 `api-public`
> 也不向 `api-internal` 提供 controller bean。如需把管理 API 与用户 / 内部 API
> 合并到同一进程，应在对应模块的 `build.gradle.kts` 显式 `api(project(...))` 拉入。
