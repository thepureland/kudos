package io.kudos.ability.cache.remote.redis.support

import io.kudos.ability.cache.common.aop.process.IRemoteCacheProcessor
import io.kudos.ability.data.memdb.redis.KudosRedisTemplate
import java.time.Duration

/**
 * Redis远程缓存处理器
 * 实现IRemoteCacheProcessor接口，提供基于Redis Hash结构的远程缓存读写功能
 */
class RedisRemoteCacheProcessor(private val kudosRedisTemplate: KudosRedisTemplate) : IRemoteCacheProcessor {

    override fun getCacheData(cacheKey: String, dataKey: String): Any? {
        return kudosRedisTemplate.defaultRedisTemplate.opsForHash<Any?, Any?>().get(cacheKey, dataKey)
    }

    override fun writeCacheData(cacheKey: String, dataKey: String, o: Any?, timeOut: Long) {
        val defaultRedisTemplate = kudosRedisTemplate.defaultRedisTemplate
        if (o != null) {
            kudosRedisTemplate.defaultRedisTemplate.opsForHash<Any?, Any?>().put(cacheKey, dataKey, o)
            defaultRedisTemplate.expire(cacheKey, Duration.ofMillis(timeOut))
        }
    }

    override fun clearCache(cacheKey: String, s: String, b: Boolean) {
        if (cacheKey.isBlank()) {
            return
        }
        if (b) {
            kudosRedisTemplate.defaultRedisTemplate.delete(cacheKey)
        } else if (s.isNotBlank()) {
            kudosRedisTemplate.defaultRedisTemplate.opsForHash<Any?, Any?>().delete(cacheKey, s)
        }
    }

}
