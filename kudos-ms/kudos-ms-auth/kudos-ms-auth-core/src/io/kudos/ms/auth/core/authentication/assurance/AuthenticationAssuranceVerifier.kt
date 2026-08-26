package io.kudos.ms.auth.core.authentication.assurance

import io.kudos.ms.auth.common.authentication.annotation.RequiresAuthenticationAssurance
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.core.authentication.assurance.spi.IAuthenticationAssurancePolicy
import java.time.Clock

/** Evaluates ACR and optional auth_time freshness against an authoritative authentication session. */
open class AuthenticationAssuranceVerifier(
    private val policy: IAuthenticationAssurancePolicy,
    private val clock: Clock = Clock.systemUTC(),
) {

    open fun verify(session: AuthenticationSession?, requiredAcr: String, maxAgeSeconds: Long) {
        val normalizedAcr = requiredAcr.trim()
        val now = clock.instant()
        check(normalizedAcr.isNotEmpty()) { "Required authentication ACR must not be blank." }
        check(maxAgeSeconds >= RequiresAuthenticationAssurance.NO_MAX_AGE) {
            "Authentication maxAgeSeconds must be -1 or greater."
        }

        if (session == null || !session.isActive(now)) {
            challenge(AuthenticationAssuranceReasonEnum.AUTHENTICATION_REQUIRED, normalizedAcr, maxAgeSeconds)
        }
        if (!policy.isSatisfied(session.acr, normalizedAcr)) {
            challenge(AuthenticationAssuranceReasonEnum.INSUFFICIENT_ACR, normalizedAcr, maxAgeSeconds)
        }
        if (maxAgeSeconds != RequiresAuthenticationAssurance.NO_MAX_AGE &&
            session.authTime.plusSeconds(maxAgeSeconds).isBefore(now)
        ) {
            challenge(AuthenticationAssuranceReasonEnum.AUTHENTICATION_TOO_OLD, normalizedAcr, maxAgeSeconds)
        }
    }

    private fun challenge(
        reason: AuthenticationAssuranceReasonEnum,
        requiredAcr: String,
        maxAgeSeconds: Long,
    ): Nothing = throw AuthenticationAssuranceRequiredException(
        reason = reason,
        requiredAcr = requiredAcr,
        maxAgeSeconds = maxAgeSeconds.takeUnless { it == RequiresAuthenticationAssurance.NO_MAX_AGE },
    )
}
