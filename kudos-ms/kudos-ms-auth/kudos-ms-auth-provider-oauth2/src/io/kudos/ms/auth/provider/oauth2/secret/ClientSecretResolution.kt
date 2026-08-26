package io.kudos.ms.auth.provider.oauth2.secret

import java.time.LocalDateTime

enum class ClientSecretResolutionStatus {
    NOT_CONFIGURED,
    RESOLVED,
    INVALID_REFERENCE,
    UNSUPPORTED_SCHEME,
    AMBIGUOUS_RESOLVER,
    POLICY_DENIED,
    NOT_FOUND,
    RESOLVER_ERROR,
}

data class ClientSecretReferenceCheck(
    val scheme: String?,
    val status: ClientSecretResolutionStatus,
    val checkedAt: LocalDateTime = LocalDateTime.now(),
)

class ClientSecretResolutionException(
    val status: ClientSecretResolutionStatus,
    cause: Throwable? = null,
) : IllegalStateException(status.name, cause)
