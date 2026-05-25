package io.kudos.ms.msg.client.template.fallback

import io.kudos.ms.msg.client.template.proxy.IMsgTemplateProxy
import org.springframework.cloud.openfeign.FallbackFactory
import org.springframework.stereotype.Component


/**
 * [MsgTemplateFallback] 的 FallbackFactory。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class MsgTemplateFallbackFactory : FallbackFactory<IMsgTemplateProxy> {
    override fun create(cause: Throwable): IMsgTemplateProxy = MsgTemplateFallback(cause)
}
