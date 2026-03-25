package io.kudos.ms.sys.common.enums.cache

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 缓存模块错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysCacheErrorCodeEnum(
    override val code: String,
    override val defaultDisplayText: String
) : IErrorCodeEnum {

    /** 缓存键不存在 */
    CACHE_KEY_NOT_FOUND("SC00000001", "缓存键不存在"),

    /** 缓存配置不存在 */
    CACHE_CONFIG_NOT_FOUND("SC00000002", "缓存配置不存在");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.cache"

}
