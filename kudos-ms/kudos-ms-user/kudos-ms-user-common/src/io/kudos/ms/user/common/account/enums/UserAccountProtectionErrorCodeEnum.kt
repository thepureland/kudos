package io.kudos.ms.user.common.account.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Account protection error codes
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserAccountProtectionErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Lookup account protection record by user_id failed */
    PROTECTION_NOT_FOUND("PROTECTION_NOT_FOUND", "Account protection record not found"),

    /** Account is locked */
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "Account is locked"),

    /** Password error count exceeded threshold */
    PASSWORD_RETRY_EXCEEDED("PASSWORD_RETRY_EXCEEDED", "Password error count exceeded threshold");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.protection"

}
