package io.kudos.ms.user.common.account.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Third-party account error codes
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserAccountThirdErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Failed to find third-party binding by primary key or (provider, openid) */
    THIRD_ACCOUNT_NOT_FOUND("THIRD_ACCOUNT_NOT_FOUND", "Third-party account binding not found"),

    /** (provider, openid) already has a binding */
    THIRD_ACCOUNT_ALREADY_BOUND("THIRD_ACCOUNT_ALREADY_BOUND", "This third-party account is already bound");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.user-third"

}
