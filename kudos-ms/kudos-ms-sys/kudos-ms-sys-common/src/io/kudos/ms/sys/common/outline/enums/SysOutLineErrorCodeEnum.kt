package io.kudos.ms.sys.common.outline.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 出网白名单错误码
 *
 * @author K
 * @since 1.0.0
 */
enum class SysOutLineErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键查找失败 */
    OUT_LINE_NOT_FOUND("OUT_LINE_NOT_FOUND", "出网白名单不存在"),

    /** 同 (system_code, tenant_id, host, port, protocol) 已存在 */
    OUT_LINE_ALREADY_EXISTS("OUT_LINE_ALREADY_EXISTS", "该出网白名单已存在");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.outline"

}
