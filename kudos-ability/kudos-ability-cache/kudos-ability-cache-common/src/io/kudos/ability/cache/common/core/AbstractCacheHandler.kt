package io.kudos.ability.cache.common.core

import io.kudos.context.kit.SpringKit


/**
 * Base class for business-defined cache handlers — implement [cacheName] / [reloadAll] to plug into the framework.
 *
 * Solves two recurring problems:
 *  - **Self-invocation defeats AOP**: calling this class's `@Cacheable` method via `this.fooCached()` directly bypasses AOP.
 *    [getSelf] resolves the proxy instance of this class from the Spring container, so callers writing `getSelf<MyHandler>().fooCached(...)`
 *    are guaranteed to go through the proxy.
 *  - **Unified reload semantics**: [reloadAll] is implemented by subclasses; the framework calls it uniformly during startup and after manual invalidations,
 *    while subclasses only care about how to reload their data.
 *
 * @author K
 * @since 1.0.0
 */
abstract class AbstractCacheHandler<T> {

    /**
     * Returns the cache name.
     *
     * @return cache name
     */
    abstract fun cacheName(): String

    /**
     * Reloads all caches.
     *
     * @param clear whether to clear before reload
     */
    abstract fun reloadAll(clear: Boolean = true)

    /**
     * When the business registers the same handler class as multiple beans (with different qualifiers), subclasses **must**
     * override this method to return the bean name that the current instance should resolve to — otherwise [selfProxy]
     * will hit `NoUniqueBeanDefinitionException` when looking up by type.
     *
     * Returns `null` by default, meaning "look up uniquely by type"; covers the typical case (one handler class registered as a single bean).
     */
    protected open fun selfBeanName(): String? = null

    /**
     * Self proxy instance; [lazy] delegate provides a thread-safe first-time resolution.
     *
     * Resolution order:
     *  1. If [selfBeanName] returns non-null -> resolve by name (explicit selection for multi-bean scenarios).
     *  2. Otherwise resolve by type: [org.springframework.context.ApplicationContext.getBean] returns directly when only one bean of the type exists;
     *     when there are several, [io.kudos.context.kit.SpringKit.getBean] arbitrates via `@Primary` / same-name matching rules; if none applies,
     *     throws `NoUniqueBeanDefinitionException` — in which case the business side should override [selfBeanName].
     *
     * Named `selfProxy` to avoid colliding with the JVM signature of the [getSelf] function (the property getter would default to `getSelf()`).
     */
    private val selfProxy: AbstractCacheHandler<*> by lazy {
        val name = selfBeanName()
        if (name != null) {
            @Suppress("UNCHECKED_CAST")
            SpringKit.getBean(name) as AbstractCacheHandler<*>
        } else {
            SpringKit.getBean(this::class)
        }
    }

    /**
     * Returns the self instance to work around Spring AOP self-invocation (here for @Cacheable and @BatchCacheable),
     * where directly calling such a method within the same class bypasses the AOP proxy.
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <S : AbstractCacheHandler<*>?> getSelf() : S = selfProxy as S

}