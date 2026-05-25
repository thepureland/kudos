package io.kudos.ms.msg.client.template.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.msg.client.template.proxy.IMsgTemplateProxy
import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry


/**
 * 消息模板 Feign 容错降级实现。读接口不可达时返回 null。
 *
 * 由 [MsgTemplateFallbackFactory] 在每次降级时构造一个实例并传入 [cause]，
 * 让日志能区分 4xx / 5xx / unreachable。
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
