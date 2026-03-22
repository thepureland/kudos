package io.kudos.base.enums.ienums

/**
 * 错误代码枚举
 *
 * @author K
 * @since 1.0.0
 */
interface IErrorCodeEnum : IDictEnum {

    /** 原始错误描述或国际化key的后缀部分 */
    val rawTrans: String

    /** 错误描述 */
    override val trans: String
        get() = if (i18nKeyPrefix.isBlank()) {
            rawTrans
        } else {
            "$i18nKeyPrefix.$code"
        }

    /** 是否打印完整堆栈 */
    val printAllStackTrace: Boolean

    /** 国际化key的前缀 */
    val i18nKeyPrefix: String

}
