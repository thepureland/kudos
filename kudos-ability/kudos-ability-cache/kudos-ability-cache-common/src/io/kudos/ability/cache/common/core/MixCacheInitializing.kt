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

    override fun afterSingletonsInstantiated() {
        log.info("Initializing cache items...")
        if (!::cacheManager.isInitialized) {
            log.error("MixCacheManager is not injected; cache item initialization skipped. Please check whether the mixCacheManager bean is declared.")
            return
        }
        cacheManager.initCacheAfterSystemInit()
        hashCacheManager?.initHashCacheAfterSystemInit()
    }

    private val log = LogFactory.getLog(this::class)
}