package io.kudos.ms.user.client.contact.fallback

import io.kudos.ms.user.client.contact.proxy.IUserContactWayProxy
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for [UserContactWayFallback].
 *
 * The contact-way proxy currently exposes no methods; the fallback is kept only as a valid
 * `@FeignClient(fallback=...)` target. This test asserts it instantiates and is wired into
 * the expected proxy type hierarchy.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserContactWayFallbackTest {

    @Test
    fun instantiates_andImplementsProxy() {
        val fallback = UserContactWayFallback()
        assertTrue(fallback is IUserContactWayProxy)
    }
}
