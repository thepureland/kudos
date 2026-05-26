package io.kudos.ms.sys.core.outline.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.isNull
import io.kudos.context.support.Consts
import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import io.kudos.ms.sys.core.outline.dao.SysOutLineDao
import io.kudos.ms.sys.core.outline.event.SysOutLineBatchDeleted
import io.kudos.ms.sys.core.outline.event.SysOutLineDeleted
import io.kudos.ms.sys.core.outline.event.SysOutLineInserted
import io.kudos.ms.sys.core.outline.event.SysOutLineUpdated
import io.kudos.ms.sys.core.outline.model.po.SysOutLine
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Outbound whitelist cache handler.
 *
 * - Source table: `sys_out_line`
 * - Caches only rules with `active=true`
 * - Cache key format: `systemCode::normalized tenantId` (see [OutLineSystemTenantKey])
 * - Cache value: `List<SysOutLineCacheEntry>`
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class OutLineBySystemAndTenantCache : AbstractKeyValueCacheHandler<List<SysOutLineCacheEntry>>() {

    @Autowired
    private lateinit var dao: SysOutLineDao

    companion object {
        private const val CACHE_NAME = "SYS_OUT_LINE_BY_SYSTEM_AND_TENANT"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): List<SysOutLineCacheEntry> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "Illegal key format for cache ${CACHE_NAME}!"
        }
        val parts = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER, limit = 2)
        val tenantId = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
        return getSelf<OutLineBySystemAndTenantCache>().listOutLines(parts[0], tenantId)
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is disabled; skip loading outbound whitelist!")
            return
        }
        val all = dao.searchAs<SysOutLineCacheEntry>(Criteria(SysOutLine::active eq true))
        log.debug("Loaded ${all.size} outbound whitelist rules from database.")
        if (clear) clear()
        val grouped = all.groupBy { OutLineSystemTenantKey.compositeKey(it.systemCode, it.tenantId) }
        grouped.forEach { (k, list) -> KeyValueCacheKit.put(CACHE_NAME, k, list) }
        log.debug("Outbound whitelist cache populated with ${grouped.size} dimensions.")
    }

    /**
     * Fetch active outbound whitelist by (systemCode, tenantId). `tenantId == null` means platform level.
     *
     * @param systemCode system code
     * @param tenantId tenant id; `null` / blank is treated as platform level
     * @return list of cache entries; empty list when nothing matches
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#systemCode.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat((#tenantId ?: '').trim())",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun listOutLines(systemCode: String, tenantId: String? = null): List<SysOutLineCacheEntry> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Outbound whitelist for systemCode=${systemCode}, tenantId=${tenantId} not in cache, loading from database...")
        }
        require(systemCode.isNotBlank()) { "systemCode must not be blank when fetching outbound whitelist" }
        val tenantCriterion = if (tenantId == null) {
            SysOutLine::tenantId.isNull()
        } else {
            SysOutLine::tenantId eq tenantId
        }
        val criteria = Criteria(SysOutLine::systemCode eq systemCode)
            .addAnd(tenantCriterion)
            .addAnd(SysOutLine::active eq true)
        return dao.searchAs(criteria)
    }

    /** Invalidate the given (systemCode, tenantId) dimension and refill on demand. */
    open fun refreshDimension(systemCode: String, tenantId: String?) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val key = OutLineSystemTenantKey.compositeKey(systemCode, tenantId)
        KeyValueCacheKit.evict(CACHE_NAME, key)
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            getSelf<OutLineBySystemAndTenantCache>().listOutLines(systemCode, tenantId)
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysOutLineInserted) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val po = dao.get(event.id) ?: return
        refreshDimension(po.systemCode, po.tenantId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysOutLineUpdated) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        // After update, the row is still queryable; if it migrated across dimensions the old dimension cache may lag slightly until the next reloadAll at startup
        val po = dao.get(event.id) ?: return
        refreshDimension(po.systemCode, po.tenantId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysOutLineDeleted) {
        refreshDimension(event.systemCode, event.tenantId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysOutLineBatchDeleted) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        event.dimensions.forEach { (systemCode, tenantId) -> refreshDimension(systemCode, tenantId) }
    }

    private val log = LogFactory.getLog(this::class)

}
