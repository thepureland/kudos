package io.kudos.ability.data.rdb.flyway.kit

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.output.MigrateResult
import org.h2.jdbcx.JdbcDataSource
import org.mockito.Mockito
import org.springframework.boot.flyway.autoconfigure.FlywayProperties
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for [FlywayKit] (no Spring container). Covers:
 * - failure path: a migration script with invalid SQL makes Flyway throw; FlywayKit must log and
 *   rethrow (never swallow) so callers can abort startup
 * - "reported failure" path: Flyway returns `MigrateResult.success=false` without throwing;
 *   FlywayKit must convert that into an exception (static-mocked Flyway, since the real engine
 *   throws instead of reporting failure)
 *
 * The happy path (real migration applied + idempotent re-run) is covered by the integration test
 * [io.kudos.ability.data.rdb.flyway.FlywayTest].
 *
 * @author K
 * @since 1.0.0
 */
internal class FlywayKitTest {

    private fun h2DataSource(dbName: String): JdbcDataSource {
        val ds = JdbcDataSource()
        ds.setURL("jdbc:h2:mem:$dbName;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;")
        ds.user = "sa"
        ds.password = "sa"
        return ds
    }

    /**
     * module_bad's V1.0.1__bad.sql is intentionally invalid; Flyway throws a [FlywayException]
     * and FlywayKit must rethrow it unchanged (the catch branch logs and propagates).
     */
    @Test
    fun migrateBadSqlRethrows() {
        val ds = h2DataSource("flyway_kit_bad")
        assertFailsWith<FlywayException> {
            FlywayKit.migrate("module_bad", ds, FlywayProperties())
        }
    }

    /**
     * Flyway reports `success=false` without throwing (rare engine behavior); FlywayKit must
     * surface this as an [IllegalStateException] mentioning the module, instead of treating
     * "fail but continue" as acceptable.
     */
    @Test
    fun migrateReportedFailureThrows() {
        val ds = h2DataSource("flyway_kit_reported_failure")
        Mockito.mockStatic(Flyway::class.java).use { mockedStatic ->
            val config = Mockito.mock(FluentConfiguration::class.java, Mockito.RETURNS_SELF)
            val flyway = Mockito.mock(Flyway::class.java)
            val failedResult = MigrateResult() // public fields; success defaults to false
            Mockito.`when`(config.load()).thenReturn(flyway)
            Mockito.`when`(flyway.migrate()).thenReturn(failedResult)
            mockedStatic.`when`<FluentConfiguration> { Flyway.configure() }.thenReturn(config)

            val e = assertFailsWith<IllegalStateException> {
                FlywayKit.migrate("module_reported", ds, FlywayProperties())
            }
            assertTrue(e.message!!.contains("module_reported"), "message should name the module: ${e.message}")
            assertTrue(e.message!!.contains("flyway failed"), "message should state the failure: ${e.message}")
        }
    }
}
