package io.kudos.ability.data.rdb.jdbc.kit

import io.kudos.test.common.init.EnableKudosTest
import org.soul.ability.data.rdb.jdbc.metadata.RdbTypeEnum
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * RdbKit测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
internal class RdbKitTest {

    private var url = "jdbc:h2:~/h2/ds1;AUTO_SERVER=TRUE;AUTO_SERVER_PORT=9092;DATABASE_TO_LOWER=TRUE;"

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
        val connection = RdbKit.newConnection(url, "sa", "sa")
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