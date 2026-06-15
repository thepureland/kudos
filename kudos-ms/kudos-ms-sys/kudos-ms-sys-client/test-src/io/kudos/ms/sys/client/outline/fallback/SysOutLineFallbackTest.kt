package io.kudos.ms.sys.client.outline.fallback

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for [SysOutLineFallback].
 *
 * Coverage:
 *  - listOutLines returns the empty-list safe default for both the nullable tenantId
 *    present and absent cases.
 *  - Edge inputs: empty strings, null, Unicode.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysOutLineFallbackTest {

    private val fallback = SysOutLineFallback()

    @Test
    fun listOutLines_returnsEmptyList() {
        assertTrue(fallback.listOutLines("sys-1", "tenant-1").isEmpty())
        assertTrue(fallback.listOutLines("sys-1", null).isEmpty())
        assertTrue(fallback.listOutLines("", "").isEmpty())
        assertTrue(fallback.listOutLines("系統-ünï", "租戶-ünï").isEmpty())
    }
}
