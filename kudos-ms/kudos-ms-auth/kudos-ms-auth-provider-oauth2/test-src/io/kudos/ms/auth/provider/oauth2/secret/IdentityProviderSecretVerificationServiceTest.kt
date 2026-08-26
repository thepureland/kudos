package io.kudos.ms.auth.provider.oauth2.secret

import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.model.po.AuthIdentityProvider
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class IdentityProviderSecretVerificationServiceTest {
    private val providerDao = mock(AuthIdentityProviderDao::class.java)
    private val registry = mock(ClientSecretResolverRegistry::class.java)
    private val service = IdentityProviderSecretVerificationService(providerDao, registry)

    @Test
    fun verifiesStoredReferenceAndReturnsOnlySafeMetadata() {
        val checkedAt = LocalDateTime.of(2026, 8, 24, 12, 0)
        `when`(providerDao.get("provider-1")).thenReturn(provider("vault:auth/google"))
        `when`(registry.verify("vault:auth/google")).thenReturn(
            ClientSecretReferenceCheck("vault", ClientSecretResolutionStatus.RESOLVED, checkedAt)
        )

        val result = service.verify(command())

        assertEquals("provider-1", result.providerId)
        assertEquals("vault", result.referenceScheme)
        assertEquals(ClientSecretResolutionStatus.RESOLVED, result.status)
        assertEquals(checkedAt, result.checkedAt)
        verify(registry, never()).invalidate("vault:auth/google")
    }

    @Test
    fun refreshInvalidatesBeforeLiveVerification() {
        `when`(providerDao.get("provider-1")).thenReturn(provider("property:kudos.ms.auth.external-secrets.google"))
        `when`(registry.verify("property:kudos.ms.auth.external-secrets.google")).thenReturn(
            ClientSecretReferenceCheck("property", ClientSecretResolutionStatus.NOT_FOUND)
        )

        val result = service.verify(command().copy(refresh = true))

        verify(registry).invalidate("property:kudos.ms.auth.external-secrets.google")
        verify(registry).verify("property:kudos.ms.auth.external-secrets.google")
        assertEquals(ClientSecretResolutionStatus.NOT_FOUND, result.status)
    }

    @Test
    fun missingSecretIsReportedWithoutProbingAnArbitraryReference() {
        `when`(providerDao.get("provider-1")).thenReturn(provider(null))
        `when`(registry.verify(null)).thenReturn(
            ClientSecretReferenceCheck(null, ClientSecretResolutionStatus.NOT_CONFIGURED)
        )

        assertEquals(ClientSecretResolutionStatus.NOT_CONFIGURED, service.verify(command()).status)
        verify(registry).verify(null)
    }

    @Test
    fun crossTenantProviderIsRejectedBeforeResolverAccess() {
        `when`(providerDao.get("provider-1")).thenReturn(provider("vault:auth/google"))

        val error = assertFailsWith<IdentityProviderSecretVerificationException> {
            service.verify(command().copy(tenantId = "tenant-2"))
        }

        assertEquals("EXTERNAL_PROVIDER_TENANT_MISMATCH", error.errorCode)
        verify(registry, never()).verify("vault:auth/google")
    }

    private fun command() = IdentityProviderSecretVerificationCommand(
        providerId = "provider-1",
        tenantId = "tenant-1",
        actorUserId = "admin-1",
        operationReason = "Check rotated credential",
    )

    private fun provider(secretRef: String?) = AuthIdentityProvider {
        id = "provider-1"
        tenantId = "tenant-1"
        clientSecretRef = secretRef
    }
}
