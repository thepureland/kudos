package io.kudos.ms.auth.common.provider.vo.response

import java.time.LocalDateTime

data class IdentityProviderTemplateAdminResponse(
    val id: String,
    val code: String,
    val protocol: String,
    val issuer: String?,
    val defaultScopes: List<String>,
    val logoUri: String?,
)

data class IdentityProviderAdminResponse(
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

data class IdentityProviderClaimMappingAdminResponse(
    val providerId: String,
    val subjectClaims: List<String>,
    val usernameClaims: List<String>,
    val displayNameClaims: List<String>,
    val emailClaims: List<String>,
    val emailVerifiedClaims: List<String>,
    val phoneClaims: List<String>,
    val phoneVerifiedClaims: List<String>,
    val avatarClaims: List<String>,
    val localeClaims: List<String>,
    val unionIdClaims: List<String>,
    val configured: Boolean,
)

/** Safe diagnostics: deliberately excludes reference location, value, length, digest, and resolver errors. */
data class IdentityProviderSecretAdminResponse(
    val providerId: String,
    val referenceScheme: String?,
    val status: String,
    val checkedAt: LocalDateTime,
)
