package io.kudos.ms.auth.core.group.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.common.group.vo.response.GroupMembershipVo
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import java.time.LocalDateTime


/**
 * Group-user relation service interface.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
interface IAuthGroupUserService : IBaseCrudService<String, AuthGroupUser> {


    /**
     * The user IDs whose membership of the group is **currently in force** — revoked memberships
     * and those outside their validity window are excluded. Use [listMemberships] for the
     * administrative view that shows lapsed and future-dated rows too.
     *
     * @param groupId group ID
     * @return set of user IDs
     * @author K
     * @author AI: Codex
     * @author AI: Claude
     * @since 1.0.0
     */
    fun getUserIdsByGroupId(groupId: String): Set<String>

    /**
     * The groups the user belongs to **right now** — revoked memberships and those outside their
     * validity window are excluded, because such a membership grants none of the group's roles.
     *
     * @param userId user ID
     * @return set of group IDs
     * @author K
     * @author AI: Codex
     * @author AI: Claude
     * @since 1.0.0
     */
    fun getGroupIdsByUserId(userId: String): Set<String>

    /**
     * Batch-binds a group to multiple users.
     *
     * @param groupId group ID
     * @param userIds user IDs to bind
     * @return number of bindings created
     * @author K
     * @author AI: Codex
     * @author AI: Claude
     * @since 1.0.0
     */
    fun batchBind(groupId: String, userIds: Collection<String>): Int

    /**
     * Removes a principal from a group.
     *
     * **Soft**, like every other revocation here: the row survives so "who was in this group, and
     * who took them out" stays answerable, and so a later re-add reinstates the original membership
     * instead of minting a new one. Resolution stops counting it immediately.
     *
     * @param groupId group ID
     * @param userId the member to remove
     * @param reason recorded on the row
     * @return true when a live membership was revoked
     * @author K
     * @author AI: Codex
     * @author AI: Claude
     * @since 1.0.0
     */
    fun unbind(groupId: String, userId: String, reason: String? = null): Boolean

    /**
     * Checks whether a membership **row** exists for the pair — revoked or not. Row-level existence,
     * for write-side decisions; for "is this principal a member right now", use [getUserIdsByGroupId].
     *
     * @param groupId group ID
     * @param userId user ID
     * @return true if a row exists
     * @author K
     * @author AI: Codex
     * @author AI: Claude
     * @since 1.0.0
     */
    fun exists(groupId: String, userId: String): Boolean

    /**
     * All memberships of a group with their validity windows, for the admin view.
     *
     * @param groupId group ID
     * @return the membership rows, including those not currently in force
     */
    fun listMemberships(groupId: String): List<GroupMembershipVo>

    /**
     * Adds a member with an explicit validity window, replacing any existing membership for the
     * same pair so the supplied window is authoritative.
     *
     * Screened exactly like a direct grant of every role the group carries — a temporary membership
     * is still a membership.
     *
     * @param groupId group ID
     * @param userId the member
     * @param startTime effective from; NULL = immediately
     * @param endTime effective until; NULL = never expires
     * @return the membership id
     * @throws IllegalArgumentException if the window is inverted, or the member fails admission
     */
    fun bindTemporal(
        groupId: String,
        userId: String,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
    ): String

    /**
     * Evicts the cached permission snapshots of every principal whose membership opened or closed
     * inside `(since, now]`.
     *
     * Why this is needed even though the read paths already honour the window: the resolved role
     * and resource sets are **cached**, and a cache entry computed while a membership was in force
     * keeps granting the group's roles until something evicts it. Nothing in the system does — the
     * database row did not change when the clock passed `end_time`, so no write event fires. This
     * sweep is that event.
     *
     * Unlike the role-grant purge, no rows are deleted. A lapsed membership is a fact about the
     * group that the admin view is documented to show, and with the read paths filtering correctly
     * the row is inert; deleting it would buy nothing and destroy the record.
     *
     * Idempotent: re-running over the same interval only repeats evictions.
     *
     * @param since exclusive lower bound
     * @param now inclusive upper bound
     * @return the number of memberships whose window boundary was announced
     */
    fun announceWindowChanges(since: LocalDateTime, now: LocalDateTime): Int

}
