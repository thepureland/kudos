package io.kudos.ms.sys.common.param.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Parameter error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysParamErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Undefined error */
    UNSPECIFIED("UNSPECIFIED", "Undefined error"),

    /** Lookup of parameter by primary key or (module, name) failed */
    PARAM_NOT_FOUND("PARAM_NOT_FOUND", "Parameter does not exist"),

    /** A parameter with the same (module, name) already exists */
    PARAM_ALREADY_EXISTS("PARAM_ALREADY_EXISTS", "A parameter with the same name already exists in this module");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.param"

}
