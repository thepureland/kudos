package io.kudos.ms.user.core.passport.security

/** User-domain port for consuming an Auth-owned, single-use MFA recovery code. */
fun interface IRecoveryCodeVerifier {
    fun consumeRecoveryCode(tenantId: String, userId: String, rawCode: String): Boolean
}

object NoopRecoveryCodeVerifier : IRecoveryCodeVerifier {
    override fun consumeRecoveryCode(tenantId: String, userId: String, rawCode: String): Boolean = false
}
