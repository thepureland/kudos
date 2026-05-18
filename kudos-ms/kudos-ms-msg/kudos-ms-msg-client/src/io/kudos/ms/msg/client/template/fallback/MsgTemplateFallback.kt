package io.kudos.ms.msg.client.template.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.msg.client.template.proxy.IMsgTemplateProxy
import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import org.springframework.stereotype.Component


/**
 * 消息模板 Feign 容错降级实现。读接口不可达时返回 null。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class MsgTemplateFallback :
    AbstractFeignFallbackSupport("MsgTemplateFallback"), IMsgTemplateProxy {

    override fun getTemplateById(id: String): MsgTemplateCacheEntry? {
        warnRead("getTemplateById", id)
        return null
    }

    override fun getTemplateByEvent(
        tenantId: String,
        eventTypeDictCode: String,
        msgTypeDictCode: String,
        localeDictCode: String?,
    ): MsgTemplateCacheEntry? {
        warnRead("getTemplateByEvent", tenantId, eventTypeDictCode, msgTypeDictCode, localeDictCode)
        return null
    }
}
