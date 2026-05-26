package io.kudos.ms.msg.common.receiver.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * Message receive cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiveCacheEntry (

    /** Primary key */
    override val id: String,

    /** Receiver ID */
    val receiverId: String?,

    /** Send ID */
    val sendId: String?,

    /** Receive status dictionary code */
    val receiveStatusDictCode: String?,

    /** Create time */
    val createTime: LocalDateTime?,

    /** Update time */
    val updateTime: LocalDateTime?,

    /** Tenant ID */
    val tenantId: String?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 8705640601695840987L
    }

}
