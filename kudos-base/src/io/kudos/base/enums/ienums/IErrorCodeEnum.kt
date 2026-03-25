package io.kudos.base.enums.ienums

/**
 * 错误代码枚举
 *
 * @author K
 * @since 1.0.0
 */
interface IErrorCodeEnum : IDictEnum {

    /** 默认展示文本 */
    val defaultDisplayText: String

    /** 展示文本或国际化key */
    override val displayText: String
        get() = if (i18nKeyPrefix.isBlank()) {
            defaultDisplayText
        } else {
            "$i18nKeyPrefix.$code"
        }

    /** 国际化key的前缀 */
    val i18nKeyPrefix: String

}
