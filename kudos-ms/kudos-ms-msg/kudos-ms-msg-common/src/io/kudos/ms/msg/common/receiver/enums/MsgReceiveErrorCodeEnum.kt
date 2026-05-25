package io.kudos.ms.msg.common.receiver.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Message receive error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class MsgReceiveErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Lookup of receive record by primary key failed */
    RECEIVE_NOT_FOUND("RECEIVE_NOT_FOUND", "Message receive record does not exist"),

    /** Current user does not match receiverId; not authorized to operate on this record */
    RECEIVE_NOT_OWNED("RECEIVE_NOT_OWNED", "Not authorized to operate on this message"),

    /** On markRead the record is already in READ / DELETED status (idempotent case, treat as warn rather than error) */
    ALREADY_READ("ALREADY_READ", "Message has already been read");

    override val i18nKeyPrefix: String
        get() = "msg.error-msg.receive"

}
