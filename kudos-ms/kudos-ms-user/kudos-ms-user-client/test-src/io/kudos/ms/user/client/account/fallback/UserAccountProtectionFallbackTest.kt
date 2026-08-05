package io.kudos.ms.user.client.account.fallback

import io.kudos.ms.user.client.account.proxy.IUserAccountProtectionProxy
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for [UserAccountProtectionFallback].
 *
 * The protection proxy currently exposes no methods; the fallback is kept only as a valid
 * `@FeignClient(fallback=...)` target. This test asserts it instantiates and is wired into
 * the expected proxy type hierarchy.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountProtectionFallbackTest {

    @Test
    fun instantiates_andImplementsProxy() {
        val fallback = UserAccountProtectionFallback()
        assertTrue(fallback is IUserAccountProtectionProxy)
    }
}
