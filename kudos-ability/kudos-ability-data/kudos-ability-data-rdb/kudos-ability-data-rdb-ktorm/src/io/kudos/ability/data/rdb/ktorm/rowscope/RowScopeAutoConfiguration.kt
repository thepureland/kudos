package io.kudos.ability.data.rdb.ktorm.rowscope

import io.kudos.base.logger.LogFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
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
open class RowScopeAutoConfiguration : IComponentInitializer, ApplicationContextAware {

    private val log = LogFactory.getLog(this::class)

    /** The context this configuration belongs to; see [uninstall] for why identity matters. */
    private lateinit var context: ApplicationContext

    @Bean
    open fun rowScopeRegistry(providers: ObjectProvider<IRowScopePolicyProvider>): RowScopeRegistry =
        RowScopeRegistry(providers.orderedStream().toList())

    @Bean
    open fun rowScopeShadowRecorder(): RowScopeShadowRecorder = RowScopeShadowRecorder()

    /** The enforcer this configuration installed, so [uninstall] can recognise its own. */
    @Volatile
    private var installed: RowScopeEnforcer? = null

    @Bean
    open fun rowScopeEnforcer(
        registry: RowScopeRegistry,
        resolver: ObjectProvider<IRowScopeResolver>,
        properties: RowScopeProperties,
        recorder: RowScopeShadowRecorder,
    ): RowScopeEnforcer = RowScopeEnforcer(registry, resolver.ifAvailable, properties, recorder)
        .also { installed = it; RowScopeEnforcer.install(it) }

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
     * Uninstalls this context's enforcer when **this** context closes.
     *
     * [RowScopeEnforcer.current] is a static holder, so without this it outlives the context that
     * built it: a test suite that starts several Spring contexts leaves whichever one started last
     * filtering the queries of every other, and the resulting failures point at the wrong suite.
     *
     * Both guards below are load-bearing, because getting this wrong turns filtering **off** —
     * silently, application-wide, in a context nobody closed:
     *
     * - `event.applicationContext === context`. Spring republishes a child context's events to its
     *   parent, so this listener fires for closures of contexts it has nothing to do with.
     * - `current === installed`. The holder is last-writer-wins, so another context may have
     *   installed its own enforcer after this one; disarming it then would be worse than leaking.
     *
     * Asking the closing context for a `RowScopeEnforcer` bean is *not* a usable check for either:
     * `getBeanProvider` resolves through to the parent factory, so a child that declares no enforcer
     * of its own hands back the parent's and the identity comparison succeeds against a context that
     * is still very much alive.
     *
     * Ordered last so that beans destroyed after this event — `@PreDestroy`, `DisposableBean`,
     * later listeners — still run their queries filtered.
     */
    @Order(Ordered.LOWEST_PRECEDENCE)
    @EventListener(ContextClosedEvent::class)
    open fun uninstall(event: ContextClosedEvent) {
        if (event.applicationContext !== context) return
        if (RowScopeEnforcer.current === installed) {
            RowScopeEnforcer.install(null)
        }
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        context = applicationContext
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
