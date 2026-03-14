package io.kudos.ms.msg.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ms.msg.core.dao.MsgReceiverGroupDao
import io.kudos.ms.msg.core.model.po.MsgReceiverGroup
import io.kudos.ms.msg.core.service.iservice.IMsgReceiverGroupService
import org.springframework.stereotype.Service


/**
 * 消息接收者群组业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
open class MsgReceiverGroupService : BaseCrudService<String, MsgReceiverGroup, MsgReceiverGroupDao>(), IMsgReceiverGroupService {



}
