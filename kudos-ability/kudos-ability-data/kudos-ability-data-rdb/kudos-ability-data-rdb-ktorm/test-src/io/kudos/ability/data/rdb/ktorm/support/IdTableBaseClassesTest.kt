package io.kudos.ability.data.rdb.ktorm.support

import org.ktorm.schema.IntSqlType
import org.ktorm.schema.LongSqlType
import org.ktorm.schema.VarcharSqlType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for the table base classes [IntIdTable], [LongIdTable], [StringIdTable] and
 * [ManagedTable]: verify the primary-key column registration (name / SQL type / primaryKey flag)
 * and, for [ManagedTable], the full set of management columns with their physical names.
 * Pure schema construction — no database required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class IdTableBaseClassesTest {

    @Test
    fun intIdTable_registersIntPrimaryKeyNamedId() {
        assertEquals("id", IntTable.id.name)
        assertSame(IntSqlType, IntTable.id.sqlType)
        assertTrue(IntTable.primaryKeys.contains(IntTable.id), "id must be the primary key")
    }

    @Test
    fun longIdTable_registersLongPrimaryKeyNamedId() {
        assertEquals("id", LongTable.id.name)
        assertSame(LongSqlType, LongTable.id.sqlType)
        assertTrue(LongTable.primaryKeys.contains(LongTable.id))
    }

    @Test
    fun stringIdTable_registersVarcharPrimaryKeyNamedId() {
        assertEquals("id", StringTable.id.name)
        assertSame(VarcharSqlType, StringTable.id.sqlType)
        assertTrue(StringTable.primaryKeys.contains(StringTable.id))
    }

    @Test
    fun managedTable_registersAllManagementColumns() {
        val expectedNames = setOf(
            "id",
            "create_time", "create_user_id", "create_user_name",
            "update_time", "update_user_id", "update_user_name",
            "active", "built_in", "remark",
        )
        assertEquals(expectedNames, ManagedFixtureTable.columns.map { it.name }.toSet())

        // Spot-check the bindings declared by ManagedTable itself
        assertEquals("create_time", ManagedFixtureTable.createTime.name)
        assertEquals("create_user_id", ManagedFixtureTable.createUserId.name)
        assertEquals("create_user_name", ManagedFixtureTable.createUserName.name)
        assertEquals("update_time", ManagedFixtureTable.updateTime.name)
        assertEquals("update_user_id", ManagedFixtureTable.updateUserId.name)
        assertEquals("update_user_name", ManagedFixtureTable.updateUserName.name)
        assertEquals("active", ManagedFixtureTable.active.name)
        assertEquals("built_in", ManagedFixtureTable.builtIn.name)
        assertEquals("remark", ManagedFixtureTable.remark.name)
        assertTrue(ManagedFixtureTable.primaryKeys.contains(ManagedFixtureTable.id), "ManagedTable inherits the varchar id primary key")
    }

    // Fixture entities: never instantiated, only used to satisfy the table type bounds and let
    // ktorm resolve the entity type from the table generics for the bindTo registrations.

    private interface IntEntity : IDbEntity<Int, IntEntity>
    private object IntTable : IntIdTable<IntEntity>("id_table_test_int")

    private interface LongEntity : IDbEntity<Long, LongEntity>
    private object LongTable : LongIdTable<LongEntity>("id_table_test_long")

    private interface StringEntity : IDbEntity<String, StringEntity>
    private object StringTable : StringIdTable<StringEntity>("id_table_test_string")

    private interface ManagedEntity : IManagedDbEntity<String, ManagedEntity>
    private object ManagedFixtureTable : ManagedTable<ManagedEntity>("id_table_test_managed")
}
