package io.kudos.ms.msg.common.send.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * Message send cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgSendCacheEntry (

    /** Primary key. */
    override val id: String,

    /** Receiver group type dict code. */
    val receiverGroupTypeDictCode: String?,

    /** Receiver group id. */
    val receiverGroupId: String?,

    /** Message instance id. */
    val instanceId: String?,

    /** Message type dict code. */
    val msgTypeDictCode: String?,

    /** Country-language dict code. */
    val localeDictCode: String?,

    /** Send status dict code. */
    val sendStatusDictCode: String?,

    /** Create time. */
    val createTime: LocalDateTime?,

    /** Update time. */
    val updateTime: LocalDateTime?,

    /** Successful send count. */
    val successCount: Int?,

    /** Failed send count. */
    val failCount: Int?,

    /** Scheduled job id. */
    val jobId: String?,

    /** Tenant id. */
    val tenantId: String?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 6949395793750221523L
    }

}
