package io.kudos.ms.auth.common.authentication.enums

/** A client action accepted by an authentication transaction. */
enum class AuthenticationActionEnum {
    IDENTIFY_ACCOUNT,
    SELECT_TENANT,
    SELECT_METHOD,
    VERIFY_PASSWORD,
    VERIFY_TOTP,
    VERIFY_RECOVERY_CODE,
    VERIFY_SMS,
    VERIFY_EMAIL,
    VERIFY_PASSKEY,
    REDIRECT_EXTERNAL_PROVIDER,
    LINK_ACCOUNT,
    ENROLL_MFA,
    CHANGE_EXPIRED_PASSWORD,
    ACCEPT_TERMS,
    COMPLETE,
}
