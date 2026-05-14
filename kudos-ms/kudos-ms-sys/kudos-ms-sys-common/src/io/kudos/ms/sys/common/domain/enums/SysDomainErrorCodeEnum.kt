package io.kudos.ms.sys.common.domain.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 域名错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysDomainErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键或域名查找失败 */
    DOMAIN_NOT_FOUND("DOMAIN_NOT_FOUND", "域名不存在"),

    /** 域名已被占用 */
    DOMAIN_ALREADY_EXISTS("DOMAIN_ALREADY_EXISTS", "该域名已存在");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.domain"

}
