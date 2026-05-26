package io.kudos.ms.user.common.login.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Login log error codes
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserLogLoginErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Undefined error */
    UNSPECIFIED("UNSPECIFIED", "Undefined error"),

    /** Lookup by primary key failed */
    LOG_LOGIN_NOT_FOUND("LOG_LOGIN_NOT_FOUND", "Login log does not exist");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.loglogin"

}
