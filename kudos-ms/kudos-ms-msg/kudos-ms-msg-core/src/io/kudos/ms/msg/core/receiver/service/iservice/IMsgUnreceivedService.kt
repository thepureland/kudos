package io.kudos.ms.msg.core.receiver.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.msg.common.receiver.enums.MsgUnreceivedReasonEnum
import io.kudos.ms.msg.core.receiver.model.po.MsgUnreceived


/**
 * Undelivered message business service interface.
 *
 * @author K
 * @since 1.0.0
 */
interface IMsgUnreceivedService : IBaseCrudService<String, MsgUnreceived> {

    /**
     * Batch registers receivers that failed in one send, with resolved=false.
     * Callers are usually channel listeners invoking this in their callback; registering the
     * same (sendId, receiverId) again will write another row (for retry-batch auditing). For
     * deduplication, callers should first query [findUnresolvedBySend].
     */
    fun recordFailures(
        sendId: String,
        receiverIds: Collection<String>,
        publishMethodDictCode: String,
        reason: MsgUnreceivedReasonEnum,
        tenantId: String,
    ): Int

    /**
     * Queries the unresolved (resolved=false) failure records for a given send batch.
     */
    fun findUnresolvedBySend(sendId: String): List<MsgUnreceived>

    /**
     * Marks a record as resolved (retry succeeded / closed by admin).
     */
    fun resolve(id: String): Boolean

    /**
     * Increments the retry count and records the retry time.
     * Does not change resolved; on retry success the caller should additionally call [resolve].
     */
    fun bumpRetry(id: String): Boolean

}
