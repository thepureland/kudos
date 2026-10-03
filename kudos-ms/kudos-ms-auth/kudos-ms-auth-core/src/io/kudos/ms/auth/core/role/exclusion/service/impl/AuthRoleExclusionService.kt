package io.kudos.ms.auth.core.role.exclusion.service.impl

import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.auth.common.exclusion.vo.response.AuthRoleExclusionViolationVo
import io.kudos.ms.auth.core.role.cache.AuthRoleHashCache
import io.kudos.ms.auth.core.role.cache.UserIdsByRoleIdCache
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.exclusion.dao.AuthRoleExclusionDao
import io.kudos.ms.auth.core.role.exclusion.model.po.AuthRoleExclusion
import io.kudos.ms.auth.core.role.exclusion.service.iservice.IAuthRoleExclusionService
import io.kudos.ms.auth.core.platform.cache.ResourceIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import io.kudos.ms.auth.core.policy.TenantAdministrationGuard
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.auth.common.exclusion.vo.request.AuthRoleExclusionQuery
import org.ktorm.entity.Entity
import org.springframework.beans.factory.annotation.Autowired
import kotlin.reflect.KClass

/**
 * Service implementation for SoD mutual-exclusion pairs.
 *
 * Invariants maintained by this class:
 *  1. Canonical pair order: roleAId < roleBId (string comparison).
 *  2. Both roles belong to the same tenant.
 *  3. No self-exclusion (roleAId ≠ roleBId).
 *  4. No duplicate pair for a tenant (checked in [insert] before the DB unique constraint fires).
 *
 * The violation check in [findViolation] expands the user's role set to include group-inherited
 * and parent-chain-inherited roles, so the guard is forward-looking: it rejects the bind if
 * ANY path causes both sides of an exclusion to be simultaneously effective.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthRoleExclusionService(
    dao: AuthRoleExclusionDao,
) : BaseCrudService<String, AuthRoleExclusion, AuthRoleExclusionDao>(dao),
    IAuthRoleExclusionService {

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleHashCache: AuthRoleHashCache

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var userIdsByRoleIdCache: UserIdsByRoleIdCache

    @Resource
    private lateinit var roleIdsByUserIdCache: RoleIdsByUserIdCache

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    private val log = LogFactory.getLog(this::class)

    /**
     * Organization principals read their own organization's SoD rules only; everybody else reads exactly
     * as before.
     */
    @Transactional(readOnly = true)
    override fun get(id: String): AuthRoleExclusion? {
        val organizationId = tenantAdministrationGuard?.callerOrganization() ?: return super.get(id)
        return super.get(id)?.also { requireOwnRow(it.tenantId, organizationId) }
    }

    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        val organizationId = tenantAdministrationGuard?.callerOrganization() ?: return super.get(id, returnType)
        val row = dao.get(id) ?: return null
        requireOwnRow(row.tenantId, organizationId)
        return super.get(id, returnType)
    }

    private fun requireOwnRow(ownerId: String, organizationId: String) =
        require(ownerId == organizationId) { "cross-organization authorization administration is forbidden." }

    @Autowired(required = false)
    private var tenantAdministrationGuard: TenantAdministrationGuard? = null

    // ---------- Overrides: canonical ordering + validation ----------

    @Transactional
    override fun insert(any: Any): String {
        val (canonicalized, tenantId) = canonicalise(any)
        val roleAId = BeanKit.getProperty(canonicalized, AuthRoleExclusion::roleAId.name) as String
        val roleBId = BeanKit.getProperty(canonicalized, AuthRoleExclusion::roleBId.name) as String
        validateBeforeInsert(roleAId, roleBId, tenantId)
        val guard = tenantAdministrationGuard
        if (guard != null && guard.isOrganizationOwner(tenantId)) {
            guard.assertCanManage(tenantId)
            val exclusion = Entity.create<AuthRoleExclusion>().also { BeanKit.copyProperties(canonicalized, it) }
            exclusion.organizationId = tenantId
            return super.insert(exclusion)
        }
        return super.insert(canonicalized)
    }

    @Transactional
    override fun update(any: Any): Boolean {
        // Only description is mutable; no re-validation of the pair needed.
        val guard = tenantAdministrationGuard ?: return super.update(any)
        val id = BeanKit.getProperty(any, AuthRoleExclusion::id.name) as String?
        val existing = id?.let(dao::get)
        if (existing == null || !guard.isOrganizationOwner(existing.tenantId)) return super.update(any)
        guard.assertCanManage(existing.tenantId)
        // The generic update copies every form property, nulls included: an organization rule keeps
        // its owner columns and its pair.
        return super.update(
            Entity.create<AuthRoleExclusion>().also {
                BeanKit.copyProperties(any, it)
                it.tenantId = existing.tenantId
                it.organizationId = existing.organizationId ?: existing.tenantId
                it.roleAId = existing.roleAId
                it.roleBId = existing.roleBId
            },
        )
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val guard = tenantAdministrationGuard
        if (guard != null) {
            dao.get(id)?.let { assertCanManageIfOrganization(guard, it.tenantId) }
        }
        return super.deleteById(id)
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val guard = tenantAdministrationGuard
        if (guard != null && ids.isNotEmpty()) {
            dao.getByIds(ids).forEach { assertCanManageIfOrganization(guard, it.tenantId) }
        }
        return super.batchDelete(ids)
    }

    /** An organization principal lists only its own organization's SoD rules. */
    @Transactional(readOnly = true)
    override fun pagingSearch(listSearchPayload: ListSearchPayload): PagingSearchResult<*> {
        val organizationId = tenantAdministrationGuard?.callerOrganization()
            ?: return super.pagingSearch(listSearchPayload)
        require(listSearchPayload is AuthRoleExclusionQuery) { "An organization-scoped SoD rule query is required" }
        require(listSearchPayload.tenantId.isNullOrBlank() || listSearchPayload.tenantId == organizationId) {
            "cross-organization authorization administration is forbidden."
        }
        // The query has no organizationId field: organization rules store the organization in tenantId.
        val scoped = listSearchPayload.copy(tenantId = organizationId).apply {
            pageNo = listSearchPayload.pageNo
            pageSize = listSearchPayload.pageSize
            orders = listSearchPayload.orders
        }
        return super.pagingSearch(scoped)
    }

    // ---------- Interface methods ----------

    @Transactional(readOnly = true)
    override fun findByRoleIds(roleIds: Collection<String>): List<AuthRoleExclusion> =
        dao.searchByRoleIds(roleIds)

    @Transactional(readOnly = true)
    override fun findViolatingUserIds(exclusionId: String): AuthRoleExclusionViolationVo {
        val exclusion = dao.get(exclusionId) ?: return AuthRoleExclusionViolationVo(
            exclusionId = exclusionId, roleAId = "", roleBId = "", violatingUserIds = emptyList(),
        )
        tenantAdministrationGuard?.let { assertCanManageIfOrganization(it, exclusion.tenantId) }
        // All users holding role A plus all users holding role B (via any path).
        val usersWithA = computeEffectiveUsersForRole(exclusion.roleAId)
        val usersWithB = computeEffectiveUsersForRole(exclusion.roleBId)
        val violating = (usersWithA intersect usersWithB).toList()
        return AuthRoleExclusionViolationVo(
            exclusionId = exclusionId,
            roleAId = exclusion.roleAId,
            roleBId = exclusion.roleBId,
            violatingUserIds = violating,
        )
    }

    @Transactional(readOnly = true)
    override fun findViolation(
        tenantId: String,
        candidateRoleId: String,
        existingRoleIds: Collection<String>,
    ): AuthRoleExclusion? {
        if (existingRoleIds.isEmpty()) return null
        // Fetch only exclusion rules that involve the candidate role (one of the two sides).
        val relevantRules = dao.searchByRoleIdAndTenant(candidateRoleId, tenantId)
        if (relevantRules.isEmpty()) return null
        for (rule in relevantRules) {
            val otherSide = if (rule.roleAId == candidateRoleId) rule.roleBId else rule.roleAId
            if (existingRoleIds.contains(otherSide)) {
                log.debug(
                    "SoD violation: adding $candidateRoleId conflicts with $otherSide " +
                        "(exclusion ${rule.id}) which is already effective for the user.",
                )
                return rule
            }
        }
        return null
    }

    // ---------- Private helpers ----------

    /** Organization-owned rules are managed by their organization only; legacy rules are unchanged. */
    private fun assertCanManageIfOrganization(guard: TenantAdministrationGuard, ownerId: String) {
        if (guard.isOrganizationOwner(ownerId)) guard.assertCanManage(ownerId)
    }

    /** Swap roles so that roleAId < roleBId, then return the canonicalized object and tenantId. */
    private fun canonicalise(any: Any): Pair<Any, String> {
        val roleAId = (BeanKit.getProperty(any, AuthRoleExclusion::roleAId.name) as? String)?.trim() ?: ""
        val roleBId = (BeanKit.getProperty(any, AuthRoleExclusion::roleBId.name) as? String)?.trim() ?: ""
        val tenantId = (BeanKit.getProperty(any, AuthRoleExclusion::tenantId.name) as? String)?.trim() ?: ""
        if (roleAId <= roleBId) return any to tenantId
        // Swap: BeanKit.setProperty modifies the object in-place when it is mutable (which our PO is).
        BeanKit.setProperty(any, AuthRoleExclusion::roleAId.name, roleBId)
        BeanKit.setProperty(any, AuthRoleExclusion::roleBId.name, roleAId)
        return any to tenantId
    }

    private fun validateBeforeInsert(roleAId: String, roleBId: String, tenantId: String) {
        require(roleAId.isNotBlank()) { "roleAId must not be blank." }
        require(roleBId.isNotBlank()) { "roleBId must not be blank." }
        require(tenantId.isNotBlank()) { "tenantId must not be blank." }
        require(roleAId != roleBId) { "A role cannot be mutually exclusive with itself (roleAId=$roleAId)." }

        val roleA = authRoleHashCache.getRoleById(roleAId)
            ?: throw IllegalArgumentException("Role not found: $roleAId")
        val roleB = authRoleHashCache.getRoleById(roleBId)
            ?: throw IllegalArgumentException("Role not found: $roleBId")

        require(roleA.tenantId == tenantId) {
            "Role $roleAId belongs to tenant ${roleA.tenantId}, not $tenantId."
        }
        require(roleB.tenantId == tenantId) {
            "Role $roleBId belongs to tenant ${roleB.tenantId}, not $tenantId."
        }
        require(!dao.pairExistsForTenant(roleAId, roleBId, tenantId)) {
            "An exclusion between roles $roleAId and $roleBId already exists for tenant $tenantId."
        }
    }

    /**
     * Compute the full set of user IDs who effectively hold [roleId] via direct assignment,
     * group-derived assignment, or as a descendant of this role (i.e. users whose effective
     * role set includes [roleId] through the parent chain).
     *
     * Used for the lax-mode violation scan ([findViolatingUserIds]). Not used on the hot bind
     * path (the bind validator uses the per-user role set instead).
     */
    private fun computeEffectiveUsersForRole(roleId: String): Set<String> {
        // Users who directly hold roleId.
        val direct = userIdsByRoleIdCache.getUserIds(roleId).toSet()

        // Users who hold a descendant of roleId (their effective set includes roleId
        // through the parent chain, which means roleId's resources apply to them).
        val descendants = authRoleDao.searchDescendantRoleIds(roleId)
        val viaDescendants = if (descendants.isEmpty()) emptySet()
        else descendants.flatMap { descId -> userIdsByRoleIdCache.getUserIds(descId) }.toSet()

        // Users who hold roleId via a group that has roleId.
        val groupsWithRole = authGroupRoleDao.searchGroupIdsByRoleId(roleId)
        val viaGroup = if (groupsWithRole.isEmpty()) emptySet()
        else groupsWithRole.flatMap { gid -> authGroupUserDao.searchUserIdsByGroupId(gid) }.toSet()

        return direct + viaDescendants + viaGroup
    }
}
