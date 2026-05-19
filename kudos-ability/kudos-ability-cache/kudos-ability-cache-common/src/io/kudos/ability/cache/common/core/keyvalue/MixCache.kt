package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.notify.CacheMessage
import io.kudos.ability.cache.common.notify.ICacheMessageHandler
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import jakarta.annotation.Nullable
import org.springframework.cache.Cache
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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
        local.get(key)?.let { return it }
        val remoteWrapper = remoteCache?.get(key) ?: return null
        local.put(key, remoteWrapper.get())
        return remoteWrapper
    }

    /**
     * 两级缓存读 + 缺失加载：与 [mixGet] 同语义；两级都未命中时调用 valueLoader 一次，
     * 写入两级（不广播 evict —— 与 mixGet 的回填行为对齐，避免把读路径变成写路径）。
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> mixGetOrLoad(key: Any, valueLoader: Callable<T>): T? {
        val local = requireLocal()
        local.get(key)?.let { return it.get() as T? }
        remoteCache?.get(key)?.let { wrapper ->
            local.put(key, wrapper.get())
            return wrapper.get() as T?
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
     *
     * **失败语义**（README 文档化的契约）：
     *
     *  | 阶段        | 异常处理                                                |
     *  |-------------|--------------------------------------------------------|
     *  | 远端写失败  | 异常上抛 → 不写本地、不广播。整网保持远端原值 → 一致      |
     *  | 本地写失败  | 异常**吞掉** + WARN 日志 → 广播仍发出。语义："远端是真相  |
     *  |             | 源，已经更新；本地写失败是降级，靠下次 miss 回源 / TTL 自愈"|
     *  | 广播失败    | 已是 fire-and-forget（[pushMsgRedis] 内部 catch）         |
     *
     * 写顺序"先远端后本地"——若广播 finally 链路出问题或被丢，本节点本地至少与远端版本一致，
     * 而不是本地新、远端老（那种顺序在分布式失败语义下更难解释）。
     *
     * [opName] 仅用于 debug 日志；[notifyKey] 与 [action] 之间是调用方约定。
     */
    private inline fun writeThrough(opName: String, notifyKey: Any?, action: (Cache) -> Unit) {
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> action(requireLocal())
            CacheStrategy.REMOTE -> action(requireRemote())
            CacheStrategy.LOCAL_REMOTE -> {
                action(requireRemote())
                // 本地失败不应该把"远端已成功更新"的广播挡掉——否则其他节点的本地副本
                // 会一直 stale。catch + warn 后继续广播，让全网最终与远端一致。
                try {
                    action(requireLocal())
                } catch (t: Throwable) {
                    log.warn(
                        "本地缓存写入失败（远端已成功），将继续广播让其他节点失效本地副本，本节点本地待下次 miss / TTL 自愈 op={0} key={1} cause={2}",
                        opName, notifyKey, t.message
                    )
                }
                val name = getName()
                log.debug("{0}远程缓存{1}。key为{2}", opName, name, notifyKey)
                pushMsgRedis(name, notifyKey)
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
                // 远端 putIfAbsent 成功——按 [writeThrough] 同款失败语义：本地失败仍广播
                try {
                    local.putIfAbsent(key, value)
                } catch (t: Throwable) {
                    log.warn("本地缓存写入失败（远端已成功），将继续广播 putIfAbsent key={0} cause={1}",
                        key, t.message)
                }
                val name = getName()
                log.debug("putIfAbsent远程缓存{0}。key为{1}", name, key)
                pushMsgRedis(name, key)
                null
            } else {
                // 远端已存在，回填本地，避免本节点后续再次回源。这是读取语义而非写入语义，不广播。
                // 回填失败也只是降级，下次 miss 再次走远端即可。
                try {
                    local.put(key, existed.get())
                } catch (t: Throwable) {
                    log.warn("回填本地缓存失败 key={0} cause={1}", key, t.message)
                }
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

    /**
     * Fire-and-forget 广播：把失效消息异步派发给所有 [ICacheMessageHandler]。
     *
     * 同步走广播会让 Redis pub/sub 等传输的 RTT 落到写路径上（一次 put → 一次跨节点广播 →
     * 至少一次网络往返）。这里改成异步：写路径只负责把消息塞进 [broadcastExecutor] 的队列，
     * 实际发送在 daemon 线程里完成。
     *
     * 失败语义：每个 handler 的发送失败被单独捕获并 WARN 日志（**不会重试**——重复广播比
     * 偶发丢一条危险得多；下游 `MixCache.clearLocal` 只是丢本地副本，下次回源会再生）。
     * 队列满时退化为 caller-runs，等价于回到同步语义，宁可拖慢写也不丢消息。
     */
    fun pushMsgRedis(name: String, key: Any?) {
        val handlers = SpringKit.getBeansOfType<ICacheMessageHandler>()
        if (handlers.isEmpty()) return
        val msg = CacheMessage(name, key)
        handlers.forEach { (beanName: String, handler: ICacheMessageHandler) ->
            broadcastExecutor.execute {
                try {
                    handler.sendMessage(msg)
                } catch (t: Throwable) {
                    log.error(
                        t,
                        "缓存失效广播失败[handler={0}, cache={1}, key={2}]——其他节点本地副本可能滞留旧值，等待下次 TTL 或显式 evict 收敛",
                        beanName, name, key
                    )
                }
            }
        }
    }

    companion object {
        private val log = LogFactory.getLog(this::class)

        private val cacheVersion: String? = null

        /**
         * 缓存失效广播的 fire-and-forget 执行器。
         *
         * 设计：
         * - core=1 / max=Runtime cpu 数 / keep-alive 60s——绝大多数广播是 Redis pub 这种短任务，
         *   并发瓶颈在网络 RTT 而非 CPU，1 个线程通常够；峰值时按 CPU 数扩
         * - 队列容量 1024——失效消息丢失=节点本地副本滞留，宁愿短暂拖慢写也不丢消息，所以
         *   满了用 [java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy] 回退到同步
         * - daemon 线程——不阻碍 JVM 退出
         */
        private val broadcastExecutor: Executor = ThreadPoolExecutor(
            1,
            maxOf(2, Runtime.getRuntime().availableProcessors()),
            60L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(1024),
            object : ThreadFactory {
                private val seq = AtomicInteger(0)
                override fun newThread(r: Runnable): Thread {
                    val t = Thread(r, "kudos-cache-broadcast-${seq.incrementAndGet()}")
                    t.isDaemon = true
                    return t
                }
            },
            ThreadPoolExecutor.CallerRunsPolicy()
        )
    }
}