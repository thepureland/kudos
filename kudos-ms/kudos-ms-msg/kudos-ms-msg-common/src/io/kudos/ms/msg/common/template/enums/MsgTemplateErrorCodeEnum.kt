package io.kudos.ms.msg.common.template.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Message template error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class MsgTemplateErrorCodeEnum(
    /** Error code. */
    override val code: String,
    /** Default display text. */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error. */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Lookup of template by primary key or by (tenant_id, event_type, msg_type, locale) failed. */
    TEMPLATE_NOT_FOUND("TEMPLATE_NOT_FOUND", "Message template not found"),

    /** (tenant_id, event_type, msg_type, locale) is already taken. */
    DUPLICATE_TEMPLATE("DUPLICATE_TEMPLATE", "A template with the same event / message type / locale already exists for this tenant");

    override val i18nKeyPrefix: String
        get() = "msg.error-msg.template"

}
