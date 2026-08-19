package io.kudos.ms.user.client.account.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.user.client.account.proxy.IUserAccountProtectionProxy


/**
 * User account protection fallback. `IUserAccountProtectionApi` currently exposes no methods;
 * the class is kept as a valid target for `@HttpServiceFallback`.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class UserAccountProtectionFallback :
    AbstractHttpFallbackSupport("UserAccountProtectionFallback"), IUserAccountProtectionProxy
