package io.kudos.ms.auth.core.role.event

/**
 * 角色-用户关系（`auth_role_user`）领域事件。Join 表，影响三条缓存投影：
 * - UserIdsByRoleIdCache：按 roleId 查 userIds
 * - RoleIdsByUserIdCache：按 userId 查 roleIds
 * - ResourceIdsByUserIdCache：按 userId 查 resourceIds（通过角色聚合，需失效）
 *
 * 关系变更同时影响 roleId 一侧和涉及的 userIds 一侧。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface AuthRoleUserEvent

/** 一组角色-用户关系变更（绑定或解绑）；listener 按 roleId / userIds 三向失效缓存。 */
data class AuthRoleUserRelationsChanged(
    val roleId: String,
    val userIds: Collection<String>,
) : AuthRoleUserEvent
