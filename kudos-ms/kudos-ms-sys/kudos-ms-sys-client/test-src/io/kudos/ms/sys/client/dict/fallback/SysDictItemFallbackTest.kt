package io.kudos.ms.sys.client.dict.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.sys.client.dict.proxy.ISysDictItemProxy
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * Unit tests for [SysDictItemFallback].
 *
 * Coverage:
 *  - The class (whose proxied API currently defines no methods) can be instantiated and
 *    is a valid [ISysDictItemProxy] / [AbstractFeignFallbackSupport] implementation, keeping
 *    it usable as a `@FeignClient(fallback=...)` target.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictItemFallbackTest {

    @Test
    fun instantiation_yieldsValidProxyFallback() {
        val fallback = SysDictItemFallback()
        assertIs<ISysDictItemProxy>(fallback)
        assertIs<AbstractFeignFallbackSupport>(fallback)
    }
}
