package io.kudos.ms.msg.common.receiver.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Message receive list query result response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiveRow (

    /** Primary key. */
    override val id: String = "",

    /** Receiver id. */
    val receiverId: String? = null,

    /** Send id. */
    val sendId: String? = null,

    /** Receive status dict code. */
    val receiveStatusDictCode: String? = null,

    /** Create time. */
    val createTime: LocalDateTime? = null,

    /** Update time. */
    val updateTime: LocalDateTime? = null,

    /** Tenant id. */
    val tenantId: String? = null,

) : IIdEntity<String>