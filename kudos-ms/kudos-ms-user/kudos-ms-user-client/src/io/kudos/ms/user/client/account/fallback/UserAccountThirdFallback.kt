package io.kudos.ms.user.client.account.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.user.client.account.proxy.IUserAccountThirdProxy
import org.springframework.stereotype.Component


/**
 * Third-party user account Feign fallback. `IUserAccountThirdApi` currently exposes no methods;
 * the class is kept as a valid target for `@FeignClient(fallback=...)`.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class UserAccountThirdFallback :
    AbstractFeignFallbackSupport("UserAccountThirdFallback"), IUserAccountThirdProxy
