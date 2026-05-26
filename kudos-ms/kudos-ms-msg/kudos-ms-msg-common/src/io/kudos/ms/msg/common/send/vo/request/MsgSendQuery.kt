package io.kudos.ms.msg.common.send.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import java.time.LocalDateTime
import io.kudos.ms.msg.common.send.vo.response.MsgSendRow


/**
 * Message send list query request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgSendQuery (

    /** Receiver group type dict code. */
    val receiverGroupTypeDictCode: String? = null,

    /** Receiver group id. */
    val receiverGroupId: String? = null,

    /** Message instance id. */
    val instanceId: String? = null,

    /** Message type dict code. */
    val msgTypeDictCode: String? = null,

    /** Country-language dict code. */
    val localeDictCode: String? = null,

    /** Send status dict code. */
    val sendStatusDictCode: String? = null,

    /** Create time. */
    val createTime: LocalDateTime? = null,

    /** Update time. */
    val updateTime: LocalDateTime? = null,

    /** Successful send count. */
    val successCount: Int? = null,

    /** Failed send count. */
    val failCount: Int? = null,

    /** Scheduled job id. */
    val jobId: String? = null,

    /** Tenant id. */
    val tenantId: String? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = MsgSendRow::class

}