package io.kudos.ms.user.core.passport.api

import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure unit test for [PassportApi] — verifies every API method delegates to [IPassportService]
 * and forwards the result unchanged. The collaborator is a Mockito mock injected via the constructor.
 *
 * No DB / Spring container required.
 *
 * @author K
 * @since 1.0.0
 */
internal class PassportApiPureTest {

    private val service = mock(IPassportService::class.java)
    private val api = PassportApi(service)

    @Test
    fun login_delegatesAndForwardsResult() {
        val req = PassportLoginRequest("t1", "alice", "pwd")
        val expected = PassportLoginResult(PassportLoginStatusEnum.SUCCESS)
        whenCalled(service.login(req)).thenReturn(expected)

        val actual = api.login(req)

        assertEquals(expected, actual)
        verify(service).login(req)
    }

    @Test
    fun logout_delegatesAndForwardsResult() {
        whenCalled(service.logout("u1")).thenReturn(true)
        assertTrue(api.logout("u1"))
        verify(service).logout("u1")
    }

    @Test
    fun logout_forwardsFalse() {
        whenCalled(service.logout("u-none")).thenReturn(false)
        assertFalse(api.logout("u-none"))
    }

    @Test
    fun verifyPassword_delegatesAndForwardsResult() {
        val req = VerifyPasswordRequest("u1", "pwd")
        whenCalled(service.verifyPassword(req)).thenReturn(true)
        assertTrue(api.verifyPassword(req))
        verify(service).verifyPassword(req)
    }

    @Test
    fun verifySecurityPassword_delegatesAndForwardsResult() {
        val req = VerifyPasswordRequest("u1", "spwd")
        whenCalled(service.verifySecurityPassword(req)).thenReturn(false)
        assertFalse(api.verifySecurityPassword(req))
        verify(service).verifySecurityPassword(req)
    }

    @Test
    fun changePassword_delegatesAndForwardsResult() {
        val req = ChangePasswordRequest("u1", "old", "new")
        whenCalled(service.changePassword(req)).thenReturn(ChangePasswordResultEnum.SUCCESS)
        assertEquals(ChangePasswordResultEnum.SUCCESS, api.changePassword(req))
        verify(service).changePassword(req)
    }

    @Test
    fun changeSecurityPassword_delegatesAndForwardsResult() {
        val req = ChangePasswordRequest("u1", "old", "new")
        whenCalled(service.changeSecurityPassword(req)).thenReturn(ChangePasswordResultEnum.OLD_PASSWORD_WRONG)
        assertEquals(ChangePasswordResultEnum.OLD_PASSWORD_WRONG, api.changeSecurityPassword(req))
        verify(service).changeSecurityPassword(req)
    }
}
