# kudos-ms-auth-core

Auth 原子服务的**领域实现层**：在 `auth-common` 契约 + `auth-sql` 表结构上实现
`IAuth*Api`，提供 Ktorm DAO / 业务 Service / 多级缓存 / 事件订阅。**不含 HTTP 控制器**
（控制器在 `auth-api-admin` / `auth-api-internal` / `auth-api-public`）。

本文档分两部分：**总体设计**（作为通用框架的权限系统目标架构）与**当前实现**（代码现状）。
设计中的条目均已落地并带回归测试；若后续新增规划项，仍以 🚧 标注。

---

# 一、总体设计

## 0. 设计哲学

**授权的本质是一个决策函数**：

```
decide(subject, action, resource, context) → PERMIT / DENY
```

角色、组、继承、组织范围……全都不是授权的本质，只是**在规模化场景下管理这张映射表的手段**。
认清这一点决定了整个架构：决策接口是系统的中心，管理模型围绕它可替换、可扩展。

四类构件严格分离（借 XACML 术语，不引入其实现）：

| 构件 | 职责 | 在 kudos 中 |
|---|---|---|
| **PDP** 决策点 | 回答 decide()，唯一真相 | 本模块的决策服务 + 缓存（§2） |
| **PEP** 执行点 | 在边界上拦截并询问 PDP | 网关/Filter、注解、DAO 行过滤（§5） |
| **PAP** 管理点 | 授权的授予/回收/治理 | 授权策略层、转授、审批、SoD（§4/§6/§7） |
| **PIP** 属性点 | 供决策消费的属性 | 租户/组织/时间等上下文（§8） |

作为**通用底层框架**（而非某个应用），两条元原则贯穿所有取舍：

1. **决策接口稳定，策略模型可替换。** 对外只承诺 `decide()` 与授权管理 SPI；RBAC
   是默认策略实现而非 API 本身。业务方无法预知的场景，靠替换/叠加策略实现解决，
   而不是靠框架穷举功能。
2. **模型生而完备，能力渐进暴露。** 表结构是框架最难回滚的决定——`effect`（deny）、
   `condition`、`principal_type` 这类字段必须在 schema 里生而有之（默认值退化为最简
   语义），管理 UI 可以晚开甚至不开。字段加晚了要迁移全量存量授权数据；字段加早了
   只是闲置一列。

以及两条底线：**默认拒绝**（未显式授权即拒绝）与**决策代数确定性**（同一输入永远
同一输出、可解释，见 §1）。

## 1. 决策模型（PDP）

### 权限的标识：语义编码，不是数据库主键 ✅

权限的稳定标识是形如 `sys:user:delete` 的**语义编码**（`域:资源类型:动作`），支持
`*` 通配（`sys:user:*`、`sys:*`）。角色绑定权限编码而非 `sys_resource` 主键 id：

- 编码跨环境/跨部署稳定，资源重建不失效授权；
- 通配符让"授予整个模块"不再需要枚举 N 条绑定；
- 非 UI 资源（定时任务、消息主题、API）无需伪造成"资源树节点"也能纳入授权。

`sys_resource` 仍是权限点的**注册表**（携带编码、类型、URL、菜单展示属性），MENU
只是权限在 UI 上的一种**投影**。各业务模块启动时通过注册 SPI 声明自己的权限点
（code-first），同步进注册表——权限点清单跟着代码走，不靠人肉录入。

### 决策代数 ✅

一次 `decide(subject, action, resource, context)` 的求值顺序**固定且封闭**：

1. **平台超级主体短路**：平台级运维主体直接 PERMIT（仍记审计）。框架必须显式定义
   这一层，否则每个落地应用都会自己发明一个绕过后门；
2. **收集**适用授权：有效角色集（§2 展开）的权限绑定 ∪ 实例级授权（§8）；
3. **条件过滤**：带 `condition` 的授权先求值（不满足则视为不存在，SPI 见 §11）；
4. **合并**：存在 `DENY` → 拒绝；否则存在 `ALLOW` → 允许；否则**默认拒绝**。

`effect ∈ {ALLOW, DENY}` 生而入模型（元原则 2）：“组内所有人都有 X，唯独张三不行”
这类例外，没有 deny 只能靠拆组/拆角色，管理成本随例外数爆炸。DENY 优先级最高且
**不可被更细的 ALLOW 覆盖**——牺牲一点表达力换取可解释性（"为什么被拒"永远只有
一个答案：命中了哪条 DENY，或什么都没命中）。

`condition` 同理生而入模型：一个受限表达式字段（IP 段、时段、自定义键值），由可插拔
的 `IConditionEvaluator` 求值。这覆盖了 ABAC 的高频需求（环境条件），而不引入
策略引擎（见 §12 非目标）。

### 决策入口 ✅

```kotlin
interface IAuthzDecisionApi {
    /** 唯一决策入口。RBAC 是它的默认实现；应用可装饰/替换。 */
    fun decide(request: AuthzRequest): AuthzDecision   // PERMIT / DENY + 命中依据
    /** 当前主体的全量权限编码（UI 渲染菜单/按钮用，一次取全）。 */
    fun permissionCodes(subject: SubjectRef): Set<String>
}
```

决策路径必须是纯缓存命中（毫秒级、无 DB 访问）；`AuthzDecision` 携带命中依据
（哪条授权/哪条 DENY），同一结构直接服务于审计与"为什么"端点（§10）。

## 2. 管理模型（PAP 之一：结构）

```
主体(principal) ──直授──────────────► 角色(auth_role) ──绑定──► 权限编码(+effect,+condition)
  │ 人类用户/机器主体                    ▲    │ parentId
  └──进组──► 组(auth_group) ──绑定──────┘    ▼
             （平结构，无树）             父角色（子继承父的权限）
```

- **有效角色集 = 直授角色 ∪ 组继承角色 ∪ 二者的父链祖先角色**。全系统唯一的合并
  语义，读路径（缓存）与写路径（校验）必须对同一集合生效。
- **角色树方向：子继承父，因此"子角色比父角色强"**。这是全模块最容易理解反的设定——
  权限沿 `parentId` 向上聚合（见 [ResourceIdsByUserIdCache](src/io/kudos/ms/auth/core/platform/cache/ResourceIdsByUserIdCache.kt)）。
  任何涉及"角色强弱比较"的逻辑（如转授判定）都不要基于树方向推导，一律用**权限集包含**判定。
- **组永远是平结构**（无 `parentId`），刻意为之：避免"组继承"与"角色继承"两套继承
  语义叠加。组织树的层级语义由数据范围维度承载（§8），不由组承载。
- **主体不只有人** ✅：服务账号、API key、定时任务等机器主体与人类用户走同一套授权模型。
  主体事实由 `IPrincipalDirectory` SPI 提供（内置 `USER` 实现读 `user_account`），
  当前请求的主体由 `ISubjectResolver` 链解析（内置实现读登录态，**排在最后**，
  以免机器调用被当成"连接上恰好残留的那个会话"）。
  此前策略层直接读 `user_account` 做存在性校验——机器主体能被建模却永远授不出去，
  于是机器访问只能绕开权限系统：共用人类账号、过滤器里开后门、代码里写白名单，
  三者都不可授予、不可回收、不可审计。
  **依赖的不变式**：主体 ID 在**各类主体之间**唯一（不只是同类内唯一），这正是决策路径
  可以只用 ID 作键的原因；kudos 自己的表用 UUID，天然满足。
- **角色来源可插拔** ✅：直授与组继承是内置的两种角色来源；岗位、组织自动角色等
  应用特有来源通过 `IRoleSourceProvider` SPI 挂入有效角色集计算，而不是改本模块。
  来源只能**贡献**角色、不能扣减（扣减是 DENY 绑定的职责，否则判定会依赖求值顺序）；
  贡献来的角色同样受停用、父链展开、租户边界约束。外部来源变更由应用发
  `ExternalRoleSourceChanged` 事件通知——本模块无法观察它不知道的表。
- 多租户：`tenant_id` 是所有授权数据的硬边界，任何关联写入都必须校验两端同租户。
  平台级（跨租户）主体是显式概念（§1 决策代数第 1 步），不靠"空租户"之类的约定
  hack。租户初始化时由框架种入 `built_in` 引导角色 ✅：`ITenantBootstrapService`
  **刻意分两步**——`seedRoles` 建角色（可由 `SysTenantInserted` 自动触发，幂等，且不授予
  任何人任何权限），`bindFirstAdministrator` 才真正授予访问权（显式、审计、默认只允许一次）。
  合并成一步就等于"建了租户 = 有人拥有了它的管理权"，正是本模块要消除的隐式授权。

## 3. 核心不变式

所有授权数据的读写必须满足下表。**读路径认什么，写路径就必须拦什么**——两边不对齐
就是绕过点（现状问题多源于此，见 §9）。

| 不变式 | 含义 | 现状 |
|---|---|---|
| 默认拒绝 | 未显式授权即拒绝，含未注册的 URL/权限点 | ✅ 已实现（默认关闭+影子模式灰度） |
| 有效性过滤 | `active=false` 的角色/组不参与有效角色集计算 | ✅ 已实现（读路径过滤 + 事件失效） |
| 时间窗 | 授权仅在 `[start_time, end_time]` 内生效；组授权同样要有时间窗 | ✅ 角色侧与组侧均完整（读路径过滤 + 到点扫描） |
| 租户一致 | 主体↔角色、组↔角色、角色↔权限、角色↔父角色 两端必须同租户 | ✅ 已实现（五个写入点统一校验） |
| 存在性 | 裸传的 userId / 权限编码必须验证存在 | ✅ 已实现 |
| SoD 互斥 | 任何会扩大某主体有效角色集的写入，都要跑互斥校验 | ✅ 已实现（五个写入点，含改父） |
| 单调收缩 | 转授出去的授权在权限集/范围/有效期/深度上 ⊆ 授权人自己的授权 | ✅ 已实现（四维判定 + 级联回收） |
| 决策封闭 | 所有执行点只问 `decide()`，不各自拼装授权判断 | ✅ 已实现 |

## 4. 统一授权策略层 `IAuthGrantPolicyService` ✅

会扩大主体有效权限的写入点共五个，校验逻辑收敛到一个策略服务，禁止各自为政
（此前 SoD 只在其中一处生效，就是"各写各的"的后果）：

| 写入点 | 等价语义 | 判定方式 |
|---|---|---|
| 角色→主体 bind | 一次授权 | canGrant(operator, role, subject) |
| 组→主体 bind | 把 `rolesOf(组)` 全部授给该主体 | 对每个 role 跑 canGrant，全过才放行 |
| 组→角色 bind | 把 role 授给 `subjectsOf(组)` 每个人（**广播式授权，最危险**） | 对每个成员跑 canGrant |
| 角色→权限 bind | 扩大该角色所有持有者的权限 | 租户/子系统一致 + 编码存在性 |
| 角色 parentId 变更 | 改变所有持有者经父链继承的权限集 | 环检测 + 租户一致 + **SoD 复验** |

所有写入先归约成统一的 `GrantCandidate(roleId, principalId, via)` 再送审——把"绕过点"
从"新写入路径的作者是否记得加校验"变成"是否走这个归约"。

批量判定拆两段以免规模爆炸：与目标主体无关的部分（角色是否存在、属哪个租户）每个角色只算
一次；与主体相关的部分（租户匹配、有效角色集、SoD）逐主体算，有效角色集按主体缓存（一次组
绑定会给同一主体带来多个角色，否则每个角色都要重算一遍）。

**为什么是"报告"而不是"抛异常"**：策略层返回 `List<GrantRejection>`，由调用方决定反应。
其中一个反直觉但重要的结论：**组↔角色绑定是一行记录，不存在"对部分成员生效"**——所以广播
绑定的正确语义是"任一成员不通过则整条绑定拒绝，并在错误里列出是谁挡住的"，而不是部分应用。
`assertNoRejection` 就是这个语义；逐条部分失败的语义只存在于笛卡尔批量入口
（`IAuthRoleService.batchBindUsers`，`BatchBindResultVo`）。

## 5. 执行层（PEP）✅

执行层实现在独立模块 [kudos-ability-security-enforcement](../../../kudos-ability/kudos-ability-security/kudos-ability-security-enforcement/README.md)，
本模块通过 `AuthzDecisionProviderAdapter` / `PermissionPointRegistry` 实现它定义的两个端口。
依赖方向是 **ms → ability**：换掉整个授权后端只需换端口实现，过滤器与注解不动。

三个执行点，全部只问 `decide()`（不变式"决策封闭"）：

| 执行点 | 位置 | 拦什么 |
|---|---|---|
| `PermissionEnforcementFilter` | web ability 层 | 请求 `(method, path)` 匹配权限点注册表的 URL → decide；**未注册路径默认拒绝**，公开路径显式白名单 |
| `@RequiresPermission("sys:user:delete")` | 方法注解 | URL 无法表达的内部方法/消息消费者/定时任务入口 |
| 数据范围行过滤 ✅ | DAO 层 | 查询 Criteria 自动追加范围条件（§8.1），读写皆拦；默认关闭 + 影子模式 |

配套约定：

- `api-internal` 内部通道不复用用户权限模型，信任边界靠网络隔离 + 服务身份（mTLS）。
- JWT 中的 roles claim 只用于网关粗粒度放行；**细粒度判定永远回源服务端缓存**，
  保证撤销即时生效。粗粒度这一层的时延窗口由**权限版本号** ✅ 关掉：
  `IPermissionVersionApi.currentVersion` 在签发时写进 token，PEP 每请求比对一次（纯缓存命中）。
  版本形如 `纪元:指纹`，两半各司其职：
  - **指纹**是解析出的授权项集合的哈希——它是权限本身的函数，**忘不掉**。计数器要靠每条
    写路径记得去自增，而漏掉的那一条会产出一个活过撤销的 token，且毫无迹象。
    指纹还有个反向好处：把角色重新绑成同一套编码时它**不变**，而计数器会把这种例行操作
    变成一次全员登出。
  - **纪元**是管理员强制项（`revokeAllTokens`），单调递增，用于权限根本没变但令牌必须全灭的
    场景（凭据泄露、离职）——这是纯派生方案表达不了的。
  版本缺失一律判定为"不是当前版本"：把"未知"当成"没问题"，正是撤销校验悄悄停止校验的方式。
- **执行侧已接通** ✅：`ITokenFreshnessValidator` 端口 + `PermissionEnforcementFilter` 第 4 步。
  凭据过期返回 **401 而非 403**——调用者很可能确实有权限，只是手上这张凭据早于某次变更，补救办法是重新登录而不是要更多权限。
  **顺序也是有意的**：过期校验排在决策之前，因为撤销前签发的 token 依然指向一个决策点乐于回答的主体,先问"他能不能"等于回答了一个关于"根本不该再认的凭据"的问题。
  三态而非布尔：`UNKNOWN`（凭据不带版本）默认放行,因为此前签发的每一张 token 都不带版本,开关一开就拒等于一次集体登出——正是本模块处处在避免的停机式接入。等签发方开始盖版本、影子日志清空后,再把 `reject-unversioned-tokens` 打开,此时"缺版本"才从事故变成伪造信号。
- 决策路径纯缓存命中（`PermissionGrantsByUserIdCache`），失效由授权变更事件驱动，与
  `ResourceIdsByUserIdCache` 订阅同一组事件。
- **决策点挂了之后不再逐个请求去付代价**(A12/A13/A14):执行层的 `DecisionGuard` 是个断路器,
  包住过滤器**整段决策**——权限点解析、主体解析、凭据新鲜度、决策调用,一个不落。任何一个抛异常
  都记为一次失败,连续 5 次熔断打开,窗口内的请求不碰后端直接拒绝。**所有失败模式一律拒绝**。
  只包在 **HTTP 过滤器**上:那里慢决策要付出 servlet 线程的代价;方法注解守的是定时任务与
  消息消费者,没有共享池可耗尽。对外仍是 403 而非 503,区别只给运维看(WARN 日志)。
  **这里没有"单次超时",而且不可能有**——见 A12 那一行。

  **实测(停掉数据库,shadow 模式,单个端点)**,三次改动各自的效果都量出来了:

  | 场景 | 每请求耗时 | 说明 |
  |---|---|---|
  | 健康 | 13ms | 基线 |
  | 注册表在请求线程上重载(A14 之前) | 90s | 30s 注册表 + 30s 决策 + 30s 控制器自身查询 |
  | 注册表改为后台刷新(A14 之后) | 60s | 注册表那 30s 从请求路径上消失 |
  | 熔断窗口内到达的请求 | **30s** | 决策那 30s 也没了 |
  | 剩下的 30s | — | **是控制器自己的查询,不归鉴权管**(影子模式放行后它自己去查库) |
  | 连接超时改为 3s 后重测 | **6s → 3s** | 熔断前 6s(决策 3s + 控制器 3s),第 5 次失败后恰好掉到 3s——断路器开、决策段瞬时拒绝,只剩控制器自己的 3s |

  两条由此得到的、不靠推理只靠测量的结论:
  - **`open-millis` 不能短于一次失败调用本身的耗时**。原本 10s 而失败要 30s,窗口在那次调用
    返回之前就过期了,下个请求到达时已是半开——几乎每个请求都成了探针,照付 30s。现在是 30s。
  - **这个断路器只能省下它自己那一段**。真正能同时约束三段的旋钮是数据源连接超时,
    而全项目没有任何地方配过它,用的是 HikariCP 默认值 30s——上表每一个 30s 都是它。
    见 §其他已知限制。

- **落地必须走影子模式**：`enabled` 默认 false，`shadow-mode` 默认 true。存量系统先开影子模式
  跑真实流量，按 WARN 日志补齐权限点，再关掉影子。没有这一步，接入鉴权就是一次停机事件。
- **迁移期的诚实行为**：resource_id 型绑定要靠 `sys_resource.permission_code` 才能参与决策；
  资源还没登记编码时，这条绑定对决策不产生任何贡献（而不是被当成"放行"）。

## 6. 受控转授（delegation）✅

原则：**授权权是"某一次授权行为"的属性，不是角色的属性**；持有 ≠ 可授；范围只能单调收缩。

模型（`auth_role_user` 一条记录 = 一条授权）：

| 新增字段 | 含义 |
|---|---|
| `granted_by` | 授权人（权限语义，区别于审计字段 `create_user_id`） |
| `parent_grant_id` | 上游授权 id，构成授权链；超管直授为 NULL |
| `delegable_depth` | 本条授权还能往下传几层；0 = 拿到即终点 |
| `scope_snapshot` | 授权时冻结的范围（防授权人事后调岗导致范围漂移） |
| `revoked` / `revoke_reason` | 软失效，保留审计 |

`auth_role` 加天花板字段 `delegable_max`（0 = 禁止转授；`built_in` 与超管角色恒 0）。
角色上只放上限，每次授权给几层由授权动作决定。默认深度上限 1（超管→A→B，B 不能
再传），需要更深由管理端显式放开。

转授判定挂在 §4 的同一个归约上：`GrantCandidate` 带 `operatorId` 时才跑转授四步（`operatorId = null`
表示平台直授/审批流落地，不是转授，显式跳过）。四步按"失败最便宜的先"排序，对应一条授权的四个维度：

1. **触达**：操作人必须通过**直授**持有该角色且 `delegable_depth ≥ 1`，且 `depth ≤ 自身深度 - 1`
   同时 `≤ role.delegable_max`。经组获得的角色永远不满足——归因不到人的授权链不能存在。
2. **权力**：`permissionsOf(role) ⊆ 操作人可实际行使的权限集`。注意是**可行使**而非**持有**：
   操作人可能持有该角色，却被另一条 DENY 拿掉了其中一项，此时转授出去就是净放大。用权限集包含
   判定而不是"是否持有该角色"，正是为了抓住这种情况（也不走角色树方向，理由见 §2）。
3. **人群**：目标主体 ∈ 操作人授权时**冻结**的 `scope_snapshot`，不是他当前的范围——否则调岗
   会悄悄放宽已经发出去的授权。
4. **寿命**：`endTime ≤ 操作人自身授权的 endTime`；源头有期限而转授不写期限，是同一个错误换个写法。

之后仍然跑 SoD（§4 的公共部分）。

组相关的两条规则：

- **经组获得的角色 `delegable_depth` 恒为 0**——转授权只沿直授链传递。组成员身份批量
  且流动，若组员可转授，"权限是谁给的"就无法归因到人，回收影响面不可控。
- 需要"某组成员都能代授权"时，正确做法是给这些人**直授**一个带深度的角色，而非让组
  传递转授权。

回收语义：

- 软失效 + 沿 `parent_grant_id` **级联**标记整棵下游子树（`revoke(grantId, reason)`）；
- **`unbind` 也必须级联**：删掉上游行而留下下游，下游会继续生效且再也追溯不到授权来源——
  典型的僵尸权限。所以先级联软失效，再删行。
- 回收前给**影响面预览**（`getRevokeImpact`）：级联的爆炸半径从被回收的那条授权上完全看不出来，
  不先展示就会把"收回一个人的权限"悄悄变成"收回四十个人的权限"；
- 所有读路径统一过滤 `revoked`（见 `AuthRoleUserDao.isLive`），行保留只为授权链可走与审计可查。

`auth_group_user` / `auth_group_role` 同样加 `granted_by` / `parent_grant_id` / `revoked`——
"把人加进组"本身就是授权行为，需可归因、可回收。

## 7. 审批流 ✅

`auth_role.approval_required = true` 的角色：

- ✅ 直接 bind（含经组授予、转授）一律拒绝，强制走 grant-request
  （[AuthRoleGrantRequestService](src/io/kudos/ms/auth/core/role/grant/service/impl/AuthRoleGrantRequestService.kt)）。
  校验落在 §4 的策略层而非控制器，所以四条写入路径全部覆盖——否则"审批"在实践中就是可选的：
  谁找到那个普通 bind 端点谁就绕过去了。审批流通过 `GrantCandidate.approvalSatisfied` 显式声明
  自己已满足该要求（而不是让策略层去猜调用栈——第一次有人包一层就会静默失效）。
- ✅ 审批人资格：**审批人必须是"本来就能授出这个角色的人"**——通过直授持有该角色且带转授额度，
  或是平台管理员。松于此则审批就是走过场：任何登录用户都能签字的流程，只往表里加了一行，
  没给系统加任何安全性。授权链天然给出了这个人群，不需要另建审批人配置表。

状态机守卫（重读行 + 仅 PENDING 可动作）已实现，保持。定期权限重认证
（access review campaign，"每季度确认一遍这些人还该有这些权限"）是企业级治理需求，
框架预留在授权链数据之上实现的可能，不内置。

## 8. 范围模型：功能权限 × 数据范围 × 实例授权 ✅

权限判定天然分三个正交层次，框架三层都要有位置：

1. **功能权限**（能不能用这个功能）：§1 的权限编码，回答 "can subject do action?"
2. **数据范围**（能看到哪一片数据）：角色携带的行级范围策略。✅ **已泛化为多维**：
   `auth_role_scope(role_id, dimension, scope_value)`，维度是**数据而不是表名**——org 只是内置的
   那一个，region / 项目 / 品牌由应用贡献一个 `IScopeDimensionProvider` bean 即可注册，
   SPI 只需回答"这个维度上的值嵌不嵌套"（`expand`）和"主体在这个维度上处于什么位置"
   （`valueOfUser`）。写入按维度隔离：改角色的机构不会顺手清掉别人在另一个界面配的区域。
   `ALL / ORG_AND_CHILD / ORG / SELF / CUSTOM` 保持为 org 维度上的策略枚举。
   解析出口 `DataScopeVo` 也已多维（`dimensions: Map<维度, Set<取值>>`，`orgIds` 保留为内置维度的
   便捷访问器）。**同一维度内取值取并集（OR），不同维度之间由消费方 AND**——解析器只产出取值，
   谓词由消费方拼，因为只有它知道哪一列承载哪个维度。
   还有一条容易搞反的语义：**展开属于策略，不属于维度**——`ORG_AND_CHILD` 带子树、`ORG` 只有本级、
   CUSTOM 就是管理员勾选的那几个。统一在末尾展开会把第二种悄悄变成第一种（此坑已由既有测试抓到）。
   ✅ 落地点 DAO 行过滤已接（§5 第三执行点、§8.1）。
3. **实例授权**（能不能动这一条数据）：✅ `auth_instance_grant(principal, resource_type,
   instance_id, action, effect, 时间窗)`——"把这份文档分享给他"式的点对点授权，
   RBAC 表达不了也不该硬表达（硬表达 = 一行数据一个角色）。`action` 用**同一套通配规则**匹配
   （`doc:read` 或 `*`），不需要学第二套规则。它在决策代数里与角色权限**同级合并**而不是叠在
   上层：分享若能盖过 DENY，"收回这个人的权限"就成了一句没人能依赖的话。
   请求不指定实例时完全不走这个分支——这也是它可以直接查库而不破坏决策路径成本的前提
   （替代方案是把所有人的所有分享常驻内存，用有界的单次读换无界的常驻集，不划算）。
   完整的关系图模型（Zanzibar/ReBAC）仍是非目标（§12）。

字段级权限不设独立机制：权限编码的动作段天然可细化（`order:read:amount`），
由应用在展示层消费；框架不做字段脱敏引擎。

### 8.1 行过滤契约 ✅

数据权限现在能配、能算、能看，但**不生效**——`resolveUserDataScope` 的结果没有任何业务消费方。
补上这一步要改 `kudos-ability-data-rdb-ktorm`，影响所有走 `BaseCrudDao` 的查询，因此先定契约。

#### 一、实体如何声明参与

声明**贴在实体上**，不放在集中配置里：范围规则和表结构必须一起改，分开放必然漂移。

```kotlin
@DataScoped(dimension = "org", column = "org_id")
@DataScoped(dimension = "region", column = "region_code")   // 可重复：一个实体可被多维切分
@DataScopedSelf(column = "create_user_id")                  // SELF 策略比对哪一列
interface BizOrder : IDbEntity<String, BizOrder> { … }
```

不属于自己的实体（第三方模块的表）走逃生口：贡献一个 `IRowScopePolicyProvider` bean 补充声明。
两条路径产出同一种 `RowScopePolicy(entityClass, dimension, column)`，过滤器只认后者。

**实体默认不参与（opt-in），这与 URL 的"未注册即拒绝"刻意相反。** 理由是两者的默认值含义不同：
一个没注册的 URL 几乎一定是漏了；而字典、配置、`auth_*` 自己这些表**本来就该全局可见**，
默认拦截会是绝大多数情况下的错误答案，而且第一个受害者就是权限管理界面自己。
代价是"忘了声明 = 没有防护"，用**启动期报告**兜底：列出所有未声明的实体，让缺口可见而不是无声。

#### 二、谓词怎么拼

由 `DataScopeVo` 直接翻译，语义在 §8 已定：

| 解析结果 | 追加的条件 |
|---|---|
| `all = true` | 不追加 |
| 每个受限维度 | `col IN (values)` —— 同维度取值 **OR** |
| 多个维度之间 | **AND** |
| `self = true` | 与上面整体 **OR**：`(dim1 AND dim2) OR create_user_id = :me` |
| 实体未声明某维度 | **该维度对这个实体不适用**，跳过 |

最后一条是唯一有争议的：也可以选"实体没声明就拒绝"。但范围的含义是"这类数据的哪些行"，
一个没有 region 列的实体本就没有 region 属性，硬套只会让无关实体整体消失。
风险留给启动期的 `strict-dimensions` 检查：报告"声明了部分维度但漏了其他已注册维度"的实体。

#### 三、拦在哪里

`BaseCrudDao` 里**所有以 Criteria 为入口**的方法，读写都要拦：

| 路径 | 是否过滤 | 理由 |
|---|---|---|
| `search` / `count` / `searchAs` / 分页 | ✅ | 看得见的边界 |
| `updateByCriteria` / `batchDeleteCriteria` | ✅ | **看不见就不能改**；只拦读等于半条边界，用条件更新照样能改到看不见的行 |
| `get(id)` / `deleteById(id)` | ✅ | 按主键直取是绕过条件过滤最容易的口子 |
| 原生 SQL / 自定义 Ktorm DSL | ❌ 拦不到 | 见下方"诚实的边界" |

#### 四、没有主体时怎么办（最危险的一个决定）

定时任务、消息消费者、服务间调用都没有登录主体。三种取值：

- **"无主体 = 不过滤"** —— 绝对不行。任何丢失上下文的路径（异步、线程池、Feign）都会静默看到全部数据，
  而且丢上下文这件事恰恰是最容易发生、最难发现的。
- **"无主体 = 看不到"** —— 定时任务静默返回 0 行，安全但会变成一类查不出原因的诡异故障。
- **✅ 采用：无主体且未显式标注 → 报错**。系统上下文必须显式声明：

```kotlin
DataScopeContext.runAsSystem { orderDao.search(criteria) }   // 或方法上 @SystemScoped
```

这样"忘了标注"在灰度期就是一次响亮的失败，而不是一次静默的数据泄露或一份空报表。
平台管理员**不需要特例**：他们解析出来就是 `all = true`，谓词自然为空——已有机制自动覆盖。

#### 五、灰度

与 `PermissionEnforcementFilter` 完全同构，且这是比 URL 鉴权更危险的开关（它改变查询结果而不只是拒绝请求）：

```yaml
kudos.ability.data.rdb.row-scope:
  enabled: false      # 默认关闭，加依赖不改变任何行为
  shadow-mode: true   # 只记录"本该追加什么条件"，不真的追加
```

影子模式记录 `(实体, 主体, 本应追加的谓词)`，用 WARN——没人打开的日志级别产出的是一份空清单。
不做"跑两遍数一数差多少行"：那是双倍查询成本，只在明确开启的 audit 模式下提供。

#### 六、诚实的边界

会被绕过、且**框架无法拦截**的地方，必须写在文档里而不是假装不存在：

- **原生 SQL 与自定义 DSL**：不经过 Criteria 组装就不会被追加条件。缓解手段是一条静态检查——
  对已声明 `@DataScoped` 的实体，禁止在 DAO 之外拼裸 SQL。
- **多表 join**：只对根实体的列生效，被 join 进来的表不受约束。
- **跨服务**：别的微服务的数据由它自己的过滤器负责，本服务的范围管不到。
- **缓存**：已按 id 缓存的对象不会二次过滤，缓存读路径需要各自判断——这也是范围过滤只应作用于
  查询列表、不应作用于按 id 取单个对象的缓存的原因。

#### 七、实施进度

| # | 事项 | 状态 |
|---|---|---|
| 1 | 契约：`@DataScoped` / `@DataScopedSelf` / `@SystemScoped` / `RowScopePolicy` / `IRowScopePolicyProvider` / `IRowScopeResolver` / `DataScopeContext`（全在 `kudos-ability-data-rdb-ktorm`，不依赖 auth） | ✅ |
| 2 | auth 侧 `RowScopeResolverAdapter` 实现端口（ms → ability） | ✅ |
| 3 | `BaseReadOnlyDao` / `BaseCrudDao` 接入（读 + 按条件写 + 按主键写）、默认关闭、影子模式、启动期报告 | ✅ |
| 4 | 全局表豁免 | ✅ 天然满足：实体 opt-in，`auth_*` / `sys_*` 未声明即不过滤 |
| 5 | 端到端验证（示例实体 + 真实 SQL） | ✅ |

**如何给一张业务表接入**，照 `TestScopedOrder`（ktorm 测试源码里的示例实体）写即可：

```kotlin
@DataScoped(dimension = "org", column = "org_id")
@DataScoped(dimension = "region", column = "region_code")
@DataScopedSelf(column = "create_user_id")
interface BizOrder : IDbEntity<String, BizOrder> { … }
```

贴上注解即参与；不贴就完全不受影响。接入后先开 `enabled: true` + `shadow-mode: true`，
按 WARN 日志确认谓词符合预期，再关影子。

测试覆盖四层：谓词组合语义（`RowScopePredicateBuilderTest`，12 例）、开关判定
（`RowScopeEnforcerTest`，10 例）、**真实表真实 SQL 的端到端**（`RowScopeEndToEndTest`，17 例，
含读路径、写路径、按主键、无主体报错、system 豁免、未声明实体不受影响）、影子记录
（`RowScopeShadowRecorderTest`，5 例）。

#### 八、迁移期的工作清单

影子模式的观察结果不只写日志，还进一条有界的内存记录，由控制台
`/auth/rowscope` 页面呈现。日志是持久记录，但**日志不是工作清单**：它没有次数、没有首末时间，
也分不清"一个我漏掉的入口"和"一条每分钟跑一千次的热路径"——而这两件事决定了下一步做什么。

两类发现直接对应两件要做的事：

| 类型 | 含义 | 该做什么 |
|---|---|---|
| `WOULD_FILTER` | 某实体将被追加谓词 | 核对谓词是否符合预期（维度、取值、是否含本人数据） |
| `WOULD_FAIL` | 已声明实体被无主体查询 | 该调用需要 `DataScopeContext.runAsSystem { }` 或 `@SystemScoped` |

记录是**有界且仅内存**的：这是迁移期的诊断脚手架，不是审计流水，持久化它意味着为一个关掉影子
模式就该删除的东西引入表、保留策略和清理任务。缓冲区满时**明确上报丢弃条数**——一份被静默截断
的清单会读成"没有待办了"，恰好是事实的反面。

## 9. 已修复的撤销缺陷（保留记录，防止回归）

| # | 缺陷 | 根因 | 状态 |
|---|---|---|---|
| A1 | **停用角色不回收任何权限**：`active` 曾是死字段 | 读路径不过滤 `active`，且 `ResourceIdsByUserIdCache` 不监听 `AuthRoleUpdated` | ✅ 已修复 |
| A2 | **改 `parentId` 不失效用户权限缓存** | 同上 | ✅ 已修复 |
| A3 | **未来生效的授权到点不生效** | 缓存快照在 bind 时刻计算并滤掉未生效授权，到点无事件触发重算 | ✅ 已修复 |
| A4 | **组成员时间窗配了不生效**：过期/已撤销的组成员仍持有组的全部角色 | 写路径与管理界面都落了 `start_time`/`end_time`/`revoked`，但 `AuthGroupUserDao` 的**读路径一个都不过滤**——与 A1 同类，且因界面已引导管理员去配，性质更坏 | ✅ 已修复 |
| A5 | **组成员到点无扫描**（A3 的组侧同构） | `ExpiredGrantPurgeScheduler` 只扫 `auth_role_user`；时钟走过 `end_time` 不改任何行，因而没有任何事件去失效缓存快照 | ✅ 已修复 |
| A6 | **实例分享绕过租户边界** | `share()` 只校验非空：不查主体存在性、不比对租户，决策侧也不比较授权行的租户。租户 A 的操作者可把实例分享给租户 B 的用户，`decide()` 照常 PERMIT——角色路径上被五个写入点死守的硬边界，在分享路径上是敞开的 | ✅ 已修复 |
| A12 | **决策没有自己的时间预算**:数据库停机时每个请求等满 30 秒(数据源连接超时)才拒绝 | 决策未命中缓存就去查存储,并**继承存储的超时**——决策本身不受任何约束。拒绝是对的,花 30 秒去拒绝不是:请求线程是固定池,一次存储故障会变成整个服务不可用 | ⚠️ **第一版修错了**,已重做为断路器(见 A13) |
| A19 | **时效性授权是"第六条写入路径"——绕过策略闸门** | `screenGrants` 的注释原话:"把锁放在这里,就是为了加第六条写入路径的人忘不掉这个保证"——上游与本侧合并恰好造出了这第六条。上游写 `bindTemporal` 时闸门还不存在,内联了三条检查(角色存在、不降级永久授权、SoD);合并保留了它。由 admin REST 直达,缺:**租户边界**(可跨租户临时授权)、**主体存在性**、**`approval_required`**(需审批的角色从这里授出不需要任何审批,时间窗设长即事实永久)、**按主体加锁**(A9 窗口原样重开)。另有两个派生问题:delete-then-insert 会**静默替换转授链行**(洗掉 granted_by/parent_grant_id,级联再也找不到它)并硬删已撤销行;`computeEffectiveRoleIds` 是"有效角色集"的**第二份实现且已漂移**(不过滤 inactive 角色) | ✅ 已修复:准入整段换 `screenGrants`(温窗语义的两条守卫——永久授权不降级、转授行不替换——保留在本地);普通窗口/已撤销行**原地更新**(行即审计,同组成员的复活约定);第二份实现连同其单测删除。探针实测:摘掉闸门,跨租户/审批/SoD/角色存在性 4 个用例全红 |
| A15 | **条件 DENY 在证据缺失时静默消失(fail-open)** | `applies()` 对条件求值失败一视同仁地"绑定视为不存在"——对 ALLOW 是收紧,对 DENY 却是放开:`DENY 条件 region=cn`,只要没人供给 `region` 属性,这条拒绝就蒸发。而 context 完全由调用方掌控:一个不传 `ip` 的程序化 `decide()` 调用绕过所有基于 ip 的 DENY。求值器 KDoc 自己写着"把无法执行的限制当无限制会静默放宽访问",`return false` 恰好让 DENY 违反了这句话 | ✅ 已修复("为假"与"不可判定"分离:后者抛 `UndecidableConditionException`,决策点按效果不对称处理——ALLOW 缺席,DENY **生效**) |
| A16 | **转授与撤销之间的写偏斜:级联漏掉并发落库的子授权** | A9 的同族,当时只修了"授"这一半。`screenGrants` 锁受赠者不锁操作者,`revoke()` 什么都不锁:转授(校验操作者授权仍活)与撤销该操作者(读子树快照)并发,新子行在快照之后落库——**父授权已撤销、子授权永远活着**,再无任何级联会回来收它 | ✅ 已修复(`screenGrants` 把操作者并入锁集;`revoke`/`unbind` 对子树持有者加锁后重读至不动点)。钉窗实测:撤销真实地被操作者锁挡住,重试后级联补上子授权;20 轮并发 0 孤儿 |
| A17 | **"首任管理员是一次性动作"的门闩存在竞态** | 门闩的不变量是**角色**的属性("还没有持有者"),而写路径上只有按主体的锁:两个并发任命绑**不同**候选人,各锁各的,都读到空持有者集,都不带 `force` 通过——代码注释明说"悄悄任命第二个管理员就是提权路径",竞态恰好做到了 | ✅ 已修复(`AuthRoleDao.lockRole`,读持有者集之前先锁角色行)。探针实测:摘掉锁 **20/20 轮产生两个管理员**,装回 0/20 |
| A18 | **`isPlatformAdmin` 每次决策逐角色查库** | 它是 `decide()` 的第一步、每个请求必经,却对每个角色发一条 `authRoleDao.get`——A11 立下的"决策路径纯缓存命中"被自己的第一步破坏,数据库故障能打到授权数据已全部入缓存的主体 | ✅ 已修复(换 `AuthRoleHashCache.getRolesByIds` 批量取) |
| A14 | **修好的断路器又被我自己的"优雅降级"弄哑了**:注册表刷新失败时改为返回旧快照,于是不再抛异常,断路器把这个等满 30 秒的请求记成**成功**,永远不开 | A13 的修法把"刷新失败仍服务旧快照"当成纯粹的可用性改进——它确实是,但断路器的健康信号只有"有没有异常逃出来"。**对数据 fail-soft 顺手把健康信号也 fail-silent 了。**更根本的是:一个可能阻塞的缓存刷新,压根不该跑在正在服务请求的线程上 | ✅ 已修复(刷新移到后台单线程;冷启动无快照时仍同步加载并**抛出**——"没有权限点"和"读不到权限点"不是同一件事) |
| A13 | **断路器只包住四个协作者中的一个**,且第一版"单次超时"是**引入的严重回归** | 两个缺陷叠在一起。其一:预算只装饰了决策端口,而过滤器每个请求还要调权限点注册表、主体解析、凭据校验——停机时前三个先各自等满存储超时,断路器根本轮不到。其二更重:为了能超时,决策被挪到线程池上执行,而主体解析读的是 `RequestContextHolder`/`KudosContextHolder` 这类**只存在于请求线程上的状态**,于是**所有已授权请求全变 403**,而这个开关默认开启。单元测试全绿——假实现没有线程亲和性,什么都察觉不到;影子模式实测也没发现,因为当时只测了无权限主体,403 看着完全正确 | ✅ 已修复(`DecisionGuard` 在调用方线程上跑,包住整段决策;删掉线程池;新增两条断言直接测"跑在哪个线程") |
| A11 | **负结果从不入缓存**:无授权项的主体每个请求都回源查库,决策路径"纯缓存命中"的承诺对他们不成立 | `getGrants` 带 `unless = result.isEmpty()`。而这恰是最该被廉价拒绝的人群(新建账号、刚被回收权限、配置错误的客户端反复重试),也正因如此,缓存层一挂**先坏的就是他们**,而有权限者靠缓存命中照常服务 | ✅ 已修复(决策路径两个缓存改为缓存空结果) |
| A10 | **编码穿越可冒充公开路径**:`/actuator/..%2f<受保护端点>` 解码后仍带 `..`,却匹配上 `/actuator/**` 公开模式而被直接放行 | 过滤器取 `servletPath.ifBlank { requestURI }`——`requestURI` 是**原始的**(带 context path、未解码、未规范化);而解码 ≠ 规范化,`UrlPathHelper` 会把 `%2f` 还原成斜杠却不消解它暴露出的 `..`。Tomcat 目前直接 400 挡掉,但那是容器的选择、不是本过滤器的保证 | ✅ 已修复(按 Spring 路由的方式解析 + 规范化 + 无法消解则拒) |
| A9 | **SoD 并发写偏斜**:两个管理员同时把互斥的两个角色授给同一人,双双通过校验、双双落库 | 所有授权写入都是"读有效角色集 → 查互斥 → 插入",两步之间没有任何东西:不加锁,而唯一的约束 `unique(role_id,user_id)` 表达不了"A 与 B 不得兼有"。40 轮并发复现 39 次 | ✅ 已修复(按主体加锁) |
| A8 | **所有 Kotlin data class 请求体反序列化失败** | `jackson-module-kotlin` 不在任何 web 模块依赖里,且 `objectMapper()` 是裸构造不发现模块;`@EnableWebMvc` 又让框架自建了一套转换器。约 23 个 `@RequestBody` 端点从未工作过 | ✅ 已修复 |
| A7 | **组成员移除是硬删除** | `revoked` 列有读路径过滤却没有任何写入方：`unbind` 直接删行，于是"谁被移出过这个组、为什么"随动作一起消失 | ✅ 已修复（软撤销 + 重新加入时原地复活） |

修复要点（回归测试见 `ResourceIdsByUserIdCacheTest` / `RoleIdsByUserIdCacheTest` / `AuthRoleUserTemporalServiceTest`）：

- **停用语义**（全模块唯一定义，见 `AuthRoleDao.filterActiveRoleIds`）：`active = false`
  **只剔除该角色自身的贡献**，父链行走照常穿过它——父链遍历跑在原始图上，过滤只作用于**结果集**。
  这样关掉一个中间节点，不会连坐把祖父的资源从其后代持有者身上悄悄拿走。组是平的，无此问题。
- **失效范围**：角色行变更影响的不只是它的持有者，还包括其**所有后代角色**的持有者（后代经父链
  消费它的资源）。这条规则收敛在 `AffectedUserResolver.usersAffectedByRoleChange` 一处，避免两个
  缓存各写一份而漂移。
- **窗口两端对称**：过期由 `purgeExpired` 删行并发事件；生效由 `activateStarted(since, now)` 扫描
  `(上次巡检, now]` 内跨过 `start_time` 的授权并发事件，复用同一条失效链路。
- **组成员的读路径分两族**（A4 的根治方式不是加一个过滤，而是让两种语义各有名字）：
  - `search*`（已过滤）= **权限契约**。进组即被授予组的全部角色，所以只有未撤销且在窗口内的
    成员身份才算数。所有回答"这个主体能做什么"的路径走这族。
  - `searchMemberUserIdsByGroupId` / `searchMembershipsByGroupId`（原始）= **失效与管理**。
    缓存失效必须走这族：多踢一个人只是多一次重载，漏踢刚过期/未来生效的那个，就会留下一份
    再也没有东西去纠正的陈旧权限集。
- **实例分享走同一道准入**（A6）：`share()` 经 `IPrincipalDirectory` 校验收件人存在、启用、且**确实属于所声明的租户**——租户不再是调用方随口传的值。决策侧再比一次授权行的租户作为纵深防御：写路径才是正经把关处，但租户边界值得两边都查。
- **组成员移除是软撤销**（A7）：与本模块其它撤销一致。由此引出的第二个坑必须一并处理——`(group, user)` 唯一，已撤销的行仍占着这个位置，任何把"有行"当成"已是成员"的代码都会让**重新加入变成静默无操作**（"他为什么加不回来"最坏的一种答案）。所以 `batchBind` 是**原地复活**而非跳过,也不新建行：行本身就是这段关系的审计记录，换一行会丢掉当初何时加入、因何被移除。
- **`auth_group_user.parent_grant_id` 是预留列**：组是平容器、不承载转授权，经组获得的角色恒为终端（depth 0），所以本列无写入方、无级联，授权链与级联回收完全在 `auth_role_user` 上。DDL 注释已按此如实说明，不再承诺一个不存在的级联。
- **组成员到点扫描不删行**（与角色侧的唯一有意差异）：读路径已经正确过滤，行只剩审计价值，而
  管理界面明确承诺展示已失效/未来生效的成员记录——删掉正好毁掉它承诺展示的东西。两端都用
  `(since, now]` 区间查询而非 `end_time < now`：后者会把历史上所有过期成员每分钟重播一遍，
  把一次缓存失效变成常驻负载。

**这批缺陷是怎么找出来的**——值得单独记一笔,因为它推翻了本模块此前的验证方式:

A8、A10、A11、A12、A13、A14 全都是**单元测试全绿、编译无警告**的情况下存在的,只有把真实后端跑
起来才看得见。A15–A18 则来自一轮针对**写路径与决策代数边角**的复审:三个是 A9 同族的
读后写竞态(锁的粒度对不上不变量的粒度),一个是决策热路径上的漏网查库。A19 来自**合并缝合处**
的复审:两侧各自全绿,缝合出的整体却违反了"所有授权写入收敛到同一道闸门"的核心不变量——
合并后的正确性不是两侧正确性的并集,缝合处要单独审。原因各不相同却同源:测试替身没有真实协作者的属性。假的 Jackson 转换器不会缺模块;
假的决策提供者没有线程亲和性,于是把决策挪到线程池上"看起来没问题";假的缓存不会因为
`unless` 而回源。**绿色测试证明的是代码符合测试作者的心智模型,不是符合运行时。**

尤其 A12:它是**修复引入的回归**,默认开启,让所有已授权请求返回 403。第一次影子模式实测
也没抓到——因为当时只验证了"无权限主体被拒",403 看着完全正确。抓到它的是后来补的那次
"**已授权主体应当 200**"。一个只测负例的验证,对"全都拒绝"这种故障是完全瞎的。

因此新增的 `DecisionGuardTest` 里有两条断言测的不是返回值而是**执行位置**
(`theDecisionRunsOnTheCallersThread` / `threadLocalStateIsVisibleInsideTheGuard`)——
把"这段代码依赖它跑在哪个线程上"这个隐性契约变成显性断言。

A14 更进一步:它是在验证 A13 的修复时才暴露的,而且**第一次测量的方式本身是错的**——
逐个发请求、每个之间隔 60 秒,这种流量下断路器按定义就该每次都放探针,于是"断路器没生效"
的结论其实是测法造成的。改成并发请求才看到真相。**一个压力机制,不能用它不适用的流量模式去证伪。**
A14 这条性质本身(跨线程、跨事务)在回滚式集成测试里观察不到,所以它没有单元测试,
只有 §5 那张实测表——与其编一个测不到真东西的测试,不如如实说明它是怎么被证实的。

### 条件求值是三值的(A15)

"条件为假"和"条件无法判断"必须是两个通道:前者返回 `false`,后者抛 `UndecidableConditionException`。
二者在决策点分岔——判为假的绑定无论什么效果都缺席(这就是条件绑定的语义);**不可判定**的绑定按效果
不对称处理:ALLOW 缺席(调用方拿不出授予访问所需的证据),DENY **生效**(限制没能被检查,不等于被解除)。
两边都是 fail-closed,只是"closed"对两种效果指向相反的方向。

自定义 `IConditionEvaluator` 实现必须遵守这个契约:对"看不懂的规则、缺失的证据"返回 `false` 的实现,
会让每条条件 DENY 在证据缺失时静默解除——而 context 完全由调用方掌控。

## 10. 审计与可解释性 ✅

权限系统的每个答案都必须能回答"为什么"：

- ✅ `getEffectivePermissions`：主体视角一次取全（直授/组/继承/权限）。
- ✅ **反向解释端点** `/api/admin/auth/authz/explain`：直接复用决策点自己的输出
  （`decide` 给结论，`resolveGrants` 给证据），而不是另写一份推导——另写必然漂移，而这里漂移
  比没有功能更糟：运维会照着错误的原因去操作。来源标注 DIRECT / GROUP / INHERITED，
  其中 INHERITED（父链继承）是运维最容易困惑的一种。条件绑定按传入上下文求值，
  "他能用我不能用"这类问题通常就是条件，脱离上下文解释会给出错误答案。
- ✅ 授权变更审计：admin 端所有变更端点接 `@WebAudit`（bind/unbind/grant/revoke/绑权限）。
- ⚠️ **决策级审计的取舍**：不对每次 `decide()` 发审计事件——那是每请求量级，写入成本与信噪比
  都不成立。DENY 由执行层日志覆盖；需要完整决策审计的部署，装饰 `IAuthzDecisionApi`
  即可（SPI 存在正是为此）。

## 11. 扩展点（SPI）清单

框架靠这些缝隙适配业务，而不是靠功能穷举：

| SPI | 用途 | 默认实现 |
|---|---|---|
| `IAuthzDecisionApi` | 决策入口，可装饰/替换 | RBAC 决策代数（§1） |
| `IRoleSourceProvider` ✅ | 有效角色集的额外来源（岗位、org 自动角色…） | 直授 + 组继承 |
| `IConditionEvaluator` | 授权条件求值（IP/时段/自定义键值） | 内置基础求值器 |
| `IScopeDimensionProvider` | 数据范围维度注册（层级与包含关系） | org 维度 |
| `IRowScopePolicyProvider` | 行过滤谓词的应用侧策略 | 按维度列匹配 |
| `IPrincipalDirectory` ✅ | 主体事实（存在性/租户/启用），使非人类主体成为一等公民 | `USER` 读 `user_account` |
| `ISubjectResolver` ✅ | 解析当前请求的主体（含机器身份） | 登录态（排最后） |
| `IAuthGrantPolicyService` | 授权写入校验，可叠加应用自有规则 | §4 五点校验 |
| `IPermissionPointNaming` ✅ | URL → 权限编码的命名约定 | `/api/{scope}/a/b/c` → `a:b:c` |
| `IPermissionPointContributor` ✅ | 声明无 HTTP 面的权限点（定时任务、消息消费者） | 无 |
| `IPermissionPointStore` ✅ | 权限点落库 | 写入 `sys_resource` |

### 权限点注册：为什么它和"强制"是两个开关 ✅

未注册路径按设计就是拒绝，所以**必须先注册、再强制**。若二者共用一个开关，唯一能填满
注册表的路径就是"先打开强制、然后被拒绝一切"——正是这个死锁让注册表在实践中只能靠手写
种子 SQL。因此注册（`kudos.ability.security.point-registration.enabled`，**默认 true**）
独立于强制（默认 false）：注册一个权限点不授予任何人任何权限，只是让动作可被命名。

启动时两个来源合并：Spring 的 `RequestMappingHandlerMapping` 里的每个端点（按命名约定
推导编码），加上 `IPermissionPointContributor` 声明的点。**显式声明胜过推导**。

推导的代价要说清楚：**推导出的编码跟着路径走**，改了端点路径就等于换了编码，旧编码上的
授权不会报错，只会悄悄不再匹配。所以一旦某个编码被真正授出去，就该用
`@RequiresPermission` 把它**钉住**，此后路径可以自由移动。注册表标记了哪些行是推导来的,
让这件事可见，而不是靠口口相传。

**装配上刻意不用 `@ConditionalOnBean`**：store 由另一模块的组件扫描贡献,而这个条件在自动配置处理阶段求值——一个 Spring 明确警告不要依赖的顺序问题。它一旦判负,注册器根本不被创建、且不打任何日志,权限点永不注册,而 URL 强制随后会把每个端点都当成"未注册"拒掉。为求整洁而保留一个"静默空操作、下一个开关一开就变成全面停机"的失效模式并不划算,因此改为始终创建 Bean、运行时用 `ObjectProvider` 判空(store 缺失则记一条日志跳过)。

落库侧（`sys_resource`）只维护 `url`；`name`/`icon`/`order_num`/`parent_id`/`active`
一律不覆盖——那些是权限在控制台的**菜单投影**，属于运营者，启动扫描每次重写会在每次发布时
抹掉人家的整理工作。**永不删除**：消失的权限点可能仍带着授权，滚动发布期间它在别的实例上
也还活着；退役一个权限点是一次明确的管理动作。

## 12. 非目标（显式不做）

- **不引入外部策略引擎/策略 DSL**（Casbin / OPA / XACML 实现）：决策代数 + condition
  hook + 决策 SPI 已覆盖其高频场景；通用表达式引擎带来的是不可解释与不可控的求值成本。
- **不做 ReBAC 关系图模型**（Zanzibar 式）：实例授权表覆盖点对点分享；图遍历式授权
  超出管理后台框架的问题域。
- **不做数字 level 比大小**（`role.level` 式）：与角色树/权限集形成三份真相，必然漂移。
  强弱判定只有一种：权限集包含。
- **不做资源粒度转授**：转授只到角色粒度；需要更细先 `copyRole` 拆小角色再授。
- **不给组建树**：理由见 §2；层级语义归数据范围维度。
- **不做字段脱敏引擎**：理由见 §8。

## 13. 演进路线

| 序 | 事项 | 性质 | 依赖 |
|---|---|---|---|
| 1 | §9 三个撤销缺陷修复 | bug 修复 | 无 |
| 2 | **模型奠基**：权限编码化（`auth_role_resource` → 权限编码绑定，+`effect`/`condition` 列）、`principal_type`、组时间窗字段 | schema 先行 | 无 |
| 3 | §4 统一 `IAuthGrantPolicyService`，五写入点接入（SoD/租户/存在性对齐） | 补洞 | 无 |
| 4 | §1/§5 决策入口 + `PermissionEnforcementFilter` + 注解执行点，默认拒绝；权限版本号 | 架构补全 | 2 |
| 5 | §6 转授（授权链落库 → canGrant → 级联回收） | 新能力 | 3、4 |
| 6 | §7 审批人校验 + §10 审计事件、反向解释端点 | 加固 | 4 |
| 7 | §8 数据范围多维化 + DAO 行过滤；实例授权表 | 能力扩展 | 2、4 |
| 8 | §11 各 SPI 抽取成型 | 框架化 | 4–7 |
| 9 | 控制台对齐：角色表单转授上限/审批开关、转授对话框、回收影响面预览、权限解释 | 管理端 | 5、6 |

其中 2 是新增的关键一步：**schema 决定了框架未来十年能表达什么**，趁存量数据少时
把模型定完备，比任何功能都优先。

---

# 二、当前实现

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
│   ├── datascope/                       数据权限（auth_role_org + 解析服务）
│   ├── exclusion/                       SoD 互斥规则
│   ├── grant/                           角色授予审批流
│   ├── temporal/                        临时授权（时间窗 + 过期清理调度）
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
- **决策与范围两个热缓存**：`PermissionGrantsByUserIdCache`（功能权限，判定路径）与
  `DataScopeByUserIdCache`（数据范围，**查询路径**）。后者不可省：行过滤是每次 `select` 都问一次，
  未缓存时每个受限用户的每条业务查询都要重跑机构子树展开和 CUSTOM 授权查询——而这份开销从开影子
  模式那一刻就开始付，恰好打在本模块建议大家先迈的那一步上。
- **空结果同样缓存**(A11):决策路径上的 `PermissionGrantsByUserIdCache` 与 `RoleIdsByUserIdCache` 去掉了 `unless = result.isEmpty()`。
  "这个主体什么都没有"是一个稳定的答案,和别的答案受同一批事件失效;把它排除在外看着像省事,实则让**每请求必然回源**,而键空间本就被已认证主体数量框住,缓存它的代价不比缓存正结果高。
  故障演练实测:一个这样的主体产生了 8 次 `auth_role_user` 查询;Redis 一停,他们立刻 500,而有权限者靠缓存照常 200。
  管理/菜单类聚合缓存(`ResourceIdsBy*`、组相关)保留原样——它们不在每请求路径上,访问频次与风险都不同。
- **`DataScopeByUserIdCache` 是全模块唯一设 TTL 的缓存**（300s），这是有意的：其余缓存的输入
  全是本模块自己的表、每次写入都发事件，所以能穷尽失效；而它的输入还包括应用通过
  `IScopeDimensionProvider` 接进来的维度——那些表本模块根本不知道，写入自然也观察不到。
  监听器精确覆盖 kudos 自己的每一个输入，TTL 兜住它结构上覆盖不了的那部分。应用若要自己的维度
  即时失效，从自己的写路径按名字踢掉这个缓存即可。

变更走 [Spring `@TransactionalEventListener`](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)
`AFTER_COMMIT` 阶段触发。事件类型：

| 事件 | 含义 |
|---|---|
| `AuthRoleInserted/Updated/Deleted` | 单条角色变更 |
| `AuthRoleBatchDeleted` | 批量删除，附 `(id, tenantId, code)` snapshot——AFTER_COMMIT 行已删除，无法回查 |
| `AuthRoleUserRelationsChanged` | 用户↔角色绑定/解绑（含 userIds） |
| `AuthRoleResourceRelationsChanged` | 角色↔资源绑定/解绑 |
| `AuthGroupRoleRelationsChanged` | 组↔角色变更 → 组下所有用户的有效角色失效 |
| `AuthGroupUserRelationsChanged` | 用户↔组变更（直接进出组，也含成员时间窗到点扫描的播报） |
| `AuthRoleScopeChanged` | 角色的数据范围授权值变更；刻意不复用 `AuthRoleUpdated`——那会为修一个缓存而冲掉三个 |
| `PrincipalTokenEpochBumped` | 管理员强制某主体所有令牌失效；刻意独立，因为它恰恰是"权限没变"的那种情况 |
| `ExternalRoleSourceChanged` | **由应用发布**：外部角色来源（岗位等）的答案变了。本模块无法观察它不知道的表，这是唯一能让变更及时生效的机制 |

**为什么 BatchDeleted 要带 snapshot**：`AuthRoleHashCache.syncOnBatchDelete` 不需要，
但 `UserIdsByTenantIdAndRoleCodeCache.on(BatchDeleted)` 需要 `(tenantId, code)` 才能算 cache key——
事件发布在 AFTER_COMMIT 后，DB 行已不存在，必须提前 snapshot。

注意：`ResourceIdsByUserIdCache` 目前**只**监听关系类事件，不监听 `AuthRoleUpdated`——
这正是 §9 A1/A2 的根因，修复时补订。

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

## 其他已知限制

（撤销类缺陷见 §9。以下为其余工程问题。）

- ❗ **`@TransactionalEventListener(fallbackExecution = true)`**：意味着即使没有外层
  事务，监听器也会触发——但 AFTER_COMMIT 语义就被破坏了。当前 Service 已在大部分入口标
  `@Transactional`，但 `getRoleIds` 等 readOnly 路径触发的 evict 在无事务下会同步执行
- ⚠️ **`copyRole` 对既有绑定重新准入,这是有意保留的行为**:复制时它把源角色的权限绑定当作**新绑定**
  送进 `screenPermissionBindings`,于是任何早于规则的历史绑定(例如资源与角色分属不同子系统)都会
  让复制失败,而不是照搬现状。由此产生一个不对称:**同一组配对作为已存储数据合法、作为新写入非法**,
  而复制是唯一把存储数据重新变成写入的操作。

  **保持现状的理由**:`auth_role_resource` 每多一行都是一条新授权,本模块的前提是所有写入收敛到
  同一道闸门;给"但这是复制"开例外,正是策略服务要防的"第六条写入路径"。跳过校验看似无害
  (复制那一刻没人权限变宽,新角色持有者集为空),实则是条**静默的洗白通道**:直接创建不出来的
  绑定组合,可以经复制得到一个看起来合规的新载体,而后续"角色→用户"的绑定校验并不回头复查
  "角色→资源"。

  **代价要认**:存在历史数据的租户上复制功能不可用,且失败是全有全无的。若将来要缓解,建议走
  本类 `batchBindUsers` 已有的表达方式——复制合规部分、逐条报告跳过项(需把 `copyRole` 的返回值
  从 `String` 改为带明细的 VO),而不是放宽闸门。

  **测试覆盖缺口**:合并时把 fixture 里那组违规配对(`ams` 角色绑另一子系统资源,原是直接灌 SQL
  绕过闸门造出来的)改成一致,所以现在覆盖的是"资源合规时复制成功";**源角色带违规绑定的情况
  没有测试**。
- **实例分享的授权粒度是端点级,不是资源级**(设计边界,非缺陷):`share`/`unshare` 由 URL 强制的
  权限点把守——持有该权限点的管理员可以分享/撤回**任何**实例的授权,没有"只有分享者或资源属主
  可撤回"的细粒度检查。这是有意的:资源归属是应用领域知识(谁"拥有"一份文档),本模块不持有;
  应用要细粒度控制时,在自己的服务层调 `decide(资源, 实例)` 先问一次即可。撤回是软删除,事后可追责。
- ❗ **测试套件存在顺序依赖**:62 个测试类共用一个 Redis 容器,数据库改动随事务回滚而缓存不会。
  `ResourceIdsByUserIdCacheTest` 在一次全量运行中失败、单独运行与再次全量运行均通过。
  写涉及缓存的测试时,必须自行驱动失效(见各缓存测试的既有写法),不能依赖前序状态。
- ❗ **`screenReparent` 与并发绑定之间存在同族写偏斜**(A9/A16/A17 的第四个成员,未修):
  校验"挂到新父角色不会让现有持有者违反 SoD"读的是持有者集合的快照,与并发的授权写入之间没有共同的锁。
  修复需要对**全体持有者**加锁(可能成千上万),代价与收益不成比例——挂父角色是罕见的管理动作,
  且窗口要求 reparent 与"恰好制造冲突的授权"同瞬发生。记录在此:若它成为真实威胁,
  正确修法是锁角色行(`AuthRoleDao.lockRole`)并让 `screenGrants` 对涉及的角色也取同一把锁,而不是锁持有者。
- ✅ **数据源连接超时已从 30s 改为 3s**(全仓库 25 个 application.yml,原值是显式抄写的 HikariCP
  默认值 30000)。这是停机演练里那些 30 秒的唯一来源,也是唯一能同时约束"过滤器之前、之内、之后"
  三段的旋钮:连接池要等 30 秒,说明数据库已经不在了,再等只是把请求线程耗光。3s 对本仓库的
  testcontainers H2 依然绰绰有余;部署到远端数据库时按网络实情复核这个值。
- ✅ **审计日志已在管理控制器层接通**(`@WebAudit` 切面):角色绑定/解绑/转授/回收、权限绑定、
  实例分享、租户引导、令牌吊销原已覆盖;本轮补齐 17 个漏网写端点——数据范围(3)、审批流(4)、
  用户组成员与角色(6)、角色启停/复制(2)、时间窗授权(2)。未注解的仅剩 3 个读形态 POST
  (`getDeleteImpact`×2、`roleNamesByResourceIds`)和 1 个内存诊断(`rowScopeFindings` clear),
  皆有意为之。**边界要说清**:审计挂在 admin API 的 web 切面上,绕过控制器直接调 core 服务的
  写路径(内部调用、未来的新入口)不产生审计——服务层审计仍是 §10 的规划项
- ❗ **跨服务调用未做并发限流**：`UserAccountHashCache.getUsersByIds(userIds)` 在 userIds
  极大时（如某个组有 10w 用户）会让 user-service 瞬时压力激增
- ❗ `AuthRoleUserService.batchBind` / `unbindUser` 返回的是关系数 / Boolean——没有把
  失败原因（资源不存在 / 角色已禁用 / 跨租户）区分给调用方（§4 的逐人失败明细将覆盖）

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
