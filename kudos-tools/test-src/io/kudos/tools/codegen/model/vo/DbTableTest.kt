package io.kudos.tools.codegen.model.vo

import kotlin.test.*

/**
 * test for DbTable
 *
 * Covers the constructor-initialized JavaFX properties (generate/tableName/tableComment), every
 * getter/setter/property accessor (including a null table comment), and the compareTo contract used to keep
 * the UI table list sorted lexicographically by table name (less/equal/greater, self-comparison, Unicode).
 *
 * Plain JavaFX beans, so no FX toolkit is required.
 *
 * @author K
 * @since 1.0.0
 */
internal class DbTableTest {

    @Test
    fun constructorInitializesAllProperties() {
        val t = DbTable(true, "t_user", "user table")
        assertTrue(t.getGenerate())
        assertEquals("t_user", t.getTableName())
        assertEquals("user table", t.getTableComment())
    }

    @Test
    fun nullTableCommentIsSupported() {
        val t = DbTable(false, "t_order", null)
        assertFalse(t.getGenerate())
        assertNull(t.getTableComment())
        assertEquals("t_order", t.getTableName())
    }

    @Test
    fun settersAndPropertiesRoundTrip() {
        val t = DbTable(false, "a", "ca")
        t.setGenerate(true)
        t.setTableName("b")
        t.setTableComment("cb")
        assertTrue(t.getGenerate())
        assertEquals("b", t.getTableName())
        assertEquals("cb", t.getTableComment())

        // property objects reflect the same backing values
        assertTrue(t.generateProperty().get())
        assertEquals("b", t.tableNameProperty().get())
        assertEquals("cb", t.tableCommentProperty().get())

        // property writes propagate back to getters
        t.tableNameProperty().set("c")
        assertEquals("c", t.getTableName())

        t.setTableComment(null)
        assertNull(t.getTableComment())
    }

    @Test
    fun compareToOrdersByTableName() {
        val a = DbTable(false, "aaa", null)
        val b = DbTable(false, "bbb", null)
        assertTrue(a.compareTo(b) < 0)
        assertTrue(b.compareTo(a) > 0)
        assertEquals(0, a.compareTo(DbTable(true, "aaa", "different comment")))
        assertEquals(0, a.compareTo(a))
    }

    @Test
    fun compareToSortsListLexicographically() {
        val list = listOf(
            DbTable(false, "table_c", null),
            DbTable(false, "table_a", null),
            DbTable(false, "table_b", null),
        )
        assertEquals(listOf("table_a", "table_b", "table_c"), list.sorted().map { it.getTableName() })
    }

    @Test
    fun compareToHandlesUnicodeNames() {
        val a = DbTable(false, "用户", null)
        val b = DbTable(false, "订单", null)
        assertEquals("用户".compareTo("订单").coerceIn(-1, 1), a.compareTo(b).coerceIn(-1, 1))
    }
}
