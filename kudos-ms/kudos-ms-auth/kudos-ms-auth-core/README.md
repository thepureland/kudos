# kudos-ms-auth-core

Auth 原子服务的**领域实现层**：在 `auth-common` 契约 + `auth-sql` 表结构上实现
`IAuth*Api`，提供 Ktorm DAO / 业务 Service / 多级缓存。**不含 HTTP 控制器**（控制器在
`auth-api-admin`）。

## 分层

```
io.kudos.ms.auth.core
├── init/AuthAutoConfiguration       Spring 装配入口
├── model
│   ├── table                        Ktorm 表对象
│   └── po                           PO / 行对象
├── dao                              DAO（Ktorm `BaseCrudDao` 子类）
├── service
│   ├── iservice                     `IAuth*Service` 契约
│   └── impl                         业务实现
├── cache                            领域缓存处理器（Caffeine + Redis）
└── api                              `Auth*Api` —— `IAuth*Api` 的 Spring 实现
```

## 关键缓存

- 角色 / 角色-资源 / 角色-用户 / 用户组 / 组-用户都走"按主键 / 按外键聚合 / 按租户聚合"的
  HashCache 模式（见 kudos-ability-cache-common）
- 权限相关缓存特别敏感——`auth_group_user` 变更要级联失效所有"包含该用户的组的所有上级 path"
  的相关缓存。设计要点见前期 fix 文档 (`fix(auth): cover auth_group path in ...`)

## 装配

`AuthAutoConfiguration`：
- `@ComponentScan("io.kudos.ms.auth.core")`
- `@AutoConfigureAfter(KtormAutoConfiguration::class)` —— 等 Ktorm Database / DataSource 就绪
- `IComponentInitializer.getComponentName() = "kudos-ms-auth-core"`

## 依赖

- `kudos-ms-auth-common`、`kudos-ms-auth-sql`
- `kudos-ability-data-rdb-ktorm`、`kudos-ability-cache-common`
- `kudos-ms-sys-client` / `kudos-ms-user-client`（为生效权限组装跨服务查询）
