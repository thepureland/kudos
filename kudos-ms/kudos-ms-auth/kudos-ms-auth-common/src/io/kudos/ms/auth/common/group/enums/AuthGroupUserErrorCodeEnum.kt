package io.kudos.ms.auth.common.group.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Group-user relationship error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class AuthGroupUserErrorCodeEnum(
    /** Error code. */
    override val code: String,
    /** Default display text. */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error. */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Lookup of the group-user relationship by (group_id, user_id) failed. */
    GROUP_USER_NOT_FOUND("GROUP_USER_NOT_FOUND", "Group-user relationship does not exist"),

    /** A binding for (group_id, user_id) already exists. */
    GROUP_USER_ALREADY_EXISTS("GROUP_USER_ALREADY_EXISTS", "The user has already joined this group");

    override val i18nKeyPrefix: String
        get() = "auth.error-msg.groupuser"

}
