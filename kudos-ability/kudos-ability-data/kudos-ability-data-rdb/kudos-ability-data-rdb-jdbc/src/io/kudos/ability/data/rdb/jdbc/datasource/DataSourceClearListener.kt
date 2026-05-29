package io.kudos.ability.data.rdb.jdbc.datasource

import io.kudos.ability.cache.common.support.CacheCleanRegister
import io.kudos.ability.cache.common.support.ICacheCleanListener
import io.kudos.base.logger.LogFactory

/**
 * Cache-clean hook that refreshes the dynamic-datasource registry whenever the sys-managed
 * datasource cache is invalidated.
 *
 * When an admin edits a [sys_datasource] row and the corresponding hash-cache entry is evicted
 * (locally or via Redis pub/sub from another node), this listener fires and asks
 * [DsContextProcessor] to rebuild the live routing for that data source. The result: changing a
 * JDBC URL / username / pool size in the sys console takes effect without a restart.
 *
 * Cache key semantics (mirror soul's port):
 *  - `null` → cache region fully cleared → refresh **every** dynamic data source.
 *  - `"ALL"` → broadcast "everyone re-warm" signal seen at startup; intentionally ignored so a
 *    fresh-booting node does not stampede every other node into a refresh.
 *  - Otherwise → parse the key as `Int` and refresh just that data source. Unparseable keys
 *    (UUIDs, composite keys, etc.) are silently ignored — the [DsContextProcessor.refreshDatasource]
 *    contract uses an integer id today.
 *
 * Wiring: registered against [cacheName] from [afterPropertiesSet]; [io.kudos.ability.data.rdb.jdbc.init.JdbcAutoConfiguration]
 * only creates the bean when `kudos-ability-cache-common` is present (the only path to
 * [CacheCleanRegister]). Apps without the cache module skip this listener entirely.
 *
 * Ported from `org.soul.ability.data.rdb.jdbc.datasource.DataSourceClearListener`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class DataSourceClearListener(
    private val processor: DsContextProcessor,
    /** Cache region to listen on. Default matches `kudos-ms-sys` `SysDataSourceHashCache.CACHE_NAME`. */
    private val cacheName: String = DEFAULT_CACHE_NAME,
) : ICacheCleanListener {

    private val log = LogFactory.getLog(this::class)

    override fun cleanCache(cacheName: String, key: Any?) {
        when {
            key == null -> {
                log.info("Full datasource refresh triggered by cache clean cacheName={0}", cacheName)
                processor.refreshDatasource(null)
            }
            key.toString() == BROADCAST_KEY -> {
                // Startup broadcast — every node receives it, so a refresh here would stampede.
                log.debug("Ignoring broadcast cache clean key=ALL cacheName={0}", cacheName)
            }
            else -> {
                val dsId = key.toString().toIntOrNull()
                if (dsId == null) {
                    log.debug("Datasource cache clean key not parseable as int; skipping refresh key={0}", key)
                    return
                }
                processor.refreshDatasource(dsId)
            }
        }
    }

    override fun afterPropertiesSet() {
        CacheCleanRegister.register(cacheName, this)
        log.info("DataSourceClearListener registered for cache region [{0}]", cacheName)
    }

    companion object {
        /** Matches `io.kudos.ms.sys.core.datasource.cache.SysDataSourceHashCache.CACHE_NAME`. */
        const val DEFAULT_CACHE_NAME: String = "SYS_DATA_SOURCE__HASH"

        /** Cache key sent at startup to signal "ignore this round". */
        const val BROADCAST_KEY: String = "ALL"
    }
}
