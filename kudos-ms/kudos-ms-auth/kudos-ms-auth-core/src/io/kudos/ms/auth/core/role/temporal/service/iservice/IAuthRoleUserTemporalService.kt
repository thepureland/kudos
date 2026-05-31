package io.kudos.ms.auth.core.role.temporal.service.iservice

import io.kudos.ms.auth.common.temporal.vo.response.RoleTemporalGrantRow
import java.time.LocalDateTime

/**
 * Temporal (时效性) role-grant business: time-bound role assignments that auto-expire.
 *
 * Effective-now filtering happens in [io.kudos.ms.auth.core.role.dao.AuthRoleUserDao] (the
 * permission-resolution queries exclude out-of-window grants); this service manages the windows
 * and drives expiry via a purge sweep that deletes lapsed grants and evicts the affected users'
 * caches.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IAuthRoleUserTemporalService {

    /**
     * Returns all grant rows for a role (including past, future and permanent grants) so the admin
     * can see who holds the role and manage the time windows.
     *
     * @param roleId the role whose grants to list
     * @param now the instant to evaluate the active flag against (defaults to current time)
     * @return grant rows sorted by userId then by startTime (nulls last)
     */
    fun getGrantsByRoleId(roleId: String, now: java.time.LocalDateTime = java.time.LocalDateTime.now()): List<RoleTemporalGrantRow>

    /**
     * Grant [roleId] to [userId] with an optional validity window (replace semantics — any existing
     * grant for the same pair is superseded). NULL [startTime] ⇒ effective immediately; NULL
     * [endTime] ⇒ never expires. Publishes a relations-changed event so caches recompute.
     *
     * @throws IllegalArgumentException if both bounds are set and [startTime] is after [endTime]
     * @return the new grant's id
     */
    fun bindTemporal(roleId: String, userId: String, startTime: LocalDateTime?, endTime: LocalDateTime?): String

    /**
     * Delete every grant whose validity window has already ended and evict the affected users'
     * caches. Intended to be invoked periodically by a scheduler (the reaper for temporal grants).
     *
     * @return the number of expired grants purged
     */
    fun purgeExpired(): Int

}
