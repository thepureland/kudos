package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.mfa.webauthn.risk.vo.WebAuthnAuthenticatorRiskPolicyAdminSaveRequest
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.EffectiveWebAuthnAuthenticatorRiskPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskPolicyException
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskPolicySaveCommand
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.service.iservice.IWebAuthnAuthenticatorRiskPolicyService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class WebAuthnAuthenticatorRiskPolicyAdminControllerTest {
    private val service = mock(IWebAuthnAuthenticatorRiskPolicyService::class.java)
    private val controller = WebAuthnAuthenticatorRiskPolicyAdminController(service)

    @BeforeTest
    fun bindOperator() {
        KudosContextHolder.set(
            KudosContext().apply {
                user = SessionUserPrincipal("admin-1", "tenant-1", "administrator")
            }
        )
    }

    @AfterTest
    fun clearOperator() = KudosContextHolder.clear()

    @Test
    fun getPinsTenantAndReturnsRiskCapability() {
        `when`(service.getEffective("tenant-1")).thenReturn(
            EffectiveWebAuthnAuthenticatorRiskPolicy("tenant-1")
        )
        `when`(service.isRiskEvaluationAvailable()).thenReturn(true)

        val response = controller.get()

        assertTrue(response.riskEvaluationAvailable)
        assertTrue(response.blockedRiskLevels.isEmpty())
        assertFalse(response.configured)
        verify(service).getEffective("tenant-1")
    }

    @Test
    fun savePinsTenantAndOperator() {
        `when`(service.save(any(WebAuthnAuthenticatorRiskPolicySaveCommand::class.java) ?: fallbackCommand()))
            .thenReturn(
                EffectiveWebAuthnAuthenticatorRiskPolicy(
                    tenantId = "tenant-1",
                    blockedRiskLevels = setOf(WebAuthnAuthenticatorRiskLevelEnum.CRITICAL),
                    configured = true,
                )
            )

        val response = controller.save(request())

        assertEquals(setOf("CRITICAL"), response.blockedRiskLevels)
        val captor = ArgumentCaptor.forClass(WebAuthnAuthenticatorRiskPolicySaveCommand::class.java)
        verify(service).save(captor.capture() ?: fallbackCommand())
        assertEquals("tenant-1", captor.value.tenantId)
        assertEquals("admin-1", captor.value.actorUserId)
        assertEquals("Block compromised authenticators", captor.value.operationReason)
    }

    @Test
    fun saveMapsMissingRiskCapabilityToConflict() {
        `when`(service.save(any(WebAuthnAuthenticatorRiskPolicySaveCommand::class.java) ?: fallbackCommand()))
            .thenThrow(WebAuthnAuthenticatorRiskPolicyException("WEBAUTHN_RISK_EVALUATION_NOT_AVAILABLE"))

        val error = assertFailsWith<ResponseStatusException> { controller.save(request()) }

        assertEquals(409, error.statusCode.value())
        assertEquals("WEBAUTHN_RISK_EVALUATION_NOT_AVAILABLE", error.reason)
    }

    @Test
    fun endpointsRequireAuthenticatedAdministrator() {
        KudosContextHolder.clear()

        val getError = assertFailsWith<ResponseStatusException> { controller.get() }
        val saveError = assertFailsWith<ResponseStatusException> { controller.save(request()) }

        assertEquals(401, getError.statusCode.value())
        assertEquals(401, saveError.statusCode.value())
        verifyNoInteractions(service)
    }

    @Test
    fun endpointsDeclareDedicatedPermissions() {
        val get = WebAuthnAuthenticatorRiskPolicyAdminController::class.java
            .getDeclaredMethod("get")
            .getAnnotation(RequiresPermission::class.java)
        val save = WebAuthnAuthenticatorRiskPolicyAdminController::class.java
            .getDeclaredMethod("save", WebAuthnAuthenticatorRiskPolicyAdminSaveRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)

        assertEquals("auth:webauthn-risk-policy:view", get.value)
        assertEquals("auth:webauthn-risk-policy:update", save.value)
    }

    private fun request() = WebAuthnAuthenticatorRiskPolicyAdminSaveRequest(
        blockedRiskLevels = setOf("CRITICAL"),
        reason = "Block compromised authenticators",
    )

    private fun fallbackCommand() = WebAuthnAuthenticatorRiskPolicySaveCommand(
        tenantId = "fallback",
        blockedRiskLevels = emptySet(),
        actorUserId = "fallback",
        operationReason = "fallback",
    )
}
