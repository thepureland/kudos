package io.kudos.ms.auth.core.authentication.assurance

/** Why an authenticated operation could not satisfy its declared assurance requirement. */
enum class AuthenticationAssuranceReasonEnum {
    AUTHENTICATION_REQUIRED,
    INSUFFICIENT_ACR,
    AUTHENTICATION_TOO_OLD,
}
