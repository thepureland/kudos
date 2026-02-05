package io.kudos.ability.cache.common.core.hash

import io.kudos.ability.cache.common.core.AbstractCacheHandler
import io.kudos.ability.cache.common.kit.HashCacheKit

/**
 * hash型缓存处理器抽象类
 *
 * @param T 缓存项类型
 * @author K
 * @since 1.0.0
 */
abstract class AbstractHashCacheHandler<T>: AbstractCacheHandler<T>() {


    protected fun hashCache() = HashCacheKit.getHashCache(cacheName())

}