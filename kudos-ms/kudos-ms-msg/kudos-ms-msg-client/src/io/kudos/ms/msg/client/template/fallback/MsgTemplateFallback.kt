package io.kudos.ms.msg.client.template.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.msg.client.template.proxy.IMsgTemplateProxy
import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry


/**
 * Feign fallback implementation for message templates. Read endpoints return null when unreachable.
 *
 * Instantiated by [MsgTemplateFallbackFactory] on each fallback with [cause] supplied,
 * so logs can distinguish 4xx / 5xx / unreachable.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class MsgTemplateFallback(
    private val cause: Throwable? = null,
) : AbstractFeignFallbackSupport("MsgTemplateFallback"), IMsgTemplateProxy {

    override fun getTemplateById(id: String): MsgTemplateCacheEntry? {
        warnRead("getTemplateById", cause, id)
        return null
    }

    override fun getTemplateByEvent(
        tenantId: String,
        eventTypeDictCode: String,
        msgTypeDictCode: String,
        localeDictCode: String?,
    ): MsgTemplateCacheEntry? {
        warnRead("getTemplateByEvent", cause, tenantId, eventTypeDictCode, msgTypeDictCode, localeDictCode)
        return null
    }
}
