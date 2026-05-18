package io.kudos.ms.msg.common.receiver.enums


/**
 * `msg_receive.receive_status_dict_code` 取值。
 *
 * 字典码与 `V1.0.0.2__insert_sys_dict_item.sql` 中的 receive_status 字典项一一对应，
 * 改这里的同时务必保证 SQL 端的 item_code 不变。
 *
 * @author K
 * @since 1.0.0
 */
enum class MsgReceiveStatusEnum(val dictCode: String) {

    /** 已接收（发送端写入接收记录的初始状态） */
    RECEIVED("11"),

    /** 未读（接收方拉取过、但尚未点开） */
    UNREAD("01"),

    /** 已读（接收方点开过） */
    READ("12"),

    /** 已删除（接收方在收件箱里删除，但保留行做审计） */
    DELETED("21");

    companion object {
        /** "未读" 类接收记录的判定集合：RECEIVED 也算未读，因为接收方还没打开过。 */
        val UNREAD_CODES: Set<String> = setOf(RECEIVED.dictCode, UNREAD.dictCode)
    }
}
