package io.kudos.ability.data.rdb.jdbc.aop

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder
import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst
import io.kudos.ability.data.rdb.jdbc.context.DbContext
import io.kudos.ability.data.rdb.jdbc.context.DbParam
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.ability.data.rdb.jdbc.datasource.RoutingCache
import io.kudos.ability.data.rdb.jdbc.init.MultipleDataSourceProperties
import io.kudos.ability.data.rdb.jdbc.kit.DatasourceKeyTool
import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import jakarta.annotation.Resource
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import java.util.concurrent.ConcurrentHashMap

/**
 * Dynamic data source routing interceptor: applied to business methods through an
 * `AspectJExpressionPointcutAdvisor` registered by
 * [io.kudos.ability.data.rdb.jdbc.init.JdbcAutoConfiguration] (expression configurable via
 * `kudos.ability.jdbc.routingPointcut`, default `within(*..biz..*)`). Decides which data source
 * the current method should use based on `forcedDs` in [DbContext], the tenant context from
 * [KudosContextHolder], and the package-path mapping configured in [MultipleDataSourceProperties].
 *
 * Decision priority:
 *  1. `DbContext.forcedDs` is non-empty and not read-only and not `_context`-prefixed -> switch
 *     directly to forcedDs.
 *  2. The package path of the service class hit by the interceptor has a config in
 *     `dataSourceProperties.lookDataSourceKey`:
 *     a. config starts with `_context` -> use "tenant + service + mode" dynamic resolution
 *        (see [DsContextProcessor]).
 *     b. otherwise the config is the data source key itself.
 *  3. Neither matches -> do not switch; inherit whatever data source the upper call stack already set.
 *
 * The [DbContext] / [KudosContextHolder] reads use `getOrNull()` — this interceptor runs on every
 * matched business call and must never create ThreadLocal state as a side effect (the old
 * `get()`-based implementation silently seeded an empty `DbParam` into every pooled thread).
 *
 * A readonly intent (`DbParam.readonly=true`) is only routable through a `_context` package
 * config; when it hits a plain-key or unconfigured package, it cannot take effect and a one-time
 * warn per package is emitted instead of failing silently.
 *
 * @author hanson
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class DynamicDataSourceInterceptor : MethodInterceptor {

    @Resource
    private lateinit var dataSourceProperties: MultipleDataSourceProperties

    @Resource
    private lateinit var dsContextProcessor: DsContextProcessor

    @Resource
    private lateinit var routingCache: RoutingCache

    /** packages already warned about an unroutable readonly intent — warn once, not per call */
    private val readonlyWarned = ConcurrentHashMap.newKeySet<String>()

    /**
     * Around logic. Based on context, decides whether to push the data source onto baomidou's
     * [DynamicDataSourceContextHolder], proceeds, and pops it back in the `finally` block. Does
     * not throw any extra exceptions; original exceptions propagate up unchanged.
     */
    override fun invoke(invocation: MethodInvocation): Any? {
        val targetClass = invocation.`this`?.javaClass ?: invocation.method.declaringClass
        val changed = changeDatasource(targetClass)
        try {
            return invocation.proceed()
        } finally {
            if (changed) {
                DynamicDataSourceContextHolder.poll()
                log.debug("Reverting data source, {0}", targetClass.packageName)
            }
        }
    }

    /**
     * Decides whether this invocation should switch data sources and writes the decision into
     * baomidou's ThreadLocal stack. Return value: true means it has been pushed and [invoke]'s
     * `finally` needs to pop it; false means the stack was not touched.
     */
    private fun changeDatasource(serviceClazz: Class<*>): Boolean {
        val dbParam = DbContext.getOrNull()
        if (forceChangeMain(dbParam)) {
            return true
        }
        // Falls back to default if no data source config is found for the package.
        val dsKeyConfig: String = dataSourceProperties.lookDataSourceKey(serviceClazz)
        if (dsKeyConfig.isBlank()) {
            warnUnroutableReadonly(dbParam, dsKeyConfig, serviceClazz)
            return false
        }
        // Convention: a _context config means "resolve against the data source table by context";
        // the actual data source is fetched / created on demand by DsContextProcessor.
        if (dsKeyConfig.startsWith(DatasourceConst.CONTEXT_DS_PREFIX)) {
            val (configKey, mode) = convertDatasourceConfig(dsKeyConfig, dbParam)
            val mapKey = DatasourceKeyTool.convertCacheMapKey(configKey, tenantDimension(), mode)
            val dsKey = routingCache.resolve(mapKey) { key ->
                dsContextProcessor.doDetermineDatasource(key, dsKeyConfig)
            } ?: return false // no kudos context / console tenant: fall through without switching
            DynamicDataSourceContextHolder.push(dsKey)
            log.debug("Dynamically switching data source, {0}={1}", serviceClazz.packageName, dsKey)
        } else {
            warnUnroutableReadonly(dbParam, dsKeyConfig, serviceClazz)
            DynamicDataSourceContextHolder.push(dsKeyConfig)
            log.debug("Dynamically switching data source, {0}={1}", serviceClazz.packageName, dsKeyConfig)
        }
        return true
    }

    /**
     * Fast path for "explicit forced switch" via `DbContext.forcedDs`. Returns true if the switch
     * has been done and pushed, in which case the caller does not need to fall through to
     * package-path matching; false means this path does not apply and routing should continue.
     *
     * Skip conditions:
     *  - forcedDs is empty -> no force intent.
     *  - is readonly or `_context`-prefixed -> these semantics are not handled in this fast path
     *    and are left for subsequent dynamic resolution.
     *  - forcedDs points to a data source that does not exist in the routing table -> skip to
     *    avoid pushing an unreachable key.
     */
    private fun forceChangeMain(dbParam: DbParam?): Boolean {
        val forcedDs = dbParam?.forcedDs
        if (forcedDs.isNullOrBlank()) {
            return false
        }
        if (dbParam.readonly || forcedDs.startsWith(DatasourceConst.CONTEXT_DS_PREFIX)) {
            return false
        }
        if (!dsContextProcessor.haveDataSource(forcedDs)) {
            return false
        }
        // Adaptation: DsChange
        DynamicDataSourceContextHolder.push(forcedDs)
        if (dbParam.enableLog) {
            log.info("Forcibly specifying data source: {0}", forcedDs)
        }
        return true
    }

    /**
     * Splits a `_context`-prefixed data source config + the current [DbParam] into a (key, mode)
     * pair for [DatasourceKeyTool.convertCacheMapKey] to produce the final cache key.
     * - readonly intent (with or without forcedDs): mode = readonly, key stays the original config.
     * - forcedDs set but not readonly: use forcedDs as the key (adapted for TenantDsChange).
     * - neither: default to master.
     */
    private fun convertDatasourceConfig(dsKeyConfig: String, dbParam: DbParam?): Pair<String, String> {
        if (dbParam?.readonly == true) {
            return dsKeyConfig to DatasourceConst.MODE_READONLY
        }
        val forcedDs = dbParam?.forcedDs
        if (!forcedDs.isNullOrBlank()) {
            // Non-context-prefixed values were already handled in the fast path. Adaptation: TenantDsChange.
            return forcedDs to DatasourceConst.MODE_MASTER
        }
        return dsKeyConfig to DatasourceConst.MODE_MASTER
    }

    /**
     * The tenant dimension of the resolution cache key. `_datasourceTenantId` is the primary
     * dimension — it is what [DsContextProcessor] feeds into
     * [io.kudos.ability.data.rdb.jdbc.datasource.IDataSourceFinder], so two tenants with the same
     * context `dataSourceId` must not share a cache entry. Falls back to `dataSourceId` for
     * non-tenant deployments (the historical key shape).
     */
    private fun tenantDimension(): String? {
        val context = KudosContextHolder.getOrNull() ?: return null
        return context._datasourceTenantId ?: context.dataSourceId
    }

    private fun warnUnroutableReadonly(dbParam: DbParam?, dsKeyConfig: String, serviceClazz: Class<*>) {
        if (dbParam?.readonly != true) return
        val pkg = serviceClazz.packageName
        if (readonlyWarned.add(pkg)) {
            log.warn(
                "Readonly routing intent is ignored for package {0}: it requires a '_context' package " +
                        "data source config, but the resolved config is '{1}'", pkg, dsKeyConfig
            )
        }
    }

    companion object {
        private val log = LogFactory.getLog(this::class)
    }
}
