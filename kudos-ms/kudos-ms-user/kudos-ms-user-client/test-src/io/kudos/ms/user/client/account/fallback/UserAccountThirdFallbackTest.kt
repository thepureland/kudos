package io.kudos.ms.user.client.account.fallback

import io.kudos.ms.user.client.account.proxy.IUserAccountThirdProxy
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for [UserAccountThirdFallback].
 *
 * The third-party account proxy currently exposes no methods; the fallback is kept only as a
 * valid `@HttpServiceFallback` target. This test asserts it instantiates and is wired into
 * the expected proxy type hierarchy.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountThirdFallbackTest {

    @Test
    fun instantiates_andImplementsProxy() {
        val fallback = UserAccountThirdFallback()
        assertTrue(fallback is IUserAccountThirdProxy)
    }
}
