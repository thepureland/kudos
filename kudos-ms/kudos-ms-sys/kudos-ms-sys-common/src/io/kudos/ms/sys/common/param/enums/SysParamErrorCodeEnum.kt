package io.kudos.ms.sys.common.param.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 参数错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysParamErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键或 (module, name) 维度查找参数失败 */
    PARAM_NOT_FOUND("PARAM_NOT_FOUND", "参数不存在"),

    /** (module, name) 已存在对应参数 */
    PARAM_ALREADY_EXISTS("PARAM_ALREADY_EXISTS", "该模块下已存在同名参数");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.param"

}
