package io.kudos.ms.auth.core.role.datascope.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.datascope.vo.response.DataScopeVo
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUpdated
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.platform.cache.AffectedUserResolver
import io.kudos.ms.auth.core.role.datascope.event.AuthRoleScopeChanged
import io.kudos.ms.auth.core.role.datascope.service.iservice.IAuthRoleDataScopeService
import io.kudos.ms.auth.core.role.event.AuthRoleUpdated
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.source.ExternalRoleSourceChanged
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.ms.user.core.account.event.UserAccountUpdated
import io.kudos.ms.user.core.org.event.UserOrgBatchDeleted
import io.kudos.ms.user.core.org.event.UserOrgDeleted
import io.kudos.ms.user.core.org.event.UserOrgUpdated
import jakarta.annotation.Resource
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * A principal's resolved [DataScopeVo], cached.
 *
 * **Why this cache has to exist.** Row filtering runs on the *query* path: every `select` through a
 * `@DataScoped` entity asks for the current subject's scope. Resolving it touches the role cache,
 * the role hash cache, then — for anything narrower than ALL — the database: the org subtree
 * expansion in [io.kudos.ms.auth.core.role.datascope.spi.OrgScopeDimensionProvider.expand] and the
 * CUSTOM grants in `auth_role_scope`. Uncached, every business query of every restricted user pays
 * one to N extra round trips, and it starts costing that the moment shadow mode is switched on —
 * precisely the step this module asks deployments to take first.
 *
 * **Why its TTL is bounded, unlike every other cache in this module.** The others are invalidated
 * exhaustively because every input is a table kudos owns and every write publishes an event. This
 * one's inputs include whatever an application contributed through
 * [io.kudos.ms.auth.core.role.datascope.spi.IScopeDimensionProvider] — a `region` or `project`
 * dimension backed by tables this module has never heard of, whose writes it cannot observe. The
 * listeners below cover every kudos-owned input exactly; the TTL is what bounds staleness for the
 * ones it structurally cannot. An application that wants immediate invalidation on its own
 * dimension evicts this cache by name from its own write path.
 *
 * Cache key: userId; value: [DataScopeVo].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class DataScopeByUserIdCache : AbstractKeyValueCacheHandler<DataScopeVo>() {

    /**
     * The service and this cache are mutually dependent by design: the cache owns the caching, the
     * service owns the computation, and the service's `resolveUserDataScope` reads through here.
     *
     * `@Lazy` breaks the bean-creation cycle on the edge where it costs nothing — this reference is
     * dereferenced only on a cache miss, long after both beans exist. Putting it on the other edge
     * would proxy the far hotter read path instead.
     */
    @Lazy
    @Resource
    private lateinit var dataScopeService: IAuthRoleDataScopeService

    @Resource
    private lateinit var affectedUserResolver: AffectedUserResolver

    companion object {
        private const val CACHE_NAME = "AUTH_DATA_SCOPE_BY_USER_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): DataScopeVo = getSelf<DataScopeByUserIdCache>().getDataScope(key)

    /**
     * Lazily populated, like the permission-grant cache: a resolved scope is only worth computing
     * for principals that actually issue queries, and pre-computing it for every account would run
     * the org expansion for the whole directory at boot.
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        if (clear) clear()
        log.debug("${CACHE_NAME} is lazily populated; nothing pre-loaded.")
    }

    /**
     * The principal's effective row-visibility policy.
     *
     * Note the absence of an `unless` guard: unlike the id-list caches, *every* value here is worth
     * caching. The narrow ones especially — `selfOnly()` is what a restricted user resolves to, and
     * skipping the cache for it would leave exactly the population this cache exists for uncached.
     *
     * @param userId the principal
     * @return the resolved scope
     */
    @Cacheable(cacheNames = [CACHE_NAME], key = "#userId")
    open fun getDataScope(userId: String): DataScopeVo = dataScopeService.computeUserDataScope(userId)

    private fun syncByUserIds(userIds: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        userIds.forEach { userId ->
            KeyValueCacheKit.evict(CACHE_NAME, userId)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<DataScopeByUserIdCache>().getDataScope(userId)
            }
        }
    }

    private fun evictByUserId(userId: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) KeyValueCacheKit.evict(CACHE_NAME, userId)
    }

    /** The principal's effective role set changed, so the roles contributing scope changed with it. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleUserRelationsChanged): Unit = syncByUserIds(event.userIds)

    /** ...including when the change came from an external role source rather than from auth. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: ExternalRoleSourceChanged): Unit = syncByUserIds(event.principalIds)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged): Unit = syncByUserIds(event.userIds)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupRoleRelationsChanged): Unit =
        syncByUserIds(affectedUserResolver.usersAffectedByGroupChange(event.groupId))

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUpdated): Unit =
        syncByUserIds(affectedUserResolver.usersAffectedByGroupChange(event.id))

    /**
     * The role row itself: `data_scope` lives on it, and so do `active` and `parent_id`, either of
     * which changes whose scope this role contributes to. Same affected population as the other
     * role-driven caches — holders of the role and of its descendants.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleUpdated): Unit =
        syncByUserIds(affectedUserResolver.usersAffectedByRoleChange(event.id))

    /** A CUSTOM role's granted values changed. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleScopeChanged): Unit =
        syncByUserIds(affectedUserResolver.usersAffectedByRoleChange(event.roleId))

    /**
     * The account row carries `org_id` — the subject's own position on the built-in dimension, which
     * ORG and ORG_AND_CHILD resolve against. Moving somebody between departments changes what they
     * can see without touching a single auth table.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountUpdated): Unit = syncByUserIds(listOf(event.id))

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountDeleted): Unit = evictByUserId(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountBatchDeleted) {
        event.ids.forEach(::evictByUserId)
    }

    /**
     * The org tree changed shape, so every ORG_AND_CHILD expansion and every CUSTOM org grant may
     * now cover a different set. Which principals those are is itself a subtree walk against the
     * table that just moved, so this clears wholesale rather than computing an affected set from a
     * structure mid-edit. Org edits are rare; a restricted user's first query afterwards pays one
     * resolution.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgUpdated): Unit = clearAllOnOrgTreeChange()

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgDeleted): Unit = clearAllOnOrgTreeChange()

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgBatchDeleted): Unit = clearAllOnOrgTreeChange()

    private fun clearAllOnOrgTreeChange() {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        clear()
        log.debug("Org tree changed; cleared ${CACHE_NAME} wholesale.")
    }

    private val log = LogFactory.getLog(this::class)
}
