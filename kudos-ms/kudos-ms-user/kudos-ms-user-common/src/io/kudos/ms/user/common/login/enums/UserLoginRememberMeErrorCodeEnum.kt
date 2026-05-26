package io.kudos.ms.user.common.login.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Remember-me login error codes
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserLoginRememberMeErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Undefined error */
    UNSPECIFIED("UNSPECIFIED", "Undefined error"),

    /** Lookup by primary key or (tenant_id, username) failed */
    REMEMBER_ME_NOT_FOUND("REMEMBER_ME_NOT_FOUND", "Remember-me record does not exist"),

    /** The provided remember-me token is expired or revoked */
    REMEMBER_ME_TOKEN_INVALID("REMEMBER_ME_TOKEN_INVALID", "Remember-me token is invalid");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.loginremember"

}
