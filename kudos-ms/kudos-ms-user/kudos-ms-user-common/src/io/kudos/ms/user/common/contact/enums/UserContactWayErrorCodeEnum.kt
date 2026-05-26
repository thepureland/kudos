package io.kudos.ms.user.common.contact.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Contact way error codes
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserContactWayErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Lookup contact way by primary key failed */
    CONTACT_WAY_NOT_FOUND("CONTACT_WAY_NOT_FOUND", "Contact way not found"),

    /** (user_id, contact_way_dict_code, contact_way_value) already exists */
    CONTACT_WAY_ALREADY_EXISTS("CONTACT_WAY_ALREADY_EXISTS", "Contact way is already bound");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.contact"

}
