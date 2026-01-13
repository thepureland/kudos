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
     * 将一个值放入指定key的阻塞队列中
     * 
     * 将值发送到指定key对应的Channel中，如果Channel不存在则自动创建。
     * 
     * 工作流程：
     * 1. 参数验证：检查value是否为null，null值会被拒绝
     * 2. 获取或创建Channel：使用computeIfAbsent确保线程安全地获取或创建Channel
     * 3. 发送值：将value发送到Channel中
     * 
     * Channel特性：
     * - 使用Channel.UNLIMITED容量，确保send操作永不阻塞
     * - 值会被保存在Channel的内部缓冲中
     * - 等待take()或poll()来取走值
     * 
     * 并发安全：
     * - 使用ConcurrentHashMap保证map操作的线程安全
     * - computeIfAbsent确保同一key只会创建一个Channel
     * - 多个协程可以同时向同一个key发送值
     * 
     * 使用场景：
     * - 生产者-消费者模式
     * - 事件通知机制
     * - 异步任务队列
     * 
     * 注意事项：
     * - value不能为null，否则会抛出IllegalArgumentException
     * - send操作不会阻塞，即使没有接收者
     * - 如果Channel缓冲已满（理论上不会，因为UNLIMITED），send会挂起
     * 
     * @param key 队列的key
     * @param value 要放入的值，不能为null
     * @throws IllegalArgumentException 如果value为null
     */
    suspend fun put(key: K, value: V) {
        requireNotNull(value) { "Value must not be null" }
        // “computeIfAbsent” 保证并发安全地创建或获取同一个 Channel
        val ch = map.computeIfAbsent(key) { Channel(Channel.UNLIMITED) }
        ch.send(value)
    }

    /**
     * 拿走指定key的一个值
     * 
     * 从指定key对应的Channel中接收一个值，如果没有值则挂起等待。
     * 
     * 工作流程：
     * 1. 获取或创建Channel：使用computeIfAbsent确保Channel存在
     * 2. 接收值：调用Channel.receive()接收值
     * 3. 挂起等待：如果Channel中没有值，协程会挂起，直到有值到达
     * 
     * 阻塞行为：
     * - 如果Channel中有值，立即返回
     * - 如果Channel中没有值，协程会挂起，不阻塞线程
     * - 当有put()操作发送值时，挂起的协程会被唤醒
     * 
     * Channel管理：
     * - Channel不会自动关闭，可以持续接收值
     * - 多个协程可以同时调用take()，形成竞争接收
     * - 每个值只会被一个协程接收
     * 
     * 使用场景：
     * - 等待异步任务完成
     * - 接收事件通知
     * - 实现阻塞队列语义
     * 
     * 注意事项：
     * - 如果一直没有值到达，协程会一直挂起
     * - 使用poll()可以实现超时等待
     * - 多个协程同时take()时，值会被随机分配给其中一个
     * 
     * @param key 队列的key
     * @return 接收到的值
     */
    suspend fun take(key: K): V {
        // 为了保证 channel 一定存在，如果还没创建，就先新建一个
        val ch = map.computeIfAbsent(key) { Channel(Channel.UNLIMITED) }
        return ch.receive()
    }

    /**
     * 带超时地尝试取走指定key的一个值
     * 
     * 在指定超时时间内尝试从Channel中接收值，超时则返回null。
     * 
     * 工作流程：
     * 1. 参数验证：检查timeout是否为负数
     * 2. 获取或创建Channel：使用computeIfAbsent确保Channel存在
     * 3. 超时接收：使用withTimeoutOrNull在指定时间内接收值
     * 4. 返回结果：如果超时则返回null，否则返回接收到的值
     * 
     * 超时机制：
     * - 使用withTimeoutOrNull实现超时控制
     * - 如果在timeout时间内有值到达，立即返回
     * - 如果超时仍没有值，返回null而不抛出异常
     * 
     * 返回值：
     * - 非null：成功接收到值
     * - null：超时或Channel中没有值
     * 
     * 使用场景：
     * - 需要超时等待的场景
     * - 避免无限期挂起
     * - 实现超时重试机制
     * 
     * 注意事项：
     * - timeout必须>=0，否则会抛出IllegalArgumentException
     * - timeout单位为毫秒
     * - 超时不会抛出异常，只是返回null
     * 
     * @param key 队列的key
     * @param timeout 超时时间（毫秒），必须>=0
     * @return 接收到的值，如果超时则返回null
     * @throws IllegalArgumentException 如果timeout为负数
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
