package io.kudos.ms.sys.client.dict.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.sys.client.dict.proxy.ISysDictItemProxy
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * Unit tests for [SysDictItemFallback].
 *
 * Coverage:
 *  - The class (whose proxied API currently defines no methods) can be instantiated and
 *    is a valid [ISysDictItemProxy] / [AbstractHttpFallbackSupport] implementation, keeping
 *    it usable as a `@HttpServiceFallback` target.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictItemFallbackTest {

    @Test
    fun instantiation_yieldsValidProxyFallback() {
        val fallback = SysDictItemFallback()
        assertIs<ISysDictItemProxy>(fallback)
        assertIs<AbstractHttpFallbackSupport>(fallback)
    }
}
