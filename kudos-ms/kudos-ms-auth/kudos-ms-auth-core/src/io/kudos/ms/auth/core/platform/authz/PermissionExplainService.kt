package io.kudos.ms.auth.core.platform.authz

import io.kudos.ms.auth.common.authz.api.IAuthzDecisionApi
import io.kudos.ms.auth.common.authz.support.PermissionCodes
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.common.authz.vo.response.PermissionExplanationVo
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Answers "why does this subject have — or not have — this permission?"
 *
 * Built on the decision point's own output rather than on a re-derivation: the verdict comes from
 * [IAuthzDecisionApi.decide] and the evidence from [IAuthzDecisionApi.resolveGrants], so an
 * explanation can never disagree with the enforcement it is explaining. A separate "explain"
 * implementation would drift, and drift here is worse than no feature: an operator who is told the
 * wrong reason will act on it.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
open class PermissionExplainService {

    @Resource
    private lateinit var authzDecisionApi: IAuthzDecisionApi

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

    /**
     * @param userId the subject
     * @param permissionCode the concrete code in question
     * @param context attributes for condition evaluation, matching what the real caller would send
     * @return the verdict plus every binding that speaks to this code
     */
    @Transactional(readOnly = true)
    open fun explain(
        userId: String,
        permissionCode: String,
        context: Map<String, Any?> = emptyMap(),
    ): PermissionExplanationVo {
        val subject = SubjectRef.ofUser(userId)
        val decision = authzDecisionApi.decide(AuthzRequest(subject, permissionCode, context))

        val directRoleIds = authRoleUserDao.searchRoleIdsByUserId(userId).toSet()
        val groupRoleIds = authGroupDao.filterActiveGroupIds(authGroupUserDao.searchGroupIdsByUserId(userId))
            .flatMap { authGroupRoleDao.searchRoleIdsByGroupId(it) }
            .toSet()

        val relevant = authzDecisionApi.resolveGrants(subject)
            .filter { PermissionCodes.matches(it.permissionCode, permissionCode) }
            .map { grant ->
                val role = grant.roleId?.let { authRoleDao.get(it) }
                PermissionExplanationVo.GrantEvidence(
                    permissionCode = grant.permissionCode,
                    effect = grant.effect,
                    roleId = grant.roleId,
                    roleCode = role?.code,
                    roleName = role?.name,
                    source = sourceOf(grant.roleId, directRoleIds, groupRoleIds),
                    condition = grant.condition,
                )
            }

        return PermissionExplanationVo(
            userId = userId,
            permissionCode = permissionCode,
            decision = decision,
            relevantGrants = relevant,
        )
    }

    /**
     * How the subject reaches the role carrying a binding.
     *
     * A role that is neither granted directly nor relayed by a group is reached through the parent
     * chain — it is an ancestor of something the subject does hold. That case is the one operators
     * find most surprising, so it is named rather than left blank.
     */
    private fun sourceOf(roleId: String?, direct: Set<String>, viaGroup: Set<String>): String = when {
        roleId == null -> "UNKNOWN"
        roleId in direct -> "DIRECT"
        roleId in viaGroup -> "GROUP"
        else -> "INHERITED"
    }
}
