package io.kudos.base.support.enums

import io.kudos.base.enums.ienums.IDictEnum
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.base.lang.EnumKit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * EnumKit测试用例
 *
 * @author K
 * @since 1.0.0
 */
internal class EnumKitTest {

    private val enumClass = TimeUnit::class
    private val enumClassStr = TimeUnit::class.java.name

    @Test
    fun enumOf() {
        var code = "9"
        assertEquals(TimeUnit.MICROSECOND, EnumKit.enumOf(enumClass, code))
        assertNull(EnumKit.enumOf(YesNotEnum::class, code))
        code = "would not find"
        assertNull(EnumKit.enumOf(enumClass, code))
    }

    @Test
    fun enumOfStr() {
        var code = "1"
        assertEquals(TimeUnit.YEAR, EnumKit.enumOf(enumClassStr, code))
        try {
            EnumKit.enumOf("err.class", code)
            assertTrue(false)
        } catch (e: IllegalArgumentException) {
            assertTrue(true)
        }
        code = "would not find"
        assertNull(EnumKit.enumOf(enumClassStr, code))
        assertNull(EnumKit.enumOf(YesNotEnum::class.java.getName(), code))
    }

    @Test
    fun getCodeMap() {
        val codeMap: Map<String, String?> = EnumKit.getCodeMap(enumClass)
        assertTrue(codeMap.size >= 9)
        assertEquals(TimeUnit.YEAR.trans, codeMap["1"])
        assertEquals(TimeUnit.MICROSECOND.trans, codeMap["9"])
    }

    @Test
    fun getCodeMapStr() {
        val codeMap: Map<String, String?> = EnumKit.getCodeMap(enumClassStr)
        //		Assert.assertTrue(codeMap.size() >= 9);
        assertEquals(TimeUnit.YEAR.trans, codeMap["1"])
        assertEquals(TimeUnit.MICROSECOND.trans, codeMap["9"])
        try {
            EnumKit.getCodeMap("")
            assertTrue(false)
        } catch (e: IllegalArgumentException) {
            assertTrue(true)
        }
        try {
            EnumKit.getCodeMap("err.class")
            assertTrue(false)
        } catch (e: IllegalArgumentException) {
            assertTrue(true)
        }
        try {
            EnumKit.getCodeMap(javaClass.name)
            assertTrue(false)
        } catch (e: IllegalArgumentException) {
            assertTrue(true)
        }
    }

    @Test
    fun getCodeEnumClass() {
        val codeEnumClass = EnumKit.getCodeEnumClass(enumClassStr)
        assertTrue(codeEnumClass == enumClass)
        try {
            EnumKit.getCodeEnumClass("")
            assertTrue(false)
        } catch (e: IllegalArgumentException) {
            assertTrue(true)
        }
        try {
            EnumKit.getCodeEnumClass("err.class")
            assertTrue(false)
        } catch (e: IllegalArgumentException) {
            assertTrue(true)
        }
        try {
            EnumKit.getCodeEnumClass(javaClass.name)
            assertTrue(false)
        } catch (e: IllegalArgumentException) {
            assertTrue(true)
        }
    }

    internal enum class TimeUnit(codeStr: String, var transStr: String) : IDictEnum {
        YEAR("1", "年"),
        MONTH("2", "月"),
        WEEK("3", "周"),
        DAY("4", "日"),
        HOUR("5", "小时"),
        MINUTE("6", "分钟"),
        SECOND("7", "秒"),
        MILLISECOND("8", "毫秒"),
        MICROSECOND("9", "微秒");

        fun intValue(): Int {
            return Integer.valueOf(code)
        }

        companion object {
            fun initTrans(map: Map<String?, String?>) {
                val values: Array<TimeUnit> = entries.toTypedArray()
                for (timeUnit in values) {
                    timeUnit.transStr = map[timeUnit.code]!!
                }
            }

            fun enumOf(code: String): TimeUnit? {
                return EnumKit.enumOf(TimeUnit::class, code)
            }
        }

        override val code: String = codeStr

        override val trans: String = transStr

    }

}