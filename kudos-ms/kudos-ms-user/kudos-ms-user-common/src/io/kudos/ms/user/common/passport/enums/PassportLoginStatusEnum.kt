package io.kudos.ms.user.common.passport.enums

/**
 * Result status of a login attempt.
 *
 * Decoupled from HTTP status codes: all failures also return HTTP 200, with this enum distinguishing the reason, allowing the frontend to give differentiated prompts.
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

    /** Account is locked (error count exceeded) — currently not further differentiated; reserved enum value */
    LOCKED,

    /**
     * Password is correct but the user has OTP enabled, and the request did not carry [PassportLoginRequest.authCode].
     * After receiving this status, the frontend should display the OTP input UI and resubmit with authCode.
     */
    OTP_REQUIRED,

    /** Password is correct but the OTP code is wrong (treated as the same failure as wrong password; error count is already +1) */
    OTP_WRONG,

    /**
     * Account is frozen (freeze_type is non-null and the current time falls within [freeze_start_time, freeze_end_time)).
     * [PassportLoginResult.message] carries the freeze reason title.
     */
    ACCOUNT_FROZEN,
}

