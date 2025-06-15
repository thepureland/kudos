package io.kudos.ability.cache.common.kit


import io.kudos.base.logger.LogFactory
import org.soul.ability.cache.common.support.CacheConfig
import org.soul.ability.cache.common.tools.CacheTool
import org.springframework.cache.Cache
import kotlin.reflect.KClass


/**
 * 缓存工具类
 *
 * @author K
 * @since 1.0.0
 */
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
    fun isCacheActive(cacheName: String): Boolean = CacheTool.isCacheActive(cacheName)

    /**
     * 根据名称获取缓存
     *
     * @param name 缓存名称
     * @return 缓存对象
     * @author K
     * @since 1.0.0
     */
    fun getCache(name: String): Cache? = CacheTool.getCache(name)

    /**
     * 获取缓存中指定key的值
     *
     * @param cacheName 缓存名称
     * @param key 缓存key
     * @param valueClass 缓存key对应的值的类型
     * @return 缓存key对应的值
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> getValue(cacheName: String, key: Any, valueClass: KClass<T>): T? =
        CacheTool.getValue(cacheName, key, valueClass.java)

    /**
     * 获取缓存中指定key的值
     *
     * @param cacheName 缓存名称
     * @param key 缓存key
     * @return 缓存key对应的值
     * @author K
     * @since 1.0.0
     */
    fun getValue(cacheName: String, key: Any): Any? = CacheTool.getValue(cacheName, key)

    /**
     * 写入缓存
     *
     * @param cacheName 缓存名称
     * @param key 缓存key
     * @param value 要缓存的值
     * @author K
     * @since 1.0.0
     */
    fun put(cacheName: String, key: Any, value: Any?) = CacheTool.put(cacheName, key, value)

    /**
     * 如果不存在，就写入缓存
     *
     * @param cacheName 缓存名称
     * @param key 缓存key
     * @param value 要缓存的值
     * @author K
     * @since 1.0.0
     */
    fun putIfAbsent(cacheName: String, key: Any, value: Any?) = CacheTool.putIfAbsent(cacheName, key, value)

    /**
     * 踢除缓存
     *
     * @param cacheName 缓存名称
     * @param key 缓存key
     * @author K
     * @since 1.0.0
     */
    fun evict(cacheName: String, key: Any) = CacheTool.evict(cacheName, key)

    /**
     * 清空缓存
     *
     * @param cacheName 缓存名称
     * @author K
     * @since 1.0.0
     */
    fun clear(cacheName: String) = CacheTool.clear(cacheName)

    /**
     * 是否在新增或更新后，立即回写缓存
     *
     * @param cacheName 缓存名称
     * @return true: 立即回写缓存, 反之为false。缓存不存在也返回false
     * @author K
     * @since 1.0.0
     */
    fun isWriteInTime(cacheName: String): Boolean = CacheTool.isWriteInTime(cacheName)

    /**
     * 返回指定名称的缓存配置信息
     *
     * @param cacheName 缓存名称
     * @return 缓存配置信息。找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getCacheConfig(cacheName: String): CacheConfig? = CacheTool.getCacheConfig(cacheName)

}