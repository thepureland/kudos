package io.kudos.base.lang.string

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals


/**
 * XString单元测试类
 *
 * @author K
 * @since 1.0.0
 */
internal class XCharSequenceTest {

    @Test
    fun replaceEach() {
        val map = mapOf("1" to "壹", "2" to "贰", "3" to "叁", null to "*", "" to "*")
        assertEquals("壹贰叁.壹", "123.1".replaceEach(map))
    }

    @Test
    fun toHexStr() {
        assertEquals("4b75646f73", "Kudos".toHexStr())
    }

    @Test
    fun decodeHexStr() {
        assertEquals("Kudos", "4b75646f73".decodeHexStr())
    }

    @Test
    fun divideAverage() {
        assertEquals(0, "".divideAverage(3).size)
        assertEquals(0, "ererr".divideAverage(0).size)
        assertEquals(0, "ererr".divideAverage(-3).size)
        var arr = "123456".divideAverage(3)
        assertEquals(3, arr.size)
        assertEquals("12", arr[0])
        assertEquals("34", arr[1])
        assertEquals("56", arr[2])
        arr = "1234567".divideAverage(3)
        assertEquals(3, arr.size)
        assertEquals("123", arr[0])
        assertEquals("456", arr[1])
        assertEquals("7", arr[2])
    }

    @Test
    fun humpToUnderscore() {
        assertEquals("", "".humpToUnderscore())
        assertEquals("HUMP_TO_UNDERLINE", "humpToUnderline".humpToUnderscore())
    }

    @Test
    fun underscoreToHump() {
        assertEquals("", "".underscoreToHump())
        assertEquals("humpToUnderline", "HUMP_TO_UNDERLINE".underscoreToHump())
        assertFalse("HumpToUnderline" == "HUMP_TO_UNDERLINE".underscoreToHump())
    }

    @Test
    fun fillTemplate() {
        val tmpl = $$$"${param1} ${param2}$${param1}"
        val paramMap = mapOf("param1" to "1", "param2" to "2")
        assertEquals("1 2$1", tmpl.fillTemplateByObjectMap(paramMap))
    }

    @Test
    fun appendIfMissing() {
        val suffix = ".txt"
        val expected = "test.txt"
        assertNotEquals(expected, "i18n".appendIfMissing(suffix))
        assertNotEquals(expected, "i18n".appendIfMissing(suffix, true))
        assertEquals(expected, "test.txt".appendIfMissing(suffix))
        assertEquals(expected, "test.txt".appendIfMissing(".Txt", true))
    }

}