package io.kudos.ability.cache.common.batch.keyvalue

import kotlin.test.*

/**
 * test for DefaultKeysGenerator
 *
 * Covers positional-zip key composition: scalar params repeated across all keys, collection/array
 * params consumed element-by-element, the "::" delimiter, key count = the common collection length,
 * and the equal-length requirement on participating collection/array params.
 *
 * @author K
 * @since 1.0.0
 */
internal class DefaultKeysGeneratorTest {

    private val generator = DefaultKeysGenerator()

    @Test
    fun delimiterIsDoubleColon() {
        assertEquals("::", generator.getDelimiter())
    }

    @Test
    fun allScalarParamsProduceSingleKey() {
        val keys = generator.generate(null, null, "1", "2", "3")
        assertEquals(listOf("1::2::3"), keys)
    }

    @Test
    fun scalarPlusCollectionExpandsAcrossCollection() {
        val keys = generator.generate(null, null, "p", listOf("a", "b"))
        assertEquals(listOf("p::a", "p::b"), keys)
    }

    @Test
    fun singleCollectionProducesOneKeyPerElement() {
        val keys = generator.generate(null, null, listOf("x", "y", "z"))
        assertEquals(listOf("x", "y", "z"), keys)
    }

    @Test
    fun scalarPlusArrayExpandsAcrossArray() {
        val keys = generator.generate(null, null, "p", arrayOf("a", "b", "c"))
        assertEquals(listOf("p::a", "p::b", "p::c"), keys)
    }

    @Test
    fun withoutFunctionAllParamIndexesAreUsed() {
        assertEquals(listOf(0, 1, 2), generator.getParamIndexes(null, "a", "b", "c"))
    }

    @Test
    fun primitiveArraysAreRejected() {
        assertFailsWith<IllegalStateException> {
            generator.generate(null, null, intArrayOf(1, 2, 3))
        }
    }

    @Test
    fun equalLengthCollectionsAreZippedPositionally() {
        // positional zip: i-th key = i-th element of each collection (NOT a cartesian product)
        val keys = generator.generate(null, null, listOf("a", "b"), arrayOf("c", "d"))
        assertEquals(listOf("a::c", "b::d"), keys)
    }

    @Test
    fun scalarIsRepeatedAcrossZippedCollections() {
        val keys = generator.generate(null, null, "1", listOf("a", "b", "c"), arrayOf("x", "y", "z"))
        assertEquals(listOf("1::a::x", "1::b::y", "1::c::z"), keys)
    }

    @Test
    fun mismatchedCollectionLengthsAreRejected() {
        assertFailsWith<IllegalStateException> {
            generator.generate(null, null, listOf("a", "b"), arrayOf("c", "d", "e"))
        }
    }

    @Test
    fun emptyCollectionProducesNoKeys() {
        assertTrue(generator.generate(null, null, "1", emptyList<String>()).isEmpty())
    }
}
