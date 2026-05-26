package io.kudos.ms.sys.common.tenant.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Tenant error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysTenantErrorCodeEnum(
    /** Error code. */
    override val code: String,
    /** Default display text. */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error. */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Tenant lookup by primary key failed. */
    TENANT_NOT_FOUND("TENANT_NOT_FOUND", "Tenant does not exist"),

    /** Tenant name is already taken (corresponds to the business uniqueness of sys_tenant.name). */
    TENANT_NAME_ALREADY_EXISTS("TENANT_NAME_ALREADY_EXISTS", "Tenant name already exists");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.tenant"

}
