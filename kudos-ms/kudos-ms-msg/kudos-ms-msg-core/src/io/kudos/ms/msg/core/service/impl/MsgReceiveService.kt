package io.kudos.ms.msg.core.service.impl

import io.kudos.base.support.service.BaseCrudService
import io.kudos.ms.msg.core.dao.MsgReceiveDao
import io.kudos.ms.msg.core.model.po.MsgReceive
import io.kudos.ms.msg.core.service.iservice.IMsgReceiveService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


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



}
