package io.kudos.ms.auth.core.authentication.assurance

/** Fail-closed result of evaluating a declared authentication-assurance requirement. */
class AuthenticationAssuranceRequiredException(
    val reason: AuthenticationAssuranceReasonEnum,
    val requiredAcr: String,
    val maxAgeSeconds: Long?,
) : RuntimeException("${reason.name}: requiredAcr=$requiredAcr, maxAgeSeconds=$maxAgeSeconds")
