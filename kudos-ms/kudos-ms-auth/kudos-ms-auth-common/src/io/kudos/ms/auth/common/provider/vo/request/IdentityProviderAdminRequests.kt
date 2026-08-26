package io.kudos.ms.auth.common.provider.vo.request

data class IdentityProviderAdminCreateRequest(
    val templateId: String,
    val code: String,
    val displayName: String,
    val issuer: String? = null,
    val clientId: String,
    /** Secret-store reference such as env:KUDOS_AUTH_NAME or vault:path; never a secret value. */
    val clientSecretRef: String? = null,
    val scopes: List<String> = emptyList(),
    val jitPolicy: String = "DISABLED",
    val linkPolicy: String = "BOUND_ONLY",
    val active: Boolean = false,
    val reason: String,
)

data class IdentityProviderAdminUpdateRequest(
    val providerId: String,
    val displayName: String,
    val issuer: String? = null,
    val clientId: String,
    /** Null preserves the existing reference; use clearClientSecretRef for a public client. */
    val clientSecretRef: String? = null,
    val clearClientSecretRef: Boolean = false,
    val scopes: List<String> = emptyList(),
    val jitPolicy: String,
    val linkPolicy: String,
    val reason: String,
)

data class IdentityProviderAdminSetActiveRequest(
    val providerId: String,
    val active: Boolean,
    val reason: String,
)

/** Provider-scoped operation; the stored reference is never accepted from or returned to this request. */
data class IdentityProviderSecretAdminRequest(
    val providerId: String,
    val reason: String,
)

/** Ordered claim paths; the first non-null value wins. Dot-separated paths may address nested maps. */
data class IdentityProviderClaimMappingAdminSaveRequest(
    val providerId: String,
    val subjectClaims: List<String>,
    val usernameClaims: List<String> = emptyList(),
    val displayNameClaims: List<String> = emptyList(),
    val emailClaims: List<String> = emptyList(),
    val emailVerifiedClaims: List<String> = emptyList(),
    val phoneClaims: List<String> = emptyList(),
    val phoneVerifiedClaims: List<String> = emptyList(),
    val avatarClaims: List<String> = emptyList(),
    val localeClaims: List<String> = emptyList(),
    val unionIdClaims: List<String> = emptyList(),
    val reason: String,
)
