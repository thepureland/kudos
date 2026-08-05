package io.kudos.ms.user.client.passport.fallback

import io.kudos.ms.user.client.passport.proxy.IPassportProxy
import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [PassportFallback].
 *
 * Coverage:
 *  - `login` returns LOCKED with the "unreachable" message and no user info / error count.
 *  - `logout` / `verifyPassword` / `verifySecurityPassword` all return false.
 *  - `changePassword` / `changeSecurityPassword` both return USER_NOT_FOUND placeholder.
 *  - Edge inputs: empty and Unicode field values still produce the same deterministic defaults.
 *  - Wiring: the fallback implements [IPassportProxy].
 *
 * @author K
 * @since 1.0.0
 */
internal class PassportFallbackTest {

    private val fallback = PassportFallback()

    private fun loginReq(tenantId: String = "t1", username: String = "alice") =
        PassportLoginRequest(tenantId = tenantId, username = username, plainPassword = "pw")

    private fun verifyReq(userId: String = "u1") =
        VerifyPasswordRequest(userId = userId, plainPassword = "pw")

    private fun changeReq(userId: String = "u1") =
        ChangePasswordRequest(userId = userId, oldPlainPassword = "old", newPlainPassword = "new")

    @Test
    fun instantiates_andImplementsProxy() {
        assertTrue(fallback is IPassportProxy)
    }

    @Test
    fun login_returnsLockedWithMessageAndNoUserInfo() {
        val result = fallback.login(loginReq())
        assertEquals(PassportLoginStatusEnum.LOCKED, result.status)
        assertEquals("Login service is unreachable, please retry later", result.message)
        assertNull(result.userInfo)
        assertNull(result.loginErrorTimes)
    }

    @Test
    fun login_isDeterministicAcrossEdgeInputs() {
        val r1 = fallback.login(loginReq(tenantId = "", username = ""))
        val r2 = fallback.login(loginReq(tenantId = "租戶", username = "用戶ünï"))
        assertEquals(PassportLoginStatusEnum.LOCKED, r1.status)
        assertEquals(PassportLoginStatusEnum.LOCKED, r2.status)
        assertEquals(r1, r2)
    }

    @Test
    fun logout_returnsFalse() {
        assertFalse(fallback.logout("u1"))
        assertFalse(fallback.logout(""))
        assertFalse(fallback.logout("用戶"))
    }

    @Test
    fun verifyPassword_returnsFalse() {
        assertFalse(fallback.verifyPassword(verifyReq()))
        assertFalse(fallback.verifyPassword(verifyReq(userId = "")))
    }

    @Test
    fun verifySecurityPassword_returnsFalse() {
        assertFalse(fallback.verifySecurityPassword(verifyReq()))
        assertFalse(fallback.verifySecurityPassword(verifyReq(userId = "")))
    }

    @Test
    fun changePassword_returnsUserNotFound() {
        assertEquals(ChangePasswordResultEnum.USER_NOT_FOUND, fallback.changePassword(changeReq()))
        assertEquals(ChangePasswordResultEnum.USER_NOT_FOUND, fallback.changePassword(changeReq(userId = "")))
    }

    @Test
    fun changeSecurityPassword_returnsUserNotFound() {
        assertEquals(ChangePasswordResultEnum.USER_NOT_FOUND, fallback.changeSecurityPassword(changeReq()))
        assertEquals(ChangePasswordResultEnum.USER_NOT_FOUND, fallback.changeSecurityPassword(changeReq(userId = "")))
    }
}
