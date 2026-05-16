package io.kudos.ms.auth.core.group.event

/**
 * 组-用户关系（`auth_group_user`）领域事件。Join 表，影响两条缓存投影：
 * - UserIdsByGroupIdCache：按 groupId 查 userIds
 * - GroupIdsByUserIdCache：按 userId 查 groupIds
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface AuthGroupUserEvent

/** 一组组-用户关系变更（绑定或解绑）；listener 按 groupId / userIds 双向失效缓存。 */
data class AuthGroupUserRelationsChanged(
    val groupId: String,
    val userIds: Collection<String>,
) : AuthGroupUserEvent
