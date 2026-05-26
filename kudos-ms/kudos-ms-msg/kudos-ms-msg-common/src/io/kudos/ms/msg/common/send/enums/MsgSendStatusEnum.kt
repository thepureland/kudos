package io.kudos.ms.msg.common.send.enums


/**
 * Values for `msg_send.send_status_dict_code`.
 *
 * Dict codes match the send_status entries in `V1.0.0.2__insert_sys_dict_item.sql`; keep both sides in sync.
 *
 * State machine:
 *   PENDING (00)
 *     | Publish service dispatched to MQ successfully
 *   SENT_TO_MQ (11) -> Set directly to FAILED_TO_SEND_TO_MQ (21) when Publish service fails to dispatch
 *     | Consumer pulled the message
 *   CONSUMED_FROM_MQ (31)
 *     | Consumer finished sending
 *   SUCCESS (33) / SUCCESS_PARTIAL (32) / FAILED_FINAL (22)
 *
 * @author K
 * @since 1.0.0
 */
enum class MsgSendStatusEnum(val dictCode: String) {

    /** Pending: record persisted but not yet dispatched to MQ. */
    PENDING("00"),

    /** Cancelled (admin stopped delivery proactively). */
    CANCELLED("01"),

    /** Sent to the message queue (Publish service dispatched to MQ successfully). */
    SENT_TO_MQ("11"),

    /** Failed to send to the message queue. */
    FAILED_TO_SEND_TO_MQ("21"),

    /** Final send failure (consumer processing failed / all recipients failed). */
    FAILED_FINAL("22"),

    /** Consumed from the message queue (consumer pulled the message but has not finished processing). */
    CONSUMED_FROM_MQ("31"),

    /** Send completed but some recipients failed. */
    SUCCESS_PARTIAL("32"),

    /** Send succeeded. */
    SUCCESS("33");
}
