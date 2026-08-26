package io.kudos.ms.auth.common.provider.vo

import io.kudos.ms.auth.common.provider.enums.ExternalProtocolEnum
import java.io.Serializable

/** Provider-neutral external identity; SDK-specific principal types must be converted to this model. */
data class ExternalPrincipal(
    val providerId: String,
    val protocol: ExternalProtocolEnum,
    val issuer: String?,
    val subject: String,
    val unionId: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val emailVerified: Boolean? = null,
    val phone: String? = null,
    val phoneVerified: Boolean? = null,
    val avatarUrl: String? = null,
    val locale: String? = null,
    val rawClaims: Map<String, Any?> = emptyMap(),
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
