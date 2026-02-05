package io.kudos.ability.cache.common.core

import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

/**
 * 混合缓存初始化器
 *
 * 在Spring容器初始化完成后，初始化所有配置的缓存项（key-value 与 Hash）。
 *
 * 核心功能：
 * 1. 延迟初始化：实现SmartInitializingSingleton接口，在所有单例Bean初始化完成后执行
 * 2. 缓存初始化：调用MixCacheManager的initCacheAfterSystemInit，再调用MixHashCacheManager的initHashCacheAfterSystemInit
 *
 * @author K
 * @since 1.0.0
 */
class MixCacheInitializing : SmartInitializingSingleton {

    @Autowired
    @Qualifier("mixCacheManager")
    private val cacheManager: MixCacheManager? = null

    @Autowired(required = false)
    @Qualifier("mixHashCacheManager")
    private val hashCacheManager: MixHashCacheManager? = null

    override fun afterSingletonsInstantiated() {
        log.info("缓存项初始化...")
        cacheManager!!.initCacheAfterSystemInit()
        hashCacheManager?.initHashCacheAfterSystemInit()
    }

    private val log = LogFactory.getLog(this)
}