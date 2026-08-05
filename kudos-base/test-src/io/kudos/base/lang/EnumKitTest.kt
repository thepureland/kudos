package io.kudos.base.lang

import io.kudos.base.enums.impl.SexEnum
import io.kudos.base.enums.impl.YesNotEnum
import java.util.EnumSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * EnumKit test cases.
 *
 * Covers: displayText / enumOf (by KClass and by FQN) / getCodeMap (by KClass and by FQN) /
 * getCodeEnumClass (valid + every require branch) / getEnumMap / getEnumList /
 * isValidEnum / getEnum / generateBitVector(iterable & vararg) / processBitVector.
 *
 * @author K
 * @since 1.0.0
 */
internal class EnumKitTest {

    private val yesNotFqn = YesNotEnum::class.java.name

    @Test
    fun displayText() {
        assertEquals("Yes", EnumKit.displayText(yesNotFqn, "1"))
        assertEquals("No", EnumKit.displayText(yesNotFqn, "0"))
        // Code not found returns null (and logs a warning)
        assertNull(EnumKit.displayText(yesNotFqn, "999"))
    }

    @Test
    fun enumOfByKClass() {
        assertEquals(YesNotEnum.YES, EnumKit.enumOf(YesNotEnum::class, "1"))
        assertEquals(YesNotEnum.NOT, EnumKit.enumOf(YesNotEnum::class, "0"))
        // Code not found returns null
        assertNull(EnumKit.enumOf(YesNotEnum::class, "999"))
        // Blank code rejected
        assertFailsWith<IllegalArgumentException> { EnumKit.enumOf(YesNotEnum::class, " ") }
    }

    @Test
    fun enumOfByFqn() {
        assertEquals(YesNotEnum.YES, EnumKit.enumOf(yesNotFqn, "1"))
        assertNull(EnumKit.enumOf(yesNotFqn, "999"))
        assertFailsWith<IllegalArgumentException> { EnumKit.enumOf(yesNotFqn, "") }
    }

    @Test
    fun getCodeMapByKClass() {
        val map = EnumKit.getCodeMap(YesNotEnum::class)
        assertEquals(2, map.size)
        assertEquals("Yes", map["1"])
        assertEquals("No", map["0"])
    }

    @Test
    fun getCodeMapByFqn() {
        val map = EnumKit.getCodeMap(yesNotFqn)
        assertEquals(2, map.size)
        assertEquals("Yes", map["1"])
    }

    @Test
    fun getCodeEnumClass() {
        assertEquals(YesNotEnum::class, EnumKit.getCodeEnumClass(yesNotFqn))

        // blank FQN
        assertFailsWith<IllegalArgumentException> { EnumKit.getCodeEnumClass("  ") }
        // non-existent class
        assertFailsWith<IllegalArgumentException> { EnumKit.getCodeEnumClass("a.b.c.NotExist") }
        // existing class but not an enum
        assertFailsWith<IllegalArgumentException> { EnumKit.getCodeEnumClass(String::class.java.name) }
        // an enum that does not implement IDictEnum
        assertFailsWith<IllegalArgumentException> { EnumKit.getCodeEnumClass(PlainEnum::class.java.name) }
    }

    @Test
    fun getEnumMap() {
        val map = EnumKit.getEnumMap(YesNotEnum::class)
        assertEquals(YesNotEnum.YES, map["YES"])
        assertEquals(YesNotEnum.NOT, map["NOT"])
    }

    @Test
    fun getEnumList() {
        val list = EnumKit.getEnumList(YesNotEnum::class)
        assertEquals(listOf(YesNotEnum.YES, YesNotEnum.NOT), list)
    }

    @Test
    fun isValidEnum() {
        assertTrue(EnumKit.isValidEnum(YesNotEnum::class, "YES"))
        assertFalse(EnumKit.isValidEnum(YesNotEnum::class, "NOPE"))
        assertFalse(EnumKit.isValidEnum(YesNotEnum::class, null))
    }

    @Test
    fun getEnum() {
        assertEquals(YesNotEnum.YES, EnumKit.getEnum(YesNotEnum::class, "YES"))
        assertNull(EnumKit.getEnum(YesNotEnum::class, "NOPE"))
        assertNull(EnumKit.getEnum(YesNotEnum::class, null))
    }

    @Test
    fun generateBitVectorAndProcess() {
        // vararg form
        val v1 = EnumKit.generateBitVector(SexEnum::class, SexEnum.MALE, SexEnum.FEMALE)
        val set1: EnumSet<SexEnum> = EnumKit.processBitVector(SexEnum::class, v1)
        assertTrue(set1.contains(SexEnum.MALE))
        assertTrue(set1.contains(SexEnum.FEMALE))

        // iterable form
        val v2 = EnumKit.generateBitVector(SexEnum::class, listOf(SexEnum.MALE))
        val set2 = EnumKit.processBitVector(SexEnum::class, v2)
        assertEquals(EnumSet.of(SexEnum.MALE), set2)

        // empty selection -> empty set
        val v0 = EnumKit.generateBitVector(SexEnum::class, emptyList())
        assertEquals(0L, v0)
        assertTrue(EnumKit.processBitVector(SexEnum::class, v0).isEmpty())
    }

    /** Plain enum that does NOT implement IDictEnum, used to hit the require branch in getCodeEnumClass. */
    @Suppress("unused")
    internal enum class PlainEnum { A, B }

}
