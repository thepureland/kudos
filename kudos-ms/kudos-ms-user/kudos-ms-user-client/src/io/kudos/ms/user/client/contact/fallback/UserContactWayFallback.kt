package io.kudos.ms.user.client.contact.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.user.client.contact.proxy.IUserContactWayProxy
import org.springframework.stereotype.Component


/**
 * User contact way Feign fallback. `IUserContactWayApi` currently exposes no methods;
 * the class is kept as a valid target for `@FeignClient(fallback=...)`.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class UserContactWayFallback :
    AbstractFeignFallbackSupport("UserContactWayFallback"), IUserContactWayProxy
