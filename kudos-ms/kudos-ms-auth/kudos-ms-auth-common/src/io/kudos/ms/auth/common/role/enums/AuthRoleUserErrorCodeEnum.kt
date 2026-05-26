package io.kudos.ms.auth.common.role.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Role-user relationship error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class AuthRoleUserErrorCodeEnum(
    /** Error code. */
    override val code: String,
    /** Default display text. */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error. */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Lookup of the role-user relationship by (role_id, user_id) failed. */
    ROLE_USER_NOT_FOUND("ROLE_USER_NOT_FOUND", "Role-user relationship does not exist"),

    /** A binding for (role_id, user_id) already exists. */
    ROLE_USER_ALREADY_EXISTS("ROLE_USER_ALREADY_EXISTS", "The user already holds this role");

    override val i18nKeyPrefix: String
        get() = "auth.error-msg.roleuser"

}
