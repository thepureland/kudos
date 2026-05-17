package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.notify.CacheMessage
import io.kudos.ability.cache.common.notify.ICacheMessageHandler
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

    override fun getName(): String = when (strategy) {
        CacheStrategy.SINGLE_LOCAL, CacheStrategy.LOCAL_REMOTE -> requireLocal().name
        CacheStrategy.REMOTE -> requireRemote().name
    }

    override fun getNativeCache(): Any {
        return this
    }

    @Nullable
    override fun get(key: Any): Cache.ValueWrapper? {
        return when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> requireLocal().get(key)
            CacheStrategy.REMOTE -> remoteCache?.get(key)
            CacheStrategy.LOCAL_REMOTE -> mixGet(key)
        }
    }

    @Nullable
    override fun <T : Any> get(key: Any, @Nullable type: Class<T>?): T? {
        return when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> requireLocal().get(key, type)
            CacheStrategy.REMOTE -> requireRemote().get(key, type)
            CacheStrategy.LOCAL_REMOTE -> {
                val wrapper = mixGet(key) ?: return null
                val value = wrapper.get()
                if (value != null && type != null && !type.isInstance(value)) {
                    throw IllegalStateException(
                        "Cached value is not of required type [${type.name}]: $value"
                    )
                }
                @Suppress("UNCHECKED_CAST")
                value as T?
            }
        }
    }

    @Nullable
    override fun <T : Any> get(key: Any, valueLoader: Callable<T>): T? {
        return when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> requireLocal().get(key, valueLoader)
            CacheStrategy.REMOTE -> requireRemote().get(key, valueLoader)
            CacheStrategy.LOCAL_REMOTE -> mixGetOrLoad(key, valueLoader)
        }
    }

    /**
     * 两级缓存读：本地命中（含缓存的 null 值，用于防穿透）直接返回；
     * 本地真正缺失才查远端；远端命中则回填本地（保留 null 语义）；
     * 两级都缺失返回 null，不做任何写入。
     */
    private fun mixGet(key: Any): Cache.ValueWrapper? {
        val local = requireLocal()
        val localWrapper = local.get(key)
        if (localWrapper != null) {
            return localWrapper
        }
        val remoteWrapper = remoteCache?.get(key) ?: return null
        local.put(key, remoteWrapper.get())
        return remoteWrapper
    }

    /**
     * 两级缓存读 + 缺失加载：与 [mixGet] 同语义；两级都未命中时调用 valueLoader 一次，
     * 写入两级（不广播 evict —— 与 mixGet 的回填行为对齐，避免把读路径变成写路径）。
     */
    private fun <T : Any> mixGetOrLoad(key: Any, valueLoader: Callable<T>): T? {
        val local = requireLocal()
        val localWrapper = local.get(key)
        if (localWrapper != null) {
            @Suppress("UNCHECKED_CAST")
            return localWrapper.get() as T?
        }
        val remoteWrapper = remoteCache?.get(key)
        if (remoteWrapper != null) {
            local.put(key, remoteWrapper.get())
            @Suppress("UNCHECKED_CAST")
            return remoteWrapper.get() as T?
        }
        val loaded: T? = valueLoader.call()
        remoteCache?.put(key, loaded)
        local.put(key, loaded)
        return loaded
    }

    private fun requireLocal(): Cache =
        requireNotNull(localCache) { "localCache is null for strategy $strategy" }

    private fun requireRemote(): Cache =
        requireNotNull(remoteCache) { "remoteCache is null for strategy $strategy" }

    /**
     * 写穿透 + 广播的统一模板。
     * - SINGLE_LOCAL：只动本地。
     * - REMOTE：只动远端。
     * - LOCAL_REMOTE：先远端再本地，最后广播给其他节点失效（[notifyKey] 用 null 表示整库清空）。
     * 写顺序"先远端再本地"是为了：若广播在 finally 链路上出问题或被丢，本节点本地至少与远端版本一致，
     * 而不是本地新、远端老（那种顺序在分布式失败语义下更难解释）。
     * [opName] 仅用于 debug 日志；[notifyKey] 与 [action] 之间是调用方约定。
     */
    private inline fun writeThrough(opName: String, notifyKey: Any?, action: (Cache) -> Unit) {
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> action(requireLocal())
            CacheStrategy.REMOTE -> action(requireRemote())
            CacheStrategy.LOCAL_REMOTE -> {
                action(requireRemote())
                action(requireLocal())
                val name = getName()
                log.debug("{0}远程缓存{1}。key为{2}", opName, name, notifyKey)
                pushMsgRedis(name, notifyKey) //TODO 异步?
            }
        }
    }

    override fun evict(key: Any) = writeThrough("evict", key) { it.evict(key) }

    override fun put(key: Any, @Nullable value: Any?) = writeThrough("put", key) { it.put(key, value) }

    /**
     * putIfAbsent 的 LOCAL_REMOTE 分支语义比一般写更细：
     * - 远端 putIfAbsent 失败（已存在）→ 不广播失效，但回填本地以避免本节点后续再回源；
     * - 远端 putIfAbsent 成功（新插入）→ 同步写本地 + 广播失效；
     * 模板 [writeThrough] 无条件 action+broadcast，无法表达上面的分叉，所以这里保留显式 when。
     */
    @Nullable
    override fun putIfAbsent(key: Any, @Nullable value: Any?): Cache.ValueWrapper? = when (strategy) {
        CacheStrategy.SINGLE_LOCAL -> requireLocal().putIfAbsent(key, value)
        CacheStrategy.REMOTE -> requireRemote().putIfAbsent(key, value)
        CacheStrategy.LOCAL_REMOTE -> {
            val remote = requireRemote()
            val local = requireLocal()
            val existed = remote.putIfAbsent(key, value)
            if (existed == null) {
                local.putIfAbsent(key, value)
                val name = getName()
                log.debug("putIfAbsent远程缓存{0}。key为{1}", name, key)
                pushMsgRedis(name, key) //TODO 异步?
                null
            } else {
                // 远端已存在，回填本地，避免本节点后续再次回源。这是读取语义而非写入语义，不广播。
                local.put(key, existed.get())
                existed
            }
        }
    }

    override fun clear() = writeThrough("clear", null) { it.clear() }

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
        val cacheMessageHandlers = SpringKit.getBeansOfType<ICacheMessageHandler>()
        if (cacheMessageHandlers.isNotEmpty()) {
            cacheMessageHandlers.forEach { (_: String?, handler: ICacheMessageHandler) ->
                val msg = CacheMessage(name, key)
                handler.sendMessage(msg)
            }
        }
    }

    companion object {
        private val log = LogFactory.getLog(this::class)

        private val cacheVersion: String? = null
    }
}