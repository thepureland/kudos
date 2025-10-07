package io.kudos.ability.cache.common.core

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.notice.CacheMessage
import io.kudos.ability.cache.common.notice.ICacheMessageHandler
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import jakarta.annotation.Nullable
import org.springframework.cache.Cache
import java.util.concurrent.Callable

/**
 * 混合缓存(两级缓存: 本地+远程)
 *
 * @author K
 * @since 1.0.0
 */
class MixCache(
    val strategy: CacheStrategy,
    private val localCache: Cache?,
    private val remoteCache: Cache?
) : Cache {
    override fun getName(): String {
        val result: String
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL, CacheStrategy.LOCAL_REMOTE -> result = localCache!!.name
            CacheStrategy.REMOTE -> result = remoteCache!!.name
        }
        return result
    }

    override fun getNativeCache(): Any {
        return this
    }

    @Nullable
    override fun get(key: Any): Cache.ValueWrapper? {
        var result: Cache.ValueWrapper?
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> result = localCache!!.get(key)
            CacheStrategy.REMOTE -> result = remoteCache?.get(key)
            CacheStrategy.LOCAL_REMOTE -> {
                result = localCache!!.get(key)
                if (result?.get() == null) {
                    result = remoteCache?.get(key)
                    localCache.put(key, result?.get())
                }
            }
        }
        return result
    }

    @Nullable
    override fun <T> get(key: Any, @Nullable type: Class<T?>?): T? {
        var result: T?
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> result = localCache!!.get<T?>(key, type)
            CacheStrategy.REMOTE -> result = remoteCache!!.get<T?>(key, type)
            CacheStrategy.LOCAL_REMOTE -> {
                result = localCache!!.get<T?>(key, type)
                if (result == null) {
                    result = remoteCache!!.get<T?>(key, type)
                    localCache.put(key, result)
                }
            }
        }
        return result
    }

    @Nullable
    override fun <T> get(key: Any, valueLoader: Callable<T?>): T? {
        var result: T?
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> result = localCache!!.get<T?>(key, valueLoader)
            CacheStrategy.REMOTE -> result = remoteCache?.get<T?>(key, valueLoader)
            CacheStrategy.LOCAL_REMOTE -> {
                result = localCache!!.get<T?>(key, valueLoader)
                if (result == null) {
                    result = remoteCache?.get<T?>(key, valueLoader)
                    localCache.put(key, result)
                }
            }
        }
        return result
    }

    override fun evict(key: Any) {
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> localCache!!.evict(key)
            CacheStrategy.REMOTE -> remoteCache!!.evict(key)
            CacheStrategy.LOCAL_REMOTE -> {
                remoteCache!!.evict(key)
                localCache!!.evict(key)
                val name = getName()
                log.debug("evict远程缓存{0}。key为{1}", name, key)
                pushMsgRedis(name, key)
            }
        }
    }

    override fun put(key: Any, @Nullable value: Any?) {
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> localCache!!.put(key, value)
            CacheStrategy.REMOTE -> remoteCache!!.put(key, value)
            CacheStrategy.LOCAL_REMOTE -> {
                remoteCache!!.put(key, value)
                localCache!!.put(key, value)
                val name = getName()
                log.debug("put远程缓存{0}。key为{1}", name, key)
                pushMsgRedis(name, key) //TODO 异步?
            }
        }
    }

    @Nullable
    override fun putIfAbsent(key: Any, @Nullable value: Any?): Cache.ValueWrapper? {
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> localCache!!.putIfAbsent(key, value)
            CacheStrategy.REMOTE -> remoteCache!!.putIfAbsent(key, value)
            CacheStrategy.LOCAL_REMOTE -> {
                remoteCache!!.putIfAbsent(key, value)
                localCache!!.putIfAbsent(key, value)
                val name = getName()
                log.debug("put远程缓存{0}。key为{1}", name, key)
                pushMsgRedis(name, key) //TODO 异步?
            }
        }
        //super putIfAbsent
        val existingValue = this.get(key)
        if (existingValue == null) {
            this.put(key, value)
        }
        return existingValue
    }

    override fun clear() {
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> localCache!!.clear()
            CacheStrategy.REMOTE -> remoteCache!!.clear()
            CacheStrategy.LOCAL_REMOTE -> {
                remoteCache!!.clear()
                localCache!!.clear()
                val name = getName()
                log.debug("clear远程缓存", name)
                pushMsgRedis(name, null)
            }
        }
    }

    /**
     * 清理本地缓存
     *
     * @param key key
     */
    fun clearLocal(key: Any?) {
        if (key == null) {
            localCache?.clear()
        } else {
            localCache?.evict(key)
        }
    }

    fun pushMsgRedis(name: String, key: Any?) {
        val cacheMessageHandlers = SpringKit.getBeansOfType(ICacheMessageHandler::class)
        if (cacheMessageHandlers.isNotEmpty()) {
            cacheMessageHandlers.forEach { (_: String?, handler: ICacheMessageHandler) ->
                val msg = CacheMessage(name, key)
                handler.sendMessage(msg)
            }
        }
    }

    companion object {
        private val log = LogFactory.getLog(this)

        private val cacheVersion: String? = null
    }
}
