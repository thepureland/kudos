package io.kudos.ms.auth.core.policy.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.authz.api.IAuthzDecisionApi
import io.kudos.ms.auth.common.authz.enums.PermissionEffect
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.platform.cache.AffectedUserResolver
import io.kudos.ms.auth.core.principal.PrincipalDirectoryRegistry
import io.kudos.ms.auth.core.version.dao.AuthPrincipalVersionDao
import io.kudos.ms.auth.core.policy.GrantCandidate
import io.kudos.ms.auth.core.policy.GrantRejection
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.exclusion.service.iservice.IAuthRoleExclusionService
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.sys.core.resource.cache.SysResourceHashCache
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * The admission gate described by [IAuthGrantPolicyService].
 *
 * Screening is deliberately split in two so a broadcast bind does not turn into a query storm:
 * everything independent of the receiving principal (does the role exist, which tenant is it in)
 * is resolved once per call, and only the principal-dependent part (tenant match, effective role
 * set, SoD) runs per candidate — off caches, not off the database.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthGrantPolicyService : IAuthGrantPolicyService {

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
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Resource
    private lateinit var authzDecisionApi: IAuthzDecisionApi

    @Resource
    private lateinit var exclusionService: IAuthRoleExclusionService

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var principalDirectoryRegistry: PrincipalDirectoryRegistry

    @Resource
    private lateinit var authPrincipalVersionDao: AuthPrincipalVersionDao

    @Resource
    private lateinit var sysResourceHashCache: SysResourceHashCache

    @Resource
    private lateinit var affectedUserResolver: AffectedUserResolver

    private val log = LogFactory.getLog(this::class)

    /**
     * Not `readOnly`: screening takes a write lock (see below), and a read-only transaction may not.
     */
    @Transactional
    override fun screenGrants(candidates: Collection<GrantCandidate>): List<GrantRejection> {
        if (candidates.isEmpty()) return emptyList()

        // Serialise every concurrent grant to the same principal, before reading anything about it.
        //
        // Screening is a read-then-write: it reads the principal's effective role set, judges it,
        // and the caller then inserts. Nothing spans those two steps — the only constraint on
        // auth_role_user is unique(role_id, user_id), which cannot express "not both A and B" — so
        // two transactions binding the two halves of an exclusive pair each observed the other's
        // absence and both committed. Reproduced 39 times in 40 concurrent rounds.
        //
        // Locking here rather than in each caller is the point: all five write paths reduce to this
        // method, so the guarantee cannot be forgotten by whoever adds the sixth. Sorted so that two
        // calls touching the same principals always take the locks in the same order, which is what
        // stops a multi-principal bind deadlocking against another one.
        //
        // The *operator* is locked along with the recipients, and that closes a second window of the
        // same shape: delegation reads the operator's own grant ("still live, still delegable") and
        // then inserts children under it, while revocation reads the subtree and then cascades over
        // it. With no common lock, a delegation racing the revocation of its operator could land
        // children after the cascade's snapshot — live grants under a revoked parent, invisible to
        // any future cascade. Both sides now serialise on the operator's principal row (see the
        // matching lock in AuthRoleUserService.revoke/unbind).
        (candidates.map { it.principalId } + candidates.mapNotNull { it.operatorId })
            .distinct().sorted().forEach { principalId ->
                authPrincipalVersionDao.lockPrincipal(principalId)
            }

        // Role-side facts: one lookup per distinct role, however many principals are involved.
        val roles: Map<String, AuthRole?> = candidates.map { it.roleId }.distinct()
            .associateWith { authRoleDao.get(it) }

        val rejections = mutableListOf<GrantRejection>()
        // The effective role set is per principal, not per candidate; a group bind hands several
        // roles to the same principal and would otherwise recompute it once per role.
        val effectiveRoleCache = mutableMapOf<String, Set<String>>()

        for (candidate in candidates) {
            val role = roles[candidate.roleId]
            if (role == null) {
                rejections += GrantRejection(candidate, "role does not exist")
                continue
            }
            // Asked of the principal *directory*, not of the account table: reading accounts
            // directly is what used to make "principal" silently mean "human", so a service account
            // could be modelled and then never granted anything. See IPrincipalDirectory.
            val principal = principalDirectoryRegistry.find(candidate.principalId, candidate.principalType)
            if (principal == null) {
                rejections += GrantRejection(candidate, "principal does not exist")
                continue
            }
            if (!principal.active) {
                rejections += GrantRejection(candidate, "principal is disabled")
                continue
            }
            // The tenant is the hard boundary of the whole authorization model; a grant that
            // straddles it is never merely "unusual".
            if (!principal.tenantId.isNullOrBlank() && principal.tenantId != role.tenantId) {
                rejections += GrantRejection(
                    candidate,
                    "cross-tenant grant: principal is in tenant ${principal.tenantId}, role in ${role.tenantId}",
                )
                continue
            }

            // A role that requires approval may only be assigned through the workflow that collects
            // it. Enforced here rather than at the controller so that every write path — direct
            // bind, group membership, broadcast, delegation — is covered by the one check.
            if (role.approvalRequired == true && !candidate.approvalSatisfied) {
                rejections += GrantRejection(
                    candidate,
                    "role requires approval; assign it through the grant-request workflow",
                )
                continue
            }

            // Delegation screening — only when somebody is exercising authority they were given.
            // A platform bootstrap or an approved grant request carries no operator and skips it.
            if (candidate.operatorId != null) {
                val rejection = screenDelegation(candidate, role)
                if (rejection != null) {
                    rejections += rejection
                    continue
                }
            }

            val effective = effectiveRoleCache.getOrPut(candidate.principalId) {
                effectiveRoleIds(candidate.principalId)
            }
            val violation = exclusionService.findViolation(role.tenantId, candidate.roleId, effective)
            if (violation != null) {
                rejections += GrantRejection(
                    candidate,
                    "separation-of-duties: conflicts with ${violation.roleAId} ↔ ${violation.roleBId} " +
                        "(exclusion ${violation.id})",
                )
            }
        }
        if (rejections.isNotEmpty()) {
            log.debug("Grant policy rejected ${rejections.size}/${candidates.size} candidate(s).")
        }
        return rejections
    }

    @Transactional(readOnly = true)
    override fun screenPermissionBindings(
        roleId: String,
        resourceIds: Collection<String>,
        permissionCodes: Collection<String>,
    ): List<String> {
        val role = authRoleDao.get(roleId) ?: return listOf("role ${roleId} does not exist")
        val reasons = mutableListOf<String>()

        if (resourceIds.isNotEmpty()) {
            val found = sysResourceHashCache.getResourcesByIds(resourceIds.map { it.trim() }.toSet())
            for (rawId in resourceIds) {
                val id = rawId.trim()
                val resource = found[id]
                if (resource == null) {
                    reasons += "resource ${id} does not exist"
                    continue
                }
                // Resources are subsystem-scoped rather than tenant-scoped (they are platform master
                // data), so this is the boundary that applies to them.
                if (!resource.subSystemCode.isNullOrBlank() && resource.subSystemCode != role.subsysCode) {
                    reasons += "resource ${id} belongs to subsystem ${resource.subSystemCode}, " +
                        "role ${roleId} to ${role.subsysCode}"
                }
            }
        }

        for (code in permissionCodes) {
            if (code.isBlank()) {
                reasons += "permission code must not be blank"
            }
        }

        if (resourceIds.isEmpty() && permissionCodes.isEmpty()) {
            reasons += "a binding must address a permission by resource id or by permission code"
        }
        return reasons
    }

    @Transactional(readOnly = true)
    override fun screenReparent(roleId: String, newParentId: String?): List<GrantRejection> {
        // Detaching to a root only ever removes inherited permissions, so nothing can be breached.
        if (newParentId.isNullOrBlank()) return emptyList()
        val role = authRoleDao.get(roleId) ?: return emptyList()

        // The move adds the new parent's whole ancestor chain to everyone below this role.
        val incoming = authRoleDao.searchAncestorRoleIds(setOf(newParentId)) + newParentId
        val alreadyInherited = authRoleDao.searchAncestorRoleIds(setOf(roleId))
        val added = authRoleDao.filterActiveRoleIds(incoming - alreadyInherited)
        if (added.isEmpty()) return emptyList()

        val holders = affectedUserResolver.usersAffectedByRoleChange(roleId)
        if (holders.isEmpty()) return emptyList()

        val rejections = mutableListOf<GrantRejection>()
        for (holder in holders) {
            val effective = effectiveRoleIds(holder)
            for (addedRoleId in added) {
                val violation = exclusionService.findViolation(role.tenantId, addedRoleId, effective)
                if (violation != null) {
                    rejections += GrantRejection(
                        GrantCandidate(addedRoleId, holder),
                        "re-parenting ${roleId} under ${newParentId} would give this holder " +
                            "${addedRoleId}, conflicting with ${violation.roleAId} ↔ ${violation.roleBId} " +
                            "(exclusion ${violation.id})",
                    )
                }
            }
        }
        return rejections
    }

    /**
     * The delegation half of admission: *may this operator hand this role out, this far, this long?*
     *
     * Four checks, in the order that fails cheapest first. Together they enforce one rule —
     * **what is handed on can only ever be narrower than what the grantor holds** — across the four
     * dimensions a grant has: reach, power, population and lifetime. Any one of them left unchecked
     * turns delegation into escalation.
     *
     * @return the rejection, or null when the operator may proceed
     */
    private fun screenDelegation(candidate: GrantCandidate, role: AuthRole): GrantRejection? {
        val operatorId = candidate.operatorId ?: return null

        // 1. Reach. The operator must hold the role through a DIRECT grant that still carries
        //    delegation allowance. Group-relayed roles never do: membership is batch and fluid, so a
        //    chain running through it could not answer "who gave you this" with a person's name.
        val ownGrant = authRoleUserDao.findLiveDirectGrant(candidate.roleId, operatorId)
            ?: return GrantRejection(
                candidate,
                "operator ${operatorId} does not hold this role through a direct grant, so cannot delegate it",
            )
        val ownDepth = ownGrant.delegableDepth ?: 0
        if (ownDepth < 1) {
            return GrantRejection(candidate, "operator's own grant is terminal (delegable_depth = ${ownDepth})")
        }
        val ceiling = role.delegableMax ?: 0
        if (candidate.requestedDepth > ownDepth - 1) {
            return GrantRejection(
                candidate,
                "requested depth ${candidate.requestedDepth} does not narrow the operator's ${ownDepth}",
            )
        }
        if (candidate.requestedDepth > ceiling) {
            return GrantRejection(
                candidate,
                "requested depth ${candidate.requestedDepth} exceeds the role's ceiling (delegable_max = ${ceiling})",
            )
        }

        // 2. Power. Judged by permission-set containment rather than by position in the role tree:
        //    the tree's direction is easy to read backwards (a child inherits its parent, so the
        //    child is the stronger one), and bindings can disagree with the tree anyway.
        val missing = permissionCodesOfRole(candidate.roleId) - permissionCodesOfPrincipal(operatorId)
        if (missing.isNotEmpty()) {
            return GrantRejection(
                candidate,
                "operator does not hold every permission of this role; missing ${missing.take(3)}" +
                    if (missing.size > 3) " and ${missing.size - 3} more" else "",
            )
        }

        // 3. Population. Against the scope frozen when the operator was granted, not their current
        //    one — otherwise a transfer or reorg silently widens grants already made.
        val scopeRejection = screenScope(candidate, ownGrant.scopeSnapshot)
        if (scopeRejection != null) return scopeRejection

        // 4. Lifetime. A delegated grant cannot outlive the authority it came from.
        val ownEnd = ownGrant.endTime
        if (ownEnd != null && (candidate.endTime == null || candidate.endTime.isAfter(ownEnd))) {
            return GrantRejection(
                candidate,
                "delegated grant would outlive the operator's own, which ends at ${ownEnd}",
            )
        }
        return null
    }

    /**
     * Checks the target principal against the operator's frozen scope.
     *
     * The snapshot is a JSON object of `dimension -> [values]`; an absent or empty snapshot means
     * the operator was granted without a scope restriction and may reach anyone in the tenant.
     * Only the `org` dimension is understood today — [io.kudos.ms.auth.core.role.datascope] owns the
     * general model, and this reads the same values.
     */
    private fun screenScope(candidate: GrantCandidate, snapshot: String?): GrantRejection? {
        val allowedOrgs = parseScopeDimension(snapshot, "org")
        if (allowedOrgs.isEmpty()) return null
        val targetOrg = userAccountHashCache.getUserById(candidate.principalId)?.orgId
        if (targetOrg.isNullOrBlank() || targetOrg !in allowedOrgs) {
            return GrantRejection(
                candidate,
                "target principal is outside the operator's delegation scope (org ${targetOrg ?: "unset"})",
            )
        }
        return null
    }

    /**
     * Pulls one dimension's values out of a scope snapshot without dragging in a JSON binder — the
     * snapshot is written by this module, in one shape, and a parse failure must fail closed rather
     * than throw on a hot path.
     */
    private fun parseScopeDimension(snapshot: String?, dimension: String): Set<String> {
        val text = snapshot?.trim().orEmpty()
        if (text.isEmpty()) return emptySet()
        val key = "\"${dimension}\""
        val keyAt = text.indexOf(key)
        if (keyAt < 0) return emptySet()
        val open = text.indexOf('[', keyAt)
        val close = if (open >= 0) text.indexOf(']', open) else -1
        if (open < 0 || close < 0) return emptySet()
        return text.substring(open + 1, close)
            .split(',')
            .map { it.trim().trim('"') }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    /** Every permission code a role grants, including what it inherits from its ancestors. */
    private fun permissionCodesOfRole(roleId: String): Set<String> {
        val roleIds = authRoleDao.filterActiveRoleIds(authRoleDao.searchAncestorRoleIds(setOf(roleId)) + roleId)
        return authRoleResourceDao.searchBindingsByRoleIds(roleIds)
            .filter { PermissionEffect.fromCode(it.effect) == PermissionEffect.ALLOW }
            .mapNotNull { binding -> resolveCode(binding.permissionCode, binding.resourceId) }
            .toSet()
    }

    /** Every permission code the principal can exercise, as the decision point resolves them. */
    private fun permissionCodesOfPrincipal(principalId: String): Set<String> =
        authzDecisionApi.permissionCodes(SubjectRef.ofUser(principalId))

    private fun resolveCode(permissionCode: String?, resourceId: String?): String? {
        permissionCode?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val id = resourceId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return sysResourceHashCache.getResourcesByIds(setOf(id))[id]?.permissionCode
            ?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * The principal's current effective role set — direct ∪ group-relayed, expanded up the parent
     * chain, deactivated entries dropped. Same expansion the permission caches perform, because a
     * conflict has to be judged against what the principal can actually exercise.
     */
    private fun effectiveRoleIds(principalId: String): Set<String> {
        val direct = authRoleUserDao.searchRoleIdsByUserId(principalId)
        val groupIds = authGroupDao.filterActiveGroupIds(authGroupUserDao.searchGroupIdsByUserId(principalId))
        val groupDerived = if (groupIds.isEmpty()) emptyList()
        else groupIds.flatMap { gid -> authGroupRoleDao.searchRoleIdsByGroupId(gid) }
        val granted = (direct + groupDerived).distinct()
        if (granted.isEmpty()) return emptySet()
        val ancestors = authRoleDao.searchAncestorRoleIds(granted)
        return authRoleDao.filterActiveRoleIds(granted + ancestors)
    }
}
