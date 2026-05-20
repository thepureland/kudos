package io.kudos.ms.msg.core.receiver.service.impl

import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.base.query.sort.Order
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.msg.common.receiver.enums.MsgReceiveStatusEnum
import io.kudos.ms.msg.common.receiver.vo.MsgReceiveCacheEntry
import io.kudos.ms.msg.core.receiver.dao.MsgReceiveDao
import io.kudos.ms.msg.core.receiver.model.po.MsgReceive
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiveService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


/**
 * 消息接收业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class MsgReceiveService(
    dao: MsgReceiveDao
) : BaseCrudService<String, MsgReceive, MsgReceiveDao>(dao), IMsgReceiveService {

    @Transactional(readOnly = true)
    override fun getReceivesByUserId(receiverId: String): List<MsgReceiveCacheEntry> =
        dao.searchAs<MsgReceiveCacheEntry>(
            Criteria(MsgReceive::receiverId eq receiverId),
            Order.desc(MsgReceive::createTime.name),
        )

    @Transactional(readOnly = true)
    override fun getUnreadCountByUserId(receiverId: String): Int = dao.count(
        Criteria(MsgReceive::receiverId eq receiverId)
            .addAnd(MsgReceive::receiveStatusDictCode inList MsgReceiveStatusEnum.UNREAD_CODES.toList())
    )

    override fun markRead(id: String): Boolean {
        val current = dao.get(id) ?: return false
        // 跳过已经是 READ / DELETED 的记录，避免重复触发后续审计副作用
        if (current.receiveStatusDictCode !in MsgReceiveStatusEnum.UNREAD_CODES) return false
        return dao.updateProperties(
            id,
            mapOf(
                MsgReceive::receiveStatusDictCode.name to MsgReceiveStatusEnum.READ.dictCode,
                MsgReceive::updateTime.name to LocalDateTime.now(),
            )
        )
    }

    override fun markAllReadByUserId(receiverId: String): Int {
        val criteria = Criteria(MsgReceive::receiverId eq receiverId)
            .addAnd(MsgReceive::receiveStatusDictCode inList MsgReceiveStatusEnum.UNREAD_CODES.toList())
        return dao.batchUpdateProperties(
            criteria,
            mapOf(
                MsgReceive::receiveStatusDictCode.name to MsgReceiveStatusEnum.READ.dictCode,
                MsgReceive::updateTime.name to LocalDateTime.now(),
            )
        )
    }

}
