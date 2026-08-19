package io.kudos.ms.auth.client.init

import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.auth.client.authz.fallback.AuthzDecisionFallback
import io.kudos.ms.auth.client.authz.proxy.IAuthzDecisionProxy
import io.kudos.ms.auth.client.role.fallback.AuthRoleFallback
import io.kudos.ms.auth.client.role.proxy.IAuthRoleProxy
import org.springframework.cloud.client.circuitbreaker.httpservice.HttpServiceFallback
import org.springframework.context.annotation.Configuration
import org.springframework.web.service.registry.ImportHttpServices

/**
 * Registration entry point for all auth interface clients.
 *
 * Replaces the 2 `@FeignClient(...)` declarations on the proxy interfaces. Auth is the module where
 * the old model was most visibly strained: **both** proxies declared `name = "auth-role"`, and
 * `IAuthzDecisionProxy` needed an extra `contextId = "authzDecision"` purely to stop Feign rejecting
 * the duplicate name. That workaround disappears here — one group, and each proxy is identified by
 * its own type.
 *
 * ```yaml
 * spring:
 *   http:
 *     serviceclient:
 *       auth:
 *         base-url: lb://kudos-ms-auth      # the auth api-internal service id in Nacos
 *         connect-timeout: 2s
 *         read-timeout: 5s
 * ```
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@ImportHttpServices(
    group = AuthClientAutoConfiguration.GROUP,
    types = [
        IAuthRoleProxy::class,
        IAuthzDecisionProxy::class,
    ]
)
@HttpServiceFallback(value = AuthRoleFallback::class, service = [IAuthRoleProxy::class], group = AuthClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = AuthzDecisionFallback::class, service = [IAuthzDecisionProxy::class], group = AuthClientAutoConfiguration.GROUP)
open class AuthClientAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ms-auth-client"

    companion object {
        /** HTTP service group name; must match the `spring.http.serviceclient.<group>` config key. */
        const val GROUP: String = "auth"
    }

}
