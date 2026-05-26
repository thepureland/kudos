package io.kudos.ms.msg.common.receiver.vo.request

import java.time.LocalDateTime

/**
 * Base fields of the message receive form (shared by create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface IMsgReceiveFormBase {

    /** Receiver ID */
    val receiverId: String?

    /** Send ID */
    val sendId: String?

    /** Receive status dictionary code */
    val receiveStatusDictCode: String?

    /** Create time */
    val createTime: LocalDateTime?

    /** Update time */
    val updateTime: LocalDateTime?

    /** Tenant ID */
    val tenantId: String?
}
