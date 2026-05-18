package io.kudos.ms.msg.core.receiver.service.impl

import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.msg.common.receiver.enums.MsgUnreceivedReasonEnum
import io.kudos.ms.msg.core.receiver.dao.MsgUnreceivedDao
import io.kudos.ms.msg.core.receiver.model.po.MsgUnreceived
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgUnreceivedService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


/**
 * 未送达消息业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Transactional
open class MsgUnreceivedService(
    dao: MsgUnreceivedDao
) : BaseCrudService<String, MsgUnreceived, MsgUnreceivedDao>(dao), IMsgUnreceivedService {

    override fun recordFailures(
        sendId: String,
        receiverIds: Collection<String>,
        publishMethodDictCode: String,
        reason: MsgUnreceivedReasonEnum,
        tenantId: String,
    ): Int {
        if (receiverIds.isEmpty()) return 0
        val now = LocalDateTime.now()
        var count = 0
        receiverIds.forEach { receiverId ->
            val po = MsgUnreceived().apply {
                this.receiverId = receiverId
                this.sendId = sendId
                this.publishMethodDictCode = publishMethodDictCode
                this.failReason = reason.code
                this.retryCount = 0
                this.lastRetryTime = null
                this.resolved = false
                this.createTime = now
                this.updateTime = null
                this.tenantId = tenantId
            }
            dao.insert(po)
            count++
        }
        return count
    }

    @Transactional(readOnly = true)
    override fun findUnresolvedBySend(sendId: String): List<MsgUnreceived> {
        val criteria = Criteria(MsgUnreceived::sendId eq sendId)
            .addAnd(MsgUnreceived::resolved eq false)
        return dao.search(criteria)
    }

    override fun resolve(id: String): Boolean {
        return dao.updateProperties(
            id,
            mapOf(
                MsgUnreceived::resolved.name to true,
                MsgUnreceived::updateTime.name to LocalDateTime.now(),
            )
        )
    }

    override fun bumpRetry(id: String): Boolean {
        val current = dao.get(id) ?: return false
        return dao.updateProperties(
            id,
            mapOf(
                MsgUnreceived::retryCount.name to (current.retryCount + 1),
                MsgUnreceived::lastRetryTime.name to LocalDateTime.now(),
                MsgUnreceived::updateTime.name to LocalDateTime.now(),
            )
        )
    }
}
