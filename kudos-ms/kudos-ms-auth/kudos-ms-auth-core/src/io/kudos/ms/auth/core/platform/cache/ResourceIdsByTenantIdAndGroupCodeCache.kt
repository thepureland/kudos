package io.kudos.ms.auth.core.platform.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.auth.core.group.cache.AuthGroupHashCache
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.event.AuthGroupBatchDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupDeleted
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.event.AuthRoleResourceRelationsChanged
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Cache handler for the list of resource ids keyed by (tenantId, groupCode).
 *
 * 1. Source tables: auth_group + auth_group_role + auth_role_resource
 * 2. Caches the resource id set of the specified group per tenant
 * 3. Cache key: tenantId::groupCode
 * 4. Cache value: list of resource ids (List<String>)
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class ResourceIdsByTenantIdAndGroupCodeCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authGroupHashCache: AuthGroupHashCache

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    companion object {
        private const val CACHE_NAME = "AUTH_RESOURCE_IDS_BY_TENANT_ID_AND_GROUP_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "Cache ${CACHE_NAME} key format must be tenantId${Consts.CACHE_KEY_DEFAULT_DELIMITER}groupCode"
        }
        val tenantAndGroupCode = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<ResourceIdsByTenantIdAndGroupCodeCache>().getResourceIds(
            tenantAndGroupCode[0], tenantAndGroupCode[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache disabled; not loading or caching resource ids for all groups!")
            return
        }

        val groups = authGroupDao.searchActiveGroupsForCache()
        val groupIdToRoleIdsMap = authGroupRoleDao.searchAllGroupIdToRoleIdsForCache()
        val roleIdToResourceIdsMap = authRoleResourceDao.searchAllRoleIdToResourceIdsForCache()

        log.debug("Loaded ${groups.size} groups, ${groupIdToRoleIdsMap.size} group-role groupings, ${roleIdToResourceIdsMap.size} role-resource groupings from DB.")

        if (clear) {
            clear()
        }

        groups.forEach { group ->
            val groupId = group.id
            if (groupId.isBlank()) return@forEach
            val tenantId = group.tenantId ?: return@forEach
            val groupCode = group.code ?: return@forEach
            val roleIds = groupIdToRoleIdsMap[groupId] ?: emptyList()
            val resourceIds = roleIds.flatMap { roleId -> roleIdToResourceIdsMap[roleId] ?: emptyList() }.distinct()
            KeyValueCacheKit.put(CACHE_NAME, getKey(tenantId, groupCode), resourceIds)
            log.debug("Cached ${resourceIds.size} resource ids for tenant=${group.tenantId} group=${group.code}.")
        }
    }

    /**
     * Get all resource ids under a group keyed by (tenantId, groupCode); on cache miss, load from DB and write back.
     *
     * @param tenantId tenant id
     * @param groupCode group code
     * @return List<resourceId>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#groupCode)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getResourceIds(tenantId: String, groupCode: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Resource ids for tenant=${tenantId} group=${groupCode} not in cache; loading from DB...")
        }

        // 1. Look up the group id from the cache.
        val groupId = authGroupHashCache.getGroupByTenantIdAndGroupCode(tenantId, groupCode)?.id
        if (groupId == null) {
            log.debug("Group not found for tenant=${tenantId} code=${groupCode}.")
            return emptyList()
        }

        // 2. Get the role ids associated with the group.
        val roleIds = authGroupRoleDao.searchRoleIdsByGroupId(groupId)
        if (roleIds.isEmpty()) {
            return emptyList()
        }

        val resourceIds = authRoleResourceDao.searchResourceIdsByRoleIds(roleIds)
        log.debug("Loaded ${resourceIds.size} resource ids from DB for tenant=${tenantId} group=${groupCode}.")
        return resourceIds.toList()
    }

    /**
     * Sync the cache after a group-role association is inserted.
     */
    open fun syncOnGroupRoleInsert(tenantId: String, groupCode: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After inserting group-role association tenant=${tenantId} group=${groupCode}, syncing ${CACHE_NAME} cache...")
            evict(getKey(tenantId, groupCode))
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ResourceIdsByTenantIdAndGroupCodeCache>().getResourceIds(tenantId, groupCode)
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after a group-role association is deleted.
     */
    open fun syncOnGroupRoleDelete(tenantId: String, groupCode: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After deleting group-role association tenant=${tenantId} group=${groupCode}, syncing ${CACHE_NAME} cache...")
            evict(getKey(tenantId, groupCode))
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ResourceIdsByTenantIdAndGroupCodeCache>().getResourceIds(tenantId, groupCode)
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after a role-resource association changes.
     */
    open fun syncOnRoleResourceChange(roleId: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After role-resource association change for role=${roleId}, syncing ${CACHE_NAME} cache...")
            // Simplification: clear the whole cache to avoid a complex reverse lookup.
            clear()
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after a group is updated.
     */
    open fun syncOnGroupUpdate(oldTenantId: String, oldGroupCode: String, newTenantId: String, newGroupCode: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After group info update, syncing ${CACHE_NAME} cache...")
            KeyValueCacheKit.evict(CACHE_NAME, getKey(oldTenantId, oldGroupCode))
            if (oldTenantId != newTenantId || oldGroupCode != newGroupCode) {
                KeyValueCacheKit.evict(CACHE_NAME, getKey(newTenantId, newGroupCode))
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    fun getKey(tenantId: String, groupCode: String): String {
        return "${tenantId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${groupCode}"
    }

    /** Local (tenantId, groupCode) eviction only; AuthGroupHashCache subscribes to AuthGroupDeleted separately. */
    private fun evictBy(tenantId: String, groupCode: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, getKey(tenantId, groupCode))
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupDeleted): Unit = evictBy(event.tenantId, event.code)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupBatchDeleted) {
        event.items.forEach { evictBy(it.tenantId, it.code) }
    }

    /**
     * Role-resource changes affect the group -> role -> resource three-level aggregate view: clear the cache conservatively.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleResourceRelationsChanged): Unit = syncOnRoleResourceChange(event.roleId)

    private val log = LogFactory.getLog(this::class)

}
