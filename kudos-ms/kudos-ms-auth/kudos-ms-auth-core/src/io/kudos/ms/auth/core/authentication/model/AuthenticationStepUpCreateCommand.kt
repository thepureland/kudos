package io.kudos.ms.auth.core.authentication.model

/** Trusted server-side facts used to create a step-up authentication transaction. */
data class AuthenticationStepUpCreateCommand(
    val userId: String,
    val tenantId: String,
    val username: String?,
    val sourceSessionId: String,
    val requiredAcr: String,
    val requestedMethod: String,
)
