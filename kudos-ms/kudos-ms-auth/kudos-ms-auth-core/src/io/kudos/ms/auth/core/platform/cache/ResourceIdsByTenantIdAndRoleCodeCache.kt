package io.kudos.ms.auth.core.platform.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.context.support.Consts
import io.kudos.ms.auth.core.role.cache.AuthRoleHashCache
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.event.AuthRoleBatchDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import jakarta.annotation.Resource
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Cache handler for the list of resource IDs keyed by (tenantId, roleCode).
 *
 * 1. Source tables: auth_role + auth_role_resource.
 * 2. Caches the resource ID list of the specified role per tenant.
 * 3. Cache key: tenantId::roleCode.
 * 4. Cache value: list of resource IDs (List<String>).
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class ResourceIdsByTenantIdAndRoleCodeCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Resource
    private lateinit var authRoleHashCache: AuthRoleHashCache

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    companion object {
        private const val CACHE_NAME = "AUTH_RESOURCE_IDS_BY_TENANT_ID_AND_ROLE_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "Cache ${CACHE_NAME} key format must be tenantId${Consts.CACHE_KEY_DEFAULT_DELIMITER}roleCode"
        }
        val tenantAndRoleCode = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<ResourceIdsByTenantIdAndRoleCodeCache>().getResourceIds(
            tenantAndRoleCode[0], tenantAndRoleCode[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache disabled; not loading or caching resource ids for all roles!")
            return
        }

        // Load all roles with active=true.
        val roleCriteria = Criteria(AuthRole::active.name, OperatorEnum.EQ, true)

        val roles = authRoleDao.search(roleCriteria)

        // Load all role-resource associations.
        val allRoleResources = authRoleResourceDao.allSearch()
        val roleIdToResourceIdsMap = allRoleResources
            .groupBy { it.roleId }
            .mapValues { entry -> entry.value.map { it.resourceId.trim() } }

        log.debug("Loaded ${roles.size} roles and ${allRoleResources.size} role-resource associations from DB.")

        // Clear the cache.
        if (clear) {
            clear()
        }

        // Cache the resource id lists.
        roles.forEach { role ->
            val roleId = role.id
            if (roleId.isBlank()) return@forEach
            val resourceIds = roleIdToResourceIdsMap[roleId] ?: emptyList()
            KeyValueCacheKit.put(CACHE_NAME, getKey(role.tenantId, role.code), resourceIds)
            log.debug("Cached ${resourceIds.size} resource ids for tenant=${role.tenantId} role=${role.code}.")
        }
    }

    /**
     * Get all resource ids owned by a role keyed by (tenantId, roleCode); on cache miss, load from DB and write back.
     *
     * @param tenantId tenant id
     * @param roleCode role code
     * @return List<resourceId>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#roleCode)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getResourceIds(tenantId: String, roleCode: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Resource ids for tenant=${tenantId} role=${roleCode} not in cache; loading from DB...")
        }

        // 1. Look up the role id from the cache (avoid hitting the DB).
        val roleId = authRoleHashCache.getRoleByTenantIdAndRoleCode(tenantId, roleCode)?.id

        if (roleId == null) {
            log.debug("Role not found for tenant=${tenantId} code=${roleCode}.")
            return emptyList()
        }

        // 2. Query resource ids by role id.
        val resourceCriteria = Criteria(AuthRoleResource::roleId.name, OperatorEnum.EQ, roleId)
        val resourceIds = authRoleResourceDao.searchProperty(resourceCriteria, AuthRoleResource::resourceId)

        log.debug("Loaded ${resourceIds.size} resource ids from DB for tenant=${tenantId} role=${roleCode}.")
        return resourceIds.filterNotNull().map { it.trim() }
    }

    /**
     * Sync the cache after a role-resource association is inserted.
     *
     * @param tenantId tenant id
     * @param roleCode role code
     */
    open fun syncOnRoleResourceInsert(tenantId: String, roleCode: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After inserting role-resource association tenant=${tenantId} role=${roleCode}, syncing ${CACHE_NAME} cache...")
            evict(getKey(tenantId, roleCode))
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ResourceIdsByTenantIdAndRoleCodeCache>().getResourceIds(tenantId, roleCode)
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after a role-resource association is deleted.
     *
     * @param tenantId tenant id
     * @param roleCode role code
     */
    open fun syncOnRoleResourceDelete(tenantId: String, roleCode: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After deleting role-resource association tenant=${tenantId} role=${roleCode}, syncing ${CACHE_NAME} cache...")
            evict(getKey(tenantId, roleCode))
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ResourceIdsByTenantIdAndRoleCodeCache>().getResourceIds(tenantId, roleCode)
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after role info is updated (role code or active flag changed).
     *
     * @param oldTenantId previous tenant id
     * @param oldRoleCode previous role code
     * @param newTenantId new tenant id (equal to the old one if unchanged)
     * @param newRoleCode new role code (equal to the old one if unchanged)
     */
    open fun syncOnRoleUpdate(oldTenantId: String, oldRoleCode: String, newTenantId: String, newRoleCode: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After role info update, syncing ${CACHE_NAME} cache...")

            // Evict the old cache entry.
            KeyValueCacheKit.evict(CACHE_NAME, getKey(oldTenantId, oldRoleCode))

            // If code or tenant changed, also evict the new cache entry (if present).
            if (oldTenantId != newTenantId || oldRoleCode != newRoleCode) {
                KeyValueCacheKit.evict(CACHE_NAME, getKey(newTenantId, newRoleCode))
            }

            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }


    /**
     * Build the cache key by joining the given parameters.
     *
     * @param tenantId tenant id
     * @param roleCode role code
     * @return cache key
     */
    fun getKey(tenantId: String, roleCode: String): String {
        return "${tenantId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${roleCode}"
    }

    /** Local (tenantId, roleCode) eviction only; AuthRoleHashCache subscribes to AuthRoleDeleted separately. */
    private fun evictBy(tenantId: String, roleCode: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, getKey(tenantId, roleCode))
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleDeleted): Unit = evictBy(event.tenantId, event.code)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleBatchDeleted) {
        event.items.forEach { evictBy(it.tenantId, it.code) }
    }

    private val log = LogFactory.getLog(this::class)

}