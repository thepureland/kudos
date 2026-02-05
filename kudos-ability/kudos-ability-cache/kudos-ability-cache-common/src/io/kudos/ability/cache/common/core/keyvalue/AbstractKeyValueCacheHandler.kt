package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.core.AbstractCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.logger.LogFactory

/**
 * key-value型缓存处理器抽象类
 *
 * @param T 缓存项类型
 * @author K
 * @since 1.0.0
 */
abstract class AbstractKeyValueCacheHandler<T> : AbstractCacheHandler<T>() {

    /**
     * 检测缓存key是否存在
     *
     * @param key 缓存的key
     * @return true: 存在于缓存中，false: 不存在
     */
    open fun isExists(key: String): Boolean {
        return value(key) != null
    }

    /**
     * 获取指定缓存key对应的值
     *
     * @param key 缓存的key
     * @return 缓存key对应的值
     */
    open fun value(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return CacheKit.getValue(cacheName(), key) as T?
    }

    /**
     * 踢除指定key的缓存
     *
     * @param key 缓存的key
     */
    open fun evict(key: String) {
        CacheKit.evict(cacheName(), key)
        log.info("手动踢除名称为${cacheName()}，key为${key}的缓存。")
    }

    /**
     * 清除所有缓存
     */
    open fun clear() {
        CacheKit.clear(cacheName())
        log.info("手动清除名称为${cacheName()}的所有缓存。")
    }

    /**
     * 重载指定key的缓存
     *
     * @param key 缓存的key
     */
    open fun reload(key: String) {
        evict(key)
        log.info("手动重载名称为${cacheName()}，key为${key}的缓存...")
        val role = doReload(key)
        if (role == null) {
            log.info("数据库中已不存在对应数据！")
        } else {
            log.info("重载成功。")
        }
    }

    /**
     * 执行重载指定key的缓存
     *
     * @param key 缓存的key
     * @return 缓存key对应的值。如果找不到，集合类型返回空集合，其它的返回null。
     */
    protected abstract fun doReload(key: String): T?

    private val log = LogFactory.getLog(this)

}