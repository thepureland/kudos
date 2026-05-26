package io.kudos.ms.sys.common.i18n.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Internationalization error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysI18NErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Failed to find i18n entry by primary key or (locale, type, namespace, atomicServiceCode, key) */
    I18N_NOT_FOUND("I18N_NOT_FOUND", "I18n entry does not exist"),

    /** An entry already exists for (locale, i18n_type_dict_code, namespace, atomic_service_code, key) */
    I18N_ALREADY_EXISTS("I18N_ALREADY_EXISTS", "An i18n entry with the same key already exists under this locale and namespace");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.i18n"

}
