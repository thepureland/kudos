package io.kudos.base.lang

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse


/**
 * test for XBoolean.kt
 *
 * @author K
 * @since 1.0.0
 */
class XBooleanTest {

    @Test
    fun toStringTrueFalse() {
        assertEquals("true", true.toStringTrueFalse())
        assertEquals("false", false.toStringTrueFalse())
    }

    @Test
    fun toStringOnOff() {
        assertEquals("on", true.toStringOnOff())
        assertEquals("off", false.toStringOnOff())
    }

    @Test
    fun toStringYesNo() {
        assertEquals("yes", true.toStringYesNo())
        assertEquals("no", false.toStringYesNo())
    }

    @Test
    fun booleanToString() {
        assertEquals("true", true.toString())
        assertEquals("false", false.toString())
    }

    @Test
    fun and() {
        assert(arrayOf(true, true).and())
        assertFalse(arrayOf(false, false).and())
        assertFalse(arrayOf(true, false).and())
        assertFalse(arrayOf(true, true, false).and())
        assert(arrayOf(true, true, true).and())
        assertFailsWith<IllegalArgumentException> { arrayOf<Boolean>().and()  }
    }


    @Test
    fun or() {
        assert(arrayOf(true, true).or())
        assertFalse(arrayOf(false, false).or())
        assert(arrayOf(true, false).or())
        assert(arrayOf(true, true, false).or())
        assert(arrayOf(true, true, true).or())
        assertFalse(arrayOf(false, false, false).or())
        assertFailsWith<IllegalArgumentException> { arrayOf<Boolean>().or()  }
    }

    @Test
    fun xor() {
        assertFalse(arrayOf(true, true).xor())
        assertFalse(arrayOf(false, false).xor())
        assert(arrayOf(true, false).xor())
        assertFalse(arrayOf(true, true, false).xor())
        assertFalse(arrayOf(false, false, false).xor())
        assertFailsWith<IllegalArgumentException> { arrayOf<Boolean>().xor()  }
    }

}