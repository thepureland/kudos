package io.kudos.ms.auth.core.role.temporal.service.impl

import io.kudos.base.logger.LogFactory
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

    private val log = LogFactory.getLog(this::class)

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
        // Replace any existing grant for this (role, user) so the supplied window is authoritative.
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
