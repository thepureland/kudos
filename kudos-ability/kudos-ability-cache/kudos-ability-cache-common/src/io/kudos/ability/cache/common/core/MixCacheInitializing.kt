package io.kudos.ability.cache.common.core

import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

/**
 * 混合缓存初始化器
 * 在Spring容器初始化完成后，初始化所有配置的缓存项
 */
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