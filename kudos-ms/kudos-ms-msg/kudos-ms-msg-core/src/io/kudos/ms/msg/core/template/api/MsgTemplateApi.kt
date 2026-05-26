package io.kudos.ms.msg.core.template.api

import io.kudos.ms.msg.common.template.api.IMsgTemplateApi
import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import io.kudos.ms.msg.core.template.service.iservice.IMsgTemplateService
import jakarta.annotation.Resource
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service


/**
 * Local implementation of the message template API.
 *
 * Marked [Primary] for the same reason as the auth/user/sys modules: the internal RPC controller
 * also implements [IMsgTemplateApi] and is registered as a bean, and injection ambiguity is resolved
 * by falling back to the local bean.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Primary
@Service
open class MsgTemplateApi : IMsgTemplateApi {

    @Resource
    private lateinit var msgTemplateService: IMsgTemplateService

    override fun getTemplateById(id: String): MsgTemplateCacheEntry? =
        msgTemplateService.getTemplateById(id)

    override fun getTemplateByEvent(
        tenantId: String,
        eventTypeDictCode: String,
        msgTypeDictCode: String,
        localeDictCode: String?,
    ): MsgTemplateCacheEntry? =
        msgTemplateService.getTemplateByEvent(tenantId, eventTypeDictCode, msgTypeDictCode, localeDictCode)

}
