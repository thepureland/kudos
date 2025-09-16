package io.kudos.ability.cache.common.core

import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

class MixCacheInitializing : SmartInitializingSingleton {
    @Autowired
    @Qualifier("mixCacheManager")
    private val cacheManager: MixCacheManager? = null

    override fun afterSingletonsInstantiated() {
        log.info("缓存项初始化...")
        cacheManager!!.initCacheAfterSystemInit()
    }

    private val log = LogFactory.getLog(this)
}