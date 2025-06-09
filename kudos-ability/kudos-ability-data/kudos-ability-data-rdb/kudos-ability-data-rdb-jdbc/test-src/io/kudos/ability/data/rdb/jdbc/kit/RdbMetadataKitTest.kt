package io.kudos.ability.data.rdb.jdbc.kit

import io.kudos.test.common.init.EnableKudosTest
import org.soul.ability.data.rdb.jdbc.metadata.TableTypeEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * RdbMetadataKit测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
internal class RdbMetadataKitTest {

    private val TABLE_NAME = "test_table"


    @Test
    fun getTablesByType() {
        val tables = RdbMetadataKit.getTablesByType(TableTypeEnum.TABLE)
        assertEquals(1, tables.filter { it.name == TABLE_NAME }.size)
    }

    @Test
    fun getTableByName() {
        assertNotNull(RdbMetadataKit.getTableByName(TABLE_NAME))
        assertNull(RdbMetadataKit.getTableByName("test_no_exists"))
    }

    @Test
    fun getColumnsByTableName() {
        val columns = RdbMetadataKit.getColumnsByTableName(TABLE_NAME)
        assert(columns.isNotEmpty())
        assert(columns.containsKey("id"))
    }
}