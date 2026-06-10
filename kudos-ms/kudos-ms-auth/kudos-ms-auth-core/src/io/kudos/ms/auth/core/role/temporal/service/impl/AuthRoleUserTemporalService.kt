package io.kudos.ms.auth.core.role.temporal.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.temporal.vo.response.RoleTemporalGrantRow
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.service.impl.AuthRoleUserService
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
 * [bindTemporal] enforces the same defences as the permanent bind path: the role must exist, the
 * grant must not violate any SoD exclusion, and an existing PERMANENT grant is never silently
 * downgraded to a windowed one — it must be unbound explicitly first.
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
    private lateinit var authRoleDao: AuthRoleDao

    // The concrete bean (not the interface) on purpose: the shared SoD helper
    // findSodViolationMessage is internal to this module and not part of IAuthRoleUserService.
    @Autowired
    private lateinit var authRoleUserService: AuthRoleUserService

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

        // Defence 1 — the role must exist. A temporal grant pointing at a non-existent role would
        // be an orphan row that skips every downstream check (same fix as batchBind).
        val role = authRoleDao.get(roleId)
            ?: throw IllegalArgumentException("Role not found: $roleId — refusing to create a temporal grant for a non-existent role.")

        // Defence 2 — never silently downgrade a permanent grant (start/end both NULL) to a
        // windowed one: that would let the temporal endpoint revoke-by-replacement. The permanent
        // grant must be removed explicitly (AuthRoleUserService.unbind) before a window is set.
        val existing = dao.searchByRoleIdAndUserId(roleId, userId)
        if (existing.any { it.startTime == null && it.endTime == null }) {
            throw IllegalArgumentException(
                "User $userId already holds role $roleId permanently — refusing to replace a permanent " +
                    "grant with a temporal one. Unbind the permanent grant first if a time window is intended."
            )
        }

        // Defence 3 — SoD: a temporal grant confers the same authority as a permanent one while
        // active, so it must pass the same mutual-exclusion check as the permanent bind path.
        val violationMessage = authRoleUserService.findSodViolationMessage(role.tenantId, roleId, userId)
        if (violationMessage != null) {
            throw IllegalArgumentException("SoD constraint violation — temporal grant blocked:\n$violationMessage")
        }

        // Replace any existing TEMPORAL grant for this (role, user) so the supplied window is authoritative.
        dao.deleteByRoleIdAndUserId(roleId, userId)
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

}
