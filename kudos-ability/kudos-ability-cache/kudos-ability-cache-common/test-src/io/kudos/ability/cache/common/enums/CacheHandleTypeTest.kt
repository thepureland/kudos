package io.kudos.ability.cache.common.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Unit tests for [CacheHandleType].
 *
 * Coverage:
 *  - Every enum constant exposes the expected [CacheHandleType.code] / [CacheHandleType.displayText].
 *  - [CacheHandleType.get] returns the matching constant for each valid code.
 *  - [CacheHandleType.get] returns null for unknown / empty / case-mismatched codes (no throw).
 *
 * @author K
 * @since 1.0.0
 */
internal class CacheHandleTypeTest {

    @Test
    fun codesAndDisplayTexts_areStable() {
        assertEquals("overload", CacheHandleType.OVERLOAD.code)
        assertEquals("overloadAll", CacheHandleType.OVERLOAD_ALL.code)
        assertEquals("evict", CacheHandleType.EVICT.code)
        assertEquals("evictAll", CacheHandleType.EVICT_ALL.code)
        assertEquals("getKey", CacheHandleType.GET_KEY.code)
        assertEquals("getValue", CacheHandleType.GET_VALUE.code)

        // displayText is non-blank for every constant (guards against accidental empties)
        CacheHandleType.entries.forEach {
            assertEquals(false, it.displayText.isBlank(), "displayText must not be blank for $it")
        }
    }

    @Test
    fun get_returnsMatchingConstant_forEveryCode() {
        CacheHandleType.entries.forEach { expected ->
            assertSame(expected, CacheHandleType.get(expected.code),
                "get(\"${expected.code}\") should return $expected")
        }
    }

    @Test
    fun get_unknownCode_returnsNull() {
        assertNull(CacheHandleType.get("no_such_code"))
    }

    @Test
    fun get_emptyCode_returnsNull() {
        assertNull(CacheHandleType.get(""))
    }

    @Test
    fun get_isCaseSensitive_returnsNullForWrongCase() {
        // codes are matched literally; uppercase variant must not match.
        assertNull(CacheHandleType.get("OVERLOAD"))
        assertNull(CacheHandleType.get("EVICT"))
    }
}
