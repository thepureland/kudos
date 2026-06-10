# kudos-ms-auth

**定位**：**鉴权（`auth`）原子服务**的 Gradle 聚合模块——承载角色 / 用户组 /
角色-资源关联 / 角色-用户关联 / 组-用户关联等领域能力，配合 `kudos-ms-sys` 的资源主数据
和 `kudos-ms-user` 的用户主数据，构成"用户 → 组 → 角色 → 资源"权限闭环。

**在工程中的角色**：`SysConsts.ATOMIC_SERVICE_NAME` 取值为 `"auth"`；管理端通过 HTTP，
其他微服务通过 Feign / `client` 调用。

---

## 子模块文档索引

| 子模块 | 说明 |
|--------|------|
| [kudos-ms-auth-common](kudos-ms-auth-common/README.md) | 跨模块共享契约（`IAuthRoleApi` / `IPermittedResource` / VO / 错误码） |
| [kudos-ms-auth-sql](kudos-ms-auth-sql/README.md) | Flyway 迁移脚本（`V1.0.0.20+` 为 `auth_*` 表 DDL；前段为 `sys_*` 种子） |
| [kudos-ms-auth-core](kudos-ms-auth-core/README.md) | DAO / Service / 多级缓存 / 事件订阅 / `IAuth*Api` 实现 |
| [kudos-ms-auth-api-admin](kudos-ms-auth-api-admin/README.md) | 管理端 REST：`/api/admin/auth/role/**`、`/api/admin/auth/group/**` |
| [kudos-ms-auth-api-public](kudos-ms-auth-api-public/README.md) | 对外 Web 启动入口 + `PermittedResourceController`（当前用户视图） |
| [kudos-ms-auth-api-internal](kudos-ms-auth-api-internal/README.md) | 对内 Provider 启动入口 + `AuthRoleInternalController`（Nacos / interservice 缓存） |
| [kudos-ms-auth-client](kudos-ms-auth-client/README.md) | `IAuthRoleProxy` Feign 代理 + `AuthRoleFallback` 降级 |

---

## 依赖关系（概念）

```
                    ┌──────────────────┐
                    │ kudos-ms-auth-   │
                    │ common           │
                    └────────┬─────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   │
┌─────────────────┐  ┌─────────────────┐         │
│ kudos-ms-auth-  │  │ kudos-ms-auth-  │         │
│ sql             │  │ client          │         │
└────────┬────────┘  └────────┬────────┘         │
         │                    │ only common      │
         └──────────┬─────────┘                  │
                    ▼                            │
            ┌───────────────┐                    │
            │ kudos-ms-auth-│                    │
            │ core          │◄───────────────────┘
            └───────┬───────┘
                    │
    ┌───────────────┼───────────────┐
    ▼               ▼               ▼
┌────────┐   ┌────────────┐   ┌────────────┐
│ api-   │   │ api-       │   │ api-       │
│ admin  │   │ public     │   │ internal   │
└────────┘   └────────────┘   └────────────┘
```

---

## 关键概念

- **角色（`auth_role`）** + **角色-资源（`auth_role_resource`）** + **角色-用户（`auth_role_user`）**：
  RBAC 三件套
- **用户组（`auth_group`）** + **组-用户（`auth_group_user`）**：用户分组管理。可作为
  角色赋予的目标，让"组里所有人"自动持有该角色权限
- **生效权限** = `直接给用户的角色` ∪ `用户所在组的角色` ∪ `用户所在组的所有上级路径` 的角色
  （详见 `auth_group` 的层级 path 字段）
- 与 `kudos-ms-sys` 的关系：`sys.sys_resource` 是资源主数据，本模块只持有"哪个角色绑了哪些资源 id"
- 与 `kudos-ms-user` 的关系：`user.sys_user` 是用户主数据，本模块只持有"哪个用户属于哪些组 / 哪些角色"

---

## 命名与约定（跨模块）

- **原子服务名**：`SysConsts.ATOMIC_SERVICE_NAME = "auth"`——所有 Feign 服务名、缓存 namespace、
  Flyway 表前缀、日志 `service` 字段都以此为锚点。
- **领域 API**：`common` 中 `IAuth*Api` 由 `core` 中 `Auth*Api` 实现；`client` 中 `IAuth*Proxy`
  继承同一接口并通过 Feign 调用远程服务。**目前仅 `role` 对外**——`group` 域只通过 admin HTTP
  暴露，无 Feign 接口。
- **方法级 Feign 路由**：所有 `IAuth*Api` 的方法上挂 `@GetMapping("/api/internal/auth/...")`
  / `@PostMapping`，接口类型上**不**放 `@RequestMapping`；`auth-api-internal` 的 Controller 直接
  `implements IAuth*Api`，路径自动继承——签名漂移可在编译期暴露。
- **管理端 vs 内部**：`/api/admin/auth/**` 仅由 `api-admin` 承载，`/api/internal/auth/**` 仅由
  `api-internal` 与 `api-public` 内的 `PermittedResourceController` 承载；两套前缀在网关层应
  分别路由。
- **跨服务种子数据**：`auth-sql` 的 `V1.0.0.0–V1.0.0.6` 是写入 `sys_*` 表的菜单 / 字典 / 缓存
  登记 / 参数 / i18n 文案——遵循"被写入方负责 DDL，写入方负责 INSERT"。

具体类名与边界以各子模块源码为准。

## 已知限制 / 后续工作

- ❗ **生效权限算法分散** — "用户 → 直接角色 ∪ 组继承角色"的合并逻辑同时存在于
  `RoleIdsByUserIdCache.computeEffectiveRoleIds` / `ResourceIdsByUserIdCache.computeEffectiveRoleIds` /
  `ResourceIdsByTenantIdAndUsernameCache.computeEffectiveRoleIds` 三处，代码完全一样——
  后续应下沉到 base 层 util，避免三处漂移
- ❗ **只有 `role` 对外开 Feign** — `group` 域只暴露 admin HTTP，跨服务想查"用户所在组" /
  "组的权限"必须走 admin 路径或自建 group Proxy
- ❗ **组层级 path 字段未文档化** — `auth_group.path` 是字符串祖先链（如 `/g1/g2/g3`），
  上级路径继承权限要靠 path LIKE 查询；README 提到"详见 path 字段"但没有具体格式说明
- ❗ **资源 / 用户主数据跨服务一致性** — auth 只持 id 不持快照；`sys_resource` / `user_account`
  被改名 / 删除时，auth 这边的 `auth_role_resource` / `auth_role_user` 会留死引用
- ❗ **`/api/admin/auth/**` 零 `@PreAuthorize`** — 修改角色权限的接口仅靠网关守护
- ❗ **fallback 只覆盖 `IAuthRoleApi`** — auth 客户端只有 1 个 Proxy 1 个 Fallback；group / 资源
  绑定关系等其他接口若新增 Proxy，需补对应 Fallback

## 改进建议（自动分析 2026-06-11）

> 本节为自动深度审查产出。已直接修复的三处（`AuthRoleApi.isUserHasResource` 参数错位、
> `AuthRoleUserService.batchBind` 绑定不存在角色、审批流自批）不在此列，下面是**未直接修改**的发现，
> 按维度分类，供后续排期。

### 安全性

1. **删除角色不回收其授权（权限残留，高优先级）** — ✅ 已修复（2026-06-11）：
   `AuthRoleService.deleteById`/`batchDelete` 现在在事务内级联删除 `auth_role_user` /
   `auth_role_resource` / `auth_group_role` / `auth_role_org` / `auth_role_exclusion` 关系行
   （删前先快照受影响的 userIds / resourceIds / groupIds），并发布
   `AuthRoleUserRelationsChanged` / `AuthRoleResourceRelationsChanged` /
   `AuthGroupRoleRelationsChanged`，使 `RoleIdsByUserIdCache` / `ResourceIdsByUserIdCache` /
   `UserIdsByRoleIdCache` / `ResourceIdsByRoleIdCache` 等正确失效；新增集成测试
   `AuthRoleDeleteCascadeTest`。剩余工作：指向已删角色的 `auth_role_grant_request`（含 PENDING）
   行未级联取消——approve 时 `batchBind` 会因角色不存在而拒绝，无安全风险，但会留下悬挂申请记录。
2. **时态授权绕过 SoD 与角色校验** — ✅ 已修复（2026-06-11）：`bindTemporal` 现在对齐
   `batchBind` 防线——先校验角色存在，再复用 `AuthRoleUserService.findSodViolationMessage`
   （batchBind 同一逻辑抽取的 internal 共享方法）做 SoD 互斥检查；且已存在**永久授权**
   （start/end 均 NULL）时直接拒绝、不再静默替换（须先显式 unbind），仅同对临时授权保留
   replace 语义。新增 4 个单测（不存在角色 / 永久授权冲突 / SoD 违规被拒、干净用户成功）。
3. **SoD 检查未覆盖候选角色的祖先链** —
   `AuthRoleUserService.batchBind` 调 `findViolation(tenantId, roleId, effectiveRoles)` 时只用候选
   roleId 本身比对互斥规则；但持有子角色等效继承祖先权限（见 `AuthRoleExclusionService.
   computeEffectiveUsersForRole` 的注释），若互斥规则定义在候选角色的**祖先**上即可绕过。应把
   `候选角色 + 其祖先` 全部送检。
4. **cancel 未校验申请人身份（水平越权）** —
   `kudos-ms-auth-core/src/io/kudos/ms/auth/core/role/grant/service/impl/AuthRoleGrantRequestService.kt`
   的 `cancel(id)` 不校验当前用户是否为 `requesterId`（DDL 注释声称 "requester flips their own"）。
   需产品决策是否允许管理员代撤销，再补归属校验。
5. **`built_in` 角色无保护** — `auth_role.built_in` 字段存在（`copyRole` 也刻意置 false），但
   `AuthRoleService` 的 update/delete/updateActive 均不阻止修改或删除内置角色。
6. **批量接口无大小上限** — `AuthRoleAdminController.bindUsers/batchBindUsers/getDeleteImpact`、
   `AuthGroupAdminController` 同名端点、`AuthResourcePermissionAdminController.roleNamesByResourceIds`
   均接收无界集合；`getDeleteImpact`/`roleNamesByResourceIds` 还会对每个 id 触发一次缓存/DAO 查询，
   可被用作放大攻击。建议统一加 `require(ids.size <= N)`。
7. **时态过滤方向不对称** — `AuthRoleUserDao.searchRoleIdsByUserId`（用户→角色）过滤生效窗口，
   而 `searchUserIdsByRoleId`（角色→用户）不过滤，导致 `UserIdsByRoleIdCache` /
   `AuthRoleService.getRoleUsers` / `getUsersByRoleCode` / SoD 违规扫描把过期、未生效的授权当作有效。

### 功能缺陷 / 可补充功能

8. **未来生效授权不会准点生效** — `start_time` 在未来的授权写入后，没有任何机制在 start_time
   到点时让 `RoleIdsByUserIdCache` 等失效（`ExpiredGrantPurgeScheduler` 只删过期行），实际生效时间
   取决于缓存何时被偶然失效。建议 purge 调度同时扫描"刚跨过 start_time"的授权并发失效事件。
9. **审批流无审批人路由** — `AuthRoleGrantRequestAdminController` KDoc 自述"没有 per-request
   approver 概念"，任何能访问 admin 端点的人都可审批任意租户的请求（垂直+租户越权面）。
10. **`searchRoleIdsByUserId` 全行加载后内存过滤** — `AuthRoleUserDao` 把用户全部授权行查出再用
    `isActiveAt` 过滤，行数大时应下推为 SQL 条件（与 `searchExpiredGrants` 一样用 Criteria 表达）。

### 测试覆盖

11. **`AuthRoleExclusionService` 无专属测试** — `kudos-ms-auth-core/test-src` 无 `role/exclusion`
    目录；`canonicalise` 交换序、同租户校验、重复 pair、`findViolation` 边界（候选在 A 侧/B 侧）等
    均未覆盖，而它是 SoD 的核心。
12. **`AuthRoleApi` 无测试** — 本次修复的 `isUserHasResource` 参数错位正是因缺少针对
    `kudos-ms-auth-core/src/io/kudos/ms/auth/core/role/api/AuthRoleApi.kt` 的委托正确性测试而长期潜伏。
13. **权限边界用例缺失** — "角色删除后用户不再持有其资源"已由 `AuthRoleDeleteCascadeTest`
    覆盖（2026-06-11），"临时授权的 SoD / 永久授权冲突被拒"已由 `AuthRoleUserTemporalServiceTest`
    新增用例覆盖；仍缺"临时授权过期后立即失权"、"跨租户绑定被拒"这类端到端断言；
    api-admin / api-internal / api-public / client 四个子模块零测试。

### 可扩展性

14. **SoD / 审批 / 数据范围策略硬编码** — 互斥判定、审批状态机、`DataScopeEnum` 解析都内嵌在各
    service；若需"三选一互斥"、多级审批等只能改核心代码，可抽 `PolicyChecker` 类 SPI。
15. **`ExpiredGrantPurgeScheduler` 无分布式锁** — KDoc 自述需 ShedLock；多实例部署时虽幂等但会
    重复扫表，建议直接集成。

### 可观测性

16. **授权变更无审计** — 角色/资源/组绑定与解绑只有 `log.debug` 与缓存事件，无审计落库
    （操作者、时间、变更前后值）；SoD 拒绝（`AuthRoleUserService.batchBind` 抛异常路径）与审批
    决策也只有 debug 级日志。安全敏感动作（bind/unbind/approve/reject/updateScope）建议
    `log.info` + 审计事件。

### 可维护性

17. **`AuthRoleService` 依赖 14 个注入、跨 role/group/sys/user 四域** — 代码注释已自知应拆
    "permissions facade"；`getEffectivePermissions` 等聚合方法宜下沉到独立 service。
18. **`@Resource` 与 `@Autowired` 混用** — 同一文件内两种注入注解并存（如 `AuthRoleUserService`），
    建议统一。
19. **`AuthRoleService.kt` 文件底部的私有 `typealias AuthGroupCacheEntryAlias`** — 为"让 import 区
    更清晰"引入别名反而增加间接性，直接 import `AuthGroupCacheEntry` 即可。

### API contract

20. **submit/cancel 放在 admin 层语义不符** — 申请人（普通用户）发起/撤销授权申请的动作走
    `/api/admin/auth/roleGrantRequest/**`，意味着申请人必须能访问 admin 网关段；宜在 api-public
    暴露受限的 submit/cancel（仅本人），admin 留 approve/reject。
21. **`findViolations` 对不存在的 exclusionId 返回空 VO** —
    `AuthRoleExclusionService.findViolatingUserIds` 查不到记录时返回 roleAId/roleBId 为空串的
    "成功"响应，调用方无法区分"无违规"与"id 不存在"，宜抛 404/IllegalArgumentException。

### 文档

22. **KDoc / README / SQL 注释总体充分**（各子模块 README、迁移脚本头注释质量高）；遗留点主要是
    组级 README 已列的 `auth_group.path` 格式未文档化，以及本节新增各项。
