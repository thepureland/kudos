package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.core.keyvalue.MixCache
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.notify.CacheOperatorVo
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import org.springframework.cache.Cache
import org.springframework.stereotype.Component
import kotlin.reflect.KClass


/**
 * 缓存工具类
 *
 * @author K
 * @since 1.0.0
 */
@Component
object CacheKit {

    private val log = LogFactory.getLog(this)

    /**
     * 是否开启缓存。必须是全局开关和指定的缓存开关都开启，才算开启
     *
     * @param cacheName 缓存名称
     * @return true: 开启缓存，false：未开启缓存
     * @author K
     * @since 1.0.0
     */
    fun isCacheActive(cacheName: String): Boolean {
        val cacheConfigProvider = getCacheConfigProvider()
        val cacheConfig = cacheConfigProvider.getCacheConfig(cacheName)
        if (cacheConfig != null) {
            return cacheConfig.active == true
        }
        return false
    }

    /**
     * 根据名称获取缓存
     *
     * @param name 缓存名称
     * @return 缓存对象
     * @author K
     * @since 1.0.0
     */
    fun getCache(name: String): Cache? {
        val cacheManager = getCacheManager() ?: return null
        val cache: Cache? = cacheManager.getCache(name)
        if (cache == null) {
            log.error("缓存【$name】不存在！")
        }
        return cache
    }

    /**
     * 获取缓存中指定key的值
     *
     * @param cacheName  缓存名称
     * @param key        缓存key
     * @param valueClass 缓存key对应的值的类型
     * @return 缓存key对应的值
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> getValue(cacheName: String, key: Any, valueClass: KClass<T>): T? {
        val cache = getCache(cacheName) ?: return null
        return cache.get<T>(key, valueClass.java)
    }

    /**
     * 获取缓存中指定key的值
     *
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @return 缓存key对应的值
     * @author K
     * @since 1.0.0
     */
    fun getValue(cacheName: String, key: Any): Any? {
        val cache = getCache(cacheName) ?: return null
        val value = cache.get(key)
        return value?.get()
    }

    /**
     * 写入缓存
     *
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @param value     要缓存的值
     * @author K
     * @since 1.0.0
     */
    fun put(cacheName: String, key: Any, value: Any?) {
        if (!isCacheActive(cacheName)) {
            return
        }
        val cache = getCache(cacheName)
        cache?.put(key, value)
    }

    /**
     * 如果不存在，就写入缓存
     *
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @param value     要缓存的值
     * @author K
     * @since 1.0.0
     */
    fun putIfAbsent(cacheName: String, key: Any, value: Any?) {
        if (!isCacheActive(cacheName)) {
            return
        }
        val cache = getCache(cacheName)
        cache?.putIfAbsent(key, value)
    }

    /**
     * 踢除缓存依赖消息通知
     *
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @author K
     * @since 1.0.0
     */
    fun evict(cacheName: String, key: Any) {
        if (!isCacheActive(cacheName)) {
            return
        }
        val cache = getCache(cacheName) as? MixCache? ?: return
        //如果是本地缓存，则需要依赖通知发布删除
        if (CacheStrategy.SINGLE_LOCAL == cache.strategy) {
            val coVo = CacheOperatorVo(CacheOperatorVo.TYPE_EVICT, cacheName, key)
            coVo.doNotify()
        } else {
            doEvict(cacheName, key)
        }
    }

    /**
     * 踢除缓存
     *
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @author K
     * @since 1.0.0
     */
    fun doEvict(cacheName: String, key: Any) {
        if (!isCacheActive(cacheName)) {
            return
        }
        val cache = getCache(cacheName)
        cache?.evict(key)
    }

    /**
     * 清空缓存通知发送消息通知
     *
     * @param cacheName 缓存名称
     * @author K
     * @since 1.0.0
     */
    fun clear(cacheName: String) {
        if (!isCacheActive(cacheName)) {
            return
        }
        val cache = getCache(cacheName) as? MixCache? ?: return
        //如果是本地缓存，则需要依赖通知发布删除
        if (CacheStrategy.SINGLE_LOCAL == cache.strategy) {
            val coVo = CacheOperatorVo(CacheOperatorVo.TYPE_CLEAR, cacheName, null)
            coVo.doNotify()
        } else {
            doClear(cacheName)
        }
    }

    /**
     * 清空缓存
     *
     * @param cacheName 缓存名称
     * @author K
     * @since 1.0.0
     */
    fun doClear(cacheName: String) {
        if (!isCacheActive(cacheName)) {
            return
        }
        val cache = getCache(cacheName)
        cache?.clear()
    }

    /**
     * 是否在新增或更新后，立即回写缓存
     *
     * @param cacheName 缓存名称
     * @return true: 立即回写缓存, 反之为false。缓存不存在也返回false
     * @author K
     * @since 1.0.0
     */
    fun isWriteInTime(cacheName: String): Boolean {
        if (!isCacheActive(cacheName)) {
            return false
        }
        val cacheConfig = getCacheConfig(cacheName) ?: return false
        return cacheConfig.writeInTime == true
    }

    /**
     * 返回指定名称的缓存配置信息
     *
     * @param cacheName 缓存名称
     * @return 缓存配置信息。找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getCacheConfig(cacheName: String): CacheConfig? {
        if (!isCacheActive(cacheName)) {
            return null
        }
        val cacheConfigProvider = getCacheConfigProvider()
        val cacheConfig = cacheConfigProvider.getCacheConfig(cacheName)
        if (cacheConfig == null) {
            log.warn("缓存【$cacheName】不存在！")
        }
        return cacheConfig
    }

    /**
     * 重新加载缓存
     *
     * @param cacheName 缓存名
     * @param key       key
     */
    fun reload(cacheName: String, key: String) {
        if (!isCacheActive(cacheName)) {
            return
        }
        val cacheConfig = getCacheConfig(cacheName) ?: return
        if (cacheConfig.writeOnBoot == true) {
            val beansOfType = SpringKit.getBeansOfType<AbstractKeyValueCacheHandler<*>>()
            beansOfType.values.forEach {
                if (it.cacheName() == cacheName) {
                    it.reload(key)
                }
            }
        } else {
            evict(cacheName, key)
        }
    }

    /**
     * 重新加载所有缓存
     *
     * @param cacheName 缓存名
     */
    fun reloadAll(cacheName: String) {
        if (!isCacheActive(cacheName)) {
            return
        }
        val cacheConfig = getCacheConfig(cacheName) ?: return
        if (cacheConfig.writeOnBoot == true) {
            val beansOfType = SpringKit.getBeansOfType<AbstractKeyValueCacheHandler<*>>()
            beansOfType.values.forEach {
                if (it.cacheName() == cacheName) {
                    it.reloadAll(true)
                }
            }
        } else {
            clear(cacheName)
        }
    }

    /**
     * 清理缓存开头的key
     * @param cacheName 缓存name
     * @param keyPattern key开头
     */
    fun evictByPattern(cacheName: String, keyPattern: String) {
        if (!isCacheActive(cacheName)) {
            return
        }
        val cacheManager = getCacheManager() ?: return
        cacheManager.evictByPattern(cacheName, keyPattern)
    }

    /**
     * 获取缓存管理器
     * 
     * 如果缓存未启用（kudos.ability.cache.enabled=false），mixCacheManager Bean 不会被创建，
     * 此时返回 null，调用方需要处理 null 的情况。
     *
     * @return MixCacheManager，如果缓存未启用则返回 null
     */
    private fun getCacheManager(): MixCacheManager? {
        return SpringKit.getBeanOrNull("mixCacheManager") as? MixCacheManager
    }

    /**
     * 获取缓存配置服务
     *
     * @return ICacheConfigProvider
     */
    private fun getCacheConfigProvider(): ICacheConfigProvider {
        return SpringKit.getBean<ICacheConfigProvider>()
    }

}