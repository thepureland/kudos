package io.kudos.ms.sys.common.cache.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Cache module error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysCacheErrorCodeEnum(
    override val code: String,
    override val defaultDisplayText: String
) : IErrorCodeEnum {

    /** Cache key not found */
    CACHE_KEY_NOT_FOUND("SC00000001", "Cache key not found"),

    /** Cache configuration not found */
    CACHE_CONFIG_NOT_FOUND("SC00000002", "Cache configuration not found");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.cache"

}
