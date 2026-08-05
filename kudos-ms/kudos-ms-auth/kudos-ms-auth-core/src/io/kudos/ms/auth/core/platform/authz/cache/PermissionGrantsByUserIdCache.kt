package io.kudos.ms.auth.core.platform.authz.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.authz.enums.PermissionEffect
import io.kudos.ms.auth.common.authz.vo.PermissionGrantVo
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUpdated
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.platform.cache.AffectedUserResolver
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleResourceRelationsChanged
import io.kudos.ms.auth.core.role.event.AuthRoleUpdated
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.source.ExternalRoleSourceChanged
import io.kudos.ms.auth.core.role.source.RoleSourceRegistry
import io.kudos.ms.sys.core.resource.cache.SysResourceHashCache
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import jakarta.annotation.Resource
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * The decision point's working set: every permission binding a principal holds, flattened.
 *
 * Resolution path — direct roles ∪ group-relayed roles, expanded up the parent chain, deactivated
 * entries dropped, then every binding of those roles. Each binding is projected to
 * `(permissionCode, effect, condition, roleId)`:
 *
 * - a **code-addressed** binding contributes its own code;
 * - a **resource-id-addressed** binding contributes the `permission_code` registered on that
 *   `sys_resource` row, and contributes *nothing* when the resource has no code yet. That is the
 *   honest behaviour during the migration to codes: an unregistered permission point is not
 *   enforceable, and the enforcement layer is opt-in precisely so a deployment can register its
 *   points before switching it on.
 *
 * Invalidation mirrors [io.kudos.ms.auth.core.platform.cache.ResourceIdsByUserIdCache] — the same
 * events change the same underlying relations.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class PermissionGrantsByUserIdCache : AbstractKeyValueCacheHandler<List<PermissionGrantVo>>() {

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Resource
    private lateinit var sysResourceHashCache: SysResourceHashCache

    @Resource
    private lateinit var affectedUserResolver: AffectedUserResolver

    @Resource
    private lateinit var roleSourceRegistry: RoleSourceRegistry

    companion object {
        private const val CACHE_NAME = "AUTH_PERMISSION_GRANTS_BY_USER_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<PermissionGrantVo> =
        getSelf<PermissionGrantsByUserIdCache>().getGrants(key)

    /**
     * Warm-up is skipped on purpose: unlike the id-list caches, a grant list is only worth
     * pre-computing for principals that actually sign in, and the per-principal cost here is higher
     * (bindings, not ids). Entries are filled on first decision and kept fresh by the listeners.
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        if (clear) clear()
        log.debug("${CACHE_NAME} is lazily populated; nothing pre-loaded.")
    }

    /**
     * Every binding reachable by [userId], deduplicated.
     *
     * @param userId the principal
     * @return the resolved bindings; empty when the principal holds no active role
     */
    /**
     * Note the absence of an `unless` guard, unlike the id-list caches around it.
     *
     * Excluding the empty result looks like an economy and is the opposite. This method is on the
     * **decision path** — every enforced request calls it — so a principal who holds nothing was
     * re-resolved from the database on every single request they made, and the module's "the
     * decision path is a pure cache hit" contract was simply false for them. Measured during a
     * deliberate outage: one such principal produced eight `auth_role_user` queries.
     *
     * They are also the population that most needs to be cheap to refuse: a freshly created account,
     * one whose roles were just revoked, a misconfigured client retrying. Those are exactly the
     * callers a system should be able to turn away without touching storage — and, before this,
     * exactly the ones who broke first when the cache tier went down while permitted users sailed on
     * from their cached entries.
     *
     * "Nobody granted them anything" is a real, stable answer, invalidated by the same events as any
     * other. The key space is bounded by the authenticated principal population, so caching it costs
     * no more than caching the positives.
     */
    @Cacheable(cacheNames = [CACHE_NAME], key = "#userId")
    open fun getGrants(userId: String): List<PermissionGrantVo> {
        val direct = authRoleUserDao.searchRoleIdsByUserId(userId)
        val groupIds = authGroupDao.filterActiveGroupIds(authGroupUserDao.searchGroupIdsByUserId(userId))
        val groupDerived = if (groupIds.isEmpty()) emptyList()
        else groupIds.flatMap { gid -> authGroupRoleDao.searchRoleIdsByGroupId(gid) }
        // Roles held by virtue of something outside auth (a position, an org attribute). Resolved
        // through the same registry as the effective-role cache so the two never disagree about
        // what somebody holds — see IRoleSourceProvider.
        val external = roleSourceRegistry.contributedRoleIds(userId)
        val granted = (direct + groupDerived + external).distinct()
        if (granted.isEmpty()) return emptyList()

        val ancestors = authRoleDao.searchAncestorRoleIds(granted)
        val effectiveRoleIds = authRoleDao.filterActiveRoleIds(granted + ancestors)
        if (effectiveRoleIds.isEmpty()) return emptyList()

        val bindings = authRoleResourceDao.searchBindingsByRoleIds(effectiveRoleIds)
        if (bindings.isEmpty()) return emptyList()

        // Resource-addressed bindings need their registered code; fetch them in one batch.
        val resourceIds = bindings.mapNotNull { it.resourceId?.trim() }.filter { it.isNotEmpty() }.toSet()
        val resources = if (resourceIds.isEmpty()) emptyMap() else sysResourceHashCache.getResourcesByIds(resourceIds)

        val grants = bindings.mapNotNull { binding ->
            val code = binding.permissionCode?.trim()?.takeIf { it.isNotEmpty() }
                ?: binding.resourceId?.trim()?.let { resources[it]?.permissionCode?.trim() }?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            PermissionGrantVo(
                permissionCode = code,
                effect = PermissionEffect.fromCode(binding.effect),
                condition = binding.condition?.trim()?.takeIf { it.isNotEmpty() },
                roleId = binding.roleId,
            )
        }.distinct()

        log.debug(
            "Resolved ${grants.size} permission grant(s) for user ${userId} " +
                "from ${effectiveRoleIds.size} active effective role(s) and ${bindings.size} binding(s).",
        )
        return grants
    }

    /** Invalidate in batch; the next decision recomputes. */
    private fun syncByUserIds(userIds: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        userIds.forEach { userId ->
            KeyValueCacheKit.evict(CACHE_NAME, userId)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<PermissionGrantsByUserIdCache>().getGrants(userId)
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleUserRelationsChanged): Unit = syncByUserIds(event.userIds)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged): Unit = syncByUserIds(event.userIds)

    /** A role's bindings changed — everyone holding it or any of its descendants is affected. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleResourceRelationsChanged): Unit =
        syncByUserIds(affectedUserResolver.usersAffectedByRoleChange(event.roleId))

    /** `active` / `parent_id` on the role row itself: same affected population. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleUpdated): Unit =
        syncByUserIds(affectedUserResolver.usersAffectedByRoleChange(event.id))

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupRoleRelationsChanged): Unit =
        syncByUserIds(affectedUserResolver.usersAffectedByGroupChange(event.groupId))

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUpdated): Unit =
        syncByUserIds(affectedUserResolver.usersAffectedByGroupChange(event.id))

    /** An external role source changed its answer; only the application can tell us. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: ExternalRoleSourceChanged): Unit = syncByUserIds(event.principalIds)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountDeleted) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) KeyValueCacheKit.evict(CACHE_NAME, event.id)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountBatchDeleted) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        event.ids.forEach { KeyValueCacheKit.evict(CACHE_NAME, it) }
    }

    private val log = LogFactory.getLog(this::class)
}
