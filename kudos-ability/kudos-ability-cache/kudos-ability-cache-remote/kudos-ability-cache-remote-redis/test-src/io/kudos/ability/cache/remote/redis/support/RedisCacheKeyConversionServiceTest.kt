package io.kudos.ability.cache.remote.redis.support

import org.springframework.cache.interceptor.SimpleKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [RedisCacheKeyConversionService].
 *
 * Covers all conversion branches of the Object→String converter built by [RedisCacheKeyConversionService.create]:
 * - raw String / non-String objects → `toString()`
 * - single-param [SimpleKey] → the bare param string (matches raw-String access)
 * - zero-param / multi-param [SimpleKey] → stable `SimpleKey.toString()` form
 * - null param inside a single-param SimpleKey, Unicode and long inputs
 *
 * @author K
 * @since 1.0.0
 */
internal class RedisCacheKeyConversionServiceTest {

    private val cs = RedisCacheKeyConversionService.create()

    @Test
    fun create_registersObjectToStringConversion() {
        assertNotNull(cs)
        assertTrue(cs.canConvert(Any::class.java, String::class.java))
        assertTrue(cs.canConvert(SimpleKey::class.java, String::class.java))
    }

    @Test
    fun rawString_convertsToItself() {
        assertEquals("k", cs.convert("k", String::class.java))
    }

    @Test
    fun nonStringObject_convertsViaToString() {
        assertEquals("42", cs.convert(42, String::class.java))
        assertEquals("3.14", cs.convert(3.14, String::class.java))
        assertEquals("true", cs.convert(true, String::class.java))
    }

    @Test
    fun singleParamSimpleKey_collapsesToBareParamString() {
        // Must match raw-String access: SimpleKey("k") and "k" land on the same Redis key.
        assertEquals("k", cs.convert(SimpleKey("k"), String::class.java))
    }

    @Test
    fun singleParamSimpleKey_nonStringParam_usesParamToString() {
        assertEquals("99", cs.convert(SimpleKey(99), String::class.java))
    }

    @Test
    fun singleParamSimpleKey_nullParam_yieldsNullLiteral() {
        assertEquals("null", cs.convert(SimpleKey(null as Any?), String::class.java))
    }

    @Test
    fun emptySimpleKey_fallsBackToSimpleKeyToString() {
        val empty = SimpleKey.EMPTY
        assertEquals(empty.toString(), cs.convert(empty, String::class.java))
    }

    @Test
    fun multiParamSimpleKey_fallsBackToSimpleKeyToString() {
        val key = SimpleKey("a", "b")
        assertEquals(key.toString(), cs.convert(key, String::class.java))
        // sanity: the stable form is Spring's "SimpleKey [a, b]" representation
        assertTrue(key.toString().contains("a"))
        assertTrue(key.toString().contains("b"))
    }

    @Test
    fun unicodeAndLongKeys_convertLosslessly() {
        val unicode = "鍵é中文-ключ-🔑"
        assertEquals(unicode, cs.convert(SimpleKey(unicode), String::class.java))
        val long = "x".repeat(10_000)
        assertEquals(long, cs.convert(SimpleKey(long), String::class.java))
    }
}
