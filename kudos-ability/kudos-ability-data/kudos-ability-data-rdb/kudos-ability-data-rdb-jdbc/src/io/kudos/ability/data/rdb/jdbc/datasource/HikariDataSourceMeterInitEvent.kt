package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.creator.DataSourceProperty
import com.baomidou.dynamic.datasource.event.DataSourceInitEvent
import com.baomidou.dynamic.datasource.event.EncDataSourceInitEvent
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.kudos.base.logger.LogFactory
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

/**
 * Combined [DataSourceInitEvent] + [MeterBinder] that exposes HikariCP connection-pool metrics
 * (`hikaricp.connections.{active,idle,pending,timeout,creation,acquire,usage,size}`) to
 * Micrometer.
 *
 * Replaces baomidou's default [DataSourceInitEvent] (which is `@ConditionalOnMissingBean` in
 * `DynamicDataSourceAssistConfiguration`) so that:
 *  1. The official encryption flow ([EncDataSourceInitEvent]) still runs — delegated, not skipped —
 *     so configurations using `ENC(...)` values keep working.
 *  2. Every [HikariDataSource] produced by baomidou's dynamic-datasource creator picks up a
 *     [MicrometerMetricsTrackerFactory] tied to the application's [MeterRegistry].
 *
 * Non-Hikari data sources (Druid / DBCP / etc.) are silently ignored — the Micrometer integration
 * is Hikari-specific. The encryption delegate still runs for them.
 *
 * **Lifecycle race**: at startup, baomidou's `DataSourceCreator` calls [afterCreate] for each data
 * source eagerly, but Spring Boot only calls [bindTo] once every `MeterBinder` bean has been built.
 * To avoid losing metrics on early-created sources, [afterCreate] buffers each source; [bindTo]
 * drains the buffer once. Sources created *after* `bindTo` are tracked immediately.
 *
 * **Thread-safety**: dynamic data source registrations (baomidou allows runtime additions) may run
 * concurrently with [bindTo]. The pending set is a [ConcurrentHashMap.newKeySet] so concurrent
 * additions never corrupt the queue, and [drain] removes its snapshot from the queue before
 * attaching trackers so a concurrent re-entry does not double-bind (Hikari's setter throws if a
 * tracker is already attached).
 *
 * Ported from `org.soul.ability.data.rdb.jdbc.datasource.HikariDataSourceMeterInitEvent`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class HikariDataSourceMeterInitEvent : DataSourceInitEvent, MeterBinder {

    private val log = LogFactory.getLog(this::class)

    /** Preserves the official encryption flow so `ENC(...)` configurations keep decrypting. */
    private val encDelegate: DataSourceInitEvent = EncDataSourceInitEvent()

    /** Sources created before [bindTo]; drained once the registry arrives. */
    private val pendingDataSources: MutableSet<DataSource> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var meterRegistry: MeterRegistry? = null

    override fun beforeCreate(dataSourceProperty: DataSourceProperty) {
        encDelegate.beforeCreate(dataSourceProperty)
    }

    override fun afterCreate(dataSource: DataSource) {
        encDelegate.afterCreate(dataSource)
        pendingDataSources += dataSource
        if (meterRegistry != null) {
            drain()
        }
    }

    override fun bindTo(registry: MeterRegistry) {
        this.meterRegistry = registry
        drain()
    }

    /**
     * Attach a [MicrometerMetricsTrackerFactory] to every queued Hikari source and clear the
     * queue. Snapshot-then-remove keeps a concurrent re-entry from double-binding.
     */
    private fun drain() {
        val registry = meterRegistry ?: return
        val snapshot = pendingDataSources.toList()
        if (snapshot.isEmpty()) return
        pendingDataSources.removeAll(snapshot.toSet())
        snapshot.forEach { ds ->
            if (ds is HikariDataSource && ds.metricRegistry == null && ds.metricsTrackerFactory == null) {
                runCatching { ds.metricsTrackerFactory = MicrometerMetricsTrackerFactory(registry) }
                    .onFailure { log.warn("Failed to bind Hikari metrics dataSource={0} cause={1}", ds.poolName, it.message) }
            }
        }
    }
}
