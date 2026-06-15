package io.kudos.base.model.vo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Single test cases.
 *
 * Covers: value mutation, component1 destructuring, update/mapInPlace in-place semantics,
 * map producing a new instance, of/singleMutable factories, toString format,
 * and reference identity for the returned instances.
 *
 * @author K
 * @since 1.0.0
 */
internal class SingleTest {

    @Test
    fun testConstructorHoldsValue() {
        val single = Single("hello")
        assertEquals("hello", single.value)
    }

    @Test
    fun testValueIsMutable() {
        val single = Single(1)
        single.value = 2
        assertEquals(2, single.value)
    }

    @Test
    fun testComponent1Destructuring() {
        val single = Single("abc")
        val (v) = single
        assertEquals("abc", v)
        assertEquals("abc", single.component1())
    }

    @Test
    fun testUpdateMutatesInPlaceAndReturnsThis() {
        val single = Single(10)
        val returned = single.update { it + 5 }
        assertEquals(15, single.value, "update should mutate value in place")
        assertSame(single, returned, "update returns this for chaining")
    }

    @Test
    fun testMapInPlaceMutatesAndReturnsThis() {
        val single = Single(3)
        val returned = single.mapInPlace { it * 4 }
        assertEquals(12, single.value)
        assertSame(single, returned, "mapInPlace returns this")
    }

    @Test
    fun testMapProducesNewInstanceWithoutMutatingOriginal() {
        val original = Single(2)
        val mapped = original.map { it.toString() + "x" }
        assertEquals("2x", mapped.value)
        assertEquals(2, original.value, "map must not mutate the original")
        assertNotSame<Any>(original, mapped)
    }

    @Test
    fun testMapCanChangeType() {
        val intSingle = Single(7)
        val lengthSingle: Single<Int> = intSingle.map { it.toString().length }
        assertEquals(1, lengthSingle.value)
    }

    @Test
    fun testOfFactory() {
        val single = Single.of("v")
        assertEquals("v", single.value)
    }

    @Test
    fun testSingleMutableExtension() {
        val single = 42.singleMutable()
        assertEquals(42, single.value)
        single.value = 43
        assertEquals(43, single.value)
    }

    @Test
    fun testNullableValueSupported() {
        val single = Single<String?>(null)
        assertNull(single.value)
        single.update { "now-set" }
        assertEquals("now-set", single.value)
    }

    @Test
    fun testToStringFormat() {
        assertEquals("Single(value=99)", Single(99).toString())
        assertEquals("Single(value=null)", Single<String?>(null).toString())
    }

    @Test
    fun testChainedInPlaceOperations() {
        val single = Single(1)
            .update { it + 1 }
            .mapInPlace { it * 10 }
        assertEquals(20, single.value)
        assertTrue(single.value == 20)
    }
}
