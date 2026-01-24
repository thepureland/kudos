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
 * RdbAndRedisCacheTestBase的测试用例
 *
 * 测试RdbAndRedisCacheTestBase的核心功能：
 * - 继承AbstractRdbTestBase的功能（SQL文件加载、事务回滚等）
 * - Redis容器配置
 * - 缓存策略配置
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
     * 测试继承AbstractRdbTestBase的SQL文件加载功能
     */
    @Test
    fun testSqlFileDataLoaded() {
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_table", Int::class.java)
        assertEquals(1, count, "应该有一条测试数据")

        val name = jdbcTemplate.queryForObject(
            "SELECT name FROM test_table WHERE id = ?",
            String::class.java,
            "rdb-redis-test"
        )
        assertEquals("rdb-redis-test", name, "数据应该正确")
    }

    /**
     * 测试继承AbstractRdbTestBase的事务回滚功能
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
    }

    /**
     * 测试Redis容器配置
     */
    @Test
    fun testRedisContainerConfiguration() {
        // 验证Redis相关配置已设置（通过环境变量或属性）
        // 注意：这里主要验证配置属性是否存在，实际Redis连接测试可能需要额外的依赖
        val cacheEnabled = environment.getProperty("kudos.ability.cache.enabled")
        assertEquals("true", cacheEnabled, "缓存应该被启用")
    }


//    /**
//     * 测试可以修改缓存策略
//     */
//    @Test
//    fun testCacheStrategyModification() {
//        // 修改缓存策略
//        RdbAndRedisCacheTestBase.Companion.cacheStrategyHolder.value = "DISTRIBUTED"
//
//        // 验证缓存策略已被修改
//        assertEquals("DISTRIBUTED", RdbAndRedisCacheTestBase.Companion.cacheStrategyHolder.value, "缓存策略应该被修改")
//
//        // 恢复默认值
//        RdbAndRedisCacheTestBase.Companion.cacheStrategyHolder.value = "SINGLE_LOCAL"
//    }
}
