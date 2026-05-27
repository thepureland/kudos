package io.kudos.ability.data.rdb.flyway

import io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceMigrator
import io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceProperties
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Integration tests for [FlywayMultiDataSourceMigrator]. Covers:
 * - happy path: full migration → tables exist and initial data is inserted; calling migrate() twice within the same process is idempotent
 * - failure branch when the data source key does not exist (resolved via single-arg `migrateByModule`)
 * - Spring Boot `spring.flyway.placeholders` propagates to Flyway scripts
 * - CSV/list parsing + execution-order ordering (validated via the Properties bean)
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest
class FlywayTest {

    @Resource
    private lateinit var migrator: FlywayMultiDataSourceMigrator

    @Resource
    private lateinit var dsContextProcessor: DsContextProcessor

    @Resource
    private lateinit var properties: FlywayMultiDataSourceProperties

    /**
     * Runs the full migration flow: invoke [FlywayMultiDataSourceMigrator.migrate] twice to verify
     * idempotency, then use raw JDBC to query the target tables and confirm the row counts match
     * the seeded contents of data.sql.
     */
    @Test
    fun migrate() {
        migrator.migrate() // Verify that repeated database updates also work without issue
        val datasource = requireNotNull(dsContextProcessor.getDataSource("ds1")) { "Data source ds1 does not exist" }
        datasource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("select count(*) from test_table_flyway").use { rs ->
                    assert(rs.next())
                    assertEquals(2, rs.getInt(1))
                }
                statement.executeQuery("select name from test_table_flyway_placeholder").use { rs ->
                    assert(rs.next())
                    assertEquals("codex-placeholder", rs.getString(1))
                }
            }
        }
    }

    /**
     * Error path for the single-module entry point: moduleX is configured under a non-existent
     * data source (`no_exists`); calling [FlywayMultiDataSourceMigrator.migrateByModule] must
     * resolve the ds, find it missing in the dynamic routing table, and throw.
     */
    @Test
    fun migrateByModuleMissingDataSource() {
        assertFailsWith<RuntimeException> { migrator.migrateByModule("moduleX") }
    }

    /**
     * Error path: a module name that's not declared anywhere in datasource-config triggers the
     * "no data source configured" branch (with a source-tracing hint in the message).
     */
    @Test
    fun migrateByModuleUnknownModule() {
        assertFailsWith<RuntimeException> { migrator.migrateByModule("never_configured_module") }
    }

    /**
     * Properties layer: CSV value is parsed into a list, list value stays a list, and reverse
     * lookup ([FlywayMultiDataSourceProperties.getDataSourceKey]) finds the hosting data source.
     */
    @Test
    fun propertiesParseAndReverseLookup() {
        val ds1Modules = properties.getDatasourceModules()["ds1"]
        assertEquals(listOf("module1", "module2"), ds1Modules)
        val noExistsModules = properties.getDatasourceModules()["no_exists"]
        assertEquals(listOf("moduleX"), noExistsModules)
        assertEquals("ds1", properties.getDataSourceKey("module1"))
        assertEquals("no_exists", properties.getDataSourceKey("moduleX"))
        assertEquals(null, properties.getDataSourceKey("never_configured_module"))
    }

    /**
     * Properties layer: `execution-order` overrides default ordering; here both ds keys are
     * listed and should keep the order ds1 → no_exists (matches yml declaration in this case).
     */
    @Test
    fun executionOrderHonored() {
        assertEquals(listOf("ds1", "no_exists"), properties.executionOrder)
    }

    /**
     * Properties layer: defensive guard so a mis-indented `execution-order` under
     * `datasource-config` is not interpreted as a data source name.
     */
    @Test
    fun reservedKeyDefense() {
        assertTrue(FlywayMultiDataSourceProperties.isReservedDatasourceConfigKey("execution-order"))
        assertTrue(FlywayMultiDataSourceProperties.isReservedDatasourceConfigKey("execution-order[0]"))
    }
}
