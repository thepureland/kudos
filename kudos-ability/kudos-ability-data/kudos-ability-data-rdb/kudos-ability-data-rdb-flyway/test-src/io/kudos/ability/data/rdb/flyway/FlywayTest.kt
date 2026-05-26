package io.kudos.ability.data.rdb.flyway

import io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceMigrator
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Integration tests for [FlywayMultiDataSourceMigrator]. Covers:
 * - happy path: full migration → tables exist and initial data is inserted; calling migrate() twice within the same process is idempotent
 * - failure branch when the data source key does not exist
 * - Spring Boot `spring.flyway.placeholders` propagates to Flyway scripts
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
     * Error path for the single-module entry point: passing a non-existent data source key must
     * throw an exception and abort the migration.
     */
    @Test
    fun migrateByModule() {
        // Data source key does not exist; should throw and abort
        assertFailsWith<RuntimeException> { migrator.migrateByModule("module3", "no_exists") }
    }

//    companion object Companion {
//        @JvmStatic
//        @DynamicPropertySource
//        private fun changeProperties(registry: DynamicPropertyRegistry) {
//            H2TestContainer.startIfNeeded(registry)
//        }
//    }

}
