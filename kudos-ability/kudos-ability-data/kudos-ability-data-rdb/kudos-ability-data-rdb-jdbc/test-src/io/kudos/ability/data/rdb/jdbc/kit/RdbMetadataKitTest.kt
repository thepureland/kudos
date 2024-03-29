package io.kudos.ability.data.rdb.jdbc.kit

import io.kudos.ability.data.rdb.jdbc.table.TestTableKit
import io.kudos.test.common.EnableKudosTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.soul.ability.data.rdb.jdbc.metadata.TableTypeEnum

/**
 * RdbMetadataKit测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RdbMetadataKitTest {

    @BeforeAll
    fun setUp() {
        TestTableKit.create()
        TestTableKit.insert()
    }

    @AfterAll
    fun tearDown() {
        TestTableKit.drop()
    }


    @Test
    fun getTablesByType() {
        val tables = RdbMetadataKit.getTablesByType(TableTypeEnum.TABLE)
        assertEquals(1, tables.filter { it.name == TestTableKit.TABLE_NAME }.size)
    }

    @Test
    fun getTableByName() {
        assertNotNull(RdbMetadataKit.getTableByName(TestTableKit.TABLE_NAME))
        assertNull(RdbMetadataKit.getTableByName("test_no_exists"))
    }

    @Test
    fun getColumnsByTableName() {
        val columns = RdbMetadataKit.getColumnsByTableName(TestTableKit.TABLE_NAME)
        assert(columns.isNotEmpty())
        assert(columns.containsKey("id"))
    }
}