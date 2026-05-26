package io.kudos.ms.user.client.login.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.user.client.login.proxy.IUserLoginRememberMeProxy
import org.springframework.stereotype.Component


/**
 * Remember-me login Feign fallback. `IUserLoginRememberMeApi` currently exposes no methods;
 * the class is kept as a valid target for `@FeignClient(fallback=...)`.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class UserLoginRememberMeFallback :
    AbstractFeignFallbackSupport("UserLoginRememberMeFallback"), IUserLoginRememberMeProxy
