package io.kudos.ms.auth.core.tenant.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.common.authz.enums.PermissionEffect
import io.kudos.ms.auth.common.tenant.vo.TenantBootstrapResult
import io.kudos.ms.auth.common.tenant.vo.TenantBootstrapStatusVo
import io.kudos.ms.auth.core.policy.GrantCandidate
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.tenant.init.properties.TenantBootstrapProperties
import io.kudos.ms.auth.core.tenant.service.iservice.ITenantBootstrapService
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import jakarta.annotation.Resource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Tenant bootstrap — see [ITenantBootstrapService] for why it is two steps rather than one.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional
open class TenantBootstrapService : ITenantBootstrapService {

    @Resource
    private lateinit var properties: TenantBootstrapProperties

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var grantPolicyService: IAuthGrantPolicyService

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional
    override fun seedRoles(tenantId: String): TenantBootstrapResult {
        require(tenantId.isNotBlank()) { "tenantId must not be blank." }
        val created = mutableListOf<String>()
        val existing = mutableListOf<String>()
        var bindings = 0

        properties.roles.forEach { spec ->
            val already = authRoleDao.searchRoleByTenantIdAndRoleCode(tenantId, spec.code)
            if (already != null) {
                // Left exactly as found, bindings included: a redeploy must not reset whatever the
                // tenant's administrators have since changed.
                existing += spec.code
                return@forEach
            }
            val roleId = authRoleDao.insert(AuthRole {
                this.code = spec.code
                this.name = spec.name
                this.tenantId = tenantId
                this.subsysCode = spec.subsysCode
                this.delegableMax = spec.delegableMax
                this.active = true
                // Marked built-in so the console can stop somebody deleting the only role that can
                // administer the tenant — the bootstrap problem in its most avoidable form.
                this.builtIn = true
                this.remark = "seeded by tenant bootstrap"
            })
            val codes = spec.permissionCodes.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            if (codes.isNotEmpty()) {
                authRoleResourceDao.batchInsert(codes.map { code ->
                    AuthRoleResource {
                        this.roleId = roleId
                        this.permissionCode = code
                        this.effect = PermissionEffect.ALLOW.name
                    }
                })
                bindings += codes.size
            }
            created += spec.code
            log.info("Seeded bootstrap role ${spec.code} for tenant ${tenantId} with ${codes.size} binding(s).")
        }

        return TenantBootstrapResult(
            tenantId = tenantId,
            createdRoleCodes = created,
            existingRoleCodes = existing,
            createdBindings = bindings,
        )
    }

    @Transactional(readOnly = true)
    override fun status(tenantId: String): TenantBootstrapStatusVo {
        require(tenantId.isNotBlank()) { "tenantId must not be blank." }
        val present = mutableListOf<String>()
        val missing = mutableListOf<String>()
        properties.roles.forEach { spec ->
            if (authRoleDao.searchRoleByTenantIdAndRoleCode(tenantId, spec.code) != null) present += spec.code
            else missing += spec.code
        }
        // Holders of the *primary* bootstrap role only: that is the one "who runs this tenant" means,
        // and unioning every seeded role would answer a question nobody asked.
        val primary = properties.roles.firstOrNull()?.code
        val holders = primary
            ?.let { authRoleDao.searchRoleByTenantIdAndRoleCode(tenantId, it) }
            ?.let { authRoleUserDao.searchUserIdsByRoleId(it.id) }
            .orEmpty()
        val names = if (holders.isEmpty()) emptyMap() else userAccountHashCache.getUsersByIds(holders.toSet())
        return TenantBootstrapStatusVo(
            tenantId = tenantId,
            seeded = missing.isEmpty() && present.isNotEmpty(),
            presentRoleCodes = present,
            missingRoleCodes = missing,
            administrators = holders.distinct().map {
                TenantBootstrapStatusVo.Administrator(it, names[it]?.username)
            },
        )
    }

    @Transactional
    override fun bindFirstAdministrator(
        tenantId: String,
        userId: String,
        roleCode: String?,
        force: Boolean,
    ): TenantBootstrapResult {
        require(tenantId.isNotBlank()) { "tenantId must not be blank." }
        require(userId.isNotBlank()) { "userId must not be blank." }
        val code = roleCode?.trim()?.takeIf { it.isNotEmpty() }
            ?: properties.roles.firstOrNull()?.code
            ?: throw IllegalArgumentException("no bootstrap role is configured.")

        val role = authRoleDao.searchRoleByTenantIdAndRoleCode(tenantId, code)
            ?: throw IllegalArgumentException(
                "tenant ${tenantId} has no bootstrap role ${code}; seed the tenant first.",
            )

        // "First administrator" is a one-time act. An endpoint that quietly appoints a second one is
        // an escalation path, so a repeat has to be asked for in as many words.
        //
        // The gate is "this role has no holders yet" — a property of the *role* — so the lock must
        // be on the role. The per-principal locks screenGrants takes cannot close this window: two
        // concurrent appointments name two *different* principals, lock two different rows, and each
        // reads an empty holder set. The holders are read only after this lock is held.
        authRoleDao.lockRole(role.id)
        val holders = authRoleUserDao.searchUserIdsByRoleId(role.id)
        if (holders.isNotEmpty() && !force) {
            throw IllegalArgumentException(
                "tenant ${tenantId} already has ${holders.size} holder(s) of ${code}; " +
                    "pass force=true to appoint another.",
            )
        }
        if (holders.contains(userId)) {
            return TenantBootstrapResult(tenantId = tenantId, boundRoleCode = code, boundUserId = userId)
        }

        // Screened like any other grant — tenant match, existence, SoD — but with no operator: there
        // is by definition nobody inside the tenant with authority to delegate this yet.
        grantPolicyService.assertNoRejection(
            grantPolicyService.screenGrants(listOf(GrantCandidate(role.id, userId))),
        )

        val existingRow = authRoleUserDao.search(
            Criteria.and(AuthRoleUser::roleId eq role.id, AuthRoleUser::userId eq userId),
        ).firstOrNull()
        if (existingRow == null) {
            authRoleUserDao.insert(AuthRoleUser {
                this.roleId = role.id
                this.userId = userId
            })
        }
        log.info("Appointed ${userId} as an administrator of tenant ${tenantId} via role ${code}.")
        eventPublisher.publishEvent(AuthRoleUserRelationsChanged(role.id, listOf(userId)))

        return TenantBootstrapResult(tenantId = tenantId, boundRoleCode = code, boundUserId = userId)
    }
}
