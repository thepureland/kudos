# Kudos 组织共享账号与租户权限设计

状态：设计已定，实施中（见[实施交接](2026-10-03-organization-tenant-implementation-handoff.md)）。

日期：2026-10-03。本版按 soul 的组织授权决定 G-1～G-19 重写（来源：`soul-workspace/work/items/SOUL-2026-0060-organization-tenant-design/architecture.md` 与 `SOUL-2026-0072`），取代此前“按权限键 SET/REMOVE、本站数据范围覆盖、BLOCK_ACCESS、自动继承、第一期含迁移”的版本。[控制台交互设计](../../../../kudos-console-ui/webApp/docs/ORGANIZATION_TENANT_PERMISSION_DESIGN.md) 使用同一套规则。

## 目标

同一客户组织持有多个 tenant。人员、账号、部门、用户组与角色在组织内维护一次；同一账号在旗下各 tenant 的权限默认相同，个别 tenant 不同时只记录差异。tenant 保留业务数据与运行配置隔离。

合成场景：组织甲持有 tenant A、B、C。小王的默认角色是“使用者管理员”“报表查看”，在 B 的覆盖是移除“使用者管理员”，所以在 A、C 有两个角色，在 B 只有“报表查看”。组织之后关联 tenant D，D 开放前无人能进入；开放后小王按默认取得两个角色。

## 已采纳的决定

| 编号 | 决定 | kudos 落地 |
|---|---|---|
| G-1 | 覆盖以角色为单位、针对个人 | `auth_tenant_role_override`：成员 × tenant × 角色 × ADD/REMOVE。不提供单权限、角色内容或数据范围的 tenant 覆盖 |
| G-2 | 新 tenant 先不开放 | `sys_tenant.organization_open` 默认 false；关联组织后为未开放 |
| G-3 | 覆盖只记差异 | 有效角色 =（默认角色 − REMOVE）∪ ADD；未覆盖部分持续跟随默认 |
| G-4 | 不另设进入资格 | 某 tenant 内对已启用系统的有效业务角色为空即不能进入，切换清单不显示 |
| G-5 | 数据范围跟随角色 | 数据范围仍是角色定义的一部分，没有 tenant 差异版本 |
| G-6 | 组织管理员全权；另设组织权限管理员 | `auth_organization_admin` 指定；组织权限管理员是内置管理角色，不具业务权 |
| G-7 | 分级管理 | 内置 tenant 权限管理员，只能设定管辖 tenant 的覆盖 |
| G-8 | 开放权 | 组织管理员与组织权限管理员可开放／改回未开放，先预览后执行，写审计 |
| G-9 | 未开放即不能进入 | 包括组织管理员 |
| G-10 | 组织管理员的指定 | 平台或现任组织管理员可指定／撤销；不能撤销自己，不能撤销最后一位；每次异动写审计并发出通知事件 |
| G-11 | 管理角色逐级授予 | 组织管理员可授予两种管理角色；组织权限管理员只能授予 tenant 权限管理员 |
| G-12 | 不能改自己持有的角色 | 组织权限管理员不能修改自己当前持有的业务角色定义 |
| G-13 | 首版只支持全新系统 | 迁移只做加法，不回填旧数据；既有数据迁移日后在框架外提供 |
| G-14 | 新旧模式以设定切换 | `kudos.ms.organization.enabled`，默认 false，关闭时与旧版行为一致 |
| G-15 | 不降低登录验证 | 切站仍执行目标 tenant 的 MFA 策略；组织模式不放宽任何 MFA 要求 |
| G-16 | 最后一位组织管理员 | 一般停用／删除须先有继任者；平台紧急停用立即生效，之后只能由平台补指定 |
| G-17 | 成员资格的结束权限 | 组织权限管理员只能停用／删除比自己层级低的成员 |
| G-18 | 有旧数据时拒绝开启 | 首次开启时若有平台 tenant 以外的 tenant 或其账号，启动失败；通过后写启用标记 |
| G-19 | 开关单向 | 已有启用标记而开关关闭时，启动失败 |

kudos 自行决定的补充：组织主档复用 `user_org` 根节点；组织模式下 OAuth2 只能绑定组织内已有账号，JIT 自动建号关闭；平台 tenant（`kudos.ms.auth.authz.platform-admin-tenant-ids`）在组织模式下保持旧模式身份与平台管理员判定。

## 模式开关与启动检查

| 开关 | 启用标记 | 启动结果 |
|---|---|---|
| 关 | 无 | 旧模式，所有组织路径不生效 |
| 开 | 无 | 若存在 `organization_id` 为空且不在平台 tenant 列表中的 tenant，或这些 tenant 下的账号，拒绝启动并给出数量；否则写标记 |
| 开 | 有 | 不再检查旧数据，正常启动 |
| 关 | 有 | 拒绝启动：组织模式开启后不能关闭 |

标记存于 `sys_organization_mode`（单行），由 sys 的启动检查写入；账号计数由 user 通过 sys common 的 `ILegacyModeDataDetector` 端口提供。错误只给数量和处理方式，不列帐号内容。之后“旧数据不得放行”由运行期保证：组织模式下非平台 tenant 必须有组织归属，账号必须属于组织，否则授权失败。

组织模式下，`organization_id` 为空的非平台 tenant 视为“未关联”，任何人都不能进入；不能再在非平台 tenant 下创建 tenant 归属的旧式账号。

## 数据模型

| 概念 | 落点 | 约束 |
|---|---|---|
| 组织 | `user_org` 根节点：`node_kind=ORGANIZATION`、`organization_id=id`、`parent_id=null` | 只有平台能创建、停用；根不能移动成别的节点的子节点 |
| 部门 | `user_org` 子节点：`node_kind=DEPARTMENT`、`organization_id=根id` | 父子同组织，不成环；名称在组织内唯一 |
| tenant 归属 | `sys_tenant.organization_id`、`sys_tenant.organization_open` | 一个 tenant 同时只有一个组织；关联、解除关联只能由平台执行；普通 tenant 更新不能改归属 |
| 成员 | `user_account.organization_id` | 用户名在组织内唯一；账号即组织成员资格，停用或删除即结束成员资格 |
| 组织角色 | `auth_role.tenant_id` 存组织 id（所有者），`organization_id` 同值标记 | 沿用原 (所有者, 编码) 唯一键；角色继承、SoD 规则同所有者 |
| 用户组 | `auth_group.tenant_id` 存组织 id，`organization_id` 同值 | 组成员、组角色同组织 |
| 默认角色 | `auth_role_user.organization_id` 非空的授予 | 组织级授予与组派生角色都是默认角色，适用于所有已开放 tenant |
| tenant 覆盖 | `auth_tenant_role_override` | (tenant, 主体, 角色) 唯一；动作 ADD 或 REMOVE；tenant 必须属于同一组织 |
| 组织管理员 | `auth_organization_admin` | (组织, 账号) 唯一；记录指定者与时间 |
| 管理角色 | `auth_management_role_grant` | 种类 ORG_PERMISSION_ADMIN 或 TENANT_PERMISSION_ADMIN；后者每行一个管辖 tenant |
| 授权修订号 | `auth_organization_revision` | 组织内任何授权事实变化都在同一事务递增 |
| 审计 | `auth_organization_audit` | 操作者、动作、对象、前后差异、原因 |

唯一约束对旧数据保持原语义：
- user：`user_org`、`user_account` 的组织行 `tenant_id` 为空串，增加生成列 `owner_key`（组织行 `ORG:<id>`，旧行 `TENANT:<tenantId>`），以它替换原 tenant 唯一键；旧行约束效果不变。
- auth：角色、组、SoD 规则的 `tenant_id` 本来就是"所有者"，组织行直接存组织 id（UUID，不会与 tenant id 冲突），原唯一键和同所有者校验原样适用。`PrincipalFacts.tenantId` 对组织账号同样给出组织 id。

迁移版本：sys `V1.0.0.26`、user `V1.0.0.37`、auth `V1.0.0.60`，全部只加列、加表、换等价唯一键，不回填。凭据表不加列（见下文）。

## 有效角色与进入

成员 m 在 tenant t、子系统 s 的有效业务角色：

> 有效角色(m, t, s) =（默认角色(m, s) − REMOVE(m, t)）∪ ADD(m, t, s)，再按角色继承展开祖先，去掉停用角色

- 默认角色 = 未撤销、在时间窗内的组织级直接授予 ∪ 有效组的组角色 ∪ 扩展角色来源 SPI 的贡献。
- 覆盖作用于成员被分配的角色（展开前），不作用于展开得到的祖先。
- REMOVE 一个不在默认中的角色、ADD 一个已在默认中的角色，当下不改变结果，系统标示为“目前无作用”，不自动删除；默认日后变化时它们会生效。
- 默认移除后再加回，原有 REMOVE 仍有效。
- 同一成员、tenant、角色不能同时有 ADD 和 REMOVE（唯一键保证），改动作即替换。
- SoD 按每个 tenant 的有效角色检查；任何会在某个已开放或未开放 tenant 产生冲突的默认或覆盖变更都被拒绝。

一般成员进入 tenant t 的子系统 s，必须同时满足：账号有效且属于组织 O；组织 O 有效；t 有效、属于 O 且已开放；s 已对 t 启用；有效角色(m, t, s) 不为空。组织管理员替代最后一项：在已开放 tenant 的已启用系统内全权，数据范围为全部。管理角色不计入，不会让成员出现在切换清单中。

tenant 启用的子系统是上限：tenant 未启用的系统，任何角色、覆盖或组织管理员都开不出来。

G-4 的已知风险：被移除全部角色的成员，可能因默认增加角色而重新取得进入资格。修改默认角色时的预览单独列出“将重新取得某 tenant 进入资格”的成员，确认后才保存。

## 管理身份

| 身份 | 取得 | 能做 | 不能做 |
|---|---|---|---|
| 组织管理员 | 平台或现任组织管理员指定 | 全部组织管理；开放／改回未开放；指定、撤销其他组织管理员；授予两种管理角色；已开放 tenant 内全权 | 超出 tenant 启用系统；跨组织；平台操作 |
| 组织权限管理员 | 组织管理员授予 | 管理成员（停用／删除只限更低层级）、部门、角色定义、组、默认角色、所有 tenant 的覆盖；开放／改回未开放；授予 tenant 权限管理员 | 业务操作；授予组织权限管理员；指定组织管理员；改自己持有的角色定义 |
| tenant 权限管理员 | 组织管理员或组织权限管理员授予，指定管辖 tenant | 在管辖 tenant 内为成员 ADD/REMOVE 既有业务角色 | 改默认、角色定义、成员；管辖外 tenant；授予任何管理角色 |

层级：组织管理员 > 组织权限管理员 > tenant 权限管理员 > 一般成员。共同约束：

1. 任何管理者不能修改自己的默认角色、覆盖、管理角色或组织管理员指定。
2. 管理角色只能由更高层级授予与撤销；组织管理员之间的指定不受此限。
3. 不能撤销最后一位组织管理员，也不能停用或删除最后一位组织管理员的账号；平台的紧急停用例外，此时组织进入无组织管理员状态，只有平台能补指定。
4. 组织权限管理员不能停用、删除组织管理员或其他组织权限管理员。
5. 所有异动写 `auth_organization_audit`；组织管理员异动额外发布 `OrganizationAdministratorChanged` 事件，由通知模块订阅发给平台与其他组织管理员。

运行期判定：
- 组织管理员在 TENANT 范围的决策直接放行（原因 `ORGANIZATION_ADMIN`），前提是进入条件除角色外全部满足。
- ORGANIZATION 范围只放行组织管理端点（`user:account:*`、`user:org:*`、`auth:role:*`、`auth:group:*`、`auth:organization:*` 等，见 `OrganizationManagementPermissions`）；细粒度规则在服务层由 `OrganizationAdministrationPolicy` 执行。
- user 的账号与部门写接口通过 user common 的 `IOrganizationMemberPolicy` 端口调用 auth 的策略，user core 不依赖 auth。

## tenant 关联与开放

- 平台在创建 tenant 时或之后关联组织，状态为未开放；未开放时可以预设覆盖，但任何人都不能进入。
- 开放、改回未开放由组织管理员或组织权限管理员执行。预览给出各角色将取得（或失去）权限的成员；执行写审计并递增修订号。改回未开放后所有业务请求被拒，覆盖保留。
- 解除关联由平台执行：删除该 tenant 的全部覆盖和 tenant 权限管理员管辖（审计保留），tenant 回到未关联、未开放；之后可关联到其他组织。

## 登录、会话与请求范围

组织模式下，登录仍以登录 tenant 定位组织，再按 (组织, 用户名) 认证共享账号；平台 tenant 沿用旧登录。登录后会话目标为下列之一：

| 范围 | 会话字段 | 条件 |
|---|---|---|
| TENANT | organizationId、tenantId、subSystemCode | 满足上节进入条件 |
| ORGANIZATION | organizationId、tenantId 为空、subSystemCode 为组织管理系统 | 组织管理员或持有任一管理角色 |

登录 tenant 不满足进入条件时：有管理身份则进入 ORGANIZATION 范围，否则登录失败。`GET /api/auth/contexts` 返回可用范围：ORGANIZATION（如有）以及可进入的 tenant 与系统；`POST /api/auth/context/switch` 在服务端重新验证并签发新会话，执行目标 tenant 的 MFA 策略，旧标签页以 `X-Kudos-Context-Version` 拒绝过期提交。

请求的组织／tenant 只是候选范围，每次请求在服务端由身份、tenant 归属、开放状态、有效角色、组织管理员指定与系统启用重新验证。不以 organizationId 取代 `KudosContext.tenantId` 或数据源 tenant。组织、账号、凭据和授权配置使用固定控制面数据源（`OrganizationControlPlaneRoutingConfiguration`）。

## 撤销与缓存

组织模式的有效授权首版不缓存：每次受控请求从权威表计算，结果版本为修订号、主体 epoch 与计算结果的摘要。以下变化递增组织修订号：默认角色、组成员、组角色、角色定义（含权限与数据范围）、覆盖、组织管理员、管理角色、tenant 归属与开放状态、tenant 启用系统。账号停用、改密等身份事件沿用主体 epoch 与全站会话撤销。

撤销承诺：提交后开始的授权检查使用新结果；已经在途的请求按其已验证结果完成。

## 凭据与外部登录

- 组织账号的密码、密码历史、TOTP、恢复码、WebAuthn 归账号所有：凭据表的 `tenant_id` 存组织 id，不论从组织哪个 tenant（或组织范围）发起，都由 `CredentialOwnerResolver` 换算到同一处；改密一次影响全部 tenant。旧账号仍按 tenant 存放，行为不变。
- 恢复码散列与 WebAuthn userHandle 对组织账号使用组织 id（全新系统无兼容负担）；MFA 策略仍按当前 tenant 取。
- OAuth2 与邮箱 OTP 的绑定沿用每个 Provider 实例（tenant）一份，可以指向组织账号；组织模式下非平台 tenant 的 JIT 自动建号关闭，未绑定的外部身份登录失败。
- 账号级安全事件撤销该账号在所有 tenant 的会话。

## 接口

| 操作 | 入口 |
|---|---|
| 可用范围 / 切换 | `GET /api/auth/contexts`、`POST /api/auth/context/switch` |
| 组织概览 | `GET /api/admin/auth/organization/overview`、`/roleCatalog` |
| 组织（平台） | `/api/admin/user/org` 以 `nodeKind=ORGANIZATION` 创建根节点；`/api/admin/auth/organization/admin/assign`（平台指定首位组织管理员同一入口） |
| tenant 关联（平台） | `POST /api/admin/auth/organization/tenant/associate`、`/dissociate` |
| tenant 开放 | `POST /api/admin/auth/organization/tenant/preview`、`/setOpen` |
| 成员角色 | `POST /api/admin/auth/organization/member/read`（默认角色、各 tenant 覆盖、有效结果与来源标签）、`/previewDefaults`、`/saveDefaults`、`/saveOverride`、`/removeOverride` |
| 组织管理员 | `POST /api/admin/auth/organization/admin/list`、`/assign`、`/revoke` |
| 管理角色 | `POST /api/admin/auth/organization/managementRole/list`、`/grant`、`/revoke` |

写操作带 `expectedRevision`，修订号不一致返回 `AUTHZ_REVISION_CONFLICT` 与当前修订号；所有写操作写审计。

## 验收矩阵

| 编号 | 情境 | 通过条件 |
|---|---|---|
| K-1 | 小王默认两个角色，在 B 移除“使用者管理员” | A、C 有两个角色，B 只有“报表查看”；决策按目标 tenant |
| K-2 | 组织甲读写组织乙的成员、角色、覆盖或 tenant | 全部拒绝 |
| K-3 | 默认增加角色；默认移除后加回被覆盖的角色；目前无作用的覆盖 | B 也取得新角色；B 的 REMOVE 仍有效；无作用覆盖被标示 |
| K-4 | D 关联后未开放、预设覆盖、开放、改回未开放、解除关联 | 未开放时含组织管理员都被拒；开放后按默认与覆盖生效；解除关联后覆盖与管辖清除 |
| K-5 | 小王在 C 的角色全部移除，之后默认增加角色 | C 不在切换清单且请求被拒；预览列出“重新取得 C 进入资格” |
| K-6 | 三种管理身份的可做与不可做 | 按“管理身份”一节逐条成立，含自我修改、最后一位、层级停用 |
| K-7 | 只有管理角色的成员 | tenant 切换清单为空，只能进入 ORGANIZATION 范围；业务请求被拒 |
| K-8 | 默认、覆盖、角色定义、开放状态变化 | 下一次请求使用新结果，修订号递增 |
| K-9 | 开关关闭（默认） | 现有测试全部通过，行为与旧版一致 |
| K-10 | 空库开启、有旧数据开启、已启用后关闭 | 正常启动并写标记；后两者拒绝启动 |
| K-11 | 组织账号改密、TOTP、恢复码、WebAuthn | 在组织任一 tenant 登录都使用同一份凭据 |
| K-12 | 组织模式下 OAuth2 首次登录 | 未绑定时拒绝且不建号；已绑定时登录到组织账号 |
