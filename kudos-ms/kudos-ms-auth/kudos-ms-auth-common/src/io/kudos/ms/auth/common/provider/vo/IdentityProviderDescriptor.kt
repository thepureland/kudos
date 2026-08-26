package io.kudos.ms.auth.common.provider.vo

import io.kudos.ms.auth.common.provider.enums.ExternalProtocolEnum
import java.io.Serializable

/** Secret-free provider information safe for login-page discovery. */
data class IdentityProviderDescriptor(
    val id: String,
    val code: String,
    val displayName: String,
    val protocol: ExternalProtocolEnum,
    val logoUri: String? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
