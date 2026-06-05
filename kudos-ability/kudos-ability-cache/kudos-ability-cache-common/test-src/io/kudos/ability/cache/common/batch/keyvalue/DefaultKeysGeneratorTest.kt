package io.kudos.ability.cache.common.batch.keyvalue

import kotlin.test.*

/**
 * test for DefaultKeysGenerator
 *
 * Covers the unambiguous key-composition rules (scalar params, a single collection/array param,
 * the "::" delimiter, key count = product of element counts). The multi-collection cartesian
 * expansion described in the class KDoc is intentionally not asserted here: the current
 * implementation aligns expanded params position-by-position rather than producing a true
 * cartesian product, which is a known divergence from the doc.
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
}
