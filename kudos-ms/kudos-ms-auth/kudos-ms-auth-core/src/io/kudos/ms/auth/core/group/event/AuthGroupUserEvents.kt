package io.kudos.ms.auth.core.group.event

/**
 * Group-user association (`auth_group_user`) domain events. Join table affecting two cache projections:
 * - UserIdsByGroupIdCache: lookup userIds by groupId
 * - GroupIdsByUserIdCache: lookup groupIds by userId
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface AuthGroupUserEvent

/** A batch of group-user association changes (bind or unbind); listeners invalidate caches on both groupId and userIds sides. */
data class AuthGroupUserRelationsChanged(
    val groupId: String,
    val userIds: Collection<String>,
) : AuthGroupUserEvent
