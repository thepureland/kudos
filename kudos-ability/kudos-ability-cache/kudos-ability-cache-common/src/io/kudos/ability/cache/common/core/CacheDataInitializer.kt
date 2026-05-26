package io.kudos.ability.cache.common.core

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.config.BeanPostProcessor


/**
 * Cache data initializer.
 *
 * Loads, at system startup, the data for all caches configured to be loaded on boot.
 *
 * Core capabilities:
 * 1. Collect cache handlers: gathers all AbstractCacheHandler instances after bean initialization.
 * 2. Deferred cache loading: loads the boot-loaded caches after all singleton beans have been initialized.
 *
 * Workflow:
 * 1. Bean post-processing: collects all cache handlers in postProcessAfterInitialization.
 * 2. Deferred init: implements SmartInitializingSingleton, executed once all singleton beans are initialized.
 * 3. Check configuration: iterates the handlers and checks whether writeOnBoot=true is configured.
 * 4. Load caches: for caches with boot-load enabled, calls reloadAll(false) to load the data.
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
class CacheDataInitializer : BeanPostProcessor, SmartInitializingSingleton {

    private var cacheHandlers = mutableListOf<AbstractCacheHandler<*>>()

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
     * Loads cache data once all singleton beans are initialized.
     *
     * Iterates the collected handlers and, for caches with boot-load configured, performs the data load.
     *
     * Workflow:
     * 1. Iterate handlers: check each collected AbstractCacheHandler.
     * 2. Fetch cache configuration: via CacheKit.
     * 3. Check the boot-load flag: if writeOnBoot is true, perform the load.
     * 4. Load cache data: call reloadAll(false) to load (without clearing existing data).
     *
     * Why deferred:
     * - Ensures dependencies such as the database are initialized.
     * - Avoids loading the cache before tools such as Flyway have initialized the database.
     * - Ensures all beans are ready.
     *
     * Loading strategy:
     * - reloadAll(false): does not clear existing data; loads new data directly.
     * - Existing cache entries are overwritten by the new data.
     *
     * Caveats:
     * - Only caches configured with writeOnBoot=true are loaded.
     * - Handlers whose cache config is missing are skipped.
     * - Loading can be time-consuming; configure boot-loaded caches with that in mind.
     */
    // Load cache data only after all non-lazy singleton beans have been instantiated. This prevents,
    // for example, code from querying the database for cache data before Flyway has initialized the database.
    override fun afterSingletonsInstantiated() {
        cacheHandlers.forEach {
            val cacheConfig = KeyValueCacheKit.getCacheConfig(it.cacheName())
            if (cacheConfig?.isWriteOnBoot == true) {
                it.reloadAll(false)
            }
        }
    }

}