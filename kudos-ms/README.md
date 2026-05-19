# kudos-ms

平台级**原子服务**集合。每个原子服务对应一个 `SysConsts.ATOMIC_SERVICE_NAME`，是平台
"系统能力"的最小部署单元。

## 服务列表

| 服务 | 子目录 | 定位 |
|---|---|---|
| **sys** | [`kudos-ms-sys`](kudos-ms-sys/README.md) | 平台基础：租户、子系统、微服务注册、资源/菜单、字典、参数、国际化、数据源、域、缓存配置、访问规则 |
| **user** | [`kudos-ms-user`](kudos-ms-user/README.md) | 用户、第三方账号、账号保护、登录日志、组织树、记住登录 |
| **auth** | [`kudos-ms-auth`](kudos-ms-auth/README.md) | RBAC：角色 / 用户组 / 角色-资源 / 角色-用户 / 组-用户 |
| **msg** | [`kudos-ms-msg`](kudos-ms-msg/README.md) | 消息模板 / 实例 / 发送 / 接收 / 未送达跟踪 |

## 统一子模块布局

每个 `kudos-ms-<service>` 内部都按相同的 7 子模块结构组织：

| 子模块 | 角色 |
|---|---|
| `*-common` | 共享契约：`I<Svc>*Api` 接口、VO、enums、consts |
| `*-sql` | Flyway 迁移脚本（纯 `.sql`） |
| `*-core` | 领域实现：DAO（Ktorm） + Service + 多级缓存 + `<Svc>*Api` 实现 |
| `*-api-admin` | 管理端 REST 控制器（不含 `@SpringBootApplication`） |
| `*-api-public` | 对外 Web 进程启动入口（管理端 HTTP / 用户面） |
| `*-api-internal` | 对内 Provider 进程启动入口（服务间 Feign） |
| `*-client` | Feign 代理 + 降级，仅依赖 `*-common` |

依赖图（每个服务都一样）：

```
       common
         │
   ┌─────┼─────┐
   sql   client│
   └──┬──┘     │
      core ◄───┘
       │
  ┌────┼────┐
admin public internal
```

## 跨服务依赖

- **auth** 调 user-client 拿用户元数据；引用 sys.sys_resource 的 id
- **user** 调 sys-client 拿子系统 / 租户元数据
- **msg** 调 user-client 把 receiverId 翻译成邮箱 / 手机号；调 `comm-*` 走具体投递通道

## 部署形态

每个 `*-api-public` 和 `*-api-internal` 是独立 JVM。典型生产部署：
- `sys-api-public:8080` / `sys-api-internal:18080`
- `user-api-public:8081` / `user-api-internal:18081`
- `auth-api-public:8082` / `auth-api-internal:18082`
- `msg-api-public:8083` / `msg-api-internal:18083`

public 出口走 Ingress / ALB；internal 出口仅集群内网 + Nacos 注册。
