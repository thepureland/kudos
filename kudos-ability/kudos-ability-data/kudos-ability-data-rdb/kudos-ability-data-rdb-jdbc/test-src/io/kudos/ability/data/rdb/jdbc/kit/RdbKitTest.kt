package io.kudos.ability.data.rdb.jdbc.kit

import io.kudos.ability.data.rdb.jdbc.metadata.RdbTypeEnum
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.PostgresTestContainer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test cases for RdbKit.
 *
 * @author K
 * @author AI: Codex
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
     * Tests determineRdbTypeByDataSource using a specified H2 data source.
     */
    @Test
    fun determineRdbTypeByDataSourceWithH2() {
        val h2Url = "jdbc:h2:mem:test;DATABASE_TO_LOWER=TRUE;"
        val dataSource = DataSourceKit.createDataSource(h2Url, "sa", "sa")
        val rdbType = RdbKit.determineRdbTypeByDataSource(dataSource)
        assertEquals(RdbTypeEnum.H2, rdbType, "should be identified as H2")
    }

    /**
     * Tests determineRdbTypeByDataSource passing null, falling back to the current context data source.
     */
    @Test
    fun determineRdbTypeByDataSourceWithNull() {
        // Use the current context data source (fetched from the Spring container).
        val rdbType = RdbKit.determineRdbTypeByDataSource(null)
        // Per the test environment configuration, this should be H2.
        assertEquals(RdbTypeEnum.H2, rdbType, "should be identified as H2")
    }

    /**
     * Tests determineRdbTypeByDataSource using a PostgreSQL URL data source.
     */
    @Test
    fun determineRdbTypeByDataSourceWithPostgreSQL() {
        PostgresTestContainer.startIfNeeded(null)
        val postgresUrl = "jdbc:postgresql://localhost:${PostgresTestContainer.PORT}/${PostgresTestContainer.DATABASE}"
        val dataSource = DataSourceKit.createDataSource(postgresUrl, "pg", "postgres")
        val rdbType = RdbKit.determineRdbTypeByDataSource(dataSource)
        assertEquals(RdbTypeEnum.POSTGRESQL, rdbType, "should be identified as PostgreSQL")
    }

//    /**
//     * Tests determineRdbTypeByDataSource using a MySQL URL data source.
//     */
//    @Test
//    fun determineRdbTypeByDataSourceWithMySQL() {
//        val mysqlUrl = "jdbc:mysql://localhost:3306/testdb"
//        val dataSource = DataSourceKit.createDataSource(mysqlUrl, "root", "password")
//        val rdbType = RdbKit.determineRdbTypeByDataSource(dataSource)
//        assertEquals(RdbTypeEnum.MYSQL, rdbType, "should be identified as MySQL")
//    }
//
//    /**
//     * Tests determineRdbTypeByDataSource using a SQL Server URL data source.
//     */
//    @Test
//    fun determineRdbTypeByDataSourceWithSQLServer() {
//        val sqlServerUrl = "jdbc:sqlserver://localhost:1433;databaseName=testdb"
//        val dataSource = DataSourceKit.createDataSource(sqlServerUrl, "sa", "password")
//        val rdbType = RdbKit.determineRdbTypeByDataSource(dataSource)
//        assertEquals(RdbTypeEnum.SQLSERVER, rdbType, "should be identified as SQL Server")
//    }

}