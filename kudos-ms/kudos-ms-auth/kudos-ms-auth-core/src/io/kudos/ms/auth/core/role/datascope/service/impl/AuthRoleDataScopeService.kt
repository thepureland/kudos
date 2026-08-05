package io.kudos.ms.auth.core.role.datascope.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.auth.common.datascope.enums.DataScopeEnum
import io.kudos.ms.auth.common.datascope.vo.response.DataScopeExplanationVo
import io.kudos.ms.auth.common.datascope.vo.response.DataScopeVo
import io.kudos.ms.auth.core.role.cache.AuthRoleHashCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.role.datascope.cache.DataScopeByUserIdCache
import io.kudos.ms.auth.core.role.datascope.event.AuthRoleScopeChanged
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.datascope.dao.AuthRoleScopeDao
import io.kudos.ms.auth.core.role.datascope.model.po.AuthRoleScope
import io.kudos.ms.auth.core.role.datascope.spi.IScopeDimensionProvider
import io.kudos.ms.auth.core.role.datascope.service.iservice.IAuthRoleDataScopeService
import io.kudos.ms.auth.core.role.event.AuthRoleUpdated
import io.kudos.ms.auth.core.role.model.po.AuthRole
import jakarta.annotation.Resource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional


/**
 * Data-scope (数据权限) business.
 *
 * Manages roles' custom scope grants (auth_role_scope, any dimension) and resolves a user's row-visibility
 * policy across all of their roles.
 *
 * Cross-MS reach is delegated: everything that needs to know how a dimension's values nest, or
 * where a subject sits on one, goes through an [IScopeDimensionProvider]. The built-in `org`
 * provider is the only thing here that reaches into kudos-ms-user, so adding a dimension never
 * touches this class.
 *
 * Resolution is multi-dimensional: a CUSTOM role contributes whatever it was granted, on every
 * dimension it was granted on. Reading only `org` — as this did before the model was generalised —
 * would have let a second dimension be configured and then silently never reach a consumer.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthRoleDataScopeService(
    dao: AuthRoleScopeDao
) : BaseCrudService<String, AuthRoleScope, AuthRoleScopeDao>(dao),
    IAuthRoleDataScopeService {

    @Resource
    private lateinit var authRoleHashCache: AuthRoleHashCache

    @Resource
    private lateinit var roleIdsByUserIdCache: RoleIdsByUserIdCache

    /**
     * The cache in front of [computeUserDataScope]. Mutually dependent with this service by design
     * and field-injected for that reason; see [DataScopeByUserIdCache] for why the query path can
     * not afford to resolve a scope per query.
     */
    @Resource
    private lateinit var dataScopeByUserIdCache: DataScopeByUserIdCache

    /**
     * Every registered dimension. Injected as a list rather than looked up by name so that adding a
     * dimension is a matter of contributing a bean — the resolver never needs to know they exist.
     */
    @Resource
    private lateinit var dimensionProviders: List<IScopeDimensionProvider>

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Resource
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional
    override fun updateScope(roleId: String, dataScope: String?): Boolean {
        // Validate the code (NULL/blank is allowed and means ALL). Reject unrecognised codes so a
        // typo can't silently disable row filtering.
        val normalized = dataScope?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized != null) {
            require(DataScopeEnum.fromCode(normalized) != null) { "Unknown data scope code: ${dataScope}" }
        }
        // Partial update — set only data_scope (mirrors AuthRoleService.updateActive).
        val role = AuthRole {
            this.id = roleId
            this.dataScope = normalized
        }
        val success = authRoleDao.update(role)
        if (success) {
            log.debug("Updated data scope of role ${roleId} to ${normalized ?: "ALL(null)"}.")
            eventPublisher.publishEvent(AuthRoleUpdated(roleId))
        } else {
            log.error("Failed to update data scope of role ${roleId}!")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun getOrgIdsByRoleId(roleId: String): Set<String> =
        dao.searchValuesByRoleId(roleId, AuthRoleScopeDao.DIMENSION_ORG)

    @Transactional(readOnly = true)
    override fun getScopeValues(roleId: String, dimension: String): Set<String> =
        dao.searchValuesByRoleId(roleId, dimension)

    @Transactional(readOnly = true)
    override fun getAllScopes(roleId: String): Map<String, Set<String>> = dao.searchAllByRoleId(roleId)

    @Transactional
    override fun bindScope(roleId: String, dimension: String, values: Collection<String>): Int {
        require(dimension.isNotBlank()) { "a scope grant must name its dimension." }
        // Replace semantics, scoped to this dimension only.
        dao.deleteByRoleId(roleId, dimension)
        val distinct = values.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (distinct.isEmpty()) {
            log.debug("Cleared all ${dimension} scope grants for role ${roleId}.")
            // Clearing is a scope change like any other — without this, revoking every value a role
            // was granted would leave the resolved scope cached exactly as it was.
            eventPublisher.publishEvent(AuthRoleScopeChanged(roleId))
            return 0
        }
        dao.batchInsert(
            distinct.map { value ->
                AuthRoleScope {
                    this.roleId = roleId
                    this.dimension = dimension
                    this.scopeValue = value
                }
            },
        )
        log.debug("Set ${distinct.size} ${dimension} scope grant(s) for role ${roleId}.")
        eventPublisher.publishEvent(AuthRoleScopeChanged(roleId))
        return distinct.size
    }

    @Transactional
    override fun bindOrgs(roleId: String, orgIds: Collection<String>): Int =
        bindScope(roleId, AuthRoleScopeDao.DIMENSION_ORG, orgIds)

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    override fun resolveUserDataScope(userId: String): DataScopeVo =
        dataScopeByUserIdCache.getDataScope(userId)

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    override fun computeUserDataScope(userId: String): DataScopeVo {
        // Effective roles include group- and parent-inherited roles (the cache already unions them).
        val roleIds = roleIdsByUserIdCache.getRoleIds(userId)
        if (roleIds.isEmpty()) return DataScopeVo.selfOnly()

        val roles = authRoleHashCache.getRolesByIds(roleIds).values
        // ALL (or an unset/NULL scope) is the broadest — short-circuit before any org lookups.
        if (roles.any { (DataScopeEnum.fromCode(it.dataScope) ?: DataScopeEnum.ALL) == DataScopeEnum.ALL }) {
            return DataScopeVo.all()
        }

        val providers = dimensionProviders.associateBy { it.dimension() }
        val orgProvider = providers[AuthRoleScopeDao.DIMENSION_ORG]

        // The user's own position on the org dimension, needed only by ORG / ORG_AND_CHILD. Read
        // through the provider rather than the user cache directly, so "where does this subject sit
        // on this dimension" has one implementation for org and for anything an application adds.
        val userOrgId: String? = orgProvider?.valueOfUser(userId)

        // dimension -> the values that resolve for this user. Already expanded where the policy
        // asked for it — expansion belongs to the policy, not to the dimension: ORG means "own org",
        // ORG_AND_CHILD means "and everything below it", and a CUSTOM list means exactly what an
        // admin picked. Expanding uniformly at the end would silently turn the first into the second.
        val granted = linkedMapOf<String, LinkedHashSet<String>>()
        fun grant(dimension: String, values: Collection<String>) {
            if (values.isEmpty()) return
            granted.getOrPut(dimension) { LinkedHashSet() }.addAll(values)
        }

        var includeSelf = false
        for (role in roles) {
            when (DataScopeEnum.fromCode(role.dataScope) ?: DataScopeEnum.ALL) {
                DataScopeEnum.ALL -> { /* unreachable: handled by the short-circuit above */ }
                // ORG / ORG_AND_CHILD are org-specific by name and stay so; the difference between
                // them is only whether the subtree comes along, which the provider's expand decides.
                DataScopeEnum.ORG_AND_CHILD ->
                    if (userOrgId != null) {
                        grant(AuthRoleScopeDao.DIMENSION_ORG, orgProvider!!.expand(setOf(userOrgId)))
                    }
                DataScopeEnum.ORG ->
                    if (userOrgId != null) grant(AuthRoleScopeDao.DIMENSION_ORG, setOf(userOrgId))
                // CUSTOM is the multi-dimensional case: take whatever the role was granted, on every
                // dimension it was granted on, exactly as granted. Reading only `org` here was the
                // gap that would have let a second dimension be configured and quietly never reach
                // a consumer.
                DataScopeEnum.CUSTOM ->
                    dao.searchAllByRoleId(role.id).forEach { (dimension, values) -> grant(dimension, values) }
                DataScopeEnum.SELF ->
                    includeSelf = true
            }
        }

        val resolved = granted.mapValues { (_, values) -> values.toSet() }.filterValues { it.isNotEmpty() }

        // Nothing concrete resolved (e.g. ORG scope but the user has no org, or CUSTOM with no
        // grants): fall back to self-only — the least-surprising restrictive outcome.
        if (resolved.isEmpty() && !includeSelf) return DataScopeVo.selfOnly()
        return DataScopeVo(all = false, self = includeSelf, dimensions = resolved)
    }

    @Transactional(readOnly = true)
    override fun getRegisteredDimensions(): List<String> =
        dimensionProviders.map { it.dimension() }.distinct().sorted()

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    override fun explainUserDataScope(userId: String): DataScopeExplanationVo {
        val resolved = resolveUserDataScope(userId)
        val roleIds = roleIdsByUserIdCache.getRoleIds(userId)
        if (roleIds.isEmpty()) {
            return DataScopeExplanationVo(userId, resolved, emptyList())
        }

        val providers = dimensionProviders.associateBy { it.dimension() }
        val orgProvider = providers[AuthRoleScopeDao.DIMENSION_ORG]
        val userOrgId = orgProvider?.valueOfUser(userId)

        // How the subject reaches each role, so the reader can go and change the right thing.
        val directRoleIds = authRoleUserDao.searchRoleIdsByUserId(userId).toSet()
        val groupRoleIds = authGroupDao.filterActiveGroupIds(authGroupUserDao.searchGroupIdsByUserId(userId))
            .flatMap { authGroupRoleDao.searchRoleIdsByGroupId(it) }
            .toSet()

        val contributions = authRoleHashCache.getRolesByIds(roleIds).values.map { role ->
            val policy = DataScopeEnum.fromCode(role.dataScope) ?: DataScopeEnum.ALL
            // What this role contributes *on its own* — deliberately not the merged result, or every
            // role would appear to contribute everything.
            val contributed: Map<String, Set<String>> = when (policy) {
                DataScopeEnum.ALL, DataScopeEnum.SELF -> emptyMap()
                DataScopeEnum.ORG ->
                    userOrgId?.let { mapOf(AuthRoleScopeDao.DIMENSION_ORG to setOf(it)) } ?: emptyMap()
                DataScopeEnum.ORG_AND_CHILD ->
                    userOrgId?.let { mapOf(AuthRoleScopeDao.DIMENSION_ORG to orgProvider!!.expand(setOf(it))) }
                        ?: emptyMap()
                DataScopeEnum.CUSTOM -> dao.searchAllByRoleId(role.id).filterValues { it.isNotEmpty() }
            }
            DataScopeExplanationVo.RoleContribution(
                roleId = role.id,
                roleCode = role.code,
                roleName = role.name,
                policy = policy.name,
                source = when {
                    role.id in directRoleIds -> "DIRECT"
                    role.id in groupRoleIds -> "GROUP"
                    else -> "INHERITED"
                },
                contributed = contributed,
                contributesSelf = policy == DataScopeEnum.SELF,
            )
        }.sortedWith(compareBy({ it.policy != DataScopeEnum.ALL.name }, { it.roleName ?: it.roleId }))

        // One ALL role makes every other restriction irrelevant. Naming it is the single most useful
        // thing this endpoint does — it is the answer to "I restricted this role, why is he still
        // seeing everything".
        val overriddenBy = if (resolved.all) contributions.firstOrNull { it.policy == DataScopeEnum.ALL.name } else null
        return DataScopeExplanationVo(userId, resolved, contributions, overriddenBy)
    }

}
