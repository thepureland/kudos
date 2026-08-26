package io.kudos.ms.user.common.passport.enums

/**
 * Result status of a login attempt.
 *
 * Decoupled from HTTP status codes. The core Passport service retains detailed outcomes for
 * audit and trusted internal callers; public authentication boundaries must collapse account-
 * revealing outcomes to [INVALID_CREDENTIALS].
 *
 * @author K
 * @since 1.0.0
 */
enum class PassportLoginStatusEnum {

    /** Login succeeded */
    SUCCESS,

    /** Username/tenant not found or has been deleted */
    USER_NOT_FOUND,

    /** Wrong password ([PassportLoginResult.loginErrorTimes] carries the cumulative error count) */
    WRONG_PASSWORD,

    /** Account is disabled (active=false) */
    INACTIVE,

    /**
     * Account is locked: consecutive login failures reached the configured threshold
     * (kudos.ms.user.passport.login-lock.max-error-times, default 5). The server arms a temporary
     * freeze (kudos.ms.user.passport.login-lock.lock-minutes, default 30) and rejects all attempts —
     * including ones with the correct password — until the window expires or an administrator unfreezes.
     */
    LOCKED,

    /**
     * Password is correct but the user has OTP enabled, and the request did not carry [PassportLoginRequest.authCode].
     * After receiving this status, the frontend should display the OTP input UI and resubmit with authCode.
     */
    OTP_REQUIRED,

    /** Password is correct but the OTP code is wrong; TOTP has an independent failure window. */
    OTP_WRONG,

    /** Password is correct but the single-use recovery code is invalid or already consumed. */
    RECOVERY_CODE_WRONG,

    /**
     * The request or credential-failure window has reached its configured limit. This is a
     * temporary traffic decision and must not be presented as an account freeze.
     */
    RATE_LIMITED,

    /**
     * Account is frozen (freeze_type is non-null and the current time falls within [freeze_start_time, freeze_end_time)).
     * [PassportLoginResult.message] carries the freeze reason title.
     */
    ACCOUNT_FROZEN,

    /**
     * Public, non-enumerating result for an invalid username/password pair or an account that
     * cannot authenticate. It intentionally carries no account state or failure counter.
     */
    INVALID_CREDENTIALS,
}

