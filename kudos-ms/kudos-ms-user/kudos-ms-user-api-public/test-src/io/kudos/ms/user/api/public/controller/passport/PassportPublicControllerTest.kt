package io.kudos.ms.user.api.public.controller.passport

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.common.passport.vo.response.UserInfoModel
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import java.time.LocalDateTime
import javax.imageio.ImageIO
import java.io.ByteArrayInputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pure unit test for [PassportPublicController].
 *
 * No Spring context, no DB. [IPassportService] is mocked with Mockito; the servlet side uses
 * Spring's [MockHttpServletRequest] / [MockHttpSession]; the "current user" is driven directly
 * through [KudosContextHolder] (the same data source [io.kudos.ms.user.common.passport.CurrentUserKit] reads).
 *
 * Covers every branch:
 *  - login: SUCCESS + userInfo (writes session), SUCCESS + null userInfo (early return, no session), non-SUCCESS
 *  - logout: explicit userId / session-resolved userId / no userId at all; session invalidation present vs absent
 *  - me: principal present (field mapping) vs absent (null)
 *  - verify / change delegations
 *  - qrCode size clamping (below min, above max, in range, default) + PNG validity
 *
 * @author K
 * @since 1.0.0
 */
internal class PassportPublicControllerTest {

    private lateinit var service: IPassportService
    private lateinit var controller: PassportPublicController

    @BeforeTest
    fun setUp() {
        KudosContextHolder.clear()
        service = mock(IPassportService::class.java)
        controller = PassportPublicController(service)
    }

    @AfterTest
    fun tearDown() {
        KudosContextHolder.clear()
    }

    private fun bindPrincipal(principal: SessionUserPrincipal?) {
        val ctx = KudosContext()
        ctx.user = principal
        KudosContextHolder.set(ctx)
    }

    private fun sampleUserInfo(id: String = "u-1", tenantId: String = "t-1", username: String = "alice") =
        UserInfoModel(
            id = id,
            username = username,
            tenantId = tenantId,
            orgId = "org-9",
            accountTypeDictCode = "EMP",
            defaultLocale = "zh_CN",
            defaultTimezone = "Asia/Shanghai",
            defaultCurrency = "CNY",
            loginTime = LocalDateTime.of(2026, 1, 2, 3, 4, 5),
        )

    private fun loginReq() = PassportLoginRequest(tenantId = "t-1", username = "alice", plainPassword = "pwd")

    // ---------------- login ----------------

    @Test
    fun login_success_withUserInfo_writesPrincipalIntoSession() {
        val info = sampleUserInfo()
        val result = PassportLoginResult.success(info)
        `when`(service.login(loginReq())).thenReturn(result)
        val request = MockHttpServletRequest()

        val res = controller.login(loginReq(), request)

        assertSame(result, res)
        val stored = request.getSession(false)!!.getAttribute(KudosContext.SESSION_KEY_USER)
        assertTrue(stored is SessionUserPrincipal)
        assertEquals("u-1", stored.id)
        assertEquals("t-1", stored.tenantId)
        assertEquals("alice", stored.username)
    }

    @Test
    fun login_success_butNullUserInfo_returnsEarlyWithoutSession() {
        // status SUCCESS but userInfo == null -> hits the `?: return res` branch, no session created
        val result = PassportLoginResult(status = PassportLoginStatusEnum.SUCCESS, userInfo = null)
        `when`(service.login(loginReq())).thenReturn(result)
        val request = MockHttpServletRequest()

        val res = controller.login(loginReq(), request)

        assertSame(result, res)
        // getSession(false) must be null: controller never touched the session
        assertNull(request.getSession(false))
    }

    @Test
    fun login_failure_doesNotWriteSession() {
        val result = PassportLoginResult.wrongPassword(3)
        `when`(service.login(loginReq())).thenReturn(result)
        val request = MockHttpServletRequest()

        val res = controller.login(loginReq(), request)

        assertSame(result, res)
        assertNull(request.getSession(false))
    }

    // ---------------- logout ----------------

    @Test
    fun logout_withExplicitUserId_callsServiceAndInvalidatesSession() {
        `when`(service.logout("u-7")).thenReturn(true)
        val request = MockHttpServletRequest()
        val session = MockHttpSession()
        request.setSession(session)

        val ok = controller.logout("u-7", request)

        assertTrue(ok)
        verify(service).logout("u-7")
        assertTrue(session.isInvalid)
    }

    @Test
    fun logout_withoutUserId_resolvesFromSessionPrincipal() {
        bindPrincipal(SessionUserPrincipal(id = "u-ctx", tenantId = "t-1", username = "bob"))
        `when`(service.logout("u-ctx")).thenReturn(true)
        val request = MockHttpServletRequest()

        val ok = controller.logout(null, request)

        assertTrue(ok)
        verify(service).logout("u-ctx")
    }

    @Test
    fun logout_withoutUserId_andNoPrincipal_returnsFalseWithoutCallingService() {
        // userId null + CurrentUserKit.currentUserIdOrNull() null -> `?: return false`
        val request = MockHttpServletRequest()

        val ok = controller.logout(null, request)

        assertFalse(ok)
        verify(service, never()).logout(anyString())
    }

    @Test
    fun logout_serviceReturnsFalse_stillInvalidatesSession() {
        `when`(service.logout("u-7")).thenReturn(false)
        val request = MockHttpServletRequest()
        val session = MockHttpSession()
        request.setSession(session)

        val ok = controller.logout("u-7", request)

        assertFalse(ok)
        // session dropped regardless of service outcome
        assertTrue(session.isInvalid)
    }

    @Test
    fun logout_withNoExistingSession_doesNotThrow() {
        // getSession(false) == null -> safe-call chain `?.invalidate()` is a no-op
        `when`(service.logout("u-7")).thenReturn(true)
        val request = MockHttpServletRequest()

        val ok = controller.logout("u-7", request)

        assertTrue(ok)
        assertNull(request.getSession(false))
    }

    // ---------------- me ----------------

    @Test
    fun me_withPrincipal_mapsThreeFieldsAndNullsRest() {
        bindPrincipal(SessionUserPrincipal(id = "u-1", tenantId = "t-1", username = "alice"))
        val before = LocalDateTime.now()

        val model = controller.me()

        assertNotNull(model)
        assertEquals("u-1", model.id)
        assertEquals("alice", model.username)
        assertEquals("t-1", model.tenantId)
        assertNull(model.orgId)
        assertNull(model.accountTypeDictCode)
        assertNull(model.defaultLocale)
        assertNull(model.defaultTimezone)
        assertNull(model.defaultCurrency)
        // loginTime is set to now() inside the method
        assertTrue(!model.loginTime.isBefore(before))
    }

    @Test
    fun me_withoutPrincipal_returnsNull() {
        assertNull(controller.me())
    }

    // ---------------- verify / change delegations ----------------

    @Test
    fun verifyPassword_delegatesToService() {
        val req = VerifyPasswordRequest(userId = "u-1", plainPassword = "p")
        `when`(service.verifyPassword(req)).thenReturn(true)
        assertTrue(controller.verifyPassword(req))
        verify(service).verifyPassword(req)
    }

    @Test
    fun verifyPassword_returnsFalseWhenServiceFalse() {
        val req = VerifyPasswordRequest(userId = "u-1", plainPassword = "p")
        `when`(service.verifyPassword(req)).thenReturn(false)
        assertFalse(controller.verifyPassword(req))
    }

    @Test
    fun verifySecurityPassword_delegatesToService() {
        val req = VerifyPasswordRequest(userId = "u-1", plainPassword = "p")
        `when`(service.verifySecurityPassword(req)).thenReturn(true)
        assertTrue(controller.verifySecurityPassword(req))
        verify(service).verifySecurityPassword(req)
    }

    @Test
    fun changePassword_delegatesToService() {
        val req = ChangePasswordRequest(userId = "u-1", oldPlainPassword = "old", newPlainPassword = "new")
        `when`(service.changePassword(req)).thenReturn(ChangePasswordResultEnum.SUCCESS)
        assertEquals(ChangePasswordResultEnum.SUCCESS, controller.changePassword(req))
        verify(service).changePassword(req)
    }

    @Test
    fun changePassword_propagatesOldPasswordWrong() {
        val req = ChangePasswordRequest(userId = "u-1", oldPlainPassword = "old", newPlainPassword = "new")
        `when`(service.changePassword(req)).thenReturn(ChangePasswordResultEnum.OLD_PASSWORD_WRONG)
        assertEquals(ChangePasswordResultEnum.OLD_PASSWORD_WRONG, controller.changePassword(req))
    }

    @Test
    fun changeSecurityPassword_delegatesToService() {
        val req = ChangePasswordRequest(userId = "u-1", oldPlainPassword = "old", newPlainPassword = "new")
        `when`(service.changeSecurityPassword(req)).thenReturn(ChangePasswordResultEnum.USER_NOT_FOUND)
        assertEquals(ChangePasswordResultEnum.USER_NOT_FOUND, controller.changeSecurityPassword(req))
        verify(service).changeSecurityPassword(req)
    }

    // ---------------- qrCode ----------------

    private fun pngWidth(bytes: ByteArray): Int {
        val img = ImageIO.read(ByteArrayInputStream(bytes))
        assertNotNull(img) { "bytes are not a readable PNG" }
        return img.width
    }

    @Test
    fun qrCode_inRangeSize_usedAsIs() {
        val bytes = controller.qrCode("hello", 300)
        assertTrue(bytes.isNotEmpty())
        assertEquals(300, pngWidth(bytes))
    }

    @Test
    fun qrCode_belowMin_clampedTo64() {
        val bytes = controller.qrCode("hello", 1)
        assertEquals(64, pngWidth(bytes))
    }

    @Test
    fun qrCode_aboveMax_clampedTo1024() {
        val bytes = controller.qrCode("hello", 5000)
        assertEquals(1024, pngWidth(bytes))
    }

    @Test
    fun qrCode_defaultSize_is200() {
        // controller default for the size param is 200, which is within [64, 1024]
        val bytes = controller.qrCode("hello", 200)
        assertEquals(200, pngWidth(bytes))
    }

    @Test
    fun qrCode_exactBoundaries_areRespected() {
        assertEquals(64, pngWidth(controller.qrCode("x", 64)))
        assertEquals(1024, pngWidth(controller.qrCode("x", 1024)))
    }

    @Test
    fun qrCode_unicodeText_rendersValidPng() {
        val bytes = controller.qrCode("otpauth://totp/kudos:测试?secret=ABC&issuer=库多斯", 256)
        assertEquals(256, pngWidth(bytes))
    }
}
