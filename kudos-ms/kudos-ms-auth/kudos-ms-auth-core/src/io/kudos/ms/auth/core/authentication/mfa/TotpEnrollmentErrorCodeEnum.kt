package io.kudos.ms.auth.core.authentication.mfa

enum class TotpEnrollmentErrorCodeEnum {
    ACCOUNT_NOT_FOUND,
    TOTP_ALREADY_ENABLED,
    TOTP_ENROLLMENT_NOT_FOUND,
    TOTP_ENROLLMENT_EXPIRED,
    INVALID_TOTP_CODE,
    TOTP_ATTEMPTS_EXCEEDED,
    TOTP_ACTIVATION_FAILED,
}
