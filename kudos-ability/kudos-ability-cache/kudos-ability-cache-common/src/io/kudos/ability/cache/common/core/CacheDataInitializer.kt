package io.kudos.ability.cache.common.core

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import java.util.concurrent.CopyOnWriteArrayList


/**
 * Cache data initializer.
 *
 * Loads, at system startup, the data for all caches configured to be loaded on boot.
 *
 * Core capabilities:
 * 1. Collect cache handlers: gathers all AbstractCacheHandler instances after bean initialization.
 * 2. Deferred cache loading: [loadBootCaches] populates the boot-loaded caches, driven by [MixCacheInitializing].
 *
 * **Who triggers the load.** This class used to implement `SmartInitializingSingleton` and preload from
 * `afterSingletonsInstantiated`, exactly like [MixCacheInitializing] — but preloading can only work *after*
 * that class has created the cache instances. Spring invokes `SmartInitializingSingleton` callbacks in bean
 * registration order and does not sort them by `Ordered`, so the two ran in whichever order the definitions
 * happened to be registered. Losing that race was silent: `getCache` returned null, every
 * `KeyValueCacheKit.put` became a no-op, and the caches simply stayed empty with nothing logged. The ordering
 * is now explicit — [MixCacheInitializing] calls [loadBootCaches] once the cache managers are ready.
 *
 * Load conditions:
 * - The cache configuration exists (cacheConfig != null).
 * - Boot loading is configured (writeOnBoot == true).
 *
 * Why deferred:
 * - Ensures dependencies such as the database are initialized (e.g. Flyway has run).
 * - Avoids attempting to load the cache before the database is ready.
 * - Ensures all beans are ready.
 *
 * Caveats:
 * - Only caches configured with writeOnBoot=true are loaded.
 * - reloadAll(false) means do not clear the existing cache, just load.
 * - If the cache configuration does not exist, the handler is skipped.
 *
 * @author K
 * @since 1.0.0
 */
class CacheDataInitializer : BeanPostProcessor {

    /**
     * Copy-on-write because [postProcessAfterInitialization] can be reached from more than one thread: singleton
     * pre-instantiation is single-threaded, but lazy or prototype handlers created later are not.
     */
    private val cacheHandlers = CopyOnWriteArrayList<AbstractCacheHandler<*>>()

    /**
     * Post-initialization processing for beans.
     *
     * Collects every AbstractCacheHandler instance for later cache data loading.
     *
     * @param bean the bean instance
     * @param beanName the bean name
     * @return the processed bean instance
     */
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is AbstractCacheHandler<*>) {
            cacheHandlers.add(bean)
        }
        return bean
    }

    /**
     * Populates every cache configured with `writeOnBoot=true`.
     *
     * Must be called **after** the cache instances exist — [MixCacheInitializing] does so once
     * `MixCacheManager` / `MixHashCacheManager` have finished initializing. Calling it earlier is not an error
     * that surfaces: the writes would land on a missing cache and be dropped silently.
     *
     * Workflow:
     * 1. Iterate handlers: check each collected AbstractCacheHandler.
     * 2. Fetch cache configuration: via CacheKit.
     * 3. Check the boot-load flag: if writeOnBoot is true, perform the load.
     * 4. Load cache data: call reloadAll(false) to load (without clearing existing data).
     *
     * A handler that throws is logged and skipped rather than aborting startup: one unreachable data source
     * should degrade to a cold cache, not take the whole application down — the entries it would have held are
     * loaded on demand instead.
     *
     * Caveats:
     * - Only caches configured with writeOnBoot=true are loaded.
     * - Handlers whose cache config is missing are skipped.
     * - Loading can be time-consuming; configure boot-loaded caches with that in mind.
     */
    fun loadBootCaches() {
        cacheHandlers.forEach { handler ->
            val cacheConfig = runCatching { KeyValueCacheKit.getCacheConfig(handler.cacheName()) }.getOrNull()
            if (cacheConfig?.isWriteOnBoot != true) return@forEach
            try {
                handler.reloadAll(false)
            } catch (t: Throwable) {
                log.error(
                    t,
                    "Preloading cache on boot failed; this cache stays cold and will fill in on demand. cache={0} handler={1}",
                    handler.cacheName(), handler::class.java.name
                )
            }
        }
    }

    private val log = LogFactory.getLog(this::class)

}