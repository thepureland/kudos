package io.kudos.ability.cache.common.core

import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 * Mixed cache initializer.
 *
 * After the Spring context is initialized, initializes all configured cache items (key-value and Hash).
 *
 * Core responsibilities:
 * 1. Deferred initialization: implements SmartInitializingSingleton to run after all singleton beans are initialized.
 * 2. Cache initialization: invokes MixCacheManager.initCacheAfterSystemInit, then MixHashCacheManager.initHashCacheAfterSystemInit.
 * 3. Boot preloading: drives [CacheDataInitializer.loadBootCaches] once the caches above exist.
 *
 * Step 3 is invoked from here on purpose. [CacheDataInitializer] used to preload from its own
 * `SmartInitializingSingleton` callback, but Spring runs those callbacks in bean-registration order without
 * honouring `Ordered`, so preloading could run before this class had created any cache. The failure was silent
 * — writes went to a cache that did not exist yet and were dropped. Sequencing both steps in one place removes
 * the race outright.
 *
 * @author K
 * @since 1.0.0
 */
@Component
class MixCacheInitializing : SmartInitializingSingleton {

    // mixCacheManager is required. Using lateinit so that a Spring injection failure yields a clear "not initialized" error instead of NPE.
    @Autowired
    @Qualifier("mixCacheManager")
    private lateinit var cacheManager: MixCacheManager

    // mixHashCacheManager is optional: users may not enable Hash caching.
    @Autowired(required = false)
    @Qualifier("mixHashCacheManager")
    private var hashCacheManager: MixHashCacheManager? = null

    // Optional: absent when the application declares no cache handlers / disables boot preloading.
    @Autowired(required = false)
    private var cacheDataInitializer: CacheDataInitializer? = null

    override fun afterSingletonsInstantiated() {
        log.info("Initializing cache items...")
        if (!::cacheManager.isInitialized) {
            log.error("MixCacheManager is not injected; cache item initialization skipped. Please check whether the mixCacheManager bean is declared.")
            return
        }
        cacheManager.initCacheAfterSystemInit()
        hashCacheManager?.initHashCacheAfterSystemInit()
        // Only now do the cache instances exist, so this is the earliest point at which preloading can write
        // anywhere. Ordering it explicitly rather than via a second SmartInitializingSingleton is deliberate;
        // see the class KDoc.
        cacheDataInitializer?.loadBootCaches()
    }

    private val log = LogFactory.getLog(this::class)
}