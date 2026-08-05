package io.kudos.ms.sys.client.locale.fallback

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SysLocaleFallback].
 *
 * Coverage:
 *  - getLocaleByCode returns the null safe default.
 *  - listActiveLocales returns the empty-list safe default.
 *  - Edge inputs: empty strings, Unicode.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysLocaleFallbackTest {

    private val fallback = SysLocaleFallback()

    @Test
    fun getLocaleByCode_returnsNull() {
        assertNull(fallback.getLocaleByCode("zh_CN"))
        assertNull(fallback.getLocaleByCode(""))
        assertNull(fallback.getLocaleByCode("語言-ünï"))
    }

    @Test
    fun listActiveLocales_returnsEmptyList() {
        assertTrue(fallback.listActiveLocales().isEmpty())
    }
}
