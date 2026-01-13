package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.context.core.KudosContextHolder
import org.springframework.cache.Cache
import kotlin.reflect.KClass

/**
 * 租户缓存工具类
 * 
 * 提供多租户环境下的缓存操作工具方法，支持租户级别的缓存隔离。
 * 
 * 核心功能：
 * 1. 租户键生成：自动在缓存key前添加租户ID，格式为"租户ID::原始key"
 * 2. 缓存隔离：通过租户键确保不同租户的缓存数据相互隔离
 * 3. 缓存操作：提供完整的缓存操作接口（get、put、evict、clear等）
 * 4. 模式删除：支持按模式删除缓存，自动添加租户前缀
 * 
 * 租户键机制：
 * - 所有缓存操作都会自动从KudosContext中获取当前租户ID
 * - 在原始key前添加"租户ID::"前缀，形成租户隔离的key
 * - 例如：租户ID为"1001"，原始key为"user:123"，实际key为"1001::user:123"
 * 
 * 使用场景：
 * - 多租户SaaS应用，需要确保不同租户的数据隔离
 * - 共享缓存存储，但需要按租户隔离数据
 * - 避免租户间的缓存数据相互干扰
 * 
 * 注意事项：
 * - 依赖KudosContext中的tenantId，如果tenantId为空可能导致缓存key异常
 * - 所有缓存操作都会自动应用租户隔离，无需手动添加租户前缀
 * - 清空缓存操作（clear）会清除所有租户的缓存，需谨慎使用
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
