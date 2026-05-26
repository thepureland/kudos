package io.kudos.ability.data.rdb.jdbc.aop

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder
import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst
import io.kudos.ability.data.rdb.jdbc.context.DbContext
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.ability.data.rdb.jdbc.init.MultipleDataSourceProperties
import io.kudos.ability.data.rdb.jdbc.kit.DatasourceKeyTool
import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import jakarta.annotation.Resource
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.core.annotation.Order
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Dynamic data source routing aspect: intercepts all methods under `*..biz..*` package paths and
 * decides which data source the current method should use based on `forcedDs` in [DbContext], the
 * tenant context from [KudosContextHolder], and the package-path mapping configured in
 * [MultipleDataSourceProperties].
 *
 * Decision priority:
 *  1. `DbContext.forcedDs` is non-empty and not read-only and not `_context`-prefixed -> switch
 *     directly to forcedDs.
 *  2. The package path of the service class hit by the aspect has a config in
 *     `dataSourceProperties.lookDataSourceKey`:
 *     a. config starts with `_context` -> use "tenant + service + mode" dynamic resolution
 *        (see [DsContextProcessor]).
 *     b. otherwise the config is the data source key itself.
 *  3. Neither matches -> do not switch; inherit whatever data source the upper call stack already set.
 *
 * Known limitation: the pointcut `within(*..biz..*)` is hardcoded and requires business code to
 * live under a `biz` sub-package; projects not following this structure will not get this aspect
 * activated (not configurable).
 *
 * @author hanson
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Aspect
@Order(-99)
class DynamicDataSourceAspect {

    @Resource
    private lateinit var dataSourceProperties: MultipleDataSourceProperties

    @Resource
    private lateinit var dsContextProcessor: DsContextProcessor

    /**
     * Pointcut: methods on all classes under the `*..biz..*` package path.
     */
    @Pointcut("within(*..biz..*)")
    fun aspService() {
    }

    /**
     * Around advice. Based on context, decides whether to push the data source onto baomidou's
     * [DynamicDataSourceContextHolder], proceeds, and pops it back in the `finally` block. Does
     * not throw any extra exceptions; original exceptions propagate up unchanged.
     */
    @Around("aspService()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val changeDatasource = changeDatasource(joinPoint.target.javaClass)
        try {
            return joinPoint.proceed()
        } finally {
            if (changeDatasource) {
                DynamicDataSourceContextHolder.poll()
                log.debug("Reverting data source, {0}", joinPoint.target.javaClass.packageName)
            }
        }
    }

    /**
     * Decides whether this invocation should switch data sources and writes the decision into
     * baomidou's ThreadLocal stack. Return value: true means it has been pushed and this aspect's
     * `finally` needs to pop it; false means the stack was not touched.
     */
    private fun changeDatasource(serviceClazz: Class<*>): Boolean {
        if (forceChangeMain()) {
            return true
        }
        // Falls back to default if no data source config is found for the package.
        val dsKeyConfig: String = dataSourceProperties.lookDataSourceKey(serviceClazz)
        if (dsKeyConfig.isBlank()) {
            return false
        }
        // Convention: specifying _context indicates a data source table; _context itself does not
        // require a datasource config, it is automatically fetched from datasource.
        if (dsKeyConfig.startsWith(CONTEXT_DATASOURCE)) {
            val datasourcePair = convertDatasourceConfig(dsKeyConfig)
            // Compatible with multi-tenancy: different contexts use different data sources.
            val mapKey: String = DatasourceKeyTool.convertCacheMapKey(
                checkNotNull(datasourcePair.first) { "datasource config first must not be null" },
                KudosContextHolder.get().dataSourceId, datasourcePair.second
            )
            // Cache the result if the data source has already been switched.
            var dsKey: String? = dsCacheMap[mapKey]
            if (dsKey.isNullOrBlank()) {
                READ_WRITE_LOCK.readLock().lock()
                try {
                    dsKey = dsCacheMap.computeIfAbsent(mapKey) { k ->
                        requireNotNull(dsContextProcessor.doDetermineDatasource(k, dsKeyConfig)) {
                            "doDetermineDatasource returned null for $k"
                        }
                    }
                } finally {
                    READ_WRITE_LOCK.readLock().unlock()
                }
            }
            DynamicDataSourceContextHolder.push(dsKey)
            log.debug("Dynamically switching data source, {0}={1}", serviceClazz.getPackageName(), dsKey)
        } else {
            DynamicDataSourceContextHolder.push(dsKeyConfig)
            log.debug("Dynamically switching data source, {0}={1}", serviceClazz.getPackageName(), dsKeyConfig)
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
    private fun forceChangeMain(): Boolean {
        val forcedDs = DbContext.get().forcedDs
        if (forcedDs.isNullOrBlank()) {
            return false
        }
        if (DbContext.get().readonly || forcedDs.startsWith(CONTEXT_DATASOURCE)) {
            return false
        }
        if (!dsContextProcessor.haveDataSource(forcedDs)) {
            return false
        }
        // Adaptation: DsChange
        DynamicDataSourceContextHolder.push(forcedDs)
        if (DbContext.get().enableLog) {
            log.info("Forcibly specifying data source: {0}", forcedDs)
        }
        return true
    }


    /**
     * Splits a `_context`-prefixed data source config + the current `DbParam` into a (key, suffix)
     * pair for [DatasourceKeyTool.convertCacheMapKey] to produce the final cache key.
     * - readonly forced: suffix = MODE_READONLY, key remains the original config.
     * - not readonly but forcedDs is set: use forcedDs as the key (adapted for TenantDsChange).
     * - neither: default to master.
     */
    private fun convertDatasourceConfig(dsKeyConfig: String?): Pair<String?, String?> {
        val mapKeySuffix = DatasourceConst.MODE_MASTER
        val forceDs = DbContext.get().forcedDs
        // Forced switching only needs to be evaluated for dynamic data sources.
        if (!DbContext.get().forcedDs.isNullOrBlank()) {
            return if (DbContext.get().readonly) {
                // Read-only database setting.
                Pair(dsKeyConfig, DatasourceConst.MODE_READONLY)
            } else {
                // If not readOnly, treat forceDs as the data source key. Non-context-prefixed
                // values are matched in the first step. Adaptation: TenantDsChange.
                Pair(forceDs, mapKeySuffix)
            }
        }
        // No forced data source specified: return the default master.
        return Pair(dsKeyConfig, DatasourceConst.MODE_MASTER)
    }

    companion object {
        private const val CONTEXT_DATASOURCE = "_context"
        private val log = LogFactory.getLog(this::class)
        private val dsCacheMap = ConcurrentHashMap<String, String>()

        private val READ_WRITE_LOCK: ReadWriteLock = ReentrantReadWriteLock()

        /**
         * Clears the "package path -> real data source key" resolution cache. Call this after a
         * tenant's data source is dynamically changed (added / modified) so the next invocation
         * re-runs [DsContextProcessor.doDetermineDatasource].
         * The `@Synchronized` + writeLock double protection is a historical artifact; writeLock
         * alone is sufficient and `synchronized` is redundant, but it is kept to avoid changing
         * externally visible behavior.
         */
        @Synchronized
        fun cacheDsCache() {
            READ_WRITE_LOCK.writeLock().lock()
            try {
                dsCacheMap.clear()
            } finally {
                READ_WRITE_LOCK.writeLock().unlock()
            }
        }
    }
}
