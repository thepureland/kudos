package io.kudos.ms.auth.core.platform.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.event.AuthRoleBatchDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleResourceRelationsChanged
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Cache handler for the list of resource IDs keyed by role id.
 *
 * 1. Source table: auth_role_resource.
 * 2. Caches the full set of resource IDs owned by each role.
 * 3. Cache key: roleId.
 * 4. Cache value: collection of resource IDs (List<String>).
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class ResourceIdsByRoleIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Autowired
    private lateinit var authRoleDao: AuthRoleDao

    companion object {
        private const val CACHE_NAME = "AUTH_RESOURCE_IDS_BY_ROLE_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<ResourceIdsByRoleIdCache>().getResourceIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is disabled; skipping load and cache of resource IDs for all roles.")
            return
        }

        val roles = authRoleDao.searchActiveRolesForCache()
        val roleIdToResourceIdsMap = authRoleResourceDao.searchAllRoleIdToResourceIdsForCache()

        log.debug("Loaded ${roles.size} roles and ${roleIdToResourceIdsMap.size} role-resource relation groups from the database.")

        // Clear the cache.
        if (clear) {
            clear()
        }

        // Cache the resource ID list for each role.
        roles.forEach { role ->
            val roleId = role.id
            if (roleId.isBlank()) return@forEach
            val resourceIds = roleIdToResourceIdsMap[roleId] ?: emptyList()
            if (resourceIds.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, roleId, resourceIds)
                log.debug("Cached ${resourceIds.size} resource IDs for role ${roleId}.")
            }
        }
    }

    /**
     * Get all resource IDs owned by the given role from the cache. If absent, load from the
     * database and write back to the cache.
     *
     * @param roleId role id
     * @return list of resource IDs
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#roleId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getResourceIds(roleId: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Cache miss for resource IDs of role ${roleId}; loading from the database...")
        }

        val resourceIds = authRoleResourceDao.searchResourceIdsByRoleIds(setOf(roleId)).toList()
        log.debug("Loaded ${resourceIds.size} resource IDs for role ${roleId} from the database.")
        return resourceIds
    }

    /**
     * Sync the cache after the role-resource relations of the given role change.
     *
     * @param roleId role id
     */
    open fun syncOnRoleResourceChange(roleId: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Role ${roleId} resource relations changed; syncing the ${CACHE_NAME} cache...")
            evict(roleId)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ResourceIdsByRoleIdCache>().getResourceIds(roleId)
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after a batch of role-resource relation changes.
     *
     * @param roleIds collection of role IDs
     */
    open fun syncOnBatchRoleResourceChange(roleIds: Collection<String>) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Batch role-resource relations changed; syncing the ${CACHE_NAME} cache...")
            roleIds.forEach { roleId ->
                KeyValueCacheKit.evict(CACHE_NAME, roleId)
                if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<ResourceIdsByRoleIdCache>().getResourceIds(roleId)
                }
            }
            log.debug("${CACHE_NAME} cache sync complete; ${roleIds.size} roles affected.")
        }
    }

    /** Evict the resourceId list for the given roleId after the role is deleted. */
    private fun evictByRoleId(roleId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, roleId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleResourceRelationsChanged): Unit = syncOnRoleResourceChange(event.roleId)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleDeleted): Unit = evictByRoleId(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleBatchDeleted) {
        event.ids.forEach(::evictByRoleId)
    }

    private val log = LogFactory.getLog(this::class)

}
