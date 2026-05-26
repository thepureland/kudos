package io.kudos.base.enums.impl

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.enums.ienums.IModuleEnum

/**
 * Common error-code enumeration.
 *
 * @author K
 * @since 1.0.0
 */
enum class CommonErrorCodeEnum(
    /** Error code. */
    override val code: String,

    /** Default display text. */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Request succeeded. */
    SUCCESS("200", "Operation succeeded"),

    /** Bad request. */
    BAD_REQUEST("400", "The request is invalid; please check and try again"),

    /** Not logged in. */
    UNAUTHORIZED("401", "Please log in first"),

    /** No permission. */
    FORBIDDEN("403", "Sorry, you do not have permission to perform this operation"),

    /** Resource not found. */
    NOT_FOUND("404", "Sorry, the content you requested does not exist"),

    /** HTTP method not supported. */
    METHOD_NOT_ALLOWED("405", "The current request method is not supported; please try a different method"),

    /** Parameter validation failed. */
    VALIDATION_ERROR("4001", "The information you provided is invalid; please check and try again"),

    /** Business processing failed. */
    BUSINESS_ERROR("4002", "Operation incomplete; please try again later"),

    /** Built-in record cannot be deleted. */
    BUILTIN_NOT_DELETABLE("4003", "Built-in records cannot be deleted"),

    /** System exception. */
    SYSTEM_ERROR("500", "The system is taking a break; please try again later");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.default"

}
