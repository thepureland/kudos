package io.kudos.ms.msg.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.msg.core.dao.MsgReceiverGroupDao
import io.kudos.ms.msg.core.model.po.MsgReceiverGroup
import io.kudos.ms.msg.core.service.iservice.IMsgReceiverGroupService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 消息接收者群组业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class MsgReceiverGroupService(
    dao: MsgReceiverGroupDao
) : BaseCrudService<String, MsgReceiverGroup, MsgReceiverGroupDao>(dao), IMsgReceiverGroupService {



}
