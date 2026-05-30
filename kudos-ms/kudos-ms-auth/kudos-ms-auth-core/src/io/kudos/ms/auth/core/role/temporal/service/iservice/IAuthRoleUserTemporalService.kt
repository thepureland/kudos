package io.kudos.ms.auth.core.role.temporal.service.iservice

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
