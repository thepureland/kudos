package io.kudos.ms.user.client.login.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.user.client.login.proxy.IUserLoginRememberMeProxy


/**
 * Remember-me login fallback. `IUserLoginRememberMeApi` currently exposes no methods;
 * the class is kept as a valid target for `@HttpServiceFallback`.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class UserLoginRememberMeFallback :
    AbstractHttpFallbackSupport("UserLoginRememberMeFallback"), IUserLoginRememberMeProxy
