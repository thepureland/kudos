package io.kudos.ms.sys.common.locale.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 语言/区域字典错误码
 *
 * @author K
 * @since 1.0.0
 */
enum class SysLocaleErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键或code查找失败 */
    LOCALE_NOT_FOUND("LOCALE_NOT_FOUND", "语言代码不存在"),

    /** code 已存在 */
    LOCALE_ALREADY_EXISTS("LOCALE_ALREADY_EXISTS", "该语言代码已存在");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.locale"

}
