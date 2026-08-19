package io.kudos.ms.msg.client.template.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.msg.client.template.proxy.IMsgTemplateProxy
import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry


/**
 * Fallback implementation for message templates. Read endpoints return null when unreachable.
 *
 * Each method comes in two shapes — see [io.kudos.ms.msg.client.send.fallback.MsgSendFallback] for why.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
open class MsgTemplateFallback : AbstractHttpFallbackSupport("MsgTemplateFallback"), IMsgTemplateProxy {

    fun getTemplateById(cause: Throwable?, id: String): MsgTemplateCacheEntry? {
        warnRead("getTemplateById", cause, id)
        return null
    }

    override fun getTemplateById(id: String): MsgTemplateCacheEntry? = getTemplateById(null, id)

    fun getTemplateByEvent(
        cause: Throwable?,
        tenantId: String,
        eventTypeDictCode: String,
        msgTypeDictCode: String,
        localeDictCode: String?,
    ): MsgTemplateCacheEntry? {
        warnRead("getTemplateByEvent", cause, tenantId, eventTypeDictCode, msgTypeDictCode, localeDictCode)
        return null
    }

    override fun getTemplateByEvent(
        tenantId: String,
        eventTypeDictCode: String,
        msgTypeDictCode: String,
        localeDictCode: String?,
    ): MsgTemplateCacheEntry? =
        getTemplateByEvent(null, tenantId, eventTypeDictCode, msgTypeDictCode, localeDictCode)

}
