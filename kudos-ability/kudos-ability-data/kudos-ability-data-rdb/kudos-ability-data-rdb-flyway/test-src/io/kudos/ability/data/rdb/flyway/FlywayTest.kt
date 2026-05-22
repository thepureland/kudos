package io.kudos.ability.data.rdb.flyway

import io.kudos.ability.data.rdb.flyway.multidatasource.FlywayMultiDataSourceMigrator
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * [FlywayMultiDataSourceMigrator] 的集成测试。覆盖：
 * - happy path：全量迁移 → 表存在且初始数据已插入；同一进程内重复调用 migrate() 是幂等的
 * - 数据源 key 不存在的失败分支
 * - Spring Boot `spring.flyway.placeholders` 能传递到 Flyway 脚本
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
     * 跑完整迁移流程：调用两次 [FlywayMultiDataSourceMigrator.migrate] 验证幂等性，
     * 再用原生 JDBC 查目标表确认行数符合 data.sql 预置内容。
     */
    @Test
    fun migrate() {
        migrator.migrate() // 模拟重复执行数据库更新也没有问题
        val datasource = requireNotNull(dsContextProcessor.getDataSource("ds1")) { "数据源 ds1 不存在" }
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
     * 单模块入口的错误路径：传一个不存在的数据源 key 必须抛异常打断迁移。
     */
    @Test
    fun migrateByModule() {
        // 数据源key不存在，应该抛异常并中断
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
