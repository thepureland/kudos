# kudos-ms-user-api-internal

User 服务**面向其他微服务**的 Spring Boot 进程。作为 Feign 调用的 provider，配合 Nacos
完成服务注册 / 配置下发。

## 启动入口

- `UserApiProviderApplication`（`@EnableKudos`）—— main 类
- `UserApiProviderAutoConfiguration` —— 装配入口

```bash
./gradlew :kudos-ms:kudos-ms-user:kudos-ms-user-api-internal:bootRun
```

## Provider 控制器清单

每个 `*InternalController` **直接实现 `IUser*Api`**——路径来自 `user-common` 接口上的方法级注解，
与 `kudos-ms-user-client` 里的 `*Proxy` 严格对称：

| 控制器 | 实现接口 | 配对 client proxy |
|---|---|---|
| `PassportInternalController` | `IPassportApi` | `IPassportProxy` |
| `UserAccountInternalController` | `IUserAccountApi` | `IUserAccountProxy` |
| `UserOrgInternalController` | `IUserOrgApi` | `IUserOrgProxy` |

> **当前 internal 暴露面是 3 个域**——`passport` / `account` / `org`。其余像
> `account-protection` / `account-third` / `contact-way` / `login-remember-me`
> 虽然 client 侧有 proxy，但在 internal 启动模块**没有 provider controller**——
> 调用会落到 fallback。如有需要按需补 `*InternalController`。

## 与 api-public / api-admin 的区别

| 维度 | api-internal |
|---|---|
| 面向 | 服务间 Feign 调用 |
| Nacos | 依赖 `kudos-ability-distributed-discovery-nacos` + `kudos-ability-distributed-config-nacos`——作为 provider 注册到注册中心 |
| 跨服务缓存 | 依赖 `kudos-ability-cache-interservice-provider`——暴露缓存失效广播端点供其他服务订阅 |
| Session | 无——调用方通过 Feign header 带身份 |

## 依赖

- `kudos-ms-user-core`
- `kudos-ability-web-springmvc`
- `kudos-ability-cache-interservice-provider`（跨服务缓存失效）
- `kudos-ability-distributed-discovery-nacos`（注册）
- `kudos-ability-distributed-config-nacos`（配置）

## 已知限制 / 部署注意

- ❗ **api-internal 与 api-public 的边界由 yml 决定，代码层未硬分隔**——两个进程都会
  装配 `user-core`，意味着 `api-internal` 启动时也会加载 `PassportPublicController`?
  不会，因为 Public Controller 在 `api-public` 模块、internal 不依赖 public——但要警惕
  反向：若把 `api-internal` 错配上 `api-public` 依赖，会在内部端口意外暴露用户态端点
- ❗ Provider controller 与 client proxy **不对等的部分会静默走 fallback**——给 client
  侧增加 fallback 单测时要记得：服务端没实现 != 客户端调用失败的同一种"远端不可达"
- ❗ Nacos 必须可达——本模块强依赖 discovery / config；本地开发无 Nacos 时启动会失败，
  需用 `application-local.yml` 关闭注册或 mock 注册中心

## 改进建议（自动分析 2026-06-11）

- ✅ 已修复（2026-06-11）**内部 RPC 返回密码哈希**（`controller/account/UserAccountInternalController.kt` +
  `controller/org/UserOrgInternalController.kt`）：`getUserById` / `getUsersByIds` / `getOrgUsers` / `getOrgAdmins`
  返回前统一调用 `user-common` 新增的 `eraseCredentials()` 脱敏拷贝（置空 `loginPassword` / `securityPassword` /
  `authenticationKey` / `sessionKey`），internal 网段消费方只能拿到用户名 / 组织 / 状态等元数据；
  密码校验保留在 user-core 进程内（passport 链路直查缓存 / DAO），不受影响。VO 字段集未变，Feign 反序列化兼容。
- **`getUserIds(tenantId)` / `getOrgUsers(orgId)` 无分页**：全量 id / 实体返回，租户规模大时响应体失控；
  Feign 默认超时下还可能放大为级联降级。
- **测试缺失**：internal controller 是纯委托层但零测试——一个 `@WebMvcTest` 即可锁住
  "接口注解路径与 client proxy 严格对称"这一关键契约。
