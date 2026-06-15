package io.kudos.ms.user.api.internal.controller.passport

import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.core.passport.api.PassportApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Test for [PassportInternalController].
 *
 * Every method forwards verbatim to the injected [PassportApi]; there is no branching or transformation.
 * The api is mocked with Mockito so no Spring container / database is started. For each method the test
 * asserts identity pass-through of the result ([assertSame] for object returns) and that the api is
 * invoked once with the same request object the controller received.
 *
 * @author K
 * @since 1.0.0
 */
internal class PassportInternalControllerTest {

    private val api = mock(PassportApi::class.java)
    private val controller = PassportInternalController(api)

    @Test
    fun login_returnsResult_andDelegatesSameRequest() {
        val req = PassportLoginRequest(
            tenantId = "tenant-1", username = "alice", plainPassword = "pw", loginIp = 167772161L, authCode = 123456L,
        )
        val result = PassportLoginResult.userNotFound()
        whenCalled(api.login(req)).thenReturn(result)

        assertSame(result, controller.login(req))
        verify(api).login(req)
    }

    @Test
    fun login_returnsLockedResult_andDelegates() {
        val req = PassportLoginRequest(tenantId = "t", username = "u", plainPassword = "p")
        val locked = PassportLoginResult.locked(5)
        whenCalled(api.login(req)).thenReturn(locked)

        val actual = controller.login(req)
        assertSame(locked, actual)
        assertEquals(5, actual.loginErrorTimes)
        verify(api).login(req)
    }

    @Test
    fun logout_true_andFalse_delegates() {
        whenCalled(api.logout("u-1")).thenReturn(true)
        whenCalled(api.logout("u-2")).thenReturn(false)

        assertTrue(controller.logout("u-1"))
        assertFalse(controller.logout("u-2"))
        verify(api).logout("u-1")
        verify(api).logout("u-2")
    }

    @Test
    fun verifyPassword_true_andFalse_delegatesSameRequest() {
        val ok = VerifyPasswordRequest(userId = "u-1", plainPassword = "right")
        val bad = VerifyPasswordRequest(userId = "u-1", plainPassword = "wrong")
        whenCalled(api.verifyPassword(ok)).thenReturn(true)
        whenCalled(api.verifyPassword(bad)).thenReturn(false)

        assertTrue(controller.verifyPassword(ok))
        assertFalse(controller.verifyPassword(bad))
        verify(api).verifyPassword(ok)
        verify(api).verifyPassword(bad)
    }

    @Test
    fun verifySecurityPassword_isDistinctFromVerifyPassword_delegates() {
        val req = VerifyPasswordRequest(userId = "u-1", plainPassword = "sec")
        whenCalled(api.verifySecurityPassword(req)).thenReturn(true)
        // login-password path stubbed false to prove the controller routes to the security method
        whenCalled(api.verifyPassword(req)).thenReturn(false)

        assertTrue(controller.verifySecurityPassword(req))
        verify(api).verifySecurityPassword(req)
    }

    @Test
    fun changePassword_returnsEnum_andDelegatesSameRequest() {
        val req = ChangePasswordRequest(userId = "u-1", oldPlainPassword = "old", newPlainPassword = "new")
        whenCalled(api.changePassword(req)).thenReturn(ChangePasswordResultEnum.SUCCESS)

        assertEquals(ChangePasswordResultEnum.SUCCESS, controller.changePassword(req))
        verify(api).changePassword(req)
    }

    @Test
    fun changePassword_oldPasswordWrong_delegates() {
        val req = ChangePasswordRequest(userId = "u-1", oldPlainPassword = "bad", newPlainPassword = "new")
        whenCalled(api.changePassword(req)).thenReturn(ChangePasswordResultEnum.OLD_PASSWORD_WRONG)

        assertEquals(ChangePasswordResultEnum.OLD_PASSWORD_WRONG, controller.changePassword(req))
        verify(api).changePassword(req)
    }

    @Test
    fun changeSecurityPassword_isDistinctFromChangePassword_delegates() {
        val req = ChangePasswordRequest(userId = "u-1", oldPlainPassword = "old", newPlainPassword = "new")
        whenCalled(api.changeSecurityPassword(req)).thenReturn(ChangePasswordResultEnum.USER_NOT_FOUND)
        // login-password path stubbed SUCCESS to prove routing to the security method
        whenCalled(api.changePassword(req)).thenReturn(ChangePasswordResultEnum.SUCCESS)

        assertEquals(ChangePasswordResultEnum.USER_NOT_FOUND, controller.changeSecurityPassword(req))
        verify(api).changeSecurityPassword(req)
    }
}
