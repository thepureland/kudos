package io.kudos.ms.msg.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.msg.core.dao.MsgInstanceDao
import io.kudos.ms.msg.core.model.po.MsgInstance
import io.kudos.ms.msg.core.service.iservice.IMsgInstanceService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 消息实例业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class MsgInstanceService(
    dao: MsgInstanceDao
) : BaseCrudService<String, MsgInstance, MsgInstanceDao>(dao), IMsgInstanceService {



}
