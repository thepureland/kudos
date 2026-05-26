package io.kudos.ability.distributed.stream.common.biz

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ability.distributed.stream.common.dao.StreamExceptionMsgDao
import io.kudos.ability.distributed.stream.common.model.po.SysMqFailMsg
import io.kudos.ability.distributed.stream.common.model.table.SysMqFailMsgs
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Default Ktorm implementation of [ISysMqFailMsgService].
 *
 * `@Transactional(REQUIRES_NEW)` makes [save] commit independently even when an outer
 * transaction rolls back — failure logs must not be lost together with business failures.
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
@Transactional
open class SysMqFailMsgService(
    dao: StreamExceptionMsgDao
) : BaseCrudService<String, SysMqFailMsg, StreamExceptionMsgDao>(dao),
    ISysMqFailMsgService {

    /**
     * Save a failed message.
     *
     * @param exceptionMsg
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun save(exceptionMsg: SysMqFailMsg): Boolean {
        dao.insert(exceptionMsg)
        return true
    }

    /**
     * Query failed messages under the given topic.
     *
     * @param topic     topic
     * @param startTime query start time
     */
    override fun query(topic: String, startTime: LocalDateTime): List<SysMqFailMsg> = dao.search(
        Criteria(SysMqFailMsgs.topic.name, OperatorEnum.EQ, topic)
            .addAnd(SysMqFailMsgs.createTime.name, OperatorEnum.GE, startTime)
    )

    /**
     * Delete failed messages.
     *
     * @param ids
     */
    @Transactional(rollbackFor = [Exception::class])
    override fun delete(ids: List<String>) {
        val count = dao.batchDelete(ids)
        LOG.info("deleted stream failed message count:{0}", count)
    }

    private val LOG = LogFactory.getLog(this::class)

}
