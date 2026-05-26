package io.kudos.ms.user.core.org.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.account.event.UserOrgUserAdminUpdated
import io.kudos.ms.user.core.account.event.UserOrgUserRelationsChanged
import io.kudos.ms.user.core.account.model.po.UserOrgUser
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.event.UserOrgBatchDeleted
import io.kudos.ms.user.core.org.event.UserOrgDeleted
import io.kudos.ms.user.core.org.event.UserOrgUpdated
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * User ID list (by org) cache handler.
 *
 * **Semantics: "the set of users under this org and all its active descendant orgs"**:
 * - Source: `user_org_user` (direct mount) + `user_org.parent_id` (recursive descendant expansion)
 * - The cached value is the deduplicated list of user IDs for self + all active descendant orgs
 *
 * Business rationale: viewing descendant org members from a parent org is a common requirement
 * (e.g., "the sales director views all sales department members"). If a caller only needs direct
 * members, use [UserOrgUserDao.searchUserIdsByOrgId] directly instead of this cache.
 *
 * Cache key: orgId; value: List<String>.
 *
 * **Invalidation**:
 * - Relation changes ([UserOrgUserRelationsChanged] / [UserOrgUserAdminUpdated]) affect the views
 *   of orgId and all its ancestors, so the listener invalidates the entire chain up via parent_id.
 * - Org tree mutations:
 *   - [UserOrgUpdated] carries oldParentId / newParentId snapshots. For non-move updates (active
 *     toggle, name change, etc.), the two are equal and a single ancestor chain is invalidated.
 *     For move updates (moveOrg), the two differ and both old and new chains are invalidated.
 *   - [UserOrgDeleted] / [UserOrgBatchDeleted] carry the parentId snapshot from before deletion
 *     and invalidate up the ancestor chain; orgId itself is also invalidated (the cache entry may
 *     still exist even if the org was deleted).
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class UserIdsByOrgIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var userOrgUserDao: UserOrgUserDao

    @Autowired
    private lateinit var userOrgDao: UserOrgDao

    companion object {
        private const val CACHE_NAME = "USER_IDS_BY_ORG_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<UserIdsByOrgIdCache>().getUserIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is not enabled; skip loading and caching user IDs for all orgs!")
            return
        }

        val orgIdToParentId = userOrgDao.searchAllOrgIdToParentId()
        val orgIdToDirectUserIds = userOrgUserDao.searchAllOrgIdToUserIds()
        // Build a reverse parentId -> [childId] index; keep only entries where parent != null (root orgs are skipped from the index but still evaluated)
        val parentToChildren: Map<String, List<String>> = orgIdToParentId.entries
            .mapNotNull { (childId, parentId) -> parentId?.let { it to childId } }
            .groupBy({ it.first }, { it.second })
        log.debug(
            "Loaded ${orgIdToParentId.size} orgs, ${orgIdToDirectUserIds.size} org-user groups, built ${parentToChildren.size} parent-child index entries."
        )

        if (clear) {
            clear()
        }

        orgIdToParentId.keys.forEach { rootOrgId ->
            val included = mutableSetOf(rootOrgId)
            val queue = ArrayDeque<String>().apply { add(rootOrgId) }
            while (queue.isNotEmpty()) {
                parentToChildren[queue.removeFirst()]?.forEach { childId ->
                    if (included.add(childId)) queue.add(childId)
                }
            }
            val userIds = included.flatMap { orgIdToDirectUserIds[it].orEmpty() }.distinct()
            if (userIds.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, rootOrgId, userIds)
                log.debug("Cached ${userIds.size} user IDs for org ${rootOrgId} (covering ${included.size} subtree nodes).")
            }
        }
    }

    /**
     * Get all user IDs under the given org (including descendant org members recursively, deduplicated).
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#orgId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getUserIds(orgId: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("No user IDs for org ${orgId} in the cache; loading from the database...")
        }
        val orgIds = userOrgDao.searchOrgAndDescendantIds(orgId)
        val userIds = userOrgUserDao.searchUserIdsByOrgIds(orgIds).distinct()
        log.debug("Loaded ${userIds.size} user IDs for org ${orgId} and its descendants (${orgIds.size} orgs total) from the database.")
        return userIds
    }

    /** @deprecated Retained for backward compatibility; new code should use the event mechanism. */
    open fun syncOnInsert(any: Any, id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("Org-user relation with id ${id} inserted; syncing ${CACHE_NAME} cache...")
        val orgId = BeanKit.getProperty(any, UserOrgUser::orgId.name) as String
        syncByOrgAndAncestors(orgId)
    }

    /** @deprecated Retained for backward compatibility. */
    open fun syncOnUpdate(any: Any?, id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("Org-user relation with id ${id} updated; syncing ${CACHE_NAME} cache...")
        val orgId = if (any == null) {
            requireNotNull(userOrgUserDao.get(id)) { "Record with id=$id not found while syncing the org-user relation cache." }.orgId
        } else {
            BeanKit.getProperty(any, UserOrgUser::orgId.name) as String
        }
        syncByOrgAndAncestors(orgId)
    }

    /** @deprecated Retained for backward compatibility. */
    open fun syncOnDelete(any: Any, id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val orgId = BeanKit.getProperty(any, UserOrgUser::orgId.name) as String
        log.debug("Org-user relation with id ${id} deleted; evicting from ${CACHE_NAME} cache...")
        syncByOrgAndAncestors(orgId)
    }

    /** @deprecated Retained for backward compatibility. */
    open fun syncOnBatchDelete(ids: Collection<String>, orgIds: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("Org-user relations with ids ${ids} batch-deleted; evicting from ${CACHE_NAME} cache...")
        orgIds.toSet().forEach { syncByOrgAndAncestors(it) }
    }

    /**
     * @deprecated Retained as a legacy public entry point for backward compatibility; new code
     * should use the event mechanism [UserOrgUserRelationsChanged].
     *
     * Difference from before: this method now walks up parent_id and invalidates both orgId itself
     * and all ancestor org cache entries. Reason: ancestor org user views include orgId's members,
     * so a relation change requires recomputation across the parent chain.
     */
    open fun syncOnOrgUserChange(orgId: String) = syncByOrgAndAncestors(orgId)

    /**
     * Unified internal invalidation path: invalidate orgId and all its ancestors' caches.
     */
    private fun syncByOrgAndAncestors(orgId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val chain = userOrgDao.searchOrgAndAncestorIds(orgId)
        chain.forEach { id ->
            KeyValueCacheKit.evict(CACHE_NAME, id)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByOrgIdCache>().getUserIds(id)
            }
        }
        log.debug("${CACHE_NAME} cache sync completed; invalidated ${chain.size} orgs up the ancestor chain.")
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgUserRelationsChanged): Unit = syncByOrgAndAncestors(event.orgId)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgUserAdminUpdated): Unit = syncByOrgAndAncestors(event.orgId)

    /**
     * The org itself has been mutated (active toggle / moveOrg / generic update). Invalidation strategy:
     * - Self: the cache entry may contain a subtree aggregate and must be recomputed -> evict
     * - oldParentId ancestor chain: after a move, the old chain's "includes subtree members" view
     *   no longer contains this org, so evict
     * - newParentId ancestor chain: after a move, the new chain gains this org's subtree, so evict
     *   For non-move updates, oldParentId == newParentId; the two chains merge into one and evict once
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgUpdated) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        evictAndReload(event.id)
        // Fallback to a now-DB-lookup when old is not provided (legacy publishers wouldn't do this, but kept robust). null means it is already a root.
        listOfNotNull(event.oldParentId, event.newParentId).distinct().forEach { syncByOrgAndAncestors(it) }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgDeleted) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        // The deleted org's own cache entry must also be cleared; its ancestor chain loses this subtree's members
        evictAndReload(event.id)
        event.parentId?.let { syncByOrgAndAncestors(it) }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgBatchDeleted) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        // Each entry independently invalidates self + ancestor chain. Ancestor chain IDs may repeat -- use a set to dedupe and save a few DB walks.
        event.items.forEach { evictAndReload(it.id) }
        val parentIds = event.items.mapNotNullTo(mutableSetOf()) { it.parentId }
        parentIds.forEach { syncByOrgAndAncestors(it) }
    }

    /** Single-key evict + on-demand write-in-time recompute. Used for the org's own entry (does not walk the parent chain). */
    private fun evictAndReload(orgId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, orgId)
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            // Recompute may return nothing if the org has already been deleted -> empty result; @Cacheable unless=isEmpty will not write back, OK
            getSelf<UserIdsByOrgIdCache>().getUserIds(orgId)
        }
    }

    private val log = LogFactory.getLog(this::class)

}
