package io.kudos.ms.auth.core.provider.management.model

data class IdentityProviderCreateCommand(
    val tenantId: String,
    val templateId: String,
    val code: String,
    val displayName: String,
    val issuer: String?,
    val clientId: String,
    val clientSecretRef: String?,
    val scopes: List<String>,
    val jitPolicy: String,
    val linkPolicy: String,
    val active: Boolean,
    val actorUserId: String,
    val operationReason: String,
)

data class IdentityProviderUpdateCommand(
    val providerId: String,
    val tenantId: String,
    val displayName: String,
    val issuer: String?,
    val clientId: String,
    val clientSecretRef: String?,
    val clearClientSecretRef: Boolean,
    val scopes: List<String>,
    val jitPolicy: String,
    val linkPolicy: String,
    val actorUserId: String,
    val operationReason: String,
)

data class IdentityProviderSetActiveCommand(
    val providerId: String,
    val tenantId: String,
    val active: Boolean,
    val actorUserId: String,
    val operationReason: String,
)

data class ManagedIdentityProvider(
    val id: String,
    val templateId: String,
    val templateCode: String,
    val protocol: String,
    val code: String,
    val displayName: String,
    val issuer: String?,
    val clientId: String,
    val clientSecretConfigured: Boolean,
    val scopes: List<String>,
    val effectiveScopes: List<String>,
    val jitPolicy: String,
    val linkPolicy: String,
    val active: Boolean,
)

data class ManagedProviderTemplate(
    val id: String,
    val code: String,
    val protocol: String,
    val issuer: String?,
    val defaultScopes: List<String>,
    val logoUri: String?,
)

class IdentityProviderManagementException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalArgumentException(errorCode, cause)
