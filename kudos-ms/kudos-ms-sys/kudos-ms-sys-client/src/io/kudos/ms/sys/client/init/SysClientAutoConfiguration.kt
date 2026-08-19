package io.kudos.ms.sys.client.init

import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.sys.client.accessrule.fallback.SysAccessRuleFallback
import io.kudos.ms.sys.client.accessrule.fallback.SysAccessRuleIpFallback
import io.kudos.ms.sys.client.accessrule.proxy.ISysAccessRuleIpProxy
import io.kudos.ms.sys.client.accessrule.proxy.ISysAccessRuleProxy
import io.kudos.ms.sys.client.cache.fallback.SysCacheFallback
import io.kudos.ms.sys.client.cache.proxy.ISysCacheProxy
import io.kudos.ms.sys.client.datasource.fallback.SysDataSourceFallback
import io.kudos.ms.sys.client.datasource.proxy.ISysDataSourceProxy
import io.kudos.ms.sys.client.dict.fallback.SysDictFallback
import io.kudos.ms.sys.client.dict.fallback.SysDictItemFallback
import io.kudos.ms.sys.client.dict.proxy.ISysDictItemProxy
import io.kudos.ms.sys.client.dict.proxy.ISysDictProxy
import io.kudos.ms.sys.client.domain.fallback.SysDomainFallback
import io.kudos.ms.sys.client.domain.proxy.ISysDomainProxy
import io.kudos.ms.sys.client.i18n.fallback.SysI18nFallback
import io.kudos.ms.sys.client.i18n.proxy.ISysI18nProxy
import io.kudos.ms.sys.client.locale.fallback.SysLocaleFallback
import io.kudos.ms.sys.client.locale.proxy.ISysLocaleProxy
import io.kudos.ms.sys.client.microservice.fallback.SysMicroServiceFallback
import io.kudos.ms.sys.client.microservice.fallback.SysSubSystemMicroServiceFallback
import io.kudos.ms.sys.client.microservice.proxy.ISysMicroServiceProxy
import io.kudos.ms.sys.client.microservice.proxy.ISysSubSystemMicroServiceProxy
import io.kudos.ms.sys.client.outline.fallback.SysOutLineFallback
import io.kudos.ms.sys.client.outline.proxy.ISysOutLineProxy
import io.kudos.ms.sys.client.param.fallback.SysParamFallback
import io.kudos.ms.sys.client.param.proxy.ISysParamProxy
import io.kudos.ms.sys.client.resource.fallback.SysResourceFallback
import io.kudos.ms.sys.client.resource.proxy.ISysResourceProxy
import io.kudos.ms.sys.client.system.fallback.SysSystemFallback
import io.kudos.ms.sys.client.system.proxy.ISysSystemProxy
import io.kudos.ms.sys.client.tenant.fallback.SysTenantFallback
import io.kudos.ms.sys.client.tenant.fallback.SysTenantLocaleFallback
import io.kudos.ms.sys.client.tenant.fallback.SysTenantResourceFallback
import io.kudos.ms.sys.client.tenant.fallback.SysTenantSystemFallback
import io.kudos.ms.sys.client.tenant.proxy.ISysTenantLocaleProxy
import io.kudos.ms.sys.client.tenant.proxy.ISysTenantProxy
import io.kudos.ms.sys.client.tenant.proxy.ISysTenantResourceProxy
import io.kudos.ms.sys.client.tenant.proxy.ISysTenantSystemProxy
import org.springframework.cloud.client.circuitbreaker.httpservice.HttpServiceFallback
import org.springframework.context.annotation.Configuration
import org.springframework.web.service.registry.ImportHttpServices

/**
 * Registration entry point for all sys interface clients.
 *
 * Replaces the 19 `@FeignClient(name = "sys-xxx", fallback = ...)` declarations that used to sit on
 * the proxy interfaces. Two things change in kind, not just in syntax:
 *
 * 1. **One group instead of 19 service names.** The old `name = "sys-dict"` / `"sys-tenant"` /
 *    `"sys-out-line"` … were never 19 microservices — they all resolve to the single sys atomic
 *    service (every path is `/api/internal/sys/...`). Collapsing them into the [GROUP] group removes
 *    the naming-drift hazard the client README flagged (`sys-out-line` vs `sys-accessruleip`) and
 *    makes "globally disable / re-time sys" a single config block instead of 19.
 * 2. **The target moves out of the code.** The proxy interfaces no longer name a service at all; the
 *    deployment picks it:
 *
 * ```yaml
 * spring:
 *   http:
 *     serviceclient:
 *       sys:                              # == SysClientAutoConfiguration.GROUP
 *         base-url: lb://kudos-ms-sys     # the sys api-internal service id in Nacos
 *         connect-timeout: 2s
 *         read-timeout: 5s
 * ```
 *
 * Without that `base-url`, the clients are registered but every call fails on an unresolvable URL —
 * there is no compile-time or startup-time check for it, so it belongs in the deployment checklist.
 *
 * **Both lists below are maintained by hand — adding a proxy means two edits here.** `@ImportHttpServices`
 * also supports `basePackages` scanning, which would remove the `types` list, but it is **not usable
 * in this codebase**: the scanner's filter matches interfaces by type-level `@HttpExchange` or by
 * *self-declared* annotated methods, and these proxies declare none (every method is inherited from
 * `ISys*Api` in `-common`). Adding a bare type-level `@HttpExchange` marker to make scanning work
 * breaks the application instead: Spring MVC 7's `RequestMappingHandlerMapping.isHandler()` treats
 * `@HttpExchange` as a controller marker, so the registered proxy beans get picked up as request
 * handlers and either collide with the real controllers ("Ambiguous mapping") or silently re-expose
 * the internal contract as HTTP endpoints. This is the same trap the `-common` build file already
 * warns about for type-level `@RequestMapping`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@ImportHttpServices(
    group = SysClientAutoConfiguration.GROUP,
    types = [
        ISysAccessRuleProxy::class,
        ISysAccessRuleIpProxy::class,
        ISysCacheProxy::class,
        ISysDataSourceProxy::class,
        ISysDictProxy::class,
        ISysDictItemProxy::class,
        ISysDomainProxy::class,
        ISysI18nProxy::class,
        ISysLocaleProxy::class,
        ISysMicroServiceProxy::class,
        ISysSubSystemMicroServiceProxy::class,
        ISysOutLineProxy::class,
        ISysParamProxy::class,
        ISysResourceProxy::class,
        ISysSystemProxy::class,
        ISysTenantProxy::class,
        ISysTenantLocaleProxy::class,
        ISysTenantResourceProxy::class,
        ISysTenantSystemProxy::class,
    ]
)
@HttpServiceFallback(value = SysAccessRuleFallback::class, service = [ISysAccessRuleProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysAccessRuleIpFallback::class, service = [ISysAccessRuleIpProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysCacheFallback::class, service = [ISysCacheProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysDataSourceFallback::class, service = [ISysDataSourceProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysDictFallback::class, service = [ISysDictProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysDictItemFallback::class, service = [ISysDictItemProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysDomainFallback::class, service = [ISysDomainProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysI18nFallback::class, service = [ISysI18nProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysLocaleFallback::class, service = [ISysLocaleProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysMicroServiceFallback::class, service = [ISysMicroServiceProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysSubSystemMicroServiceFallback::class, service = [ISysSubSystemMicroServiceProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysOutLineFallback::class, service = [ISysOutLineProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysParamFallback::class, service = [ISysParamProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysResourceFallback::class, service = [ISysResourceProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysSystemFallback::class, service = [ISysSystemProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysTenantFallback::class, service = [ISysTenantProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysTenantLocaleFallback::class, service = [ISysTenantLocaleProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysTenantResourceFallback::class, service = [ISysTenantResourceProxy::class], group = SysClientAutoConfiguration.GROUP)
@HttpServiceFallback(value = SysTenantSystemFallback::class, service = [ISysTenantSystemProxy::class], group = SysClientAutoConfiguration.GROUP)
open class SysClientAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ms-sys-client"

    companion object {
        /**
         * The HTTP service group name; must match the `spring.http.serviceclient.<group>` config key.
         */
        const val GROUP: String = "sys"
    }

}
