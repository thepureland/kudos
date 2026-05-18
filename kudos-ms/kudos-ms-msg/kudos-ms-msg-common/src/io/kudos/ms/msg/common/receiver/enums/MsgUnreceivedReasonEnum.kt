package io.kudos.ms.msg.common.receiver.enums


/**
 * 未送达失败原因常量（写入 `msg_unreceived.fail_reason`）。
 *
 * 用枚举而非自由文本，避免每次失败一行就多写一种新拼写，给后续按原因聚合统计留口子。
 * admin 端展示时可以加 i18n key 翻译。
 *
 * @author K
 * @since 1.0.0
 */
enum class MsgUnreceivedReasonEnum(val code: String) {

    /** 用户没配置对应渠道的联系方式（如要发邮件但用户没邮箱） */
    NO_CONTACT("NO_CONTACT"),

    /** 渠道服务端返回失败（SMTP 拒收、SMS 接口报错等） */
    CHANNEL_REJECT("CHANNEL_REJECT"),

    /** 渠道调用超时 */
    TIMEOUT("TIMEOUT"),

    /** Listener 处理过程中抛了异常 */
    LISTENER_ERROR("LISTENER_ERROR"),

    /** 接收人 id 集合为空 */
    EMPTY_RECEIVERS("EMPTY_RECEIVERS"),

    /** 其它/未分类 */
    UNKNOWN("UNKNOWN");
}
