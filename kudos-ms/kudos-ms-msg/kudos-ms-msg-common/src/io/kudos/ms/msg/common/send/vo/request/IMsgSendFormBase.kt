package io.kudos.ms.msg.common.send.vo.request

import java.time.LocalDateTime

/**
 * Common fields for message send forms (shared by create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface IMsgSendFormBase {

    /** Receiver group type dict code. */
    val receiverGroupTypeDictCode: String?

    /** Receiver group id. */
    val receiverGroupId: String?

    /** Message instance id. */
    val instanceId: String?

    /** Message type dict code. */
    val msgTypeDictCode: String?

    /** Country-language dict code. */
    val localeDictCode: String?

    /** Send status dict code. */
    val sendStatusDictCode: String?

    /** Create time. */
    val createTime: LocalDateTime?

    /** Update time. */
    val updateTime: LocalDateTime?

    /** Successful send count. */
    val successCount: Int?

    /** Failed send count. */
    val failCount: Int?

    /** Scheduled job id. */
    val jobId: String?

    /** Tenant id. */
    val tenantId: String?
}
