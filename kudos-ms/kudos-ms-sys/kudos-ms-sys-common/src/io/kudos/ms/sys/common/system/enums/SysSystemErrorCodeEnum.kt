package io.kudos.ms.sys.common.system.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * System module error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysSystemErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** System not found by primary key (note: the system PK is code) */
    SYSTEM_NOT_FOUND("SYSTEM_NOT_FOUND", "System not found"),

    /** System code already taken */
    SYSTEM_CODE_ALREADY_EXISTS("SYSTEM_CODE_ALREADY_EXISTS", "System code already exists");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.system"

}
