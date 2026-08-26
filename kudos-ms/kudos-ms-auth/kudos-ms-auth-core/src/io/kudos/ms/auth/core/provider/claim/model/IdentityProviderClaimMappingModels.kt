package io.kudos.ms.auth.core.provider.claim.model

data class IdentityProviderClaimMappingSaveCommand(
    val providerId: String,
    val tenantId: String,
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
    val actorUserId: String,
    val operationReason: String,
)

data class EffectiveIdentityProviderClaimMapping(
    val providerId: String,
    val subjectClaims: List<String>,
    val usernameClaims: List<String> = listOf("preferred_username", "login"),
    val displayNameClaims: List<String> = listOf("name"),
    val emailClaims: List<String> = listOf("email"),
    val emailVerifiedClaims: List<String> = listOf("email_verified"),
    val phoneClaims: List<String> = listOf("phone_number"),
    val phoneVerifiedClaims: List<String> = listOf("phone_number_verified"),
    val avatarClaims: List<String> = listOf("picture", "avatar_url"),
    val localeClaims: List<String> = listOf("locale"),
    val unionIdClaims: List<String> = listOf("union_id"),
    val configured: Boolean = false,
)

class IdentityProviderClaimMappingException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalArgumentException(errorCode, cause)
