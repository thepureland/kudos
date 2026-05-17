package io.kudos.ms.user.core.account.event

/**
 * 用户-机构关系（`user_org_user`）领域事件。Join 表，两条缓存投影：
 * - UserIdsByOrgIdCache：按 orgId 查 userIds
 * - OrgIdsByUserIdCache：按 userId 查 orgIds
 *
 * 关系变更通常同时影响 orgId 一侧和涉及的 userIds 一侧。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface UserOrgUserEvent

/** 一组用户-机构关系变更（绑定或解绑）；listener 按 orgId / userIds 双向失效缓存。 */
data class UserOrgUserRelationsChanged(
    val orgId: String,
    val userIds: Collection<String>,
) : UserOrgUserEvent

/**
 * 修改管理员标记。仅同步 `userIdsByOrgIdCache` 中该 orgId 视图
 * （缓存不含 orgAdmin 字段，但调用方约定保持一致性）。
 */
data class UserOrgUserAdminUpdated(
    val id: String,
    val orgId: String,
) : UserOrgUserEvent
