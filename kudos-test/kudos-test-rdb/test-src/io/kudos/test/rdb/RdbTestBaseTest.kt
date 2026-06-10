package io.kudos.test.rdb

import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.H2TestContainer
import jakarta.annotation.Resource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for AbstractRdbTestBase.
 *
 * Verifies the core functionality of AbstractRdbTestBase:
 * - SQL file loading and execution
 * - Transaction rollback
 * - Data source retrieval
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class RdbTestBaseTest : SqlTestBase() {

    companion object Companion {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            H2TestContainer.startIfNeeded(registry)
        }
    }

    @Resource
    private lateinit var jdbcTemplate: JdbcTemplate

    @Resource
    private lateinit var dsContextProcessor: DsContextProcessor

    /**
     * Tests that data in the SQL file is loaded correctly.
     */
    @Test
    fun testSqlFileDataLoaded() {
        val count = jdbcTemplate.queryForObject<Int>("SELECT COUNT(*) FROM test_table")
        assertEquals(1, count, "expected exactly one test data row")

        val name = jdbcTemplate.queryForObject<String>(
            "SELECT name FROM test_table WHERE id = ?",
            "abstract-rdb-test"
        )
        assertEquals("abstract-rdb-test", name, "data should be correct")
    }

    /**
     * Tests that data inserted within a test method disappears after the transaction rolls back.
     */
    @Test
    fun testTransactionRollback() {
        // Insert data within the test method
        jdbcTemplate.update(
            "INSERT INTO test_table (id, name) VALUES (?, ?)",
            "rollback-test",
            "rollback"
        )

        // The inserted data should be visible within the same transaction
        val count = jdbcTemplate.queryForObject<Int>(
            "SELECT COUNT(*) FROM test_table WHERE id = ?",
            "rollback-test"
        )
        assertEquals(1, count, "inserted data should be visible in the test method")

        // After the test method ends, the data should be rolled back due to @Transactional.
        // This assertion needs to be performed in another test method.
    }

    /**
     * Tests that data is cleaned up after transaction rollback.
     */
    @Test
    fun testDataRolledBack() {
        // Data previously inserted by another test should not exist (because the transaction was rolled back)
        val count = jdbcTemplate.queryForObject<Int>(
            "SELECT COUNT(*) FROM test_table WHERE id = ?",
            "rollback-test"
        )
        assertEquals(0, count, "data should not exist after rollback")

        // But the data from the test data SQL file should exist
        val testDataCount = jdbcTemplate.queryForObject<Int>("SELECT COUNT(*) FROM test_table")
        assertEquals(1, testDataCount, "test data should exist")
    }

    /**
     * Tests that the data source can be retrieved.
     */
    @Test
    fun testDataSourceRetrieval() {
        val dataSource = dsContextProcessor.getDataSource(null)
        assertNotNull(dataSource, "should be able to retrieve a data source")
    }

    /**
     * Tests that a data source can be retrieved by a specified key.
     */
    @Test
    fun testDataSourceRetrievalWithKey() {
        val dataSource = dsContextProcessor.getDataSource("primary")
        assertNotNull(dataSource, "should be able to retrieve a data source by key")
    }



}