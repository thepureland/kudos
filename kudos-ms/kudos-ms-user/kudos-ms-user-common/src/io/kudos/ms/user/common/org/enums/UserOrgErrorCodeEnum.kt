package io.kudos.ms.user.common.org.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Organization error codes
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserOrgErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Lookup organization by primary key failed */
    ORG_NOT_FOUND("ORG_NOT_FOUND", "Organization not found"),

    /** (tenant_id, code) is already taken */
    ORG_CODE_ALREADY_EXISTS("ORG_CODE_ALREADY_EXISTS", "Organization code already exists for this tenant"),

    /** Parent organization does not exist or is disabled */
    PARENT_ORG_NOT_FOUND("PARENT_ORG_NOT_FOUND", "Parent organization does not exist or is disabled");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.org"

}
