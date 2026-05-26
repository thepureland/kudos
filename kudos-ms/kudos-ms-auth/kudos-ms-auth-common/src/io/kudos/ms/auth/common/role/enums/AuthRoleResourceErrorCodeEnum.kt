package io.kudos.ms.auth.common.role.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Role-resource relationship error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class AuthRoleResourceErrorCodeEnum(
    /** Error code. */
    override val code: String,
    /** Default display text. */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error. */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Lookup of the role-resource relationship by (role_id, resource_id) failed. */
    ROLE_RESOURCE_NOT_FOUND("ROLE_RESOURCE_NOT_FOUND", "Role-resource relationship does not exist"),

    /** A binding for (role_id, resource_id) already exists. */
    ROLE_RESOURCE_ALREADY_EXISTS("ROLE_RESOURCE_ALREADY_EXISTS", "The role already holds this resource");

    override val i18nKeyPrefix: String
        get() = "auth.error-msg.roleresource"

}
