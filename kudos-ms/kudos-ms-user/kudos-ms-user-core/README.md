# kudos-ms-user-core

User 原子服务的**领域实现层**：DAO / 业务 Service / 多级缓存 / `IUser*Api` 实现。
**不含 HTTP 控制器**（控制器在 `user-api-admin`）。

## 分层

```
io.kudos.ms.user.core
├── init/UserAutoConfiguration       Spring 装配入口
├── model
│   ├── table                        Ktorm 表对象（SysUsers / SysOrgs 等）
│   └── po                           PO / 行对象
├── dao                              DAO 类
├── service
│   ├── iservice                     IUser*Service 接口
│   └── impl                         业务实现
├── cache                            领域缓存（含基于组织树 path 的级联失效）
└── api                              IUser*Api 的 Spring 实现
```

## 关键缓存敏感点

- `getOrgUserIds(orgId)` 返回该组织 + 所有后代组织的用户列表 —— 组织 path 变更要级联清缓存
- 历史 fix（`fix(user): precise cache invalidation on org-tree mutations`）：移动 / 重命名
  组织时要按"老 path + 新 path 的所有上下级"做精确失效，否则 stale data 持续到 TTL 自然过期

## 装配

`UserAutoConfiguration`：
- `@ComponentScan("io.kudos.ms.user.core")`
- `@AutoConfigureAfter(KtormAutoConfiguration::class)`
- `IComponentInitializer.getComponentName() = "kudos-ms-user-core"`

## 依赖

- `kudos-ms-user-common`、`kudos-ms-user-sql`
- `kudos-ability-data-rdb-ktorm`、`kudos-ability-cache-common`
- `kudos-ms-sys-client`（拿子系统 / 租户元数据）
