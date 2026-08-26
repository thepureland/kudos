package io.kudos.ms.auth.provider.oauth2.secret

import java.time.LocalDateTime

data class IdentityProviderSecretVerificationCommand(
    val providerId: String,
    val tenantId: String,
    val actorUserId: String,
    val operationReason: String,
    val refresh: Boolean = false,
)

data class IdentityProviderSecretVerification(
    val providerId: String,
    val referenceScheme: String?,
    val status: ClientSecretResolutionStatus,
    val checkedAt: LocalDateTime,
)

class IdentityProviderSecretVerificationException(
    val errorCode: String,
    cause: Throwable? = null,
) : RuntimeException(errorCode, cause)
