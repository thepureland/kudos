package io.kudos.ms.msg.common.send.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 消息发送错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class MsgSendErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** publish 入参 receiverIds 为空 */
    RECEIVER_IDS_EMPTY("RECEIVER_IDS_EMPTY", "接收者列表为空"),

    /** 按 (tenant_id, event_type, msg_type, locale) 未匹配到可用模板 */
    TEMPLATE_NOT_FOUND("TEMPLATE_NOT_FOUND", "未找到匹配的消息模板"),

    /** 投递到 notify producer 失败（producer 未装配 或 notify 抛异常） */
    MQ_PUBLISH_FAILED("MQ_PUBLISH_FAILED", "消息投递失败");

    override val i18nKeyPrefix: String
        get() = "msg.error-msg.send"

}
