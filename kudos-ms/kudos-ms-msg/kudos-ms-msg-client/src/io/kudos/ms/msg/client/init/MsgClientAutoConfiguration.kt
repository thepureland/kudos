package io.kudos.ms.msg.client.init

import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.msg.client.instance.fallback.MsgInstanceFallback
import io.kudos.ms.msg.client.instance.proxy.IMsgInstanceProxy
import io.kudos.ms.msg.client.receiver.fallback.MsgReceiveFallback
import io.kudos.ms.msg.client.receiver.fallback.MsgReceiverGroupFallback
import io.kudos.ms.msg.client.receiver.proxy.IMsgReceiveProxy
import io.kudos.ms.msg.client.receiver.proxy.IMsgReceiverGroupProxy
import io.kudos.ms.msg.client.send.fallback.MsgSendFallback
import io.kudos.ms.msg.client.send.proxy.IMsgSendProxy
import io.kudos.ms.msg.client.template.fallback.MsgTemplateFallback
import io.kudos.ms.msg.client.template.proxy.IMsgTemplateProxy
import org.springframework.cloud.client.circuitbreaker.httpservice.HttpServiceFallback
import org.springframework.context.annotation.Configuration
import org.springframework.web.service.registry.ImportHttpServices

/**
 * Registration entry point for all msg interface clients.
 *
 * Replaces `@EnableFeignClients` plus the 5 `@FeignClient(name = "msg-xxx", fallbackFactory = ...)`
 * declarations. Two msg-specific notes:
 *
 * 1. **`@EnableFeignClients` has no counterpart and needs none.** Feign required a scan to find the
 *    annotated interfaces; here the interfaces are named outright in `types`, so this class replaces
 *    both the enable-annotation and the five client declarations.
 * 2. **`fallbackFactory` is gone, but cause-aware logging is not.** The five `Msg*FallbackFactory`
 *    classes were deleted; each fallback now declares `Throwable`-first overloads, which
 *    `CircuitBreakerConfigurerUtils.resolveFallbackMethod` prefers over the plain contract signature.
 *    See `MsgSendFallback` for the full explanation.
 *
 * ```yaml
 * spring:
 *   http:
 *     serviceclient:
 *       msg:
 *         base-url: lb://kudos-ms-msg       # the msg api-internal service id in Nacos
 *         connect-timeout: 2s
 *         read-timeout: 5s
 * ```
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@ImportHttpServices(
    group = MsgClientAutoConfiguration.GROUP,
    types = [
        IMsgInstanceProxy::class,
        IMsgReceiveProxy::class,
        IMsgReceiverGroupProxy::class,
        IMsgSendProxy::class,
        IMsgTemplateProxy::class,
    ]
)
@HttpServiceFallback(value = MsgInstanceFallback::class, service = [IMsgInstanceProxy::class], group = MsgClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = MsgReceiveFallback::class, service = [IMsgReceiveProxy::class], group = MsgClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = MsgReceiverGroupFallback::class, service = [IMsgReceiverGroupProxy::class], group = MsgClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = MsgSendFallback::class, service = [IMsgSendProxy::class], group = MsgClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = MsgTemplateFallback::class, service = [IMsgTemplateProxy::class], group = MsgClientAutoConfiguration.GROUP)
open class MsgClientAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ms-msg-client"

    companion object {
        /** HTTP service group name; must match the `spring.http.serviceclient.<group>` config key. */
        const val GROUP: String = "msg"
    }

}
