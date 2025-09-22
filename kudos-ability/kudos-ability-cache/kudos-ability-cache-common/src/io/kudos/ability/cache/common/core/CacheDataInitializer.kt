package io.kudos.ability.cache.common.core

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.config.BeanPostProcessor


/**
 * 缓存数据初始化器。用于加载所有需要在启动时加载的缓存数据
 *
 * @author K
 * @since 1.0.0
 */
class CacheDataInitializer : BeanPostProcessor, SmartInitializingSingleton {

    private var cacheHandlers = mutableListOf<AbstractCacheHandler<*>>()

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is AbstractCacheHandler<*>) {
            cacheHandlers.add(bean)
        }
        return bean
    }

    // 所有非懒加载的单例 bean 都实例化完成后，再加载缓存数据。防止类似flyway还未初始化数据库, 就可能有地方先去库里加载缓存的事情发生。
    override fun afterSingletonsInstantiated() {
        cacheHandlers.forEach {
            val cacheConfig = CacheKit.getCacheConfig(it.cacheName())
            if (cacheConfig != null && cacheConfig.writeOnBoot == true) {
                it.reloadAll(false)
            }
        }
    }

}