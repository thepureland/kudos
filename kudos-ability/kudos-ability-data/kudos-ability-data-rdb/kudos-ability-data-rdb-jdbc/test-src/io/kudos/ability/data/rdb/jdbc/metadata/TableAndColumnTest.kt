package io.kudos.ability.data.rdb.jdbc.metadata

import java.sql.Types
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the metadata value classes [Table], [Column] and [TableTypeEnum].
 *
 * Covers: default values, toString rendering, camel-case conversion of column names and the
 * anonymous-class failure mode of getKotlinTypeName().
 *
 * @author K
 * @since 1.0.0
 */
internal class TableAndColumnTest {

    @Test
    fun table_defaultsAreNull() {
        val t = Table()
        assertNull(t.name)
        assertNull(t.comment)
        assertNull(t.schema)
        assertNull(t.catalog)
        assertNull(t.type)
    }

    @Test
    fun table_toStringContainsAllFields() {
        val t = Table().apply {
            name = "t_user"
            comment = "用户表"
            schema = "public"
            catalog = "db1"
            type = TableTypeEnum.VIEW
        }
        val s = t.toString()
        assertTrue("t_user" in s)
        assertTrue("用户表" in s)
        assertTrue("public" in s)
        assertTrue("db1" in s)
        assertTrue("VIEW" in s)
    }

    @Test
    fun column_defaults() {
        val c = Column()
        assertEquals(Types.VARCHAR, c.jdbcType)
        assertNull(c.comment)
        assertNull(c.length)
        assertNull(c.decimalDigits)
        assertNull(c.defaultValue)
        assertTrue(c.nullable)
        assertFalse(c.primaryKey)
        assertFalse(c.foreignKey)
        assertFalse(c.indexed)
        assertFalse(c.unique)
        assertFalse(c.dictCode)
        assertFalse(c.autoIncrement)
    }

    @Test
    fun column_getKotlinTypeName() {
        val c = Column().apply { kotlinType = String::class }
        assertEquals("String", c.getKotlinTypeName())
        c.kotlinType = java.math.BigDecimal::class
        assertEquals("BigDecimal", c.getKotlinTypeName())
    }

    @Test
    fun column_getKotlinTypeName_anonymousClassThrows() {
        val anonymous = object {}
        val c = Column().apply { kotlinType = anonymous::class }
        val e = assertFailsWith<IllegalArgumentException> { c.getKotlinTypeName() }
        assertTrue(e.message!!.contains("simpleName"), "message should explain the null simpleName")
    }

    @Test
    fun column_getColumnHumpName() {
        assertEquals("userName", Column().apply { name = "user_name" }.getColumnHumpName())
        assertEquals("id", Column().apply { name = "id" }.getColumnHumpName())
    }

    @Test
    fun tableTypeEnum_containsAllJdbcTableTypes() {
        assertEquals(
            listOf("TABLE", "VIEW", "SYSTEM_TABLE", "GLOBAL_TEMPORARY", "LOCAL_TEMPORARY", "ALIAS", "SYNONYM"),
            TableTypeEnum.entries.map { it.name }
        )
    }
}
