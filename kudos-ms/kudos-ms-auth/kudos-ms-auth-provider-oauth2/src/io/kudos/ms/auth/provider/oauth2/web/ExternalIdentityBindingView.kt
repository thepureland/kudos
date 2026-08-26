package io.kudos.ms.auth.provider.oauth2.web

import java.time.LocalDateTime

/** Secret-free binding view returned to the current account owner. */
data class ExternalIdentityBindingView(
    val bindingId: String,
    val providerId: String?,
    val providerCode: String,
    val providerDisplayName: String,
    val externalDisplayName: String?,
    val avatarUrl: String?,
    val lastLoginTime: LocalDateTime?,
)
