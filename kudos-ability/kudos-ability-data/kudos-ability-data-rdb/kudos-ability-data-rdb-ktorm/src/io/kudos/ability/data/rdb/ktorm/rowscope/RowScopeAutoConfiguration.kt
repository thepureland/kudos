package io.kudos.ability.data.rdb.ktorm.rowscope

import io.kudos.base.logger.LogFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.core.env.Environment


/**
 * Wires row-level data-scope filtering.
 *
 * Nothing is gated on a property here: the enforcer is always built, and it is the enforcer that
 * reads [RowScopeProperties.enabled]. That way the startup report below runs — and tells a
 * deployment what *would* be filtered — before anybody switches filtering on.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RowScopeProperties::class)
open class RowScopeAutoConfiguration : IComponentInitializer {

    private val log = LogFactory.getLog(this::class)

    @Bean
    open fun rowScopeRegistry(providers: ObjectProvider<IRowScopePolicyProvider>): RowScopeRegistry =
        RowScopeRegistry(providers.orderedStream().toList())

    @Bean
    open fun rowScopeShadowRecorder(): RowScopeShadowRecorder = RowScopeShadowRecorder()

    @Bean
    open fun rowScopeEnforcer(
        registry: RowScopeRegistry,
        resolver: ObjectProvider<IRowScopeResolver>,
        properties: RowScopeProperties,
        recorder: RowScopeShadowRecorder,
    ): RowScopeEnforcer = RowScopeEnforcer(registry, resolver.ifAvailable, properties, recorder)
        .also { RowScopeEnforcer.install(it) }

    /** Production must never silently run with tenant row filtering disabled or observe-only. */
    @Bean
    open fun rowScopeProductionSafetyCheck(
        properties: RowScopeProperties,
        environment: Environment,
    ): SmartInitializingSingleton = SmartInitializingSingleton {
        val production = environment.activeProfiles.any { active ->
            properties.productionProfiles.any { it.equals(active, ignoreCase = true) }
        }
        if (production && properties.failOnInsecureProduction) {
            check(properties.enabled) { "Row-scope enforcement is disabled in a production profile." }
            check(!properties.shadowMode) { "Row-scope enforcement is still in shadow mode in a production profile." }
        }
    }

    /**
     * Reports the configuration on startup, because both halves of it are invisible otherwise:
     * whether filtering is on at all, and which entities have declared themselves into it.
     */
    @EventListener(ContextRefreshedEvent::class)
    open fun report(event: ContextRefreshedEvent) {
        val properties = event.applicationContext.getBean(RowScopeProperties::class.java)
        if (!properties.enabled) {
            log.info("[row-scope] disabled; no query is filtered. Enable it in shadow mode first.")
            return
        }
        val mode = if (properties.shadowMode) "SHADOW (logging only, nothing filtered)" else "ENFORCING"
        val registry = event.applicationContext.getBean(RowScopeRegistry::class.java)
        log.info("[row-scope] ${mode}; declared entities: ${registry.declaredEntities().map { it.simpleName }}")
    }

    override fun getComponentName() = "kudos-ability-data-rdb-ktorm-row-scope"
}
