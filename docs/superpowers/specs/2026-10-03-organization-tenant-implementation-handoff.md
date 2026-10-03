# Kudos 组织共享账号与租户权限改造交接

日期：2026-10-03。状态：按 soul 的组织授权决定 G-1～G-19 重做中，工作区未提交。规则见[后端设计](2026-10-03-organization-tenant-identity-permission-design.md)与[控制台设计](../../../../kudos-console-ui/webApp/docs/ORGANIZATION_TENANT_PERMISSION_DESIGN.md)。

## 与上一版的关系

上一版半成品（权限键级 SET/REMOVE、本站数据范围覆盖、BLOCK_ACCESS、默认即切到组织路径、迁移回填旧数据）已按用户确认整体丢弃。kudos 后端恢复到提交 `6e3ddb040` 后重写，备份在 `kudos/build/org-wip-backup/`（`kudos-wip.patch`、`kudos-wip-untracked.tgz`，控制台同名两份）。复用了其中的 VO 字段、Redis 主体会话索引、账号级撤销、切站骨架和 JWT 声明。

## 实现要点

| 范围 | 做法 | 入口 |
|---|---|---|
| 模式开关 | `kudos.ms.organization.enabled`，默认关；启动检查与单向标记 `sys_organization_mode`；平台 tenant 取 `kudos.ms.auth.authz.platform-admin-tenant-ids` | sys-core `organization/` |
| 归属 | user：组织行 `tenant_id=''`、`organization_id` 必填、`owner_key` 生成列保唯一；auth：角色/组/SoD 的 `tenant_id` 存组织 id | `UserTenantAccessGuard`、`TenantAdministrationGuard` |
| 管理规则 | G-6～G-17 集中在 `OrganizationAdministrationPolicy`，同时实现 user 的 `IOrganizationMemberPolicy` 端口 | auth-core `organization/service/` |
| 有效角色 | `EffectiveRoleResolver`：（默认 − REMOVE）∪ ADD，展开祖先，限 tenant 启用系统 | 同上 |
| 进入 | `TenantEntryService`（G-4、G-9）；会话范围 `OrganizationSessionTargeting`（TENANT / ORGANIZATION） | 同上 |
| 决策 | `AuthzDecisionApi` 对组织账号走 `OrganizationAuthorizationResolver`，不缓存 | auth-core `platform/authz/` |
| 凭据 | `CredentialOwnerResolver`：组织账号凭据记在组织 id 下 | auth-core `authentication/credential/` |
| 接口 | `/api/admin/auth/organization/**`、`/api/auth/contexts`、`/api/auth/context/switch` | auth api-admin / api-public |
| 迁移 | sys V1.0.0.26、user V1.0.0.37、auth V1.0.0.60，均只加法 | 各 `*-sql` |

## 验证

- 开关关闭：sys、user 全量测试与 HEAD 基线一致（基线已有失败：`SysI18NServiceTest` 5 个、`UserLoginRememberMeServiceTest` 1 个）。auth 全量对比见本次会话记录。
- 开关打开：`OrganizationModeIntegrationTest`（auth-core，真实 Bean + H2）覆盖 K-1～K-7 与修订号冲突。
- 测试共用本机 Redis 容器，必须 `--no-parallel --max-workers=1` 串行跑，否则出现 `ERR no such key` 的假失败。

## 已知限制与待办

1. 组织模式授权首版不缓存，每个受控请求都查库计算；性能需在 shadowDemo 实测后再决定缓存方案。
2. 无请求主体的内部决策（RPC 代判）对组织账号一律拒绝。
3. 外部身份（OAuth2、邮箱 OTP）绑定仍按 Provider 实例（tenant）一份；跨 tenant 共用一个绑定需另行设计身份域。
4. 组织管理员异动只发布 `OrganizationAdministratorChanged` 事件，尚未接通知模块。
5. 用户组成员与组角色的变更没有"重新取得进入资格"预览，预览只在成员默认角色编辑里提供。
6. 旧模式下 `user_org_user` 仍无同租户校验（基线即如此，组织数据已校验）。
7. 控制面路由（仅组织模式）拒绝在已打开的 tenant 库事务里调用 sys/user/auth 服务。业务模块需要在 tenant 事务之外取身份与授权，或改走远程客户端；这是有意的失败即关闭。
8. 开关关闭时仍有几处无害的差异：启动时读一次 `sys_organization_mode`（需要先跑迁移）；请求带 `X-Kudos-Context-Version` 且与会话不符时返回 409（旧控制台不带此头）；每个会话都写一份 Redis 主体索引。
9. 会话、凭据的管理端点仍按"操作者 tenant = 目标 tenant"比较，组织账号的会话与凭据暂时只能由平台管理。
10. shadowDemo 已在开启组织模式时启动成功（启动检查通过、组织接口权限点完成登记、未认证请求返回 401）；登录态的正例需要 api-public 一起起，尚未活体打流量。
