package io.kudos.ms.auth.core.role.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.role.exclusion.service.iservice.IAuthRoleExclusionService
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.common.delegation.vo.RevokeImpactVo
import io.kudos.ms.auth.common.delegation.vo.RoleGrantRequest
import io.kudos.ms.auth.core.policy.GrantCandidate
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.ms.auth.core.platform.cache.ResourceIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.UserIdsByRoleIdCache
import io.kudos.ms.auth.core.version.dao.AuthPrincipalVersionDao
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Role-User relation business.
 *
 * [batchBind] does not decide for itself whether an assignment is admissible: it deduplicates
 * against what the users already hold, then hands the remainder to
 * [IAuthGrantPolicyService] — the one place where tenant, existence and separation-of-duties
 * constraints live, shared with the group write paths that used to enforce none of them.
 *
 * A direct bind is all-or-nothing: it names its users explicitly, so a rejected member aborts the
 * call rather than being silently skipped. The batch-across-roles entry point
 * (`IAuthRoleService.batchBindUsers`) keeps per-role granularity and reports failures in
 * `BatchBindResultVo`.
 *
 * @author K
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthRoleUserService(
    dao: AuthRoleUserDao
) : BaseCrudService<String, AuthRoleUser, AuthRoleUserDao>(dao),
    IAuthRoleUserService {


    @Autowired
    private lateinit var userIdsByRoleIdCache: UserIdsByRoleIdCache

    @Autowired
    private lateinit var roleIdsByUserIdCache: RoleIdsByUserIdCache

    @Autowired
    private lateinit var resourceIdsByUserIdCache: ResourceIdsByUserIdCache

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Resource
    private lateinit var grantPolicyService: IAuthGrantPolicyService

    @Resource
    private lateinit var authPrincipalVersionDao: AuthPrincipalVersionDao

    // Needed by findSodViolationMessage below, which the temporal bind path calls directly.
    @Autowired
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Resource
    private lateinit var exclusionService: IAuthRoleExclusionService

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getUserIdsByRoleId(roleId: String): Set<String> =
        userIdsByRoleIdCache.getUserIds(roleId).toSet()

    @Transactional(readOnly = true)
    override fun getRoleIdsByUserId(userId: String): Set<String> =
        roleIdsByUserIdCache.getRoleIds(userId).toSet()

    /**
     * Batch-bind users to a role, respecting SoD constraints.
     *
     * For each user, the method:
     *   1. Skips users already holding this role.
     *   2. Computes the user's effective role set (direct + group-inherited + ancestors).
     *   3. Checks whether adding [roleId] would violate any SoD exclusion.
     *   4. Throws [IllegalArgumentException] for violating users; non-violating users are bound.
     *
     * The role's tenant is read from the cache to scope the exclusion lookup.
     *
     * @param roleId The role to bind users into; must reference an existing role.
     * @param userIds Users to bind; already-bound users are silently skipped.
     * @return Count of newly created bindings.
     * @throws IllegalArgumentException if the role does not exist, or if ANY user would violate
     *   an SoD constraint (the message names the violating user and the conflicting role pair).
     */
    @Transactional
    override fun batchBind(roleId: String, userIds: Collection<String>): Int =
        doBind(roleId, userIds, approvalSatisfied = false)

    @Transactional
    override fun bindApproved(roleId: String, userId: String): Int =
        doBind(roleId, listOf(userId), approvalSatisfied = true)

    /**
     * The shared bind, with the one thing that differs between an ordinary assignment and an
     * approved one made explicit.
     *
     * @param approvalSatisfied true only when a grant-request workflow has collected the sign-off a
     *   role marked `approval_required` demands. Passed through to the policy layer rather than
     *   checked here, so the rule lives in one place with the others.
     */
    private fun doBind(roleId: String, userIds: Collection<String>, approvalSatisfied: Boolean): Int {
        if (userIds.isEmpty()) return 0

        val existing = dao.searchUserIdsByRoleId(roleId).toSet()
        val candidates = userIds.toSet() - existing
        if (candidates.isEmpty()) {
            log.debug("All ${userIds.size} users already hold role $roleId; nothing to bind.")
            return 0
        }

        // All-or-nothing: a direct role bind is an explicit administrative act on a named set of
        // users, so a rejected member aborts the call rather than being quietly dropped.
        grantPolicyService.assertNoRejection(
            grantPolicyService.screenGrants(
                candidates.map { GrantCandidate(roleId, it, approvalSatisfied = approvalSatisfied) },
            ),
        )

        val relations = candidates.map { userId ->
            AuthRoleUser {
                this.roleId = roleId
                this.userId = userId
            }
        }
        dao.batchInsert(relations)
        log.debug("Bound ${candidates.size} users to role $roleId (policy check passed, ${existing.size} already existed).")
        eventPublisher.publishEvent(AuthRoleUserRelationsChanged(roleId, candidates.toList()))
        return candidates.size
    }

    @Transactional
    override fun grant(request: RoleGrantRequest, operatorId: String): List<String> {
        require(operatorId.isNotBlank()) { "a delegated grant must name the operator making it." }
        if (request.userIds.isEmpty()) return emptyList()

        val existing = dao.searchUserIdsByRoleId(request.roleId).toSet()
        val recipients = request.userIds.toSet() - existing
        if (recipients.isEmpty()) {
            log.debug("All ${request.userIds.size} recipients already hold role ${request.roleId}; nothing granted.")
            return emptyList()
        }

        grantPolicyService.assertNoRejection(
            grantPolicyService.screenGrants(
                recipients.map { userId ->
                    GrantCandidate(
                        roleId = request.roleId,
                        principalId = userId,
                        operatorId = operatorId,
                        requestedDepth = request.delegableDepth,
                        endTime = request.endTime,
                    )
                },
            ),
        )

        // The operator's own grant is the parent of every grant made from it — the edge revocation
        // will later follow, and the reason a grantor losing their role takes these with it. Its
        // scope is copied rather than re-read at revoke time, so a later reorg cannot widen what was
        // handed out here.
        val ownGrant = dao.findLiveDirectGrant(request.roleId, operatorId)
        val ids = recipients.map { userId ->
            dao.insert(
                AuthRoleUser {
                    this.roleId = request.roleId
                    this.userId = userId
                    this.grantedBy = operatorId
                    this.parentGrantId = ownGrant?.id
                    this.delegableDepth = request.delegableDepth
                    this.scopeSnapshot = ownGrant?.scopeSnapshot
                    this.startTime = request.startTime
                    this.endTime = request.endTime
                    this.revoked = false
                },
            )
        }
        log.debug(
            "Operator ${operatorId} delegated role ${request.roleId} to ${recipients.size} principal(s) " +
                "at depth ${request.delegableDepth}, parent grant ${ownGrant?.id ?: "none"}.",
        )
        eventPublisher.publishEvent(AuthRoleUserRelationsChanged(request.roleId, recipients.toList()))
        return ids
    }

    @Transactional
    override fun revoke(grantId: String, reason: String?): Int {
        val root = dao.get(grantId) ?: return 0
        val subtree = listOf(root) + lockedDelegatedDescendants(root)
        val live = subtree.filter { it.revoked != true }
        if (live.isEmpty()) return 0

        live.forEach { grant ->
            grant.revoked = true
            grant.revokeReason = reason
            dao.update(grant)
        }
        // One event per role so each cache invalidates exactly the principals it must.
        live.groupBy { it.roleId }.forEach { (roleId, grants) ->
            eventPublisher.publishEvent(AuthRoleUserRelationsChanged(roleId, grants.map { it.userId }.distinct()))
        }
        log.debug("Revoked grant ${grantId} and ${live.size - 1} grant(s) delegated from it. Reason: ${reason ?: "none given"}.")
        return live.size
    }

    @Transactional(readOnly = true)
    override fun getRevokeImpact(grantId: String): RevokeImpactVo {
        val root = dao.get(grantId) ?: return RevokeImpactVo.none(grantId)
        val descendants = dao.searchDelegatedDescendants(listOf(grantId)).filter { it.revoked != true }

        // Depth is computed from the chain rather than stored: it describes a position in the tree,
        // which changes as ancestors are revoked, whereas delegable_depth describes an allowance.
        val parentOf = descendants.associate { it.id to it.parentGrantId }
        fun depthOf(id: String?): Int {
            var current = id
            var depth = 0
            while (current != null && current != grantId && depth < MAX_CHAIN_WALK_DEPTH) {
                current = parentOf[current]
                depth++
            }
            return depth
        }

        val cascaded = descendants.map { grant ->
            RevokeImpactVo.CascadedGrant(
                grantId = grant.id,
                roleId = grant.roleId,
                userId = grant.userId,
                depth = depthOf(grant.id),
            )
        }
        return RevokeImpactVo(
            grantId = grantId,
            roleId = root.roleId,
            userId = root.userId,
            cascadedGrants = cascaded,
            affectedUserCount = (cascaded.map { it.userId } + root.userId).distinct().size,
        )
    }

    @Transactional
    override fun unbind(roleId: String, userId: String): Boolean {
        // Whatever this principal delegated from this role goes with it. Cascading *before* the
        // delete matters: once the row is gone the chain below it is unreachable, and the downstream
        // grants would keep working with no traceable source of authority — the exact shape of a
        // zombie permission.
        //
        // The principal being unbound is locked first — the same lock every delegation takes on its
        // operator (see AuthGrantPolicyService.screenGrants) — so a delegation racing this removal
        // either committed its children before the descendant read below, or waits and finds the
        // grant gone.
        authPrincipalVersionDao.lockPrincipal(userId)
        val cascaded = lockedDescendantsOf(
            dao.searchGrantsByRoleId(roleId).filter { it.userId == userId }.map { it.id },
        ).filter { it.revoked != true }
        cascaded.forEach { grant ->
            grant.revoked = true
            grant.revokeReason = "upstream grant of role ${roleId} was removed from ${userId}"
            dao.update(grant)
        }

        val count = dao.deleteByRoleIdAndUserId(roleId, userId)
        val success = count > 0
        if (success) {
            log.debug("Unbound role $roleId from user $userId; ${cascaded.size} delegated grant(s) cascaded.")
            eventPublisher.publishEvent(AuthRoleUserRelationsChanged(roleId, listOf(userId)))
            cascaded.groupBy { it.roleId }.forEach { (cascadedRoleId, grants) ->
                eventPublisher.publishEvent(
                    AuthRoleUserRelationsChanged(cascadedRoleId, grants.map { it.userId }.distinct()),
                )
            }
        } else {
            log.warn("Failed to unbind relation between role $roleId and user $userId; relation does not exist.")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(roleId: String, userId: String): Boolean = dao.exists(roleId, userId)

    /** [lockedDescendantsOf] rooted at one grant, with the root's own holder locked first. */
    private fun lockedDelegatedDescendants(root: AuthRoleUser): List<AuthRoleUser> {
        root.userId?.let { authPrincipalVersionDao.lockPrincipal(it) }
        return lockedDescendantsOf(listOf(root.id))
    }

    // ---------- Shared SoD helpers ----------

    /**
     * Single-user SoD check shared by [batchBind] and the temporal bind path
     * ([io.kudos.ms.auth.core.role.temporal.service.impl.AuthRoleUserTemporalService.bindTemporal]):
     * expands [userId]'s effective role set and asks the exclusion service whether adding
     * [roleId] would violate any SoD rule of [tenantId].
     *
     * `internal open` on purpose: internal so only this module's bind paths can call it, open so
     * the CGLIB transaction proxy delegates the call to the initialized target bean.
     *
     * @return a human-readable violation message naming the user and the conflicting pair, or
     *   null when the bind is allowed
     */
    internal open fun findSodViolationMessage(tenantId: String, roleId: String, userId: String): String? {
        val effectiveRoles = computeEffectiveRoleIds(userId)
        val violation = exclusionService.findViolation(tenantId, roleId, effectiveRoles) ?: return null
        return "User $userId: binding role $roleId would conflict with " +
            "${violation.roleAId} ↔ ${violation.roleBId} (exclusion ${violation.id})."
    }

    /**
     * The delegation subtree under [rootGrantIds], read under the same per-principal locks that
     * every delegation takes on its operator.
     *
     * A cascade is a read-then-write over rows other transactions are still adding to: every holder
     * in the subtree may be an operator with a delegation in flight, and children inserted after a
     * plain snapshot would survive the cascade as live grants under a revoked parent — authority
     * with no traceable source, which no later cascade would ever find again. So each holder is
     * locked (blocking their in-flight delegations) and the subtree re-read until no new holders
     * appear. Chains are short and contention rare; the loop settles in one round in practice.
     *
     * Locks are necessarily acquired incrementally as the subtree is discovered, so a global lock
     * order cannot be promised here the way `screenGrants` promises it. The accepted worst case is a
     * database-detected deadlock aborting one side loudly — a retryable error, chosen over the
     * quiet alternative of cascading past an unlocked branch.
     */
    private fun lockedDescendantsOf(rootGrantIds: Collection<String>): List<AuthRoleUser> {
        if (rootGrantIds.isEmpty()) return emptyList()
        val locked = mutableSetOf<String>()
        while (true) {
            val descendants = dao.searchDelegatedDescendants(rootGrantIds)
            val holders = descendants.mapNotNull { it.userId }.toSortedSet()
            val unlocked = holders - locked
            if (unlocked.isEmpty()) return descendants
            unlocked.forEach { authPrincipalVersionDao.lockPrincipal(it) }
            locked += unlocked
        }
    }

    companion object {
        /** Safety cap on the delegation-chain walk; the column is a plain self-reference. */
        private const val MAX_CHAIN_WALK_DEPTH = 32
    }

    /**
     * Full effective role set for [userId]: direct + group-derived + ancestor (parent-chain) roles.
     * This is the set against which the SoD check is run — it mirrors the expansion that
     * ResourceIdsByUserIdCache performs when computing accessible resources.
     */
    private fun computeEffectiveRoleIds(userId: String): Set<String> {
        val direct = dao.searchRoleIdsByUserId(userId)
        val groupIds = authGroupUserDao.searchGroupIdsByUserId(userId)
        val groupDerived = if (groupIds.isEmpty()) emptyList()
        else groupIds.flatMap { gid -> authGroupRoleDao.searchRoleIdsByGroupId(gid) }
        val granted = (direct + groupDerived).distinct()
        if (granted.isEmpty()) return emptySet()
        val ancestors = authRoleDao.searchAncestorRoleIds(granted)
        return (granted + ancestors).toSet()
    }
}
