package io.dudos.ability.data.rdb.jdbc.kit

import io.kudos.test.common.SpringTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.soul.ability.data.rdb.jdbc.metadata.RdbTypeEnum

/**
 * RdbKit测试用例
 *
 * @author K
 * @since 1.0.0
 */
internal class RdbKitTest : SpringTest() {

    private val url = "jdbc:h2:tcp://localhost:9092/./h2;DATABASE_TO_LOWER=TRUE"

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
        val connection = RdbKit.newConnection(url, "sa", null)
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

}