package io.kudos.ms.user.client.init

import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.user.client.account.fallback.UserAccountFallback
import io.kudos.ms.user.client.account.fallback.UserAccountProtectionFallback
import io.kudos.ms.user.client.account.fallback.UserAccountThirdFallback
import io.kudos.ms.user.client.account.proxy.IUserAccountProtectionProxy
import io.kudos.ms.user.client.account.proxy.IUserAccountProxy
import io.kudos.ms.user.client.account.proxy.IUserAccountThirdProxy
import io.kudos.ms.user.client.contact.fallback.UserContactWayFallback
import io.kudos.ms.user.client.contact.proxy.IUserContactWayProxy
import io.kudos.ms.user.client.login.fallback.UserLoginRememberMeFallback
import io.kudos.ms.user.client.login.proxy.IUserLoginRememberMeProxy
import io.kudos.ms.user.client.org.fallback.UserOrgFallback
import io.kudos.ms.user.client.org.proxy.IUserOrgProxy
import io.kudos.ms.user.client.passport.fallback.PassportFallback
import io.kudos.ms.user.client.passport.proxy.IPassportProxy
import org.springframework.cloud.client.circuitbreaker.httpservice.HttpServiceFallback
import org.springframework.context.annotation.Configuration
import org.springframework.web.service.registry.ImportHttpServices

/**
 * Registration entry point for all user interface clients.
 *
 * Replaces the 7 `@FeignClient(name = "user-xxx", fallback = ...)` declarations that used to sit on
 * the proxy interfaces. As in `kudos-ms-sys-client`, the per-proxy "service names"
 * (`user-account`, `user-contact-way`, `user-login-remember-me`, …) were never distinct
 * microservices — every path is `/api/internal/user/...` — so they collapse into the single [GROUP]
 * group, and the target moves into deployment configuration:
 *
 * ```yaml
 * spring:
 *   http:
 *     serviceclient:
 *       user:
 *         base-url: lb://kudos-ms-user      # the user api-internal service id in Nacos
 *         connect-timeout: 2s
 *         read-timeout: 5s
 * ```
 *
 * Both lists below are maintained by hand; `basePackages` scanning is not usable here (it needs a
 * type-level `@HttpExchange` on each proxy, which Spring MVC would then treat as a controller
 * marker). See `SysClientAutoConfiguration` for the full reasoning.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@ImportHttpServices(
    group = UserClientAutoConfiguration.GROUP,
    types = [
        IUserAccountProxy::class,
        IUserAccountProtectionProxy::class,
        IUserAccountThirdProxy::class,
        IUserContactWayProxy::class,
        IUserLoginRememberMeProxy::class,
        IUserOrgProxy::class,
        IPassportProxy::class,
    ]
)
@HttpServiceFallback(value = UserAccountFallback::class, service = [IUserAccountProxy::class], group = UserClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = UserAccountProtectionFallback::class, service = [IUserAccountProtectionProxy::class], group = UserClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = UserAccountThirdFallback::class, service = [IUserAccountThirdProxy::class], group = UserClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = UserContactWayFallback::class, service = [IUserContactWayProxy::class], group = UserClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = UserLoginRememberMeFallback::class, service = [IUserLoginRememberMeProxy::class], group = UserClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = UserOrgFallback::class, service = [IUserOrgProxy::class], group = UserClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = PassportFallback::class, service = [IPassportProxy::class], group = UserClientAutoConfiguration.GROUP)
open class UserClientAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ms-user-client"

    companion object {
        /** HTTP service group name; must match the `spring.http.serviceclient.<group>` config key. */
        const val GROUP: String = "user"
    }

}
