package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.context.core.KudosContextHolder
import org.springframework.cache.Cache
import kotlin.reflect.KClass

/**
 * 租户缓存工具类
 * 提供多租户环境下的缓存操作工具方法，支持租户级别的缓存隔离
 */
object TenantCacheTool {

    /**
     * 是否开启缓存。必须是全局开关和指定的缓存开关都开启，才算开启
     *
     * @param cacheName 缓存名称
     * @return true: 开启缓存，false：未开启缓存
     * @author K
     * @since 1.0.0
     */
    fun isCacheActive(cacheName: String): Boolean {
        return CacheKit.isCacheActive(cacheName)
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
        return CacheKit.getCache(name)
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
    fun <T: Any> getValue(cacheName: String, key: Any, valueClass: KClass<T>): T? {
        return CacheKit.getValue(cacheName, getTenantKey(key), valueClass)
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
        return CacheKit.getValue(cacheName, getTenantKey(key))
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
        CacheKit.put(cacheName, getTenantKey(key), value)
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
        CacheKit.putIfAbsent(cacheName, getTenantKey(key), value)
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
        doEvict(cacheName, key)
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
        CacheKit.doEvict(cacheName, getTenantKey(key))
    }

    /**
     * 清空缓存通知发送消息通知
     *
     * @param cacheName 缓存名称
     * @author K
     * @since 1.0.0
     */
    fun clear(cacheName: String) {
        CacheKit.clear(cacheName)
    }

    /**
     * 清空缓存
     *
     * @param cacheName 缓存名称
     * @author K
     * @since 1.0.0
     */
    fun doClear(cacheName: String) {
        CacheKit.doClear(cacheName)
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
        return CacheKit.isWriteInTime(cacheName)
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
        return CacheKit.getCacheConfig(cacheName)
    }

    /**
     * 重新加载缓存
     *
     * @param cacheName 缓存名
     * @param key       key
     */
    fun reload(cacheName: String, key: String) {
        CacheKit.reload(cacheName, getTenantKey(key))
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
        val realKeyPattern = getTenantKey(keyPattern)
        CacheKit.evictByPattern(cacheName, realKeyPattern)
    }

    /**
     * 重新加载所有缓存
     *
     * @param cacheName 缓存名
     */
    fun reloadAll(cacheName: String) {
        CacheKit.reloadAll(cacheName)
    }

    private fun getTenantKey(key: Any): String {
        val tenantId = KudosContextHolder.get().tenantId
        return "$tenantId::$key"
    }
}
