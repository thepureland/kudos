package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource
import com.baomidou.dynamic.datasource.creator.DataSourceProperty
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator
import io.kudos.ability.data.rdb.jdbc.aop.DynamicDataSourceAspect
import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst
import io.kudos.ability.data.rdb.jdbc.kit.DatasourceKeyTool
import io.kudos.base.logger.LogFactory
import io.kudos.base.support.KeyLockRegistry
import io.kudos.context.core.KudosContextHolder
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * Processor that translates the "context dataSourceId" into a data source key that
 * actually exists in the baomidou dynamic routing table.
 *
 * Role: after [DynamicDataSourceAspect] hits a `_context::*`-type routing intent,
 * it calls this processor to resolve the current context (tenant id, service code,
 * master/readonly mode) into a concrete dsKey and look it up in the routing table.
 * If the DataSource for that dsKey is not yet loaded into the routing table, it
 * goes through [IDynamicDataSourceLoad] to fetch the config + [DsDataSourceCreator]
 * to build on the fly + [IDataSourceProxy] to wrap a proxy + register back into
 * the routing table.
 *
 * In single-data-source scenarios (the injected `dataSource` is not a
 * [DynamicRoutingDataSource]), [getDataSource] / [haveDataSource] /
 * [currentDataSource] degrade to "return the only data source / always true".
 *
 * @author damon
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class DsContextProcessor {

    @Resource
    private lateinit var dataSource: DataSource

    @Resource
    private lateinit var dataSourceCreator: DefaultDataSourceCreator

    @Resource
    private lateinit var dynamicDataSourceLoad: IDynamicDataSourceLoad

    @Autowired(required = false)
    private val dataSourceProxy: IDataSourceProxy? = null

    @Autowired(required = false)
    private val dataSourceFinder: IDataSourceFinder? = null

    @Value($$"${spring.datasource.dynamic.primary:master}")
    private val primary: String? = null

    private val keyLockRegistry = KeyLockRegistry<String>()

    /**
     * Main entry: translates the cache map key computed by [DynamicDataSourceAspect]
     * into the real dsKey in the routing table. Returns `null` to indicate "there
     * is currently no context (e.g. unauthenticated request)" or "console tenant,
     * skip routing".
     *
     * Flow:
     *  1. Take a snapshot of [KudosContextHolder] (`getOrNull` does not pollute the
     *     ThreadLocal — the key fix from the anti-pattern we addressed previously);
     *     return null if the snapshot is null.
     *  2. The console tenant ([DatasourceConst.CONSOLE_TENANT_ID]) skips routing.
     *  3. Default dsId = context.dataSourceId / mode = master; if the dsKey has the
     *     readOnly suffix, switch to context.readOnlyDataSourceId / mode = readonly.
     *  4. Use [keyLockRegistry] to lock on the dsKey (to avoid duplicate loading
     *     when concurrent threads create the DataSource for the same key).
     *  5. If [dataSourceFinder] is present, let the business side override the real
     *     dsId by "tenant + service + mode".
     *  6. Finally call [getDatasourceKey] to ensure the DataSource for that dsId
     *     is registered in the routing table.
     */
    fun doDetermineDatasource(dsKey: String, dsKeyConfig: String?): String? {
        val context = KudosContextHolder.getOrNull() ?: return null
        if (context._datasourceTenantId == DatasourceConst.CONSOLE_TENANT_ID) return null
        // The standby dsKey suffix triggers switching to readOnlyDataSourceId / readonly mode; otherwise use the master id + master from the context.
        val (defaultDsId, mode) = if (DatasourceKeyTool.isReadOnly(dsKey) && context.readOnlyDataSourceId != null) {
            context.readOnlyDataSourceId to DatasourceConst.MODE_READONLY
        } else {
            context.dataSourceId to DatasourceConst.MODE_MASTER
        }
        keyLockRegistry.tryLock(dsKey)
        try {
            val realDsId = dataSourceFinder
                ?.findDataSourceId(context._datasourceTenantId, DatasourceKeyTool.getServerCode(dsKey), mode)
                ?: defaultDsId
            return getDatasourceKey(realDsId)
        } finally {
            keyLockRegistry.unlock(dsKey)
        }
    }

    /**
     * Ensures the DataSource for the given dsId already exists in the baomidou
     * routing table; if not, builds and registers it on the fly, then returns the
     * dsId as the key. On failure ([IDynamicDataSourceLoad] cannot find the config)
     * it throws [RuntimeException].
     *
     * `protected` — left for subclasses to override (e.g. to fetch/register from
     * the routing table in another way); not called directly from outside.
     */
    protected fun getDatasourceKey(dsId: String?): String? { //TODO
        val ds = dataSource as DynamicRoutingDataSource
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
    fun getDataSource(dsKey: String?): DataSource? =
        (dataSource as? DynamicRoutingDataSource)?.getDataSource(dsKey) ?: dataSource

    /**
     * Whether the given dsKey exists in the routing table. Always returns true in
     * single-data-source scenarios ("there is only this one data source"); in
     * multi-data-source scenarios it queries the routing table's internal map.
     */
    fun haveDataSource(dsKey: String?): Boolean =
        (dataSource as? DynamicRoutingDataSource)?.dataSources?.containsKey(dsKey) ?: true

    /**
     * Refreshes the data source entry for a given dsId in the routing table;
     * `dsId == null` means "refresh all data sources except primary". Also clears
     * [DynamicDataSourceAspect]'s resolution cache so the next routing resolution
     * goes through [doDetermineDatasource] again.
     *
     * Use case: notify reloads when a tenant data source is modified in the
     * metadata center.
     */
    fun refreshDatasource(dsId: Int?) {
        log.warn("Received request to refresh data source id: {0}", dsId)
        val ds = dataSource as DynamicRoutingDataSource
        if (dsId == null) {
            // Full refresh: clear all routes except primary, then afterPropertiesSet() to rebuild.
            ds.dataSources.keys.filter { it != primary }.forEach(ds::removeDataSource)
            ds.afterPropertiesSet()
        } else {
            ds.removeDataSource(dsId.toString())
        }
        DynamicDataSourceAspect.cacheDsCache()
        log.warn("Data source refresh succeeded...")
    }

    /** The current thread's "logical current data source". In multi-data-source scenarios, delegates to baomidou's determineDataSource. */
    fun currentDataSource(): DataSource? =
        (dataSource as? DynamicRoutingDataSource)?.determineDataSource() ?: dataSource

    private val log = LogFactory.getLog(this::class)

}
