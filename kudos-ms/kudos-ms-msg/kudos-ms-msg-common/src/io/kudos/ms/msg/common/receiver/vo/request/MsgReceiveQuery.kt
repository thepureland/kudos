package io.kudos.ms.msg.common.receiver.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import java.time.LocalDateTime
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiveRow


/**
 * Query criteria request VO for the message receive list.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiveQuery (

    /** Receiver ID */
    val receiverId: String? = null,

    /** Send ID */
    val sendId: String? = null,

    /** Receive status dictionary code */
    val receiveStatusDictCode: String? = null,

    /** Create time */
    val createTime: LocalDateTime? = null,

    /** Update time */
    val updateTime: LocalDateTime? = null,

    /** Tenant ID */
    val tenantId: String? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = MsgReceiveRow::class

}