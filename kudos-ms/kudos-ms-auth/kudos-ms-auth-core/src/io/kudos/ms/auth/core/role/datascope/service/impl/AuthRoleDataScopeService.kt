package io.kudos.ms.auth.core.role.datascope.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.auth.common.datascope.enums.DataScopeEnum
import io.kudos.ms.auth.common.datascope.vo.response.DataScopeVo
import io.kudos.ms.auth.core.role.cache.AuthRoleHashCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.role.datascope.dao.AuthRoleOrgDao
import io.kudos.ms.auth.core.role.datascope.model.po.AuthRoleOrg
import io.kudos.ms.auth.core.role.datascope.service.iservice.IAuthRoleDataScopeService
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.org.dao.UserOrgDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional


/**
 * Data-scope (数据权限) business.
 *
 * Manages roles' custom org grants (auth_role_org) and resolves a user's effective row-visibility
 * policy across all of their roles.
 *
 * Cross-MS reach: org-tree expansion uses [UserOrgDao] and the user's primary org comes from
 * [UserAccountHashCache], both from kudos-ms-user (an `api` dependency of auth-core). This matches
 * the existing style of [io.kudos.ms.auth.core.role.service.impl.AuthRoleService], which already
 * injects user/sys caches across the MS boundary.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthRoleDataScopeService(
    dao: AuthRoleOrgDao
) : BaseCrudService<String, AuthRoleOrg, AuthRoleOrgDao>(dao),
    IAuthRoleDataScopeService {

    @Resource
    private lateinit var authRoleHashCache: AuthRoleHashCache

    @Resource
    private lateinit var roleIdsByUserIdCache: RoleIdsByUserIdCache

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var userOrgDao: UserOrgDao

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getOrgIdsByRoleId(roleId: String): Set<String> = dao.searchOrgIdsByRoleId(roleId)

    @Transactional
    override fun bindOrgs(roleId: String, orgIds: Collection<String>): Int {
        // Replace semantics: clear the role's grants, then insert the desired set.
        dao.deleteByRoleId(roleId)
        val distinct = orgIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (distinct.isEmpty()) {
            log.debug("Cleared all custom data-scope orgs for role ${roleId}.")
            return 0
        }
        val relations = distinct.map { oid ->
            AuthRoleOrg {
                this.roleId = roleId
                this.orgId = oid
            }
        }
        dao.batchInsert(relations)
        log.debug("Set ${distinct.size} custom data-scope orgs for role ${roleId}.")
        return distinct.size
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    override fun resolveUserDataScope(userId: String): DataScopeVo {
        // Effective roles include group- and parent-inherited roles (the cache already unions them).
        val roleIds = roleIdsByUserIdCache.getRoleIds(userId)
        if (roleIds.isEmpty()) return DataScopeVo.selfOnly()

        val roles = authRoleHashCache.getRolesByIds(roleIds).values
        // ALL (or an unset/NULL scope) is the broadest — short-circuit before any org lookups.
        if (roles.any { (DataScopeEnum.fromCode(it.dataScope) ?: DataScopeEnum.ALL) == DataScopeEnum.ALL }) {
            return DataScopeVo.all()
        }

        // The user's primary org, needed only by ORG / ORG_AND_CHILD scopes.
        val userOrgId: String? = userAccountHashCache.getUsersByIds(setOf(userId))[userId]?.orgId
            ?.takeIf { it.isNotBlank() }

        val orgIds = LinkedHashSet<String>()
        var includeSelf = false
        for (role in roles) {
            when (DataScopeEnum.fromCode(role.dataScope) ?: DataScopeEnum.ALL) {
                DataScopeEnum.ALL -> { /* unreachable: handled by the short-circuit above */ }
                DataScopeEnum.ORG_AND_CHILD ->
                    if (userOrgId != null) orgIds.addAll(userOrgDao.searchOrgAndDescendantIds(userOrgId))
                DataScopeEnum.ORG ->
                    if (userOrgId != null) orgIds.add(userOrgId)
                DataScopeEnum.CUSTOM ->
                    orgIds.addAll(dao.searchOrgIdsByRoleId(role.id))
                DataScopeEnum.SELF ->
                    includeSelf = true
            }
        }

        // Nothing concrete resolved (e.g. ORG scope but the user has no org, or CUSTOM with no
        // grants): fall back to self-only — the least-surprising restrictive outcome.
        if (orgIds.isEmpty() && !includeSelf) return DataScopeVo.selfOnly()
        return DataScopeVo(all = false, self = includeSelf, orgIds = orgIds)
    }

}
