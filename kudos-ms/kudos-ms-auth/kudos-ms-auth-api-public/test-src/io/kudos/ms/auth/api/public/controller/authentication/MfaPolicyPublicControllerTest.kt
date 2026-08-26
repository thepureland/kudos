package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaRequirementModeEnum
import io.kudos.ms.auth.core.authentication.mfa.policy.model.EffectiveTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.model.MfaPolicyDecision
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.ITenantMfaPolicyService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.springframework.web.server.ResponseStatusException

internal class MfaPolicyPublicControllerTest {
    private val service = mock(ITenantMfaPolicyService::class.java)
    private val controller = MfaPolicyPublicController(service)

    @Test
    fun statusIsPinnedToCurrentManagedSessionPrincipal() {
        val expiresAt = LocalDateTime.of(2026, 9, 1, 10, 0)
        val policy = EffectiveTenantMfaPolicy(
            tenantId = "t-1",
            mode = MfaRequirementModeEnum.REQUIRED,
            allowedMethods = setOf(MfaMethodEnum.TOTP),
            configured = true,
        )
        `when`(service.evaluate("t-1", "u-1")).thenReturn(
            MfaPolicyDecision(policy, true, false, true, true, expiresAt)
        )

        val result = controller.status(request())

        assertEquals("REQUIRED", result.mode)
        assertTrue(result.enrollmentRequired)
        assertEquals(expiresAt, result.graceExpiresAt)
        verify(service).evaluate("t-1", "u-1")
    }

    @Test
    fun missingManagedSessionIsUnauthorized() {
        assertFailsWith<ResponseStatusException> {
            controller.status(MockHttpServletRequest())
        }
    }

    private fun request() = MockHttpServletRequest().apply {
        getSession(true)!!.setAttribute(
            KudosContext.SESSION_KEY_USER,
            SessionUserPrincipal("u-1", "t-1", "alice"),
        )
    }
}
