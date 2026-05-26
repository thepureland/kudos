package io.kudos.ms.auth.common.group.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Group error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class AuthGroupErrorCodeEnum(
    /** Error code. */
    override val code: String,
    /** Default display text. */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error. */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Lookup of the user group by primary key or (tenant_id, code) failed. */
    GROUP_NOT_FOUND("GROUP_NOT_FOUND", "User group does not exist"),

    /** (tenant_id, code) is already taken. */
    GROUP_CODE_ALREADY_EXISTS("GROUP_CODE_ALREADY_EXISTS", "Group code already exists under this tenant");

    override val i18nKeyPrefix: String
        get() = "auth.error-msg.group"

}
