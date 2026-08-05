package io.kudos.ms.auth.core.version.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.authz.api.IPermissionVersionApi
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.version.cache.PermissionVersionByUserIdCache
import io.kudos.ms.auth.core.version.dao.AuthPrincipalVersionDao
import io.kudos.ms.auth.core.version.event.PrincipalTokenEpochBumped
import io.kudos.ms.auth.core.version.model.po.AuthPrincipalVersion
import jakarta.annotation.Resource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional


/**
 * Permission-version business — see [IPermissionVersionApi] for what the mechanism is for.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional
open class PermissionVersionService : IPermissionVersionApi {

    @Resource
    private lateinit var dao: AuthPrincipalVersionDao

    @Resource
    private lateinit var versionCache: PermissionVersionByUserIdCache

    @Resource
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    override fun currentVersion(subject: SubjectRef): String =
        versionCache.getVersion(subject.principalId)

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    override fun isCurrent(subject: SubjectRef, version: String?): Boolean {
        // A token with no version cannot be shown to be current. Treating "unknown" as "fine" is how
        // a revocation check quietly stops checking — and it would be indistinguishable, from here,
        // from an attacker simply omitting the claim.
        val presented = version?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        return presented == currentVersion(subject)
    }

    @Transactional
    override fun revokeAllTokens(principalId: String, reason: String?): Long {
        require(principalId.isNotBlank()) { "principalId must not be blank." }
        val existing = dao.findByPrincipalId(principalId)
        val next = (existing?.epoch ?: 0L) + 1
        if (existing == null) {
            dao.insert(AuthPrincipalVersion {
                this.principalId = principalId
                this.epoch = next
                this.reason = reason
            })
        } else {
            dao.update(AuthPrincipalVersion {
                this.id = existing.id
                this.epoch = next
                this.reason = reason
            })
        }
        log.info("Token epoch for principal ${principalId} bumped to ${next} (reason: ${reason ?: "not given"}).")
        eventPublisher.publishEvent(PrincipalTokenEpochBumped(principalId, next, reason))
        return next
    }
}
