package io.kudos.ms.sys.client.datasource.fallback

import kotlin.test.Test
import kotlin.test.assertNull

/**
 * Unit tests for [SysDataSourceFallback].
 *
 * Coverage:
 *  - getDataSourceFromCache returns the null safe default for both the nullable
 *    atomicServiceCode present and absent cases.
 *  - Edge inputs: empty strings, null, Unicode, very long codes.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDataSourceFallbackTest {

    private val fallback = SysDataSourceFallback()

    @Test
    fun getDataSourceFromCache_returnsNull() {
        assertNull(fallback.getDataSourceFromCache("tenant-1", "atomic-1"))
        assertNull(fallback.getDataSourceFromCache("tenant-1", null))
        assertNull(fallback.getDataSourceFromCache("", ""))
        assertNull(fallback.getDataSourceFromCache("租戶-ünï", "服務-ünï"))
        assertNull(fallback.getDataSourceFromCache("t".repeat(10_000), null))
    }
}
