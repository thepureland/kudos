package io.kudos.ms.msg.common.receiver.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Receiver group error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class MsgReceiverGroupErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Lookup of receiver group by primary key failed */
    GROUP_NOT_FOUND("GROUP_NOT_FOUND", "Receiver group does not exist"),

    /** Receiver group has active=false and cannot be used for dispatch */
    GROUP_INACTIVE("GROUP_INACTIVE", "Receiver group is disabled"),

    /** receiver_group_type dict code is not one of the 11 supported types (department / role / tag / online, etc.) */
    INVALID_GROUP_TYPE("INVALID_GROUP_TYPE", "Unsupported receiver group type");

    override val i18nKeyPrefix: String
        get() = "msg.error-msg.receivergroup"

}
