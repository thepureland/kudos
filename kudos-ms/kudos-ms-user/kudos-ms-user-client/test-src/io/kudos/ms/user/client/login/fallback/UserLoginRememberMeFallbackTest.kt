package io.kudos.ms.user.client.login.fallback

import io.kudos.ms.user.client.login.proxy.IUserLoginRememberMeProxy
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for [UserLoginRememberMeFallback].
 *
 * The remember-me proxy currently exposes no methods; the fallback is kept only as a valid
 * `@HttpServiceFallback` target. This test asserts it instantiates and is wired into
 * the expected proxy type hierarchy.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserLoginRememberMeFallbackTest {

    @Test
    fun instantiates_andImplementsProxy() {
        val fallback = UserLoginRememberMeFallback()
        assertTrue(fallback is IUserLoginRememberMeProxy)
    }
}
