package io.kudos.ability.data.rdb.jdbc.kit

import io.kudos.ability.data.rdb.jdbc.metadata.RdbTypeEnum
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.PostgresTestContainer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * RdbKit测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
internal class RdbKitTest {

    private var url = "jdbc:h2:~/h2/ds1;DATABASE_TO_LOWER=TRUE;"

    @Test
    fun getDataSource() {
        RdbKit.getDataSource()
    }

//    @Test
//    fun getDatabase() {
//        RdbKit.getDatabase()
//    }

    @Test
    fun testConnection() {
        val connection = RdbKit.newConnection(url, "sa", "sa")
        assert(RdbKit.testConnection(connection))
    }

    @Test
    fun determinRdbTypeByUrl() {
        assertEquals(RdbTypeEnum.H2, RdbKit.determinRdbTypeByUrl(url))
    }

    @Test
    fun getTestStatement() {
        assertEquals("select 1", RdbKit.getTestStatement(RdbTypeEnum.H2))
    }

    /**
     * 测试determineRdbTypeByDataSource方法
     * 使用指定的H2数据源
     */
    @Test
    fun determineRdbTypeByDataSourceWithH2() {
        val h2Url = "jdbc:h2:mem:test;DATABASE_TO_LOWER=TRUE;"
        val dataSource = DataSourceKit.createDataSource(h2Url, "sa", "sa")
        val rdbType = RdbKit.determineRdbTypeByDataSource(dataSource)
        assertEquals(RdbTypeEnum.H2, rdbType, "应该识别为H2数据库")
    }

    /**
     * 测试determineRdbTypeByDataSource方法
     * 传入null，使用当前上下文的数据源
     */
    @Test
    fun determineRdbTypeByDataSourceWithNull() {
        // 使用当前上下文的数据源（从Spring容器获取）
        val rdbType = RdbKit.determineRdbTypeByDataSource(null)
        // 根据测试环境的配置，应该是H2
        assertEquals(RdbTypeEnum.H2, rdbType, "应该识别为H2数据库")
    }

    /**
     * 测试determineRdbTypeByDataSource方法
     * 使用PostgreSQL URL格式的数据源
     */
    @Test
    fun determineRdbTypeByDataSourceWithPostgreSQL() {
        PostgresTestContainer.startIfNeeded(null)
        val postgresUrl = "jdbc:postgresql://localhost:25432/test"
        val dataSource = DataSourceKit.createDataSource(postgresUrl, "pg", "postgres")
        val rdbType = RdbKit.determineRdbTypeByDataSource(dataSource)
        assertEquals(RdbTypeEnum.POSTGRESQL, rdbType, "应该识别为PostgreSQL数据库")
    }

//    /**
//     * 测试determineRdbTypeByDataSource方法
//     * 使用MySQL URL格式的数据源
//     */
//    @Test
//    fun determineRdbTypeByDataSourceWithMySQL() {
//        val mysqlUrl = "jdbc:mysql://localhost:3306/testdb"
//        val dataSource = DataSourceKit.createDataSource(mysqlUrl, "root", "password")
//        val rdbType = RdbKit.determineRdbTypeByDataSource(dataSource)
//        assertEquals(RdbTypeEnum.MYSQL, rdbType, "应该识别为MySQL数据库")
//    }
//
//    /**
//     * 测试determineRdbTypeByDataSource方法
//     * 使用SQL Server URL格式的数据源
//     */
//    @Test
//    fun determineRdbTypeByDataSourceWithSQLServer() {
//        val sqlServerUrl = "jdbc:sqlserver://localhost:1433;databaseName=testdb"
//        val dataSource = DataSourceKit.createDataSource(sqlServerUrl, "sa", "password")
//        val rdbType = RdbKit.determineRdbTypeByDataSource(dataSource)
//        assertEquals(RdbTypeEnum.SQLSERVER, rdbType, "应该识别为SQL Server数据库")
//    }

}