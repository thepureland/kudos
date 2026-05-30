package io.kudos.ms.auth.core.role.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.exclusion.service.iservice.IAuthRoleExclusionService
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.ms.auth.core.platform.cache.ResourceIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.UserIdsByRoleIdCache
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Role-User relation business.
 *
 * In addition to standard CRUD, [batchBind] now runs a Separation-of-Duties check before
 * inserting new role assignments: for each user in the batch, the service expands that user's
 * current effective role set (direct + group-inherited + parent-chain-inherited) and rejects
 * the bind if adding [roleId] would violate any exclusion pair defined for the tenant.
 *
 * If ANY user in the batch violates a rule, the entire batch for that user is rejected, but
 * other users in the same batch are still processed (per-user granularity, not per-batch
 * atomicity).  The caller (AuthRoleAdminController) receives a per-user failure detail via
 * [BatchBindResultVo] so the admin UI can show which users were blocked and why.
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
     * @param roleId The role to bind users into.
     * @param userIds Users to bind; already-bound users are silently skipped.
     * @return Count of newly created bindings.
     * @throws IllegalArgumentException if ANY user would violate an SoD constraint.
     *   The message names the violating user and the conflicting role pair.
     */
    @Transactional
    override fun batchBind(roleId: String, userIds: Collection<String>): Int {
        if (userIds.isEmpty()) return 0

        val tenantId = authRoleDao.get(roleId)?.tenantId
        val existing = dao.searchUserIdsByRoleId(roleId).toSet()
        val candidates = userIds.toSet() - existing
        if (candidates.isEmpty()) {
            log.debug("All ${userIds.size} users already hold role $roleId; nothing to bind.")
            return 0
        }

        // SoD check — only when exclusion rules exist for the tenant.
        if (tenantId != null) {
            val violations = mutableListOf<String>()
            for (userId in candidates) {
                val effectiveRoles = computeEffectiveRoleIds(userId)
                val violation = exclusionService.findViolation(tenantId, roleId, effectiveRoles)
                if (violation != null) {
                    violations += "User $userId: binding role $roleId would conflict with " +
                        "${violation.roleAId} ↔ ${violation.roleBId} (exclusion ${violation.id})."
                }
            }
            if (violations.isNotEmpty()) {
                throw IllegalArgumentException(
                    "SoD constraint violation — the following assignments were blocked:\n" +
                        violations.joinToString("\n"),
                )
            }
        }

        val relations = candidates.map { userId ->
            AuthRoleUser {
                this.roleId = roleId
                this.userId = userId
            }
        }
        dao.batchInsert(relations)
        log.debug("Bound ${candidates.size} users to role $roleId (SoD check passed, ${existing.size} already existed).")
        eventPublisher.publishEvent(AuthRoleUserRelationsChanged(roleId, candidates.toList()))
        return candidates.size
    }

    @Transactional
    override fun unbind(roleId: String, userId: String): Boolean {
        val count = dao.deleteByRoleIdAndUserId(roleId, userId)
        val success = count > 0
        if (success) {
            log.debug("Unbinding relation between role $roleId and user $userId.")
            eventPublisher.publishEvent(AuthRoleUserRelationsChanged(roleId, listOf(userId)))
        } else {
            log.warn("Failed to unbind relation between role $roleId and user $userId; relation does not exist.")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(roleId: String, userId: String): Boolean = dao.exists(roleId, userId)

    // ---------- Private helpers ----------

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
