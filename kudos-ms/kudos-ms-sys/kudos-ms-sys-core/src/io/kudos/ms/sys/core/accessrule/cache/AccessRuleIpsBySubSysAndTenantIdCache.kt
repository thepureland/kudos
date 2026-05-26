package io.kudos.ms.sys.core.accessrule.cache
import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleIpQuery
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleDao
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleIpDao
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleBatchDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleInserted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpBatchDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpInserted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpUpdated
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleUpdated
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Cache handler for IP access rules.
 *
 * - Source tables: `sys_access_rule` & `sys_access_rule_ip`
 * - Only caches rules with `active=true`
 * - Cache key format: `systemCode::normalizedTenantId`. "Normalization" means [AccessRuleTenantKey.normalize]
 *   converts `null` / blank values to empty string, consistent with the secondary index lookups in [SysAccessRuleHashCache].
 * - Cache value: list of [SysAccessRuleIpCacheEntry]
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class AccessRuleIpsBySubSysAndTenantIdCache : AbstractKeyValueCacheHandler<List<SysAccessRuleIpCacheEntry>>() {

    @Autowired
    private lateinit var sysAccessRuleIpDao: SysAccessRuleIpDao

    @Autowired
    private lateinit var sysAccessRuleDao: SysAccessRuleDao

    companion object Companion {
        private const val CACHE_NAME = "SYS_ACCESS_RULE_IPS_BY_SYSTEM_CODE_AND_TENANT_ID"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): List<SysAccessRuleIpCacheEntry> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "Cache ${CACHE_NAME} key format is illegal!"
        }
        val parts = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER, limit = 2)
        // Already normalized: empty string means platform-level, corresponding to `tenant_id IS NULL` in DB; the query layer still uses null
        val tenantId = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
        return getSelf<AccessRuleIpsBySubSysAndTenantIdCache>().getAccessRuleIps(parts[0], tenantId)
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is not enabled; skipping load of all active IP access rules!")
            return
        }

        // Joined query
        val searchPayload = SysAccessRuleIpQuery(
            active = true,
            parentRuleActive = true
        )
        val results = sysAccessRuleIpDao.pagingSearch(searchPayload)

        // Clear cache
        if (clear) {
            clear()
        }

        // Cache data
        val ipRulesMap = results.mapNotNull { record ->
            val systemCode = record.systemCode ?: return@mapNotNull null
            AccessRuleTenantKey.compositeKey(systemCode, record.tenantId) to record
        }.groupBy({ it.first }, { it.second })
        ipRulesMap.forEach { (key, ipRules) ->
            val cacheItems = mapToCacheItems(ipRules)
            KeyValueCacheKit.put(CACHE_NAME, key, cacheItems)
        }
        log.debug("Cached ${results.size} IP access rules in total.")
    }

    /**
     * Get IP access rules from cache; on miss, load from DB and backfill.
     *
     * Cache key format: `systemCode::normalizedTenantId`. `null` / blank values are normalized to empty string
     * (platform-level), corresponding to `tenant_id IS NULL` in the DB.
     *
     * @param systemCode System code
     * @param tenantId Tenant id; pass `null` (or blank) for platform-level rules
     * @return List of IP cache entries; empty list if no match
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#systemCode.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat((#tenantId ?: '').trim())",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getAccessRuleIps(systemCode: String, tenantId: String? = null): List<SysAccessRuleIpCacheEntry> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug(
                "No IP access rules found in ${CACHE_NAME} cache for systemCode=${systemCode} and tenantId=${tenantId}; loading from database..."
            )
        }
        require(systemCode.isNotBlank()) { "systemCode must be specified when fetching IP access rules!" }
        val searchPayload = SysAccessRuleIpQuery(
            active = true,
            parentRuleActive = true,
            systemCode = systemCode,
            tenantId = tenantId,
            explicitNullProperties = if (tenantId == null) {
                listOf(SysAccessRule::tenantId.name)
            } else {
                null
            },
        )

        val results = sysAccessRuleIpDao.pagingSearch(searchPayload)
        return if (results.isEmpty()) {
            log.warn("No IP access rules found in database for tenantId=${tenantId} and systemCode=${systemCode}!")
            listOf()
        } else {
            mapToCacheItems(results)
        }
    }

    // region Event subscription: parent rule (sys_access_rule) changes drive IP cache updates ----------------------------------

    /** Parent rule inserted: refresh the corresponding dimension (no IPs yet, just creates an empty slot). */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onParentInserted(event: SysAccessRuleInserted): Unit =
        syncOnDeleteBySystemAndTenant(event.systemCode, event.tenantId)

    /** Parent rule updated: refresh the new dimension; if the dimension changed, refresh the old one as well. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onParentUpdated(event: SysAccessRuleUpdated) {
        syncOnDeleteBySystemAndTenant(event.systemCode, event.tenantId)
        if (event.dimensionChanged) {
            syncOnDeleteBySystemAndTenant(event.beforeSystemCode!!, event.beforeTenantId)
        }
    }

    /** Parent rule deleted: refresh the corresponding dimension (clear residual IP cache). */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onParentDeleted(event: SysAccessRuleDeleted): Unit =
        syncOnDeleteBySystemAndTenant(event.systemCode, event.tenantId)

    /** Parent rule batch deleted: refresh each dimension key carried by the event. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onParentBatchDeleted(event: SysAccessRuleBatchDeleted) {
        event.dimensions.forEach { (sysCode, tid) -> syncOnDeleteBySystemAndTenant(sysCode, tid) }
    }

    // endregion

    // region Event subscription: IP rule's own changes -----------------------------------------------------------

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onIpInserted(event: SysAccessRuleIpInserted) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        if (!event.active) {
            log.debug("Inserted IP rule ${event.id} has active=false; skipping ${CACHE_NAME} sync.")
            return
        }
        refreshDimension(event.parentSystemCode, event.parentTenantId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onIpUpdated(event: SysAccessRuleIpUpdated): Unit =
        refreshDimension(event.parentSystemCode, event.parentTenantId)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onIpDeleted(event: SysAccessRuleIpDeleted): Unit =
        refreshDimension(event.parentSystemCode, event.parentTenantId)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onIpBatchDeleted(event: SysAccessRuleIpBatchDeleted) {
        event.dimensions.forEach { (sysCode, tid) -> refreshDimension(sysCode, tid) }
    }

    // endregion

    /**
     * Evict and optionally backfill the cache for a single `(systemCode, tenantId)` dimension.
     *
     * Flow:
     * 1. Evict the cache entry for this dimension
     * 2. If the cache policy requires write-through backfill ([KeyValueCacheKit.isWriteInTime]), invoke via
     *    `getSelf` proxy so Spring AOP re-routes through the `@Cacheable` path for backfill -- a direct
     *    `this.getAccessRuleIps` call is self-invocation and bypasses the proxy / cache interceptor.
     *
     * @param systemCode System code
     * @param tenantId Tenant id; null = platform-level
     * @author K
     * @since 1.0.0
     */
    private fun refreshDimension(systemCode: String, tenantId: String?) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, getKey(systemCode, tenantId))
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            getSelf<AccessRuleIpsBySubSysAndTenantIdCache>().getAccessRuleIps(systemCode, tenantId)
        }
    }

    /**
     * Evict and backfill cache by system code and tenant dimension; used when the dimension key is known before deletion.
     */
    open fun syncOnDeleteBySystemAndTenant(systemCode: String, tenantId: String?) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val cacheKey = getKey(systemCode, tenantId)
        KeyValueCacheKit.evict(CACHE_NAME, cacheKey)
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            getSelf<AccessRuleIpsBySubSysAndTenantIdCache>().getAccessRuleIps(systemCode, tenantId)
        }
    }

    /**
     * Flatten DB row [SysAccessRuleIpRow] into cache entry [SysAccessRuleIpCacheEntry].
     * Only keeps fields actually used during authorization (IP range / type / expiration time);
     * other columns are excluded from the cache to reduce memory footprint.
     *
     * @param ruleIpRecords DB row list
     * @return List of cache entries
     * @author K
     * @since 1.0.0
     */
    private fun mapToCacheItems(ruleIpRecords: List<SysAccessRuleIpRow>): List<SysAccessRuleIpCacheEntry> {
        return ruleIpRecords.map {
            SysAccessRuleIpCacheEntry(
                id = it.id,
                ipStart = it.ipStart,
                ipEnd = it.ipEnd,
                ipTypeDictCode = it.ipTypeDictCode,
                expirationTime = it.expirationTime
            )
        }
    }

    /**
     * Returns the composite key built from the parameters. `null` / blank values are normalized to empty string
     * (platform-level), consistent with the SpEL expression in [@Cacheable].
     *
     * @param systemCode System code
     * @param tenantId Tenant id; `null` / blank treated as platform-level
     * @return Cache key
     */
    open fun getKey(systemCode: String, tenantId: String? = null): String =
        AccessRuleTenantKey.compositeKey(systemCode, tenantId)

    private val log = LogFactory.getLog(this::class)

}
