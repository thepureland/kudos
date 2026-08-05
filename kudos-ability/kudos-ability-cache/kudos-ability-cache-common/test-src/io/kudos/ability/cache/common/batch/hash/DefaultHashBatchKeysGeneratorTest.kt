package io.kudos.ability.cache.common.batch.hash

import io.kudos.context.support.Consts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for [DefaultHashBatchKeysGenerator].
 *
 * Covers:
 *  - single id-collection fast path (Collection / Array / scalar),
 *  - cartesian-product multi-parameter expansion,
 *  - empty collection short-circuit,
 *  - primitive-array guard,
 *  - delimiter and param-index resolution (function == null branch).
 *
 * @author K
 * @since 1.0.0
 */
internal class DefaultHashBatchKeysGeneratorTest {

    private val generator = DefaultHashBatchKeysGenerator()

    @Test
    fun delimiterIsDefault() {
        assertEquals(Consts.CACHE_KEY_DEFAULT_DELIMITER, generator.getDelimiter())
    }

    @Test
    fun singleCollectionParamMapsEachElementToKey() {
        val keys = generator.generate(null, null, listOf(1, 2, 3))
        assertEquals(listOf("1", "2", "3"), keys)
    }

    @Test
    fun singleArrayParamMapsEachElementToKey() {
        val keys = generator.generate(null, null, arrayOf("a", "b"))
        assertEquals(listOf("a", "b"), keys)
    }

    @Test
    fun singleScalarParamMapsToSingleKey() {
        val keys = generator.generate(null, null, 42)
        assertEquals(listOf("42"), keys)
    }

    @Test
    fun withoutFunctionAllParamIndexesAreUsed() {
        assertEquals(listOf(0, 1), generator.getParamIndexes(null, "a", "b"))
    }

    @Test
    fun multiParamCartesianProduct() {
        // two participating collections -> cartesian product
        val keys = generator.generate(null, null, listOf("a", "b"), listOf("x", "y"))
        assertEquals(listOf("a${Consts.CACHE_KEY_DEFAULT_DELIMITER}x",
            "a${Consts.CACHE_KEY_DEFAULT_DELIMITER}y",
            "b${Consts.CACHE_KEY_DEFAULT_DELIMITER}x",
            "b${Consts.CACHE_KEY_DEFAULT_DELIMITER}y"), keys)
    }

    @Test
    fun multiParamScalarAndArrayCartesianProduct() {
        // scalar counts as a single-element column; array expands
        val keys = generator.generate(null, null, "p", arrayOf("1", "2"))
        assertEquals(listOf("p${Consts.CACHE_KEY_DEFAULT_DELIMITER}1",
            "p${Consts.CACHE_KEY_DEFAULT_DELIMITER}2"), keys)
    }

    @Test
    fun emptyCollectionInMultiParamYieldsEmptyKeys() {
        // totalCount becomes 0 -> generateKeys short-circuits to emptyList
        val keys = generator.generate(null, null, emptyList<String>(), listOf("x"))
        assertTrue(keys.isEmpty())
    }

    @Test
    fun primitiveArrayIsRejected() {
        assertFailsWith<IllegalStateException> {
            generator.generate(null, null, intArrayOf(1, 2))
        }
    }

    @Test
    fun primitiveArrayInMultiParamIsRejected() {
        assertFailsWith<IllegalStateException> {
            generator.generate(null, null, "p", charArrayOf('a', 'b'))
        }
    }
}
