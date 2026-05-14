package io.kudos.ms.sys.common.i18n.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 国际化错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysI18NErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键或 (locale, type, namespace, atomicServiceCode, key) 查找国际化条目失败 */
    I18N_NOT_FOUND("I18N_NOT_FOUND", "国际化条目不存在"),

    /** (locale, i18n_type_dict_code, namespace, atomic_service_code, key) 已存在条目 */
    I18N_ALREADY_EXISTS("I18N_ALREADY_EXISTS", "该 locale 与命名空间下已存在同 key 的国际化条目");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.i18n"

}
