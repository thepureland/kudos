package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.core.authentication.lifecycle.model.AuthenticationInvalidationResult
import io.kudos.ms.auth.core.authentication.lifecycle.service.iservice.IAuthenticationLifecycleService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when` as whenCalled
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AuthenticationLogoutPublicControllerTest {

    private val lifecycleService = mock(IAuthenticationLifecycleService::class.java)
    private val controller = AuthenticationLogoutPublicController(lifecycleService)

    @Test
    fun logoutAll_invalidatesEveryAuthenticationArtifactThenCurrentHttpSession() {
        val request = MockHttpServletRequest()
        val httpSession = request.getSession(true) as MockHttpSession
        httpSession.setAttribute(
            KudosContext.SESSION_KEY_USER,
            SessionUserPrincipal("u-1", "t-1", "alice"),
        )
        whenCalled(lifecycleService.invalidateAll("t-1", "u-1", "USER_LOGOUT_ALL"))
            .thenReturn(AuthenticationInvalidationResult(3, 2, 4))

        assertTrue(controller.logoutAll(request))

        verify(lifecycleService).invalidateAll("t-1", "u-1", "USER_LOGOUT_ALL")
        assertTrue(httpSession.isInvalid)
    }

    @Test
    fun logoutAll_withoutAuthenticatedSessionDoesNothing() {
        assertFalse(controller.logoutAll(MockHttpServletRequest()))
        verifyNoInteractions(lifecycleService)
    }
}
