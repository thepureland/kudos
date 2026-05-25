package io.kudos.ms.msg.common.instance.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 消息实例错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class MsgInstanceErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键查找消息实例失败 */
    INSTANCE_NOT_FOUND("INSTANCE_NOT_FOUND", "消息实例不存在"),

    /** 当前时间不在 [validTimeStart, validTimeEnd] 区间内 */
    INSTANCE_EXPIRED("INSTANCE_EXPIRED", "消息实例已过有效期");

    override val i18nKeyPrefix: String
        get() = "msg.error-msg.instance"

}
