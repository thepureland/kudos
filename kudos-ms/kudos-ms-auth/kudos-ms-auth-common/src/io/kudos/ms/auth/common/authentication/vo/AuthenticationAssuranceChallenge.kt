package io.kudos.ms.auth.common.authentication.vo

/** Machine-readable challenge returned when an operation requires stronger or newer authentication. */
data class AuthenticationAssuranceChallenge(
    val success: Boolean = false,
    val code: String = ERROR_CODE,
    val message: String = "Additional authentication is required.",
    val reason: String,
    val requiredAcr: String,
    val maxAgeSeconds: Long? = null,
    val stepUpEndpoint: String = STEP_UP_ENDPOINT,
) {
    companion object {
        const val ERROR_CODE = "AUTHENTICATION_ASSURANCE_REQUIRED"
        const val STEP_UP_ENDPOINT = "/api/public/auth/authentication/transactions/step-up"
    }
}
