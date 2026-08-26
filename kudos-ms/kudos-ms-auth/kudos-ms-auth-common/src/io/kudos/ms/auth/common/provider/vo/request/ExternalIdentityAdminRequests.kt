package io.kudos.ms.auth.common.provider.vo.request

/** Administrator request to pre-provision a verified external subject for an existing local account. */
data class ExternalIdentityAdminPrebindRequest(
    val userId: String,
    val providerId: String,
    val subject: String,
    val issuer: String? = null,
    val unionId: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val reason: String,
)

/** Administrator request to disable an external identity binding. */
data class ExternalIdentityAdminUnbindRequest(
    val bindingId: String,
    val reason: String,
)
