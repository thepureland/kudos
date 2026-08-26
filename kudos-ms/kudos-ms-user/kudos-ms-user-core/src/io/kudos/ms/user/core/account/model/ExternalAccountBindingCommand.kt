package io.kudos.ms.user.core.account.model

/** Verified external identity to bind to an existing local account. */
data class ExternalAccountBindingCommand(
    val userId: String,
    val tenantId: String,
    val identityProviderId: String,
    val providerCode: String,
    val issuer: String?,
    val subject: String,
    val unionId: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
)
