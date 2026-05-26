package io.kudos.ability.distributed.stream.common.biz

import io.kudos.ability.distributed.stream.common.model.po.SysMqFailMsg
import io.kudos.base.support.service.iservice.IBaseCrudService
import java.time.LocalDateTime

/**
 * Persist / query / clean service for stream failed messages.
 *
 * Backing table `sys_mq_fail_msg` ([SysMqFailMsg]). Typical callers:
 *  - [io.kudos.ability.distributed.stream.common.handler.StreamGlobalExceptionHandler.globalHandleError]
 *    records consumer-side exceptions into the table
 *  - business-side manual recovery (pull by topic + time window / resend / delete)
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
interface ISysMqFailMsgService : IBaseCrudService<String, SysMqFailMsg> {
    /**
     * Save a failed message.
     *
     * @param exceptionMsg
     */
    fun save(exceptionMsg: SysMqFailMsg): Boolean

    /**
     * Query failed messages under the given topic.
     *
     * @param topic     topic
     * @param startTime query start time
     */
    fun query(topic: String, startTime: LocalDateTime): List<SysMqFailMsg>

    /**
     * Delete failed messages.
     *
     * @param ids
     */
    fun delete(ids: List<String>)
}