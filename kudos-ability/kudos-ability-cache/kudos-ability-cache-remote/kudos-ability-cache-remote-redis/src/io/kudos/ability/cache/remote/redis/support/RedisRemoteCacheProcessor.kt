package io.kudos.ability.cache.remote.redis.support

import io.kudos.ability.cache.common.aop.keyvalue.process.IRemoteCacheProcessor
import io.kudos.ability.data.memdb.redis.RedisTemplates
import java.time.Duration

/**
 * Redis远程缓存处理器
 * 实现IRemoteCacheProcessor接口，提供基于Redis Hash结构的远程缓存读写功能
 */
class RedisRemoteCacheProcessor(private val redisTemplates: RedisTemplates) : IRemoteCacheProcessor {

    /**
     * 获取缓存数据
     * 
     * 从Redis Hash结构中读取指定key的缓存数据。
     * 
     * 数据结构：
     * - 使用Redis Hash结构存储缓存数据
     * - cacheKey是Hash的key，dataKey是Hash中的field
     * - 支持在同一个Hash中存储多个缓存项
     * 
     * @param cacheKey 缓存的主key（Hash的key）
     * @param dataKey 要获取的缓存项key（Hash的field）
     * @return 缓存数据，如果不存在返回null
     */
    override fun getCacheData(cacheKey: String, dataKey: String): Any? {
        return redisTemplates.defaultRedisTemplate.opsForHash<Any?, Any?>().get(cacheKey, dataKey)
    }

    /**
     * 写入缓存数据
     * 
     * 将数据写入Redis Hash结构，并设置缓存过期时间。
     * 
     * 工作流程：
     * 1. 检查数据是否为null，如果为null则不执行任何操作
     * 2. 将数据写入Hash结构：cacheKey作为Hash的key，dataKey作为field，o作为value
     * 3. 设置Hash的过期时间：使用expire命令设置整个Hash的过期时间
     * 
     * 数据结构：
     * - 使用Redis Hash结构存储缓存数据
     * - cacheKey是Hash的key，dataKey是Hash中的field，o是Hash中的value
     * - 支持在同一个Hash中存储多个缓存项，共享同一个过期时间
     * 
     * 过期时间：
     * - 过期时间应用到整个Hash结构，而不是单个field
     * - 如果Hash中还有其他field，它们也会共享同一个过期时间
     * - 时间单位：毫秒
     * 
     * 注意事项：
     * - 如果数据为null，不会写入缓存，也不会设置过期时间
     * - 过期时间会覆盖之前设置的过期时间
     * - 使用Hash结构可以高效地存储多个相关的缓存项
     * 
     * @param cacheKey 缓存的主key（Hash的key）
     * @param dataKey 缓存项的key（Hash的field）
     * @param o 要缓存的数据（Hash的value），如果为null则不执行任何操作
     * @param timeOut 缓存过期时间（毫秒）
     */
    override fun writeCacheData(cacheKey: String, dataKey: String, o: Any?, timeOut: Long) {
        val defaultRedisTemplate = redisTemplates.defaultRedisTemplate
        if (o != null) {
            redisTemplates.defaultRedisTemplate.opsForHash<Any?, Any?>().put(cacheKey, dataKey, o)
            defaultRedisTemplate.expire(cacheKey, Duration.ofMillis(timeOut))
        }
    }

    /**
     * 清除缓存
     * 
     * 根据参数决定是清除整个缓存还是清除指定key的缓存。
     * 
     * 工作流程：
     * 1. 检查cacheKey是否为空，为空则直接返回
     * 2. 根据参数b决定清除方式：
     *    - b=true：清除整个缓存（删除整个Hash结构）
     *    - b=false且s不为空：清除指定key的缓存（删除Hash中的指定field）
     *    - b=false且s为空：不执行任何操作
     * 
     * 清除方式：
     * - 清除整个缓存：使用delete命令删除整个cacheKey（Hash结构）
     * - 清除指定key：使用Hash的delete命令删除cacheKey中的指定field（s）
     * 
     * 数据结构：
     * - 使用Redis Hash结构存储缓存数据
     * - cacheKey是Hash的key，s是Hash中的field
     * - 支持在同一个Hash中存储多个缓存项
     * 
     * 注意事项：
     * - cacheKey为空时会直接返回，不执行任何操作
     * - 清除整个缓存会删除Hash中的所有数据，需谨慎使用
     * - 清除指定key只会删除Hash中的指定field，不影响其他数据
     * 
     * @param cacheKey 缓存的主key（Hash的key）
     * @param s 要清除的缓存项key（Hash的field），如果b=true则此参数无效
     * @param b 是否清除整个缓存，true表示清除整个缓存，false表示清除指定key
     */
    override fun clearCache(cacheKey: String, s: String, b: Boolean) {
        if (cacheKey.isBlank()) {
            return
        }
        if (b) {
            redisTemplates.defaultRedisTemplate.delete(cacheKey)
        } else if (s.isNotBlank()) {
            redisTemplates.defaultRedisTemplate.opsForHash<Any?, Any?>().delete(cacheKey, s)
        }
    }

}
