package io.kudos.test.rdb

import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.test.container.containers.H2TestContainer
import jakarta.annotation.Resource
import org.junit.jupiter.api.assertThrows
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * AbstractRdbTestBase的测试用例
 *
 * 测试AbstractRdbTestBase的核心功能：
 * - SQL文件加载和执行
 * - 事务回滚
 * - 数据源获取
 *
 * @author K
 * @since 1.0.0
 */
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
     * 测试SQL文件中的数据是否被正确加载
     */
    @Test
    fun testSqlFileDataLoaded() {
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_table", Int::class.java)
        assertEquals(1, count, "应该有一条测试数据")

        val name = jdbcTemplate.queryForObject(
            "SELECT name FROM test_table WHERE id = ?",
            String::class.java,
            "abstract-rdb-test"
        )
        assertEquals("abstract-rdb-test", name, "数据应该正确")
    }

    /**
     * 测试在测试方法中插入的数据会在事务回滚后消失
     */
    @Test
    fun testTransactionRollback() {
        // 在测试方法中插入数据
        jdbcTemplate.update(
            "INSERT INTO test_table (id, name) VALUES (?, ?)",
            "rollback-test",
            "rollback"
        )

        // 在同一个事务中应该能看到插入的数据
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM test_table WHERE id = ?",
            Int::class.java,
            "rollback-test"
        )
        assertEquals(1, count, "测试方法中应该能看到插入的数据")

        // 测试方法结束后，由于@Transactional，数据应该被回滚
        // 这个验证需要在另一个测试方法中进行
    }

    /**
     * 测试事务回滚后的数据清理
     */
    @Test
    fun testDataRolledBack() {
        // 查询之前测试插入的数据应该不存在（因为事务回滚了）
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM test_table WHERE id = ?",
            Int::class.java,
            "rollback-test"
        )
        assertEquals(0, count, "回滚后数据应该不存在")

        // 但是测试数据SQL文件中的数据应该存在
        val testDataCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_table", Int::class.java)
        assertEquals(1, testDataCount, "测试数据应该存在")
    }

    /**
     * 测试能够获取到数据源
     */
    @Test
    fun testDataSourceRetrieval() {
        val dataSource = dsContextProcessor.getDataSource(null)
        assertNotNull(dataSource, "应该能获取到数据源")
    }

    /**
     * 测试能够通过指定的key获取数据源
     */
    @Test
    fun testDataSourceRetrievalWithKey() {
        val dataSource = dsContextProcessor.getDataSource("primary")
        assertNotNull(dataSource, "应该能通过key获取到数据源")
    }



}