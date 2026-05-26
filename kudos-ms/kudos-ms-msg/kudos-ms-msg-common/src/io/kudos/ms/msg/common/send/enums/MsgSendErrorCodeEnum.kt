package io.kudos.ms.msg.common.send.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Message send error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class MsgSendErrorCodeEnum(
    /** Error code. */
    override val code: String,
    /** Default display text. */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error. */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** publish request receiverIds is empty. */
    RECEIVER_IDS_EMPTY("RECEIVER_IDS_EMPTY", "Recipient list is empty"),

    /** No available template matched by (tenant_id, event_type, msg_type, locale). */
    TEMPLATE_NOT_FOUND("TEMPLATE_NOT_FOUND", "No matching message template found"),

    /** Dispatch to notify producer failed (producer not wired or notify threw). */
    MQ_PUBLISH_FAILED("MQ_PUBLISH_FAILED", "Message dispatch failed");

    override val i18nKeyPrefix: String
        get() = "msg.error-msg.send"

}
