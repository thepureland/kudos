package io.kudos.ms.auth.core.role.event

/**
 * 角色-资源关系（`auth_role_resource`）领域事件。Join 表，影响两条缓存投影：
 * - ResourceIdsByRoleIdCache：按 roleId 查 resourceIds
 * - ResourceIdsByUserIdCache：按 userId 查 resourceIds（通过角色聚合，需失效）
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface AuthRoleResourceEvent

/** 一组角色-资源关系变更（绑定或解绑）；listener 按 roleId 失效缓存。 */
data class AuthRoleResourceRelationsChanged(
    val roleId: String,
    val resourceIds: Collection<String>,
) : AuthRoleResourceEvent
