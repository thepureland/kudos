package io.kudos.ms.user.core.account.model

/** Administrator-controlled external identity pre-provisioning command. */
data class AdminExternalAccountBindingCommand(
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
    val actorUserId: String,
    val operationReason: String,
)
