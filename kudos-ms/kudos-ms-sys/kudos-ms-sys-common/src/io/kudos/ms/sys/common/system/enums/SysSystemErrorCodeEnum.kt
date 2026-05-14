package io.kudos.ms.sys.common.system.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 系统错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysSystemErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键查找系统失败（注：系统 PK 即 code） */
    SYSTEM_NOT_FOUND("SYSTEM_NOT_FOUND", "系统不存在"),

    /** 系统编码已被占用 */
    SYSTEM_CODE_ALREADY_EXISTS("SYSTEM_CODE_ALREADY_EXISTS", "系统编码已存在");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.system"

}
