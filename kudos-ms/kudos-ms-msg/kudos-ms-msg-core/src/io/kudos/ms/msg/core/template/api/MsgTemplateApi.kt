package io.kudos.ms.msg.core.template.api

import io.kudos.ms.msg.common.template.api.IMsgTemplateApi
import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import io.kudos.ms.msg.core.template.service.iservice.IMsgTemplateService
import jakarta.annotation.Resource
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service


/**
 * 消息模板API本地实现。
 *
 * 标 [Primary] 与 auth/user/sys 模块同样原因：内部 RPC controller 也实现 [IMsgTemplateApi]
 * 注册为 bean，注入歧义由本地 bean 兜底。
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
