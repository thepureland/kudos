# kudos-ms-auth-core

Auth 原子服务的**领域实现层**：在 `auth-common` 契约 + `auth-sql` 表结构上实现
`IAuth*Api`，提供 Ktorm DAO / 业务 Service / 多级缓存 / 事件订阅。**不含 HTTP 控制器**
（控制器在 `auth-api-admin` / `auth-api-internal` / `auth-api-public`）。

## 分层

```
io.kudos.ms.auth.core
├── platform/
│   ├── init/AuthAutoConfiguration       Spring 装配入口
│   ├── api/PermittedResourceApi         IPermittedResource 实现
│   ├── cache/                           多个跨域聚合缓存
│   └── enums/dict/AuthModuleEnum
├── role/
│   ├── model/{table,po}                 Ktorm 表 + PO（AuthRole / AuthRoleUser / AuthRoleResource）
│   ├── dao/                             3 个 BaseCrudDao 子类
│   ├── event/                           AuthRoleInserted/Updated/Deleted/BatchDeleted/
│   │                                    AuthRoleUserRelationsChanged / AuthRoleResourceRelationsChanged
│   ├── cache/                           AuthRoleHashCache / RoleIdsByUserIdCache /
│   │                                    UserIdsByRoleIdCache / UserIdsByTenantIdAndRoleCodeCache
│   ├── service/{iservice,impl}          3 个 Service
│   └── api/AuthRoleApi                  IAuthRoleApi 实现
└── group/                               与 role 同形结构
```

## 关键设计

### 角色 = 直接授权 ∪ 组继承

[RoleIdsByUserIdCache.getRoleIds](src/io/kudos/ms/auth/core/role/cache/RoleIdsByUserIdCache.kt) 算"有效"角色，
不是"直接绑定"角色。下游 [AuthRoleService.hasRole](src/io/kudos/ms/auth/core/role/service/impl/AuthRoleService.kt:213)
/ [getUserResourceIds](src/io/kudos/ms/auth/core/role/service/impl/AuthRoleService.kt:256) 都依赖这个语义。
若需要"直接绑定"角色，直接调 [AuthRoleUserDao.searchRoleIdsByUserId](src/io/kudos/ms/auth/core/role/dao/AuthRoleUserDao.kt)。

### 多级缓存 + 事件失效

- **HashCache**（按主键 + 二级索引）：`AuthRoleHashCache` / `AuthGroupHashCache`
- **KeyValue 聚合缓存**（按 userId/groupId/roleId 等聚合）：`RoleIdsByUserIdCache`
  / `UserIdsByRoleIdCache` / `ResourceIdsByUserIdCache` 等

变更走 [Spring `@TransactionalEventListener`](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)
`AFTER_COMMIT` 阶段触发。事件类型：

| 事件 | 含义 |
|---|---|
| `AuthRoleInserted/Updated/Deleted` | 单条角色变更 |
| `AuthRoleBatchDeleted` | 批量删除，附 `(id, tenantId, code)` snapshot——AFTER_COMMIT 行已删除，无法回查 |
| `AuthRoleUserRelationsChanged` | 用户↔角色绑定/解绑（含 userIds） |
| `AuthRoleResourceRelationsChanged` | 角色↔资源绑定/解绑 |
| `AuthGroupRoleRelationsChanged` | 组↔角色变更 → 组下所有用户的有效角色失效 |
| `AuthGroupUserRelationsChanged` | 用户↔组变更（直接进出组） |

**为什么 BatchDeleted 要带 snapshot**：`AuthRoleHashCache.syncOnBatchDelete` 不需要，
但 `UserIdsByTenantIdAndRoleCodeCache.on(BatchDeleted)` 需要 `(tenantId, code)` 才能算 cache key——
事件发布在 AFTER_COMMIT 后，DB 行已不存在，必须提前 snapshot。

### 跨服务依赖

通过 Feign client 调用 sys-client / user-client：
- `UserAccountHashCache.getUsersByIds` —— 拼装角色下用户的展示信息
- `SysResourceHashCache.getResourcesByIds` —— 拼装角色拥有的资源

`AuthRoleService.getUsersByRoleCode` / `getResources` 路径展开后会跨服务取 2 个缓存。
跨服务超时 / fallback 由 client 模块 Resilience4j 处理。

## 装配

`AuthAutoConfiguration`：
- `@ComponentScan("io.kudos.ms.auth.core")`
- `@AutoConfigureAfter(KtormAutoConfiguration::class)` —— 等 Ktorm Database / DataSource 就绪
- `IComponentInitializer.getComponentName() = "kudos-ms-auth-core"`——由 kudos 自定义 SPI
  (`ComponentInitializerSelector`) 按类路径扫描发现

## 测试覆盖

| 路径 | 用例数 | 类型 |
|---|---|---|
| `test-src/.../cache/*` | 9 个 cache 测试 | DAO + 缓存联合集成测试 |
| `test-src/.../dao/*` | 6 个 dao 测试 | Ktorm DAO 集成测试 |
| `test-src/.../service/*` | 6 个 service 测试 | Service + Event 联合 |

均用 h2 + `application.yml` + `test-resources/sql/h2/*.sql` 初始化。事件发布走 Spring `ApplicationEventPublisher`，
测试可直接断言缓存命中 / 失效行为。

## 已知限制 / 后续工作

- ❗ **`@TransactionalEventListener(fallbackExecution = true)`**：意味着即使没有外层
  事务，监听器也会触发——但 AFTER_COMMIT 语义就被破坏了。当前 Service 已在大部分入口标
  `@Transactional`，但 `getRoleIds` 等 readOnly 路径触发的 evict 在无事务下会同步执行
- ❗ **没有 controller 层 `@PreAuthorize`**：admin / internal 控制器全部依赖网关 / 外部
  鉴权过滤器做访问控制。`auth-api-admin/.../AuthRoleAdminController.bindUsers` 等敏感
  endpoint 若网关挂了或路由错配，直接暴露
- ❗ **没有审计日志**：`bindUsers` / `unbindUser` / `bindResources` 等关键鉴权变更没接
  `AuditLogTool`。生产合规场景需自行接入
- ❗ **`PermittedResourceApi` 路径深度不限**：`auth_group` 是嵌套树，当前路径展开未做
  深度上限——异常配置（自引用 / 极深嵌套）会让 cache reload 跑很久
- ❗ **跨服务调用未做并发限流**：`UserAccountHashCache.getUsersByIds(userIds)` 在 userIds
  极大时（如某个组有 10w 用户）会让 user-service 瞬时压力激增
- ❗ `AuthRoleUserService.batchBind` / `unbindUser` 返回的是关系数 / Boolean——没有把
  失败原因（资源不存在 / 角色已禁用 / 跨租户）区分给调用方

## 依赖

- `kudos-ms-auth-common` / `kudos-ms-auth-sql`
- `kudos-ability-data-rdb-ktorm` / `kudos-ability-cache-common`
- `kudos-ms-sys-client` / `kudos-ms-user-client`（跨服务查询）
- `kudos-ability-data-audit`（事件机制基座）

## Kotlin 风格

- **一律 `open class` + Spring CGLIB 代理**——`@Transactional` / `@Cacheable` / `@TransactionalEventListener`
  切面都要求方法 `open`。`AuthRoleService` / `AuthGroupService` / `*RelationsService` 全部 `open class`。
- **DAO 通过 ctor 注入；Service 大部分通过 `@Resource` 注入**避免循环依赖——`AuthRoleService` 单类
  就 7+ 个 `@Resource` 字段（cache / 同域 service / 跨服务 client / 事件发布器互引时很常见）。
- **Cache 类用 `getSelf<XxxCache>()` 拿 Spring 代理对象**——让 `@Cacheable` 在 `doReload` 等同类内部
  调用也能生效（绕开 self-invocation 不走代理的 Spring 限制）。本模块所有"key/value 聚合缓存"
  （`RoleIdsByUserIdCache` / `UserIdsByRoleIdCache` / `UserIdsByTenantIdAndRoleCodeCache` /
  `UserIdsByGroupIdCache` / `GroupIdsByUserIdCache` 等）都遵循该模式。
