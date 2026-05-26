package io.kudos.ms.sys.common.domain.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Domain module error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysDomainErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Domain not found by primary key or domain */
    DOMAIN_NOT_FOUND("DOMAIN_NOT_FOUND", "Domain not found"),

    /** Domain already taken */
    DOMAIN_ALREADY_EXISTS("DOMAIN_ALREADY_EXISTS", "Domain already exists");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.domain"

}
