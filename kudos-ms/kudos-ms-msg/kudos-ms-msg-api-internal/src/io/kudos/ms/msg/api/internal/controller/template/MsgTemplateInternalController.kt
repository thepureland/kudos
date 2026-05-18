package io.kudos.ms.msg.api.internal.controller.template

import io.kudos.ms.msg.common.template.api.IMsgTemplateApi
import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import io.kudos.ms.msg.core.template.api.MsgTemplateApi
import org.springframework.web.bind.annotation.RestController


/**
 * 消息模板 内部 RPC 控制器。路径继承自 [IMsgTemplateApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class MsgTemplateInternalController(
    private val msgTemplateApi: MsgTemplateApi,
) : IMsgTemplateApi {

    override fun getTemplateById(id: String): MsgTemplateCacheEntry? =
        msgTemplateApi.getTemplateById(id)

    override fun getTemplateByEvent(
        tenantId: String,
        eventTypeDictCode: String,
        msgTypeDictCode: String,
        localeDictCode: String?,
    ): MsgTemplateCacheEntry? =
        msgTemplateApi.getTemplateByEvent(tenantId, eventTypeDictCode, msgTypeDictCode, localeDictCode)

}
