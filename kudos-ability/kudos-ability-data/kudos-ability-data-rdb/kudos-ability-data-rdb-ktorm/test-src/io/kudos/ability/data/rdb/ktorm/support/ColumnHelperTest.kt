package io.kudos.ability.data.rdb.ktorm.support

import org.ktorm.schema.Table
import org.ktorm.schema.varchar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [ColumnHelper] covering the full multi-step fallback resolution order:
 *  1. camelCase → lowercase underscore direct hit
 *  2. uppercase retry (H2 / Oracle default uppercase naming)
 *  3. case-insensitive scan over `table.columns` (by column name or by property name)
 *  4. `id` → `table.primaryKeys[0]` fallback
 * plus the unresolvable-property error, the empty-input shortcut and the per-table cache.
 *
 * Each fixture table uses a unique table name because the cache is keyed by table name and is
 * shared JVM-wide — colliding names would leak resolutions across tests.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class ColumnHelperTest {

    /** Column registered with an UPPERCASE physical name; step 1 misses, step 2 (uppercase retry) hits. */
    private object UpperTable : Table<Nothing>("column_helper_upper_tbl") {
        val userId = varchar("USER_ID")
    }

    /** Column registered with a Mixed_Case physical name; steps 1-2 miss, step 3 scan matches case-insensitively. */
    private object MixedTable : Table<Nothing>("column_helper_mixed_tbl") {
        val userId = varchar("User_Id")
    }

    /** Column whose physical name equals the property name without underscores; matched by the second clause of step 3. */
    private object NoUnderscoreTable : Table<Nothing>("column_helper_nounderscore_tbl") {
        val userId = varchar("userid")
    }

    /** Primary key column NOT named id; the logical property "id" must fall back to primaryKeys[0] (step 4). */
    private object PkFallbackTable : Table<Nothing>("column_helper_pk_tbl") {
        val uid = varchar("uid").primaryKey()
    }

    @Test
    fun emptyPropertyNames_returnsEmptyMap() {
        assertEquals(emptyMap(), ColumnHelper.columnOf(UpperTable))
    }

    @Test
    fun resolvesUppercaseColumnName() {
        val map = ColumnHelper.columnOf(UpperTable, "userId")
        assertEquals("USER_ID", map["userId"]!!.name)
    }

    @Test
    fun resolvesMixedCaseColumnNameByScanning() {
        val map = ColumnHelper.columnOf(MixedTable, "userId")
        assertEquals("User_Id", map["userId"]!!.name)
    }

    @Test
    fun resolvesColumnByPropertyNameComparison() {
        // "userId" -> "user_id"/"USER_ID" both miss; the scan matches it.name "userid" against the
        // raw property name case-insensitively.
        val map = ColumnHelper.columnOf(NoUnderscoreTable, "userId")
        assertEquals("userid", map["userId"]!!.name)
    }

    @Test
    fun idFallsBackToPrimaryKeyColumn() {
        val map = ColumnHelper.columnOf(PkFallbackTable, "id")
        assertEquals("uid", map["id"]!!.name, "property 'id' must resolve to the table's primary key column when no id column exists")
        assertTrue(PkFallbackTable.primaryKeys.any { it === map["id"] })
    }

    @Test
    fun unresolvableProperty_throwsWithTableAndPropertyInMessage() {
        val ex = assertFailsWith<IllegalStateException> {
            ColumnHelper.columnOf(UpperTable, "noSuchProperty")
        }
        assertTrue(ex.message!!.contains("noSuchProperty"), "error must name the unresolvable property")
        assertTrue(ex.message!!.contains("column_helper_upper_tbl"), "error must name the table")
    }

    @Test
    fun secondLookupReturnsCachedColumnInstance() {
        val first = ColumnHelper.columnOf(UpperTable, "userId")["userId"]
        val second = ColumnHelper.columnOf(UpperTable, "userId")["userId"]
        assertSame(first, second, "resolved columns must be served from the per-table cache on repeat lookups")
    }
}
