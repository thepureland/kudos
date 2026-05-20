package io.kudos.ms.msg.core.template.service.impl

import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
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

    @Transactional(readOnly = true)
    override fun getTemplateById(id: String): MsgTemplateCacheEntry? =
        dao.getAs<MsgTemplateCacheEntry>(id)

    @Transactional(readOnly = true)
    override fun getTemplateByEvent(
        tenantId: String,
        eventTypeDictCode: String,
        msgTypeDictCode: String,
        localeDictCode: String?,
    ): MsgTemplateCacheEntry? {
        val criteria = Criteria(MsgTemplate::tenantId eq tenantId)
            .addAnd(MsgTemplate::eventTypeDictCode eq eventTypeDictCode)
            .addAnd(MsgTemplate::msgTypeDictCode eq msgTypeDictCode)
        localeDictCode?.let { criteria.addAnd(MsgTemplate::localeDictCode eq it) }
        return dao.searchAs<MsgTemplateCacheEntry>(criteria).firstOrNull()
    }

}
