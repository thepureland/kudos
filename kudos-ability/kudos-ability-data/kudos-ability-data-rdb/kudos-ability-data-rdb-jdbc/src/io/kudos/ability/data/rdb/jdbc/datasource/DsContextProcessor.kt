package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator
import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst
import io.kudos.ability.data.rdb.jdbc.init.MultipleDataSourceProperties
import io.kudos.ability.data.rdb.jdbc.kit.DatasourceKeyTool
import io.kudos.base.logger.LogFactory
import io.kudos.base.support.KeyLockRegistry
import io.kudos.context.core.KudosContextHolder
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

/**
 * Processor that translates the "context dataSourceId" into a data source key that
 * actually exists in the baomidou dynamic routing table.
 *
 * Role: after [io.kudos.ability.data.rdb.jdbc.aop.DynamicDataSourceInterceptor] hits a
 * `_context::*`-type routing intent, it calls this processor to resolve the current context
 * (tenant id, service code, master/readonly mode) into a concrete dsKey and look it up in the
 * routing table. If the DataSource for that dsKey is not yet loaded into the routing table, it
 * goes through [IDynamicDataSourceLoad] to fetch the config + [DsDataSourceCreator]
 * to build on the fly + [IDataSourceProxy] to wrap a proxy + register back into
 * the routing table.
 *
 * In single-data-source scenarios (the injected `dataSource` is not a
 * [DynamicRoutingDataSource]), every method degrades gracefully: lookups return the only data
 * source, [getDatasourceKey] skips registration (the only pool serves everything), and
 * [refreshDatasource] is a warn-logged no-op.
 *
 * @author damon
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class DsContextProcessor {

    @Resource
    private lateinit var dataSource: DataSource

    @Resource
    private lateinit var dataSourceCreator: DefaultDataSourceCreator

    @Resource
    private lateinit var dynamicDataSourceLoad: IDynamicDataSourceLoad

    @Resource
    private lateinit var routingCache: RoutingCache

    @Resource
    private lateinit var dataSourceProperties: MultipleDataSourceProperties

    @Autowired(required = false)
    private val dataSourceProxy: IDataSourceProxy? = null

    @Autowired(required = false)
    private val dataSourceFinder: IDataSourceFinder? = null

    @Value($$"${spring.datasource.dynamic.primary:master}")
    private val primary: String? = null

    private val keyLockRegistry = KeyLockRegistry<String>()

    /**
     * Main entry: translates the routing cache key computed by the interceptor into the real
     * dsKey in the routing table. Returns `null` to indicate "there is currently no context
     * (e.g. unauthenticated request)" or "console tenant, skip routing".
     *
     * Flow:
     *  1. Take a snapshot of [KudosContextHolder] (`getOrNull` does not pollute the ThreadLocal);
     *     return null if the snapshot is null.
     *  2. The console tenant ([DatasourceConst.CONSOLE_TENANT_ID]) skips routing.
     *  3. Default dsId = context.dataSourceId / mode = master; if the dsKey has the readOnly
     *     suffix, switch to context.readOnlyDataSourceId / mode = readonly.
     *  4. If [dataSourceFinder] is present, let the business side override the real dsId by
     *     "tenant + service + mode".
     *  5. Register under a per-**dsId** lock so concurrent threads targeting the same data source
     *     wait for each other instead of both creating its pool — the lock key is the resolved
     *     dsId, not the cache key, because several cache keys can map onto one dsId. The wait is
     *     bounded by [MultipleDataSourceProperties.dataSourceCreateWaitSeconds]; exceeding it
     *     throws rather than queueing threads behind a hung creation.
     *  6. Finally call [getDatasourceKey] to ensure the DataSource for that dsId is registered
     *     in the routing table.
     */
    open fun doDetermineDatasource(dsKey: String, dsKeyConfig: String?): String? {
        val context = KudosContextHolder.getOrNull() ?: return null
        if (context._datasourceTenantId == DatasourceConst.CONSOLE_TENANT_ID) return null
        // The standby dsKey suffix triggers switching to readOnlyDataSourceId / readonly mode; otherwise use the master id + master from the context.
        val (defaultDsId, mode) = if (DatasourceKeyTool.isReadOnly(dsKey) && context.readOnlyDataSourceId != null) {
            context.readOnlyDataSourceId to DatasourceConst.MODE_READONLY
        } else {
            context.dataSourceId to DatasourceConst.MODE_MASTER
        }
        val realDsId = dataSourceFinder
            ?.findDataSourceId(context._datasourceTenantId, DatasourceKeyTool.getServerCode(dsKey), mode)
            ?: defaultDsId
        // Lock on the resolved dsId, NOT on the cache key: several cache keys (different tenants /
        // server codes) can resolve to the same dsId, and it is that dsId's pool creation that must
        // not race. Locking the cache key would let two of them build the same pool twice, leaking
        // the one that loses the addDataSource race.
        if (realDsId == null) return getDatasourceKey(null)
        val waitSeconds = dataSourceProperties.dataSourceCreateWaitSeconds
        keyLockRegistry.tryLock(realDsId, waitSeconds, TimeUnit.SECONDS)
            ?: throw IllegalStateException(
                "Timed out after ${waitSeconds}s waiting for another thread to initialize data source " +
                        "dsId=$realDsId. Failing fast instead of queueing; raise " +
                        "kudos.ability.jdbc.dataSourceCreateWaitSeconds if creation is legitimately slow."
            )
        try {
            return getDatasourceKey(realDsId)
        } finally {
            keyLockRegistry.unlock(realDsId)
        }
    }

    /**
     * Ensures the DataSource for the given dsId already exists in the baomidou routing table;
     * if not, builds and registers it on the fly, then returns the dsId as the key. On failure
     * ([IDynamicDataSourceLoad] cannot find the config) it throws [RuntimeException].
     *
     * Single-data-source degradation: without a [DynamicRoutingDataSource] there is no routing
     * table to register into — the only pool serves every request, so the dsId is returned as-is
     * (the pushed key is simply never consulted).
     *
     * `protected open` — left for subclasses to override (e.g. to fetch/register from the
     * routing table in another way); not called directly from outside.
     */
    protected open fun getDatasourceKey(dsId: String?): String? {
        val ds = dataSource as? DynamicRoutingDataSource ?: return dsId
        if (!ds.dataSources.containsKey(dsId)) {
            // The DataSource for this dsKey is not yet initialized; load the config and initialize.
            val dsProperty = dynamicDataSourceLoad.getPropertyById(dsId)
                ?: run {
                    log.warn("Dynamic data source id is not configured: {0}", dsId)
                    throw RuntimeException("Dynamic data source id is not configured! dsId=$dsId")
                }
            log.warn("Starting to create and load data source id={0}...", dsId)
            val created = dataSourceCreator.createDataSource(dsProperty)
            val toRegister = dataSourceProxy?.proxyDatasource(created) ?: created
            ds.addDataSource(dsId, toRegister)
        }
        return dsId
    }

    /**
     * Returns the real data source for a given dsKey. The routing table is
     * baomidou's [DynamicRoutingDataSource]; in single-data-source scenarios
     * `dataSource` is not that type, so this degrades to "ignore the key and
     * return the only data source".
     */
    open fun getDataSource(dsKey: String?): DataSource? =
        (dataSource as? DynamicRoutingDataSource)?.getDataSource(dsKey) ?: dataSource

    /**
     * Whether the given dsKey exists in the routing table. Always returns true in
     * single-data-source scenarios ("there is only this one data source"); in
     * multi-data-source scenarios it queries the routing table's internal map.
     */
    open fun haveDataSource(dsKey: String?): Boolean =
        (dataSource as? DynamicRoutingDataSource)?.dataSources?.containsKey(dsKey) ?: true

    /**
     * Refreshes the data source entry for a given dsId in the routing table;
     * `dsId == null` means "refresh all data sources except primary". Also invalidates the
     * [RoutingCache] so the next routing resolution goes through [doDetermineDatasource] again.
     *
     * Removal is lazy-rebuilding: the entry is only dropped here; the next `_context` routing
     * hit recreates it through [getDatasourceKey] with the fresh configuration.
     *
     * Use case: notify reloads when a tenant data source is modified in the metadata center.
     * No-op (with a warn) in single-data-source scenarios.
     */
    open fun refreshDatasource(dsId: String?) {
        log.warn("Received request to refresh data source id: {0}", dsId)
        val ds = dataSource as? DynamicRoutingDataSource
            ?: run {
                log.warn("Ignoring data source refresh: not running on a DynamicRoutingDataSource")
                return
            }
        if (dsId == null) {
            // Full refresh: clear all routes except primary, then afterPropertiesSet() to rebuild.
            ds.dataSources.keys.filter { it != primary }.forEach(ds::removeDataSource)
            ds.afterPropertiesSet()
        } else if (ds.dataSources.containsKey(dsId)) {
            ds.removeDataSource(dsId)
        } else {
            log.warn("Data source id {0} is not in the routing table; nothing to remove", dsId)
        }
        routingCache.invalidateAll()
        log.warn("Data source refresh succeeded...")
    }

    /** The current thread's "logical current data source". In multi-data-source scenarios, delegates to baomidou's determineDataSource. */
    open fun currentDataSource(): DataSource? =
        (dataSource as? DynamicRoutingDataSource)?.determineDataSource() ?: dataSource

    private val log = LogFactory.getLog(this::class)

}
