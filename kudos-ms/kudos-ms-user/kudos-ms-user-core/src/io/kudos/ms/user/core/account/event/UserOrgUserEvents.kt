package io.kudos.ms.user.core.account.event

/**
 * User-organization association (`user_org_user`) domain events. Join table with two cache projections:
 * - UserIdsByOrgIdCache: lookup userIds by orgId
 * - OrgIdsByUserIdCache: lookup orgIds by userId
 *
 * Association changes normally affect both the orgId side and the involved userIds side.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface UserOrgUserEvent

/** A batch of user-organization association changes (bind or unbind); listeners invalidate caches on both orgId and userIds sides. */
data class UserOrgUserRelationsChanged(
    val orgId: String,
    val userIds: Collection<String>,
) : UserOrgUserEvent

/**
 * Update of the admin flag. Only refreshes the orgId view in `userIdsByOrgIdCache`
 * (the cache does not include the orgAdmin field, but callers agree to keep them consistent).
 */
data class UserOrgUserAdminUpdated(
    val id: String,
    val orgId: String,
) : UserOrgUserEvent
