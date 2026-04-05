package io.kudos.ms.msg.core.template.service.impl
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.msg.core.template.dao.MsgTemplateDao
import io.kudos.ms.msg.core.template.model.po.MsgTemplate
import io.kudos.ms.msg.core.template.service.iservice.IMsgTemplateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 消息模板业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class MsgTemplateService(
    dao: MsgTemplateDao
) : BaseCrudService<String, MsgTemplate, MsgTemplateDao>(dao), IMsgTemplateService {



}
