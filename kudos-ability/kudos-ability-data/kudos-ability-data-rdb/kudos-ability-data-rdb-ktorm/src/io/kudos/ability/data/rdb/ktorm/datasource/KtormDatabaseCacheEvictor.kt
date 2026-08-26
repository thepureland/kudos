package io.kudos.ability.data.rdb.ktorm.datasource

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource
import io.kudos.base.logger.LogFactory
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * Retires the cached ktorm `Database` objects of a closing application context.
 *
 * **Why the weak keys were not enough.** The cache is a `WeakHashMap`, which holds keys weakly but values
 * strongly, and the value — a `Database` built by `connectWithSpringSupport` — captures the *routing*
 * datasource while the key is the datasource that routing resolved to. When nothing is routing, those are the
 * same object, so the value strongly references its own key and the entry can never be collected. When
 * something is routing, the key can be a long-lived inner datasource while the value still points at the
 * routing datasource of whichever context happened to build it first. Either way an entry can outlive the
 * context that owns its connection pool, and the next caller gets a `Database` whose pool is shut down —
 * which surfaces as `ProxyConnection.close()` failing with a null delegate, some distance from the cause.
 *
 * A context closing is the moment its pools go away, so it is also the moment those entries stop being usable.
 *
 * **Only this context's entries go.** An earlier version dropped the whole cache, on the grounds that the
 * cache key is a resolved inner datasource which need not be a bean of the closing context, so ownership could
 * not be answered. It can be answered from the other direction: ask the closing context for its `DataSource`
 * beans, expand any routing datasource into the members it delegates to, and retire that set. Entries are then
 * matched on both the key and the routing datasource they wrap. Dropping everything also discarded entries
 * belonging to contexts that are still running, which is wrong the moment two contexts share a JVM — the shape
 * every multi-context test suite has.
 *
 * **Retire rather than merely remove.** `ContextClosedEvent` is published before the context destroys its
 * singletons, so a query already in flight can rebuild an entry over a pool that is about to shut down. The
 * retirement mark makes the removal stick: that query still gets its `Database`, but it cannot leave one in the
 * cache for the callers that come after.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class KtormDatabaseCacheEvictor : ApplicationListener<ContextClosedEvent> {

    override fun onApplicationEvent(event: ContextClosedEvent) {
        val owned = runCatching {
            event.applicationContext.getBeansOfType(DataSource::class.java).values
        }.getOrElse {
            // A context already too far into shutdown to answer. Nothing to retire beats failing the close.
            log.debug("Could not read the closing context's datasources; nothing retired: ${it.message}")
            return
        }
        val retiring = owned.flatMap(::expand).toList()
        retireKtormDatabases(retiring)
        log.debug("Retired ${retiring.size} datasource(s) of a closing application context from the ktorm Database cache.")
    }

    /**
     * A routing datasource plus everything it routes to.
     *
     * The cache is keyed by what the routing *resolved* to, and those members are usually not beans, so a
     * closing context that owns only the routing bean would otherwise retire nothing that is actually filed.
     */
    private fun expand(dataSource: DataSource): List<DataSource> {
        val routed = runCatching {
            (dataSource as? DynamicRoutingDataSource)?.dataSources?.values?.toList()
        }.getOrNull().orEmpty()
        return listOf(dataSource) + routed
    }

    private val log = LogFactory.getLog(this::class)
}
