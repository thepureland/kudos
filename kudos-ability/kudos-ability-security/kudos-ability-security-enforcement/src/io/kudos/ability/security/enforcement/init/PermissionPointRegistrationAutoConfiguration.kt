package io.kudos.ability.security.enforcement.init

import io.kudos.ability.security.enforcement.point.ConventionalPermissionPointNaming
import io.kudos.ability.security.enforcement.point.IPermissionPointContributor
import io.kudos.ability.security.enforcement.point.IPermissionPointNaming
import io.kudos.ability.security.enforcement.point.IPermissionPointStore
import io.kudos.ability.security.enforcement.point.PermissionPointRegistrar
import io.kudos.ability.security.enforcement.point.PermissionPointRegistrationProperties
import io.kudos.context.init.IComponentInitializer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping


/**
 * Wires permission-point registration.
 *
 * **Separate from [EnforcementAutoConfiguration] on purpose.** That one is gated on
 * `kudos.ability.security.enforcement.enabled`, which is false by default and must stay that way.
 * If registration lived there, a deployment could only populate its registry by first enabling
 * enforcement — and enforcement with an empty registry denies everything. The two switches are
 * ordered: register, verify in shadow mode, then enforce.
 *
 * Still gated on an [IPermissionPointStore] bean: with nowhere to write, there is nothing to do.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "kudos.ability.security.point-registration",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(PermissionPointRegistrationProperties::class)
open class PermissionPointRegistrationAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ability-security-enforcement-point-registration"

    @Bean
    @ConditionalOnMissingBean(IPermissionPointNaming::class)
    open fun conventionalPermissionPointNaming(): IPermissionPointNaming = ConventionalPermissionPointNaming()

    /**
     * The store is resolved through an [ObjectProvider] rather than gated with `@ConditionalOnBean`.
     *
     * That gate would be evaluated while auto-configurations are being processed, against a bean
     * contributed by a *component scan* in another module — an ordering question Spring explicitly
     * warns not to depend on. When it loses, nothing is created and nothing is logged: the registrar
     * is simply absent, permission points never register, and the URL filter then refuses every
     * endpoint as unregistered. A silent no-op that turns into a total outage one flag later is not
     * a failure mode worth keeping for tidiness.
     *
     * So the bean is always created and decides at run time, when every bean definitely exists.
     */
    @Bean
    @ConditionalOnMissingBean(PermissionPointRegistrar::class)
    open fun permissionPointRegistrar(
        handlerMappings: ObjectProvider<RequestMappingHandlerMapping>,
        contributors: ObjectProvider<IPermissionPointContributor>,
        naming: IPermissionPointNaming,
        store: ObjectProvider<IPermissionPointStore>,
        properties: PermissionPointRegistrationProperties,
    ): PermissionPointRegistrar = PermissionPointRegistrar(
        handlerMappings = handlerMappings.orderedStream().toList(),
        contributors = contributors.orderedStream().toList(),
        naming = naming,
        storeProvider = { store.getIfAvailable() },
        properties = properties,
    )
}
