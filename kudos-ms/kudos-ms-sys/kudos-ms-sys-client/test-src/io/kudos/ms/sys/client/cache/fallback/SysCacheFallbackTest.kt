package io.kudos.ms.sys.client.cache.fallback

import io.kudos.ms.sys.client.cache.proxy.ISysCacheProxy
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for [SysCacheFallback].
 *
 * The cache proxy currently exposes no public methods; the fallback is kept only as a
 * valid `@FeignClient(fallback=...)` target. This test asserts it can be instantiated
 * and is wired into the expected proxy/support type hierarchy.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysCacheFallbackTest {

    @Test
    fun instantiates_andImplementsProxy() {
        val fallback = SysCacheFallback()
        assertTrue(fallback is ISysCacheProxy)
    }
}
