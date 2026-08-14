package io.kudos.ability.data.rdb.jdbc.kit

import io.kudos.ability.data.rdb.jdbc.metadata.RdbTypeEnum
import io.kudos.base.query.sort.Order
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pure unit tests for [RdbKit] — no Spring container.
 *
 * Covers: url-based type inference (incl. the SQLSERVER special case and unknown vendors),
 * test statements per vendor, ORDER BY SQL generation (blank / quoted / empty edge cases),
 * the context-data-source branch of getDataSource(), and testConnection(null) against an
 * in-memory H2 placed in the Kudos context.
 *
 * @author K
 * @since 1.0.0
 */
internal class RdbKitUnitTest {

    @AfterTest
    fun tearDown() {
        KudosContextHolder.clear()
    }

    // ----- determinRdbTypeByUrl -----

    @Test
    fun determinRdbTypeByUrl_recognizesCommonVendors() {
        assertEquals(RdbTypeEnum.H2, RdbKit.determinRdbTypeByUrl("jdbc:h2:mem:test"))
        assertEquals(RdbTypeEnum.MYSQL, RdbKit.determinRdbTypeByUrl("jdbc:mysql://localhost:3306/db"))
        assertEquals(RdbTypeEnum.POSTGRESQL, RdbKit.determinRdbTypeByUrl("jdbc:postgresql://localhost/db"))
        assertEquals(RdbTypeEnum.ORACLE, RdbKit.determinRdbTypeByUrl("jdbc:oracle:thin:@localhost:1521:orcl"))
        assertEquals(RdbTypeEnum.SQLITE, RdbKit.determinRdbTypeByUrl("jdbc:sqlite:sample.db"))
        assertEquals(RdbTypeEnum.DB2, RdbKit.determinRdbTypeByUrl("jdbc:db2://localhost:50000/sample"))
    }

    @Test
    fun determinRdbTypeByUrl_sqlServerSpecialCase() {
        // sqlserver urls are matched by substring (the enum name SQLSERVER differs from the
        // url scheme "sqlserver"), including whitespace removal and case folding
        assertEquals(
            RdbTypeEnum.SQLSERVER,
            RdbKit.determinRdbTypeByUrl("jdbc:sqlserver://localhost:1433;databaseName=db")
        )
        assertEquals(
            RdbTypeEnum.SQLSERVER,
            RdbKit.determinRdbTypeByUrl("JDBC: SQLSERVER ://h;databaseName=db")
        )
    }

    @Test
    fun determinRdbTypeByUrl_unknownVendorThrows() {
        assertFailsWith<IllegalArgumentException> {
            RdbKit.determinRdbTypeByUrl("jdbc:nosuchdb://localhost/db")
        }
    }

    // ----- getTestStatement -----

    @Test
    fun getTestStatement_perVendor() {
        assertEquals("select 1 from dual", RdbKit.getTestStatement(RdbTypeEnum.ORACLE))
        assertEquals("select 1 from sysibm.sysdummy1", RdbKit.getTestStatement(RdbTypeEnum.DB2))
        assertEquals("select 1", RdbKit.getTestStatement(RdbTypeEnum.H2))
        assertEquals("select 1", RdbKit.getTestStatement(RdbTypeEnum.MYSQL))
        assertEquals("select 1", RdbKit.getTestStatement(RdbTypeEnum.POSTGRESQL))
    }

    // ----- getOrderSql -----

    @Test
    fun getOrderSql_singleAndMultipleOrders() {
        assertEquals("ORDER BY name ASC", RdbKit.getOrderSql(Order.asc("name")))
        assertEquals(
            "ORDER BY name ASC,age DESC",
            RdbKit.getOrderSql(Order.asc("name"), Order.desc("age"))
        )
    }

    @Test
    fun getOrderSql_filtersBlankProperty() {
        assertEquals("ORDER BY age DESC", RdbKit.getOrderSql(Order(""), Order.desc("age")))
        assertEquals("ORDER BY age DESC", RdbKit.getOrderSql(Order("   "), Order.desc("age")))
    }

    @Test
    fun getOrderSql_rejectsSuspiciousPropertiesLoudly() {
        // ORDER BY cannot be parameterized: anything outside [A-Za-z0-9_.] throws instead of
        // being silently dropped (quotes are not required for ORDER BY injection)
        assertFailsWith<IllegalArgumentException> { RdbKit.getOrderSql(Order.asc("na'me")) }
        assertFailsWith<IllegalArgumentException> { RdbKit.getOrderSql(Order.asc("name; DROP TABLE x")) }
        assertFailsWith<IllegalArgumentException> { RdbKit.getOrderSql(Order.asc("(CASE WHEN 1=1 THEN a END)")) }
    }

    @Test
    fun getOrderSql_returnsEmptyWhenNothingSurvives() {
        assertEquals("", RdbKit.getOrderSql())
        assertEquals("", RdbKit.getOrderSql(Order(""), Order("   ")))
    }

    // ----- getDataSource / testConnection via context -----

    @Test
    fun getDataSource_prefersContextOtherInfos() {
        val ds = DataSourceKit.createDataSource("jdbc:h2:mem:rdbkit_unit;DB_CLOSE_DELAY=-1", "sa", "")
        KudosContextHolder.get().addOtherInfos(KudosContext.OTHER_INFO_KEY_DATA_SOURCE to ds)
        try {
            assertSame(ds, RdbKit.getDataSource(), "context-provided data source wins over the Spring bean")
            assertSame(ds, DataSourceKit.getCurrentDataSource(), "DataSourceKit forwards to RdbKit")
        } finally {
            (ds as AutoCloseable).close()
        }
    }

    @Test
    fun testConnection_nullConnUsesContextDataSourceAndCloses() {
        val ds = DataSourceKit.createDataSource("jdbc:h2:mem:rdbkit_unit2;DB_CLOSE_DELAY=-1", "sa", "")
        KudosContextHolder.get().addOtherInfos(KudosContext.OTHER_INFO_KEY_DATA_SOURCE to ds)
        try {
            assertTrue(RdbKit.testConnection(null), "H2 'select 1' must succeed")
        } finally {
            (ds as AutoCloseable).close()
        }
    }

    @Test
    fun newConnection_and_testConnection_withExplicitConnection() {
        RdbKit.newConnection("jdbc:h2:mem:rdbkit_unit3;DB_CLOSE_DELAY=-1", "sa", null).use { conn ->
            assertTrue(RdbKit.testConnection(conn))
        }
    }

    @Test
    fun testConnection_deadConnectionReturnsFalse() {
        val conn = RdbKit.newConnection("jdbc:h2:mem:rdbkit_unit4", "sa", null)
        conn.close()
        assertFalse(RdbKit.testConnection(conn), "a dead connection must yield false, not an exception")
    }
}
