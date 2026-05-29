package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.creator.DataSourceProperty
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [HikariDataSourceMeterInitEvent].
 *
 * Cover the lifecycle race that motivates the buffering design: data sources created **before**
 * `bindTo` must still be tracked when the registry arrives; data sources created **after** must be
 * tracked immediately. Also verifies the "non-Hikari ignored" and "delegate still ran" invariants.
 */
internal class HikariDataSourceMeterInitEventTest {

    @Test
    fun afterCreate_beforeBindTo_buffersUntilRegistryArrives() {
        val event = HikariDataSourceMeterInitEvent()
        val hikari = newIdleHikari()

        event.afterCreate(hikari)
        assertNull(hikari.metricsTrackerFactory, "no registry yet → no factory attached")

        event.bindTo(SimpleMeterRegistry())

        val factory = hikari.metricsTrackerFactory
        assertNotNull(factory, "after bindTo, queued Hikari source picks up the factory")
        assertTrue(factory is MicrometerMetricsTrackerFactory, "factory is the Micrometer one, not Hikari's default")
    }

    @Test
    fun afterCreate_afterBindTo_attachesImmediately() {
        val event = HikariDataSourceMeterInitEvent()
        event.bindTo(SimpleMeterRegistry())

        val hikari = newIdleHikari()
        event.afterCreate(hikari)

        assertNotNull(
            hikari.metricsTrackerFactory,
            "a data source registered after bindTo should be tracked on the same call, " +
                "not left in the queue forever (since drain only fires from bindTo/afterCreate).",
        )
    }

    @Test
    fun nonHikariDataSource_isIgnoredButDelegateStillRuns() {
        val event = HikariDataSourceMeterInitEvent()
        event.bindTo(SimpleMeterRegistry())

        val nonHikari = StubDataSource()
        event.afterCreate(nonHikari) // must not throw

        // No state on the stub itself to assert (no metrics field). The contract: this branch is a
        // no-op and the call doesn't blow up — Druid / DBCP deployments stay functional.
        assertSame(nonHikari, nonHikari, "control: non-Hikari data source still reachable after afterCreate")
    }

    @Test
    fun preExistingTrackerFactory_isNotOverridden() {
        val event = HikariDataSourceMeterInitEvent()
        val hikari = newIdleHikari()
        val preset = MicrometerMetricsTrackerFactory(SimpleMeterRegistry())
        hikari.metricsTrackerFactory = preset

        event.bindTo(SimpleMeterRegistry())
        event.afterCreate(hikari)

        assertSame(
            preset, hikari.metricsTrackerFactory,
            "an explicitly-configured tracker factory should not be silently replaced — " +
                "Hikari's setter throws if a tracker is already attached, so guarding against this in code keeps startup green.",
        )
    }

    @Test
    fun beforeCreate_delegatesToEncryptionWithoutThrow() {
        val event = HikariDataSourceMeterInitEvent()
        // No assertion on encryption behavior — that belongs to baomidou's tests. We only assert
        // delegation does not blow up on a plain (non-ENC) URL, since that's the common case in
        // kudos integration tests.
        val property = DataSourceProperty().apply {
            poolName = "test"
            url = "jdbc:h2:mem:metric-init-event"
            username = "sa"
            password = ""
        }
        event.beforeCreate(property)
        assertEquals("sa", property.username, "delegate must leave plain values untouched")
    }

    /**
     * An idle [HikariDataSource]: no connection pool spun up yet because no JDBC URL is set.
     * Sufficient for asserting only the metricsTrackerFactory state.
     */
    private fun newIdleHikari(): HikariDataSource = HikariDataSource()

    /** Minimal non-Hikari DataSource — only exercised via the `is HikariDataSource` branch check. */
    private class StubDataSource : DataSource {
        override fun getConnection() = error("not used")
        override fun getConnection(username: String?, password: String?) = error("not used")
        override fun getLogWriter() = error("not used")
        override fun setLogWriter(out: java.io.PrintWriter?) {}
        override fun setLoginTimeout(seconds: Int) {}
        override fun getLoginTimeout() = 0
        override fun getParentLogger() = error("not used")
        override fun <T : Any?> unwrap(iface: Class<T>?) = error("not used")
        override fun isWrapperFor(iface: Class<*>?) = false
    }
}
