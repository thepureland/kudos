package io.kudos.ms.sys.common.locale.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Error codes for language/locale dictionary.
 *
 * @author K
 * @since 1.0.0
 */
enum class SysLocaleErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Undefined error */
    UNSPECIFIED("UNSPECIFIED", "Undefined error"),

    /** Lookup by primary key or code failed */
    LOCALE_NOT_FOUND("LOCALE_NOT_FOUND", "Language code does not exist"),

    /** Code already exists */
    LOCALE_ALREADY_EXISTS("LOCALE_ALREADY_EXISTS", "Language code already exists");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.locale"

}
