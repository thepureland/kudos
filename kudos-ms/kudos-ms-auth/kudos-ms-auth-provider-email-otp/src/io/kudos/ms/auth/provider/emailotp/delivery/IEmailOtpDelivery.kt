package io.kudos.ms.auth.provider.emailotp.delivery

import java.time.Instant

/** Secret-bearing handoff to a deployment's mail transport; production implementations must never log [code]. */
data class EmailOtpDelivery(
    val transactionId: String,
    val tenantId: String,
    val email: String,
    val code: String,
    val expiresAt: Instant,
)

/**
 * Accepts an OTP for delivery or throws before returning.
 * Returning successfully means the transport has durably accepted the message, not necessarily delivered it.
 */
fun interface IEmailOtpDelivery {
    fun deliver(message: EmailOtpDelivery)
}
