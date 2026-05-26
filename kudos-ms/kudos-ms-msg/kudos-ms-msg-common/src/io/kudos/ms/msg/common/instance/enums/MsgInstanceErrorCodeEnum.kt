package io.kudos.ms.msg.common.instance.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Message instance error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class MsgInstanceErrorCodeEnum(
    /** Error code. */
    override val code: String,
    /** Default display text. */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error. */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Lookup of message instance by primary key failed. */
    INSTANCE_NOT_FOUND("INSTANCE_NOT_FOUND", "Message instance not found"),

    /** Current time is outside the [validTimeStart, validTimeEnd] range. */
    INSTANCE_EXPIRED("INSTANCE_EXPIRED", "Message instance has expired");

    override val i18nKeyPrefix: String
        get() = "msg.error-msg.instance"

}
