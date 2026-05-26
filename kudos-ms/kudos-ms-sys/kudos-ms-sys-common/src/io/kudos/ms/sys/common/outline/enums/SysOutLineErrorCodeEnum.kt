package io.kudos.ms.sys.common.outline.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Outbound whitelist error codes.
 *
 * @author K
 * @since 1.0.0
 */
enum class SysOutLineErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Undefined error */
    UNSPECIFIED("UNSPECIFIED", "Undefined error"),

    /** Lookup by primary key failed */
    OUT_LINE_NOT_FOUND("OUT_LINE_NOT_FOUND", "Outbound whitelist entry does not exist"),

    /** Same (system_code, tenant_id, host, port, protocol) already exists */
    OUT_LINE_ALREADY_EXISTS("OUT_LINE_ALREADY_EXISTS", "Outbound whitelist entry already exists");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.outline"

}
