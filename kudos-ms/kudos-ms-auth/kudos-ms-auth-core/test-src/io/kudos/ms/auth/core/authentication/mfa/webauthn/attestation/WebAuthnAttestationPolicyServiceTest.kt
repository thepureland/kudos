package io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.dao.AuthWebAuthnAttestationPolicyDao
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAaguidPolicyModeEnum
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAttestationPolicyException
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAttestationPolicySaveCommand
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.po.AuthWebAuthnAttestationPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.impl.WebAuthnAttestationPolicyService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.IWebAuthnAttestationTrustCapability
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class WebAuthnAttestationPolicyServiceTest {
    private val now = Instant.parse("2026-08-25T10:00:00Z")
    private val dao = mock(AuthWebAuthnAttestationPolicyDao::class.java)
    private val service = WebAuthnAttestationPolicyService(dao, Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun defaultPolicyAllowsVerifiedRegistrationMetadata() {
        val policy = service.getEffective("t-1")

        assertFalse(policy.configured)
        assertEquals(WebAuthnAaguidPolicyModeEnum.NONE, policy.aaguidMode)
        service.enforce("t-1", AAGUID_1, "none", false)
    }

    @Test
    fun saveNormalizesAllowListFormatsAndAuditFields() {
        val result = service.save(
            command(
                aaguidMode = " allow_list ",
                aaguids = setOf(AAGUID_1.uppercase()),
                formats = setOf(" PACKED ", "none"),
            )
        )

        assertEquals(WebAuthnAaguidPolicyModeEnum.ALLOW_LIST, result.aaguidMode)
        assertEquals(setOf(AAGUID_1), result.aaguids)
        assertEquals(setOf("none", "packed"), result.allowedAttestationFormats)
        val captor = ArgumentCaptor.forClass(AuthWebAuthnAttestationPolicy::class.java)
        verify(dao).insert(captor.capture() ?: fallbackPolicy())
        assertEquals("none,packed", captor.value.allowedAttestationFormats)
        assertEquals("admin-1", captor.value.updateUserId)
        assertEquals(LocalDateTime.ofInstant(now, ZoneOffset.UTC), captor.value.updateTime)
    }

    @Test
    fun listModeAndEntriesMustBeConsistent() {
        val noneWithEntries = assertFailsWith<WebAuthnAttestationPolicyException> {
            service.save(command(aaguidMode = "NONE", aaguids = setOf(AAGUID_1)))
        }
        val allowWithoutEntries = assertFailsWith<WebAuthnAttestationPolicyException> {
            service.save(command(aaguidMode = "ALLOW_LIST"))
        }

        assertEquals("WEBAUTHN_AAGUID_POLICY_INVALID", noneWithEntries.errorCode)
        assertEquals(noneWithEntries.errorCode, allowWithoutEntries.errorCode)
        verify(dao, never()).insert(any(AuthWebAuthnAttestationPolicy::class.java) ?: fallbackPolicy())
    }

    @Test
    fun trustedAttestationCannotBeRequiredWithoutDeploymentTrustSource() {
        val error = assertFailsWith<WebAuthnAttestationPolicyException> {
            service.save(command(requireTrusted = true))
        }

        assertEquals("WEBAUTHN_ATTESTATION_TRUST_NOT_AVAILABLE", error.errorCode)
    }

    @Test
    fun availableTrustSourceAllowsSavingAndEnforcesTrustedResult() {
        val capableService = WebAuthnAttestationPolicyService(
            dao,
            Clock.fixed(now, ZoneOffset.UTC),
            IWebAuthnAttestationTrustCapability { true },
        )
        val saved = capableService.save(command(requireTrusted = true))
        `when`(dao.get("t-1")).thenReturn(policy(requireTrusted = true))

        assertTrue(saved.requireTrustedAttestation)
        val error = assertFailsWith<WebAuthnAttestationPolicyException> {
            capableService.enforce("t-1", AAGUID_1, "packed", false)
        }
        assertEquals("WEBAUTHN_ATTESTATION_UNTRUSTED", error.errorCode)
        capableService.enforce("t-1", AAGUID_1, "packed", true)
    }

    @Test
    fun reportsWhetherTrustedAttestationCanBeConfigured() {
        val capableService = WebAuthnAttestationPolicyService(
            dao,
            Clock.fixed(now, ZoneOffset.UTC),
            IWebAuthnAttestationTrustCapability { true },
        )

        assertFalse(service.isTrustedAttestationAvailable())
        assertTrue(capableService.isTrustedAttestationAvailable())
    }

    @Test
    fun configuredPolicyEnforcesFormatAndAaguidRules() {
        `when`(dao.get("t-1")).thenReturn(
            policy(
                mode = WebAuthnAaguidPolicyModeEnum.ALLOW_LIST,
                aaguids = AAGUID_1,
                formats = "packed",
            )
        )

        val formatError = assertFailsWith<WebAuthnAttestationPolicyException> {
            service.enforce("t-1", AAGUID_1, "none", false)
        }
        val aaguidError = assertFailsWith<WebAuthnAttestationPolicyException> {
            service.enforce("t-1", AAGUID_2, "packed", false)
        }

        assertEquals("WEBAUTHN_ATTESTATION_FORMAT_NOT_ALLOWED", formatError.errorCode)
        assertEquals("WEBAUTHN_AAGUID_NOT_ALLOWED", aaguidError.errorCode)
        service.enforce("t-1", AAGUID_1, "packed", false)
    }

    private fun command(
        aaguidMode: String = "NONE",
        aaguids: Set<String> = emptySet(),
        formats: Set<String> = emptySet(),
        requireTrusted: Boolean = false,
    ) = WebAuthnAttestationPolicySaveCommand(
        tenantId = "t-1",
        aaguidMode = aaguidMode,
        aaguids = aaguids,
        allowedAttestationFormats = formats,
        requireTrustedAttestation = requireTrusted,
        actorUserId = "admin-1",
        operationReason = "Enterprise authenticator baseline",
    )

    private fun policy(
        mode: WebAuthnAaguidPolicyModeEnum = WebAuthnAaguidPolicyModeEnum.NONE,
        aaguids: String? = null,
        formats: String? = null,
        requireTrusted: Boolean = false,
    ) = AuthWebAuthnAttestationPolicy {
        id = "t-1"
        tenantId = "t-1"
        aaguidMode = mode.name
        this.aaguids = aaguids
        allowedAttestationFormats = formats
        requireTrustedAttestation = requireTrusted
        createUserId = "admin-1"
        createReason = "created"
        createTime = LocalDateTime.ofInstant(now.minusSeconds(60), ZoneOffset.UTC)
        updateUserId = "admin-1"
        updateReason = "updated"
        updateTime = LocalDateTime.ofInstant(now, ZoneOffset.UTC)
    }

    private fun fallbackPolicy() = policy()

    private companion object {
        const val AAGUID_1 = "00112233-4455-6677-8899-aabbccddeeff"
        const val AAGUID_2 = "11112233-4455-6677-8899-aabbccddeeff"
    }
}
