package io.kudos.ms.auth.common.authentication.enums

/** Lifecycle status of a recoverable authentication transaction. */
enum class AuthenticationTransactionStatusEnum {
    PENDING,
    WAITING_FOR_ACTION,
    WAITING_FOR_EXTERNAL_PROVIDER,
    CHALLENGE_REQUIRED,
    ACCOUNT_LINK_REQUIRED,
    PROFILE_REQUIRED,
    COMPLETED,
    FAILED,
    EXPIRED,
    CANCELLED,
}
