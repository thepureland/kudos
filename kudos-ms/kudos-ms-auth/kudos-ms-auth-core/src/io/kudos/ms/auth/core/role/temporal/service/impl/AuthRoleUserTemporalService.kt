package io.kudos.ms.auth.core.role.temporal.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.temporal.vo.response.RoleTemporalGrantRow
import io.kudos.ms.auth.core.policy.GrantCandidate
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.temporal.service.iservice.IAuthRoleUserTemporalService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


/**
 * Temporal role-grant business.
 *
 * Kept separate from [io.kudos.ms.auth.core.role.service.impl.AuthRoleUserService] (which owns the
 * SoD-checked permanent binds): windowed grants and the expiry sweep are a distinct concern, and a
 * separate bean avoids entangling the hot batch-bind path. Both share [AuthRoleUserDao].
 *
 * [bindTemporal] goes through [IAuthGrantPolicyService] like **every** other grant write — a
 * windowed grant confers the full authority of the role while active, so it faces the full gate:
 * tenant boundary, principal existence, approval requirement, duty separation, and the
 * per-principal lock. An earlier version carried its own inline subset of these checks, which made
 * this the sixth write path and the only one outside the gate — precisely the drift the policy
 * service exists to prevent (a temporal grant with a generous window was, among other things, an
 * approval-workflow bypass). Only the two *temporal* rules stay local: an existing permanent grant
 * is never silently narrowed to a window, and a delegation-chain row is never silently replaced.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthRoleUserTemporalService(
    private val dao: AuthRoleUserDao,
) : IAuthRoleUserTemporalService {

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var grantPolicyService: IAuthGrantPolicyService

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getGrantsByRoleId(roleId: String, now: LocalDateTime): List<RoleTemporalGrantRow> {
        return dao.searchGrantsByRoleId(roleId)
            .sortedWith(compareBy({ it.userId }, { it.startTime ?: LocalDateTime.MAX }))
            .map { grant ->
                val start = grant.startTime
                val end = grant.endTime
                val isActive = (start == null || !start.isAfter(now)) && (end == null || !end.isBefore(now))
                RoleTemporalGrantRow(
                    id = grant.id,
                    userId = grant.userId,
                    startTime = grant.startTime,
                    endTime = grant.endTime,
                    active = isActive,
                )
            }
    }

    @Transactional
    override fun bindTemporal(
        roleId: String,
        userId: String,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
    ): String {
        if (startTime != null && endTime != null) {
            require(!startTime.isAfter(endTime)) {
                "start_time must not be after end_time (role=${roleId}, user=${userId})."
            }
        }

        // Admission — the same gate as every other grant write: role and principal existence,
        // tenant boundary, approval requirement, duty separation. It also takes the per-principal
        // lock, which makes the row inspection below race-free against concurrent binds.
        grantPolicyService.assertNoRejection(
            grantPolicyService.screenGrants(listOf(GrantCandidate(roleId, userId, endTime = endTime))),
        )

        // unique(role_id, user_id): at most one row exists. What may be done with it is *temporal*
        // semantics rather than admission, so these two rules live here and not in the gate.
        val existing = dao.searchByRoleIdAndUserId(roleId, userId).firstOrNull()
        if (existing != null) {
            // A delegation-chain row must not be silently replaced, live or revoked: rewriting it
            // would erase granted_by / parent_grant_id, detaching the grant from the cascade that
            // revoking its operator relies on — a delegated grant laundered into a direct one.
            if (existing.parentGrantId != null || existing.grantedBy != null) {
                throw IllegalArgumentException(
                    "User $userId holds role $roleId through a delegated grant — refusing to replace " +
                        "it with a windowed one. Revoke the delegated grant first (which cascades " +
                        "properly) if a time window is intended."
                )
            }
            // A live permanent grant is never silently narrowed to a window: that would let the
            // temporal endpoint revoke-by-replacement. It must be unbound explicitly first.
            if (existing.revoked != true && existing.startTime == null && existing.endTime == null) {
                throw IllegalArgumentException(
                    "User $userId already holds role $roleId permanently — refusing to replace a permanent " +
                        "grant with a temporal one. Unbind the permanent grant first if a time window is intended."
                )
            }
            // A plain windowed (or revoked) row is updated in place, same convention as group
            // membership: the row is the relationship's audit record, and replacing it would throw
            // away when it was first granted and why it was ever revoked.
            existing.startTime = startTime
            existing.endTime = endTime
            existing.revoked = false
            existing.revokeReason = null
            dao.update(existing)
            log.debug("Temporal grant role=${roleId} user=${userId} window=[${startTime}, ${endTime}] updated in place (${existing.id}).")
            eventPublisher.publishEvent(AuthRoleUserRelationsChanged(roleId, listOf(userId)))
            return existing.id
        }

        val grant = AuthRoleUser {
            this.roleId = roleId
            this.userId = userId
            this.startTime = startTime
            this.endTime = endTime
        }
        val id = dao.insert(grant)
        log.debug("Temporal grant role=${roleId} user=${userId} window=[${startTime}, ${endTime}] id=${id}.")
        eventPublisher.publishEvent(AuthRoleUserRelationsChanged(roleId, listOf(userId)))
        return id
    }

    @Transactional
    override fun purgeExpired(): Int {
        val expired = dao.searchExpiredGrants()
        if (expired.isEmpty()) return 0
        dao.batchDelete(expired.map { it.id })
        // Evict caches for every affected (role → users) pair so resolved sets drop the lapsed roles.
        expired.groupBy { it.roleId }.forEach { (roleId, grants) ->
            eventPublisher.publishEvent(AuthRoleUserRelationsChanged(roleId, grants.map { it.userId }.distinct()))
        }
        log.debug("Purged ${expired.size} expired role-user grants.")
        return expired.size
    }

    @Transactional
    override fun activateStarted(since: LocalDateTime, now: LocalDateTime): Int {
        if (!since.isBefore(now)) return 0
        val started = dao.searchGrantsStartedBetween(since, now)
        if (started.isEmpty()) return 0
        // No row changes — the grants were already stored; only the cached snapshots are stale.
        started.groupBy { it.roleId }.forEach { (roleId, grants) ->
            eventPublisher.publishEvent(AuthRoleUserRelationsChanged(roleId, grants.map { it.userId }.distinct()))
        }
        log.debug("Activated ${started.size} role-user grant(s) whose window opened in (${since}, ${now}].")
        return started.size
    }

}
