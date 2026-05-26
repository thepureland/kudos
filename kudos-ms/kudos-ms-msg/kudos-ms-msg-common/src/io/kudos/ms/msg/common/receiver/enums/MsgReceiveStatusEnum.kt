package io.kudos.ms.msg.common.receiver.enums


/**
 * Values of `msg_receive.receive_status_dict_code`.
 *
 * The dict codes correspond one-to-one with the receive_status dict items defined in
 * `V1.0.0.2__insert_sys_dict_item.sql`; when changing these, make sure the SQL-side
 * item_code values stay the same.
 *
 * @author K
 * @since 1.0.0
 */
enum class MsgReceiveStatusEnum(val dictCode: String) {

    /** Received (the initial status written by the sender into the receive record) */
    RECEIVED("11"),

    /** Unread (the recipient has fetched but not yet opened it) */
    UNREAD("01"),

    /** Read (the recipient has opened it) */
    READ("12"),

    /** Deleted (removed by the recipient from the inbox, but the row is kept for audit) */
    DELETED("21");

    companion object {
        /** Set used to detect "unread" receive records: RECEIVED also counts as unread since the recipient has not opened it yet. */
        val UNREAD_CODES: Set<String> = setOf(RECEIVED.dictCode, UNREAD.dictCode)
    }
}
