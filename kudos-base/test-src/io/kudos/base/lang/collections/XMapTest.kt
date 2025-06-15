package io.kudos.base.lang.collections

import kotlin.test.Test
import kotlin.test.assertEquals


/**
 * XMap测试用例
 *
 * @author K
 * @since 1.0.0
 */
class XMapTest {

    @Test
    fun testToArrOfArr() {
        val map = mapOf("k1" to "v1", "k2" to "v2", "k3" to "v3")
        val arrOfArr = map.toArrOfArr()
        assertEquals(3, arrOfArr.size.toLong())
        assertEquals("k1", arrOfArr[0][0])
        assertEquals("v1", arrOfArr[0][1])
        assertEquals("k2", arrOfArr[1][0])
        assertEquals("v2", arrOfArr[1][1])
        assertEquals("k3", arrOfArr[2][0])
        assertEquals("v3", arrOfArr[2][1])
    }

}