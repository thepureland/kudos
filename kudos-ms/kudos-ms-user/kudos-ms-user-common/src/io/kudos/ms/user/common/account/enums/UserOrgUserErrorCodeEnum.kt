package io.kudos.ms.user.common.account.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Organization-user relationship error codes
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UserOrgUserErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Failed to find organization-user relationship by (user_id, org_id) */
    ORG_USER_NOT_FOUND("ORG_USER_NOT_FOUND", "Organization-user relationship not found"),

    /** (user_id, org_id) binding already exists */
    ORG_USER_ALREADY_EXISTS("ORG_USER_ALREADY_EXISTS", "This user is already bound to this organization");

    override val i18nKeyPrefix: String
        get() = "user.error-msg.orguser"

}
