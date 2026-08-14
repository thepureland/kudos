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
    fun ofProductName_relaxedMatching() {
        // case-insensitive: JDBC metadata casing quirks must not break the lookup
        assertEquals(RdbTypeEnum.H2, RdbTypeEnum.ofProductName("h2"))
        // real-world product names: SQL Server reports "Microsoft SQL Server"
        assertEquals(RdbTypeEnum.SQLSERVER, RdbTypeEnum.ofProductName("Microsoft SQL Server"))
        // contains fallback: edition suffixes / protocol-compatible servers still resolve
        assertEquals(RdbTypeEnum.MYSQL, RdbTypeEnum.ofProductName("MySQL Community Server"))
        assertEquals(RdbTypeEnum.MARIA, RdbTypeEnum.ofProductName("MariaDB"))
    }

    @Test
    fun ofProductName_unknownThrowsWithName() {
        val e = assertFailsWith<NoSuchElementException> { RdbTypeEnum.ofProductName("NoSuchDb") }
        assertEquals(true, e.message!!.contains("NoSuchDb"), "error must name the unmatched product")
        assertFailsWith<NoSuchElementException> { RdbTypeEnum.ofProductName(null) }
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
        assertEquals("Microsoft SQL Server", RdbTypeEnum.SQLSERVER.productName)
        assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDriver", RdbTypeEnum.SQLSERVER.jdbcDriverName)
        assertEquals(9, RdbTypeEnum.entries.size)
    }
}
