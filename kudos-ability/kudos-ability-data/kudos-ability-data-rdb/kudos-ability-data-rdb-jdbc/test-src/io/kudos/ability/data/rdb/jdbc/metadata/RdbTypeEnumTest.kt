package io.kudos.ability.data.rdb.jdbc.metadata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Unit tests for [RdbTypeEnum]: product-name / driver-name bidirectional lookups for every entry
 * and the not-found failure mode.
 *
 * @author K
 * @since 1.0.0
 */
internal class RdbTypeEnumTest {

    @Test
    fun ofProductName_roundTripsEveryEntry() {
        RdbTypeEnum.entries.forEach { entry ->
            assertEquals(entry, RdbTypeEnum.ofProductName(entry.productName))
        }
    }

    @Test
    fun ofJdbcDriverName_roundTripsEveryEntry() {
        RdbTypeEnum.entries.forEach { entry ->
            assertEquals(entry, RdbTypeEnum.ofJdbcDriverName(entry.jdbcDriverName))
        }
    }

    @Test
    fun ofProductName_unknownThrows() {
        assertFailsWith<NoSuchElementException> { RdbTypeEnum.ofProductName("NoSuchDb") }
        assertFailsWith<NoSuchElementException> { RdbTypeEnum.ofProductName(null) }
        // lookup is case-sensitive on purpose: JDBC product names are returned verbatim
        assertFailsWith<NoSuchElementException> { RdbTypeEnum.ofProductName("h2") }
    }

    @Test
    fun ofJdbcDriverName_unknownThrows() {
        assertFailsWith<NoSuchElementException> { RdbTypeEnum.ofJdbcDriverName("com.example.Driver") }
        assertFailsWith<NoSuchElementException> { RdbTypeEnum.ofJdbcDriverName(null) }
    }

    @Test
    fun wellKnownBindings() {
        assertEquals("org.h2.Driver", RdbTypeEnum.H2.jdbcDriverName)
        assertEquals("MySQL", RdbTypeEnum.MYSQL.productName)
        assertEquals("PostgreSQL", RdbTypeEnum.POSTGRESQL.productName)
        assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDriver", RdbTypeEnum.SQLSERVER.jdbcDriverName)
        assertEquals(9, RdbTypeEnum.entries.size)
    }
}
