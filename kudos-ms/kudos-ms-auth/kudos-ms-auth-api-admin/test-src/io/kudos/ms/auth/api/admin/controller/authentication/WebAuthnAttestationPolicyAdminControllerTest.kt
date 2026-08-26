package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.mfa.webauthn.attestation.vo.WebAuthnAttestationPolicyAdminSaveRequest
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.EffectiveWebAuthnAttestationPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAaguidPolicyModeEnum
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAttestationPolicyException
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAttestationPolicySaveCommand
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.iservice.IWebAuthnAttestationPolicyService
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

internal class WebAuthnAttestationPolicyAdminControllerTest {
    private val service = mock(IWebAuthnAttestationPolicyService::class.java)
    private val controller = WebAuthnAttestationPolicyAdminController(service)

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
    fun getPinsTenantAndReturnsDeploymentCapability() {
        `when`(service.getEffective("tenant-1")).thenReturn(
            EffectiveWebAuthnAttestationPolicy("tenant-1")
        )
        `when`(service.isTrustedAttestationAvailable()).thenReturn(true)

        val response = controller.get()

        assertEquals("NONE", response.aaguidMode)
        assertTrue(response.trustSourceAvailable)
        assertFalse(response.configured)
        verify(service).getEffective("tenant-1")
    }

    @Test
    fun savePinsTrustedTenantAndOperator() {
        `when`(service.save(any(WebAuthnAttestationPolicySaveCommand::class.java) ?: fallbackCommand()))
            .thenReturn(
                EffectiveWebAuthnAttestationPolicy(
                    tenantId = "tenant-1",
                    aaguidMode = WebAuthnAaguidPolicyModeEnum.ALLOW_LIST,
                    aaguids = setOf(AAGUID),
                    configured = true,
                )
            )

        val response = controller.save(request())

        assertEquals(setOf(AAGUID), response.aaguids)
        val captor = ArgumentCaptor.forClass(WebAuthnAttestationPolicySaveCommand::class.java)
        verify(service).save(captor.capture() ?: fallbackCommand())
        assertEquals("tenant-1", captor.value.tenantId)
        assertEquals("admin-1", captor.value.actorUserId)
        assertEquals("Enterprise authenticator baseline", captor.value.operationReason)
    }

    @Test
    fun saveMapsMissingTrustCapabilityToConflict() {
        `when`(service.save(any(WebAuthnAttestationPolicySaveCommand::class.java) ?: fallbackCommand()))
            .thenThrow(WebAuthnAttestationPolicyException("WEBAUTHN_ATTESTATION_TRUST_NOT_AVAILABLE"))

        val error = assertFailsWith<ResponseStatusException> { controller.save(request()) }

        assertEquals(409, error.statusCode.value())
        assertEquals("WEBAUTHN_ATTESTATION_TRUST_NOT_AVAILABLE", error.reason)
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
        val get = WebAuthnAttestationPolicyAdminController::class.java
            .getDeclaredMethod("get")
            .getAnnotation(RequiresPermission::class.java)
        val save = WebAuthnAttestationPolicyAdminController::class.java
            .getDeclaredMethod("save", WebAuthnAttestationPolicyAdminSaveRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)

        assertEquals("auth:webauthn-attestation-policy:view", get.value)
        assertEquals("auth:webauthn-attestation-policy:update", save.value)
    }

    private fun request() = WebAuthnAttestationPolicyAdminSaveRequest(
        aaguidMode = "ALLOW_LIST",
        aaguids = setOf(AAGUID),
        allowedAttestationFormats = setOf("packed"),
        requireTrustedAttestation = false,
        reason = "Enterprise authenticator baseline",
    )

    private fun fallbackCommand() = WebAuthnAttestationPolicySaveCommand(
        tenantId = "fallback",
        aaguidMode = "NONE",
        aaguids = emptySet(),
        allowedAttestationFormats = emptySet(),
        requireTrustedAttestation = false,
        actorUserId = "fallback",
        operationReason = "fallback",
    )

    private companion object {
        const val AAGUID = "00112233-4455-6677-8899-aabbccddeeff"
    }
}
