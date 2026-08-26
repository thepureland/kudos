package io.kudos.ms.auth.provider.oauth2.secret

import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Verifies only the secret reference stored on a tenant-owned Provider. */
@Service
@Transactional(readOnly = true)
open class IdentityProviderSecretVerificationService(
    private val providerDao: AuthIdentityProviderDao,
    private val registry: ClientSecretResolverRegistry,
) {

    open fun verify(command: IdentityProviderSecretVerificationCommand): IdentityProviderSecretVerification {
        val providerId = required(command.providerId, 36, "EXTERNAL_PROVIDER_ID_REQUIRED")
        val tenantId = required(command.tenantId, 36, "EXTERNAL_PROVIDER_TENANT_REQUIRED")
        required(command.actorUserId, 36, "EXTERNAL_PROVIDER_ACTOR_REQUIRED")
        required(command.operationReason, 512, "EXTERNAL_PROVIDER_REASON_INVALID")

        val provider = providerDao.get(providerId)
            ?: fail("EXTERNAL_PROVIDER_NOT_AVAILABLE")
        if (provider.tenantId != tenantId) fail("EXTERNAL_PROVIDER_TENANT_MISMATCH")

        if (command.refresh) registry.invalidate(provider.clientSecretRef)
        val check = registry.verify(provider.clientSecretRef)
        return IdentityProviderSecretVerification(
            providerId = provider.id,
            referenceScheme = check.scheme,
            status = check.status,
            checkedAt = check.checkedAt,
        )
    }

    private fun required(value: String, maxLength: Int, errorCode: String): String {
        val normalized = value.trim()
        if (normalized.isBlank() || normalized.length > maxLength) fail(errorCode)
        return normalized
    }

    private fun fail(errorCode: String, cause: Throwable? = null): Nothing =
        throw IdentityProviderSecretVerificationException(errorCode, cause)
}
