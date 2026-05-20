package io.kudos.ms.msg.core.send.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.msg.core.send.dao.MsgSendDao
import io.kudos.ms.msg.core.send.model.po.MsgSend
import io.kudos.ms.msg.core.send.service.iservice.IMsgSendService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


/**
 * 消息发送业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class MsgSendService(
    dao: MsgSendDao
) : BaseCrudService<String, MsgSend, MsgSendDao>(dao), IMsgSendService {

    override fun updateSendStatus(sendId: String, sendStatusDictCode: String): Boolean =
        dao.updateProperties(
            sendId,
            mapOf(
                MsgSend::sendStatusDictCode.name to sendStatusDictCode,
                MsgSend::updateTime.name to LocalDateTime.now(),
            )
        )

    override fun finishSend(
        sendId: String,
        successDelta: Int,
        failDelta: Int,
        finalStatusDictCode: String,
    ): Boolean {
        // 读 → 改 → 写。读不到（被删了）返回 false。
        val current = dao.get(sendId) ?: return false
        val newSuccess = (current.successCount ?: 0) + successDelta
        val newFail = (current.failCount ?: 0) + failDelta
        return dao.updateProperties(
            sendId,
            mapOf(
                MsgSend::successCount.name to newSuccess,
                MsgSend::failCount.name to newFail,
                MsgSend::sendStatusDictCode.name to finalStatusDictCode,
                MsgSend::updateTime.name to LocalDateTime.now(),
            )
        )
    }
}
