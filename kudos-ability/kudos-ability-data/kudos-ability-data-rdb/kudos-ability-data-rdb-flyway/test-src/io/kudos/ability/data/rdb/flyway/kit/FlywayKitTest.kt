package io.kudos.ability.data.rdb.flyway.kit

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.output.MigrateResult
import org.h2.jdbcx.JdbcDataSource
import org.mockito.Mockito
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for [FlywayKit] (no Spring container). Covers:
 * - failure path: a migration script with invalid SQL makes Flyway throw; FlywayKit must log and
 *   rethrow (never swallow) so callers can abort startup
 * - "reported failure" path: Flyway returns `MigrateResult.success=false` without throwing;
 *   FlywayKit must convert that into an exception (static-mocked Flyway, since the real engine
 *   throws instead of reporting failure)
 * - `fail-on-missing-locations` propagation: a module without a `sql/<module>/<dbType>` directory
 *   must fail instead of silently reporting "up to date"
 * - customizer hook: [IFlywayModuleConfigCustomizer]s run after the property mapping and may
 *   override anything (proven by redirecting the history table)
 * - module name validation: names outside [A-Za-z0-9_-] are rejected before touching the database
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
            FlywayKit.migrate("module_bad", ds, FlywayConfig())
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
                FlywayKit.migrate("module_reported", ds, FlywayConfig())
            }
            assertTrue(e.message!!.contains("module_reported"), "message should name the module: ${e.message}")
            assertTrue(e.message!!.contains("flyway failed"), "message should state the failure: ${e.message}")
        }
    }

    /**
     * `fail-on-missing-locations=true` (the kudos default in kudos-ability-data-rdb-flyway.yml):
     * a module whose `sql/<module>/<dbType>` directory does not exist on the classpath must fail
     * loudly instead of silently succeeding with 0 migrations ("up to date"), which would let the
     * app start against an empty schema.
     */
    @Test
    fun migrateMissingDbTypeLocationFails() {
        val ds = h2DataSource("flyway_kit_missing_location")
        val props = FlywayConfig().apply { failOnMissingLocations = true }
        assertFailsWith<FlywayException> {
            FlywayKit.migrate("module_without_scripts", ds, props)
        }
    }

    /**
     * Customizers run after the standard property mapping and can override anything: this one
     * redirects the history table, then we verify the migration was recorded in the redirected
     * table (placeholder replacement is disabled because module1's V1.0.3 script contains a
     * placeholder that is only defined in the Spring integration test).
     */
    @Test
    fun customizerIsAppliedAfterPropertyMapping() {
        val ds = h2DataSource("flyway_kit_customizer")
        val customizedModules = mutableListOf<String>()
        val customizer = IFlywayModuleConfigCustomizer { moduleName, configuration ->
            customizedModules.add(moduleName)
            configuration.table("flyway_history_customized")
        }
        val props = FlywayConfig().apply { placeholderReplacement = false }
        FlywayKit.migrate("module1", ds, props, customizers = listOf(customizer))
        assertEquals(listOf("module1"), customizedModules)
        ds.connection.use { conn ->
            conn.createStatement().use { statement ->
                statement.executeQuery("select count(*) from flyway_history_customized").use { rs ->
                    assertTrue(rs.next())
                    assertTrue(rs.getInt(1) > 0, "the redirected history table should record the migrations")
                }
            }
        }
    }

    /** Module names are embedded into the history table name and classpath location → reject illegal characters early. */
    @Test
    fun illegalModuleNameRejected() {
        val ds = h2DataSource("flyway_kit_illegal_name")
        assertFailsWith<IllegalArgumentException> {
            FlywayKit.migrate("bad name;drop", ds, FlywayConfig())
        }
    }

    /**
     * Dry-run: [FlywayKit.pendingMigrations] lists everything that WOULD be applied — the merged
     * stream of sql/module1/common (V1.0.0) + sql/module1/h2 (V1.0.1..3) — without creating any
     * table; after a real migrate the pending list is empty.
     */
    @Test
    fun pendingMigrationsPreviewWithoutApplying() {
        val ds = h2DataSource("flyway_kit_pending")
        val props = FlywayConfig().apply { placeholderReplacement = false }

        val pending = FlywayKit.pendingMigrations("module1", ds, props)
        assertEquals(4, pending.size, "common + h2 scripts should all be pending: $pending")
        assertTrue(pending.first().contains("1.0.0"), "the common script must merge into the version stream: $pending")
        ds.connection.use { conn ->
            conn.createStatement().use { statement ->
                statement.executeQuery(
                    "select count(*) from information_schema.tables where lower(table_name) like 'test_table_flyway%' or lower(table_name) like 'flyway_history%'"
                ).use { rs ->
                    assertTrue(rs.next())
                    assertEquals(0, rs.getInt(1), "pendingMigrations must not create any table")
                }
            }
        }

        FlywayKit.migrate("module1", ds, props)
        assertTrue(FlywayKit.pendingMigrations("module1", ds, props).isEmpty(), "nothing should be pending after migrate")
    }

    /** [FlywayKit.repair] on a healthy history is a no-op that must not throw nor change the pending state. */
    @Test
    fun repairRunsAgainstExistingHistory() {
        val ds = h2DataSource("flyway_kit_repair")
        val props = FlywayConfig().apply { placeholderReplacement = false }
        FlywayKit.migrate("module1", ds, props)
        FlywayKit.repair("module1", ds, props)
        assertTrue(FlywayKit.pendingMigrations("module1", ds, props).isEmpty())
    }
}
