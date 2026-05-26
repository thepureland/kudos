package io.kudos.ms.sys.common.microservice.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Microservice error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysMicroServiceErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Undefined error */
    UNSPECIFIED("UNSPECIFIED", "Undefined error"),

    /** Lookup of microservice by primary key failed (note: microservice PK is the code). */
    MICRO_SERVICE_NOT_FOUND("MICRO_SERVICE_NOT_FOUND", "Microservice does not exist"),

    /** Microservice code is already in use */
    MICRO_SERVICE_CODE_ALREADY_EXISTS("MICRO_SERVICE_CODE_ALREADY_EXISTS", "Microservice code already exists");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.microservice"

}
