package io.kudos.ms.msg.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ms.msg.core.dao.MsgTemplateDao
import io.kudos.ms.msg.core.model.po.MsgTemplate
import io.kudos.ms.msg.core.service.iservice.IMsgTemplateService
import org.springframework.stereotype.Service


/**
 * 消息模板业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
open class MsgTemplateService : BaseCrudService<String, MsgTemplate, MsgTemplateDao>(), IMsgTemplateService {



}
