package io.kudos.ms.msg.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.msg.core.dao.MsgSendDao
import io.kudos.ms.msg.core.model.po.MsgSend
import io.kudos.ms.msg.core.service.iservice.IMsgSendService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


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



}
