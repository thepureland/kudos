package io.kudos.ms.auth.provider.emailotp.identity

/** Active local subject resolved or provisioned only after ownership of [email] was verified. */
data class EmailOtpPrincipal(
    val userId: String,
    val tenantId: String,
    val username: String,
)

/** Deployment policy for mapping a verified email to Kudos user master data. */
fun interface IEmailOtpPrincipalService {
    fun resolveOrProvision(tenantId: String, email: String): EmailOtpPrincipal
}
