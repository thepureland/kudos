package io.kudos.base.lang.collections

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * 一个基于 Kotlin 协程的“阻塞式”Map：
 *  - 对每个 key 用一个 [Channel] 来存放尚未被取走的 value
 *  - 调用 [put] 时向 channel 发送元素；
 *  - 调用 [take] 时从 channel 接收元素，将在没有元素时挂起；
 *  - 调用 [poll] 时带超时地从 channel 接收元素，超时返回 null。
 *
 * @author ChatGPT
 *  @author K
 *  @since 1.0.0
 */
open class BlockingHashMap<K, V> {

    // 每个 key 对应一个 Channel<V>（使用 UNLIMITED 缓冲，避免 send 时因无接收者而挂起）
    private val map: ConcurrentMap<K, Channel<V>> = ConcurrentHashMap()

    /**
     * 将一个值放入指定 key 的阻塞队列中。如果此 key 对应的 Channel 尚未创建，先创建它。
     * 发送操作永不阻塞（因为 Channel.UNLIMITED），值会被保存在 channel 的内部缓冲里，
     * 等待一个或多个 take() 来取走。
     */
    suspend fun put(key: K, value: V) {
        requireNotNull(value) { "Value must not be null" }
        // “computeIfAbsent” 保证并发安全地创建或获取同一个 Channel
        val ch = map.computeIfAbsent(key) { Channel(Channel.UNLIMITED) }
        ch.send(value)
    }

    /**
     * 拿走指定 key 的一个值。如果此时还没有值可拿，协程会挂起，直到有 put() 推入一个值后才返回。
     * 取到值后不会自动关闭 channel，保留给后续的 take() 或 poll() 使用。
     */
    suspend fun take(key: K): V {
        // 为了保证 channel 一定存在，如果还没创建，就先新建一个
        val ch = map.computeIfAbsent(key) { Channel(Channel.UNLIMITED) }
        return ch.receive()
    }

    /**
     * 带超时地尝试取走指定 key 的一个值。如果在超时内没有值到达，返回 null。
     */
    suspend fun poll(key: K, timeout: Long): V? {
        require(timeout >= 0) { "Timeout must not be negative" }
        val ch = map.computeIfAbsent(key) { Channel(Channel.UNLIMITED) }
        // 使用 withTimeoutOrNull 若超时，则返回 null
        return withTimeoutOrNull(timeout) {
            ch.receive()
        }
    }

}
