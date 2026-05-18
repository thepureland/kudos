package io.kudos.ms.msg.common.send.enums


/**
 * `msg_send.send_status_dict_code` 取值。
 *
 * 字典码对齐 `V1.0.0.2__insert_sys_dict_item.sql` 中的 send_status 字典项；改这里要同步 SQL 端。
 *
 * 状态机：
 *   PENDING (00)
 *     ↓ Publish service 投 MQ 成功
 *   SENT_TO_MQ (11) ─→ Publish service 投 MQ 失败时直接置 FAILED_TO_SEND_TO_MQ (21)
 *     ↓ Consumer 拉到消息
 *   CONSUMED_FROM_MQ (31)
 *     ↓ Consumer 完成发送
 *   SUCCESS (33) / SUCCESS_PARTIAL (32) / FAILED_FINAL (22)
 *
 * @author K
 * @since 1.0.0
 */
enum class MsgSendStatusEnum(val dictCode: String) {

    /** 等待发送，刚落表还没投 MQ */
    PENDING("00"),

    /** 取消发送（admin 主动止损） */
    CANCELLED("01"),

    /** 已发送给消息队列（Publish service 投 MQ 成功） */
    SENT_TO_MQ("11"),

    /** 发送给消息队列失败 */
    FAILED_TO_SEND_TO_MQ("21"),

    /** 最终发送失败（consumer 处理失败 / 所有用户都失败） */
    FAILED_FINAL("22"),

    /** 已从消息队列消费（consumer 拉到消息，未执行完） */
    CONSUMED_FROM_MQ("31"),

    /** 发送完成，但部分用户失败 */
    SUCCESS_PARTIAL("32"),

    /** 发送成功 */
    SUCCESS("33");
}
