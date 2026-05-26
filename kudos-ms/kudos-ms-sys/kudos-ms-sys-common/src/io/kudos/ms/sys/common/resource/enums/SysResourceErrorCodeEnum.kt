package io.kudos.ms.sys.common.resource.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Resource module error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysResourceErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Resource not found by primary key */
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Resource not found"),

    /** A resource with the same (sub_system_code, url) already exists */
    RESOURCE_URL_ALREADY_EXISTS("RESOURCE_URL_ALREADY_EXISTS", "A resource with the same URL already exists under this sub-system"),

    /** Parent resource not found or disabled; cannot mount child resources */
    PARENT_RESOURCE_NOT_FOUND("PARENT_RESOURCE_NOT_FOUND", "Parent resource not found or disabled");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.resource"

}
