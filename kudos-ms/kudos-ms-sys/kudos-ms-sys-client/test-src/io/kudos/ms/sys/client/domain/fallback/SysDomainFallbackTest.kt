package io.kudos.ms.sys.client.domain.fallback

import kotlin.test.Test
import kotlin.test.assertNull

/**
 * Unit tests for [SysDomainFallback].
 *
 * Coverage:
 *  - getDomainByName returns the null safe default.
 *  - Edge inputs: empty strings, Unicode, very long names.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDomainFallbackTest {

    private val fallback = SysDomainFallback()

    @Test
    fun getDomainByName_returnsNull() {
        assertNull(fallback.getDomainByName("example.com"))
        assertNull(fallback.getDomainByName(""))
        assertNull(fallback.getDomainByName("網域-ünï"))
        assertNull(fallback.getDomainByName("d".repeat(10_000)))
    }
}
