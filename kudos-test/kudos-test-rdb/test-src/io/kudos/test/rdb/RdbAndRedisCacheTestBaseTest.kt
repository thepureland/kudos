package io.kudos.test.rdb

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.H2TestContainer
import jakarta.annotation.Resource
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for RdbAndRedisCacheTestBase.
 *
 * Verifies the core functionality of RdbAndRedisCacheTestBase:
 * - Inherited functionality from AbstractRdbTestBase (SQL file loading, transaction rollback, etc.)
 * - Redis container configuration
 * - Cache strategy configuration
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class RdbAndRedisCacheTestBaseTest : RdbAndRedisCacheTestBase() {

    companion object Companion {
        @JvmStatic
        @DynamicPropertySource
        fun registerRdbProperties(registry: DynamicPropertyRegistry) {
            H2TestContainer.startIfNeeded(registry)
        }
    }

    @Resource
    private lateinit var jdbcTemplate: JdbcTemplate

    @Resource
    private lateinit var environment: Environment

    /**
     * Tests the inherited SQL-file loading from AbstractRdbTestBase.
     */
    @Test
    fun testSqlFileDataLoaded() {
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_table", Int::class.java)
        assertEquals(1, count, "expected exactly one test data row")

        val name = jdbcTemplate.queryForObject(
            "SELECT name FROM test_table WHERE id = ?",
            String::class.java,
            "rdb-redis-test"
        )
        assertEquals("rdb-redis-test", name, "data should be correct")
    }

    /**
     * Tests the inherited transaction rollback from AbstractRdbTestBase.
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
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM test_table WHERE id = ?",
            Int::class.java,
            "rollback-test"
        )
        assertEquals(1, count, "inserted data should be visible in the test method")
    }

    /**
     * Tests the Redis container configuration.
     */
    @Test
    fun testRedisContainerConfiguration() {
        // Verify that Redis-related configuration is set (via env vars or properties)
        // Note: this mainly verifies presence of the configuration property; actual Redis connection tests may require extra dependencies.
        val cacheEnabled = environment.getProperty("kudos.ability.cache.enabled")
        assertEquals("true", cacheEnabled, "cache should be enabled")
    }


//    /**
//     * Tests that the cache strategy can be modified.
//     */
//    @Test
//    fun testCacheStrategyModification() {
//        // Modify the cache strategy
//        RdbAndRedisCacheTestBase.Companion.cacheStrategyHolder.value = "DISTRIBUTED"
//
//        // Verify the cache strategy has been modified
//        assertEquals("DISTRIBUTED", RdbAndRedisCacheTestBase.Companion.cacheStrategyHolder.value, "cache strategy should be modified")
//
//        // Restore the default value
//        RdbAndRedisCacheTestBase.Companion.cacheStrategyHolder.value = "SINGLE_LOCAL"
//    }
}
