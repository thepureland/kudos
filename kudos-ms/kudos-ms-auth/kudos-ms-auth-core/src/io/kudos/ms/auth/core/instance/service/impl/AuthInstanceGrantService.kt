package io.kudos.ms.auth.core.instance.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.auth.common.authz.enums.PermissionEffect
import io.kudos.ms.auth.core.instance.dao.AuthInstanceGrantDao
import io.kudos.ms.auth.common.instance.vo.InstanceGrantVo
import io.kudos.ms.auth.core.instance.model.po.AuthInstanceGrant
import io.kudos.ms.auth.core.principal.PrincipalDirectoryRegistry
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import jakarta.annotation.Resource
import io.kudos.ms.auth.core.instance.service.iservice.IAuthInstanceGrantService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


/**
 * Sharing implementation. See [IAuthInstanceGrantService] for why this exists next to the role model.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthInstanceGrantService(
    dao: AuthInstanceGrantDao,
) : BaseCrudService<String, AuthInstanceGrant, AuthInstanceGrantDao>(dao),
    IAuthInstanceGrantService {

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var principalDirectoryRegistry: PrincipalDirectoryRegistry

    private val log = LogFactory.getLog(this::class)

    @Transactional
    override fun share(
        principalId: String,
        tenantId: String,
        resourceType: String,
        instanceId: String,
        action: String,
        effect: String,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        grantedBy: String?,
        principalType: String,
    ): String {
        require(principalId.isNotBlank()) { "a share must name its recipient." }
        require(resourceType.isNotBlank() && instanceId.isNotBlank()) { "a share must name the instance." }
        require(action.isNotBlank()) { "a share must say what may be done." }
        require(tenantId.isNotBlank()) { "a share must name its tenant." }

        // The same admission questions the role write paths ask, asked here too. Sharing was the one
        // way into the authorization model that validated nothing about its receiver: any string was
        // a principal, and the tenant was whatever the caller typed. That made "share this document"
        // a cross-tenant grant primitive — the one boundary the whole model treats as hard.
        val principal = principalDirectoryRegistry.find(principalId, principalType)
        requireNotNull(principal) { "share recipient ${principalId} does not exist." }
        require(principal.active) { "share recipient ${principalId} is disabled." }
        // Compared rather than trusted: with the recipient's real tenant known, a caller can no
        // longer place the row in a tenant the recipient is not in.
        require(principal.tenantId.isNullOrBlank() || principal.tenantId == tenantId) {
            "cross-tenant share: recipient is in tenant ${principal.tenantId}, share names ${tenantId}."
        }
        if (startTime != null && endTime != null) {
            require(!startTime.isAfter(endTime)) { "start_time must not be after end_time." }
        }
        // Normalised so an unrecognised value cannot silently become something other than intended.
        val normalizedEffect = PermissionEffect.fromCode(effect).name

        val existing = dao.findGrant(principalId, resourceType, instanceId, action)
        if (existing != null) {
            existing.tenantId = tenantId
            existing.principalType = principalType
            existing.effect = normalizedEffect
            existing.startTime = startTime
            existing.endTime = endTime
            existing.grantedBy = grantedBy
            // Re-sharing something previously withdrawn revives it rather than leaving a dead row
            // shadowing the new intent.
            existing.revoked = false
            existing.revokeReason = null
            dao.update(existing)
            log.debug("Updated share of ${resourceType}#${instanceId} with ${principalId} (${normalizedEffect}).")
            return existing.id
        }

        val id = dao.insert(
            AuthInstanceGrant {
                this.principalId = principalId
                this.principalType = principalType
                this.tenantId = tenantId
                this.resourceType = resourceType
                this.instanceId = instanceId
                this.action = action
                this.effect = normalizedEffect
                this.startTime = startTime
                this.endTime = endTime
                this.grantedBy = grantedBy
                this.revoked = false
            },
        )
        log.debug("Shared ${resourceType}#${instanceId} with ${principalId} (${normalizedEffect}, action ${action}).")
        return id
    }

    @Transactional
    override fun unshare(grantId: String, reason: String?): Boolean {
        val grant = dao.get(grantId) ?: return false
        if (grant.revoked == true) return false
        grant.revoked = true
        grant.revokeReason = reason
        val success = dao.update(grant)
        log.debug("Withdrew share ${grantId}. Reason: ${reason ?: "none given"}.")
        return success
    }

    @Transactional(readOnly = true)
    override fun listShares(resourceType: String, instanceId: String): List<AuthInstanceGrant> =
        dao.searchLiveGrantsOnInstance(resourceType, instanceId)

    @Transactional(readOnly = true)
    override fun listShareVos(resourceType: String, instanceId: String): List<InstanceGrantVo> =
        project(dao.searchLiveGrantsOnInstance(resourceType, instanceId))

    @Transactional(readOnly = true)
    override fun listSharesOfPrincipal(principalId: String, resourceType: String?): List<InstanceGrantVo> =
        project(dao.searchLiveGrantsOfPrincipal(principalId, resourceType))

    /**
     * Resolves principal names in one batch and evaluates each window once, so an API surface never
     * has to re-derive "is this share in force" and get it subtly different from the decision point.
     */
    private fun project(grants: List<AuthInstanceGrant>): List<InstanceGrantVo> {
        if (grants.isEmpty()) return emptyList()
        val names = userAccountHashCache.getUsersByIds(grants.map { it.principalId }.toSet())
        val now = LocalDateTime.now()
        return grants.map { grant ->
            val start = grant.startTime
            val end = grant.endTime
            InstanceGrantVo(
                id = grant.id,
                principalId = grant.principalId,
                principalName = names[grant.principalId]?.username,
                principalType = grant.principalType,
                resourceType = grant.resourceType,
                instanceId = grant.instanceId,
                action = grant.action,
                effect = grant.effect ?: PermissionEffect.ALLOW.name,
                startTime = start,
                endTime = end,
                grantedBy = grant.grantedBy,
                active = (start == null || !start.isAfter(now)) && (end == null || !end.isBefore(now)),
            )
        }.sortedWith(compareBy({ !it.active }, { it.principalName ?: it.principalId }))
    }
}
