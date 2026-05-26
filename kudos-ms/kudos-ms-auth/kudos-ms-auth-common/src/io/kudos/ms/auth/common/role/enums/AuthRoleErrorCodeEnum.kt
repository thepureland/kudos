package io.kudos.ms.auth.common.role.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Role error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class AuthRoleErrorCodeEnum(
    /** Error code. */
    override val code: String,
    /** Default display text. */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error. */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Lookup of the role by primary key or (tenant_id, code) failed. */
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Role does not exist"),

    /** (tenant_id, code) is already taken. */
    ROLE_CODE_ALREADY_EXISTS("ROLE_CODE_ALREADY_EXISTS", "Role code already exists under this tenant");

    override val i18nKeyPrefix: String
        get() = "auth.error-msg.role"

}
