package io.kudos.base.lang.collections

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * A coroutine-based "blocking" Map:
 *  - each key has its own [Channel] that holds values not yet taken;
 *  - [put] sends an element to the channel;
 *  - [take] receives an element from the channel and suspends while none is available;
 *  - [poll] receives an element from the channel with a timeout, returning null on timeout.
 *
 * @author AI: ChatGPT
 *  @author K
 *  @since 1.0.0
 */
open class BlockingHashMap<K, V> {

    // Each key has a corresponding Channel<V> (using UNLIMITED buffer so that send never suspends due to lack of receivers).
    private val map: ConcurrentMap<K, Channel<V>> = ConcurrentHashMap()

    /**
     * Puts a value into the blocking queue for the given key.
     *
     * Sends the value to the Channel associated with the key, creating the Channel if it does not exist.
     *
     * Workflow:
     * 1. Argument validation: check whether value is null; null values are rejected.
     * 2. Get or create the Channel: use computeIfAbsent for thread-safe acquisition or creation.
     * 3. Send the value: send value to the Channel.
     *
     * Channel characteristics:
     * - Uses Channel.UNLIMITED capacity, ensuring send never blocks.
     * - Values are kept in the Channel's internal buffer.
     * - Waits for take() or poll() to consume the values.
     *
     * Concurrency safety:
     * - Uses ConcurrentHashMap to guarantee thread-safe map operations.
     * - computeIfAbsent ensures only one Channel is created per key.
     * - Multiple coroutines may send values to the same key concurrently.
     *
     * Use cases:
     * - Producer/consumer patterns
     * - Event notification mechanisms
     * - Asynchronous task queues
     *
     * Notes:
     * - value must not be null; otherwise IllegalArgumentException is thrown.
     * - send does not block, even when there are no receivers.
     * - If the Channel buffer is full (theoretically impossible since UNLIMITED is used), send will suspend.
     *
     * @param key the queue's key
     * @param value the value to put; must not be null
     * @throws IllegalArgumentException if value is null
     */
    suspend fun put(key: K, value: V) {
        requireNotNull(value) { "Value must not be null" }
        // "computeIfAbsent" guarantees concurrency-safe creation or retrieval of the same Channel.
        val ch = map.computeIfAbsent(key) { Channel(Channel.UNLIMITED) }
        ch.send(value)
    }

    /**
     * Takes one value for the given key.
     *
     * Receives a value from the Channel associated with the key; suspends if no value is available.
     *
     * Workflow:
     * 1. Get or create the Channel: use computeIfAbsent to ensure the Channel exists.
     * 2. Receive the value: call Channel.receive() to receive a value.
     * 3. Suspend and wait: if the Channel has no values, the coroutine suspends until one arrives.
     *
     * Blocking behavior:
     * - If the Channel has values, returns immediately.
     * - If the Channel is empty, the coroutine suspends without blocking the thread.
     * - When a put() sends a value, the suspended coroutine is resumed.
     *
     * Channel management:
     * - The Channel is not automatically closed and can keep receiving values.
     * - Multiple coroutines may call take() simultaneously, competing to receive.
     * - Each value is received by exactly one coroutine.
     *
     * Use cases:
     * - Waiting for asynchronous task completion
     * - Receiving event notifications
     * - Implementing blocking queue semantics
     *
     * Notes:
     * - The coroutine will suspend indefinitely if no value ever arrives.
     * - Use poll() for timeout-bounded waiting.
     * - When multiple coroutines call take(), the value is delivered to one of them arbitrarily.
     *
     * @param key the queue's key
     * @return the received value
     */
    suspend fun take(key: K): V {
        // Ensure the channel exists; create one if it does not.
        val ch = map.computeIfAbsent(key) { Channel(Channel.UNLIMITED) }
        return ch.receive()
    }

    /**
     * Attempts to take a value for the given key within a timeout.
     *
     * Tries to receive a value from the Channel within the specified timeout; returns null on timeout.
     *
     * Workflow:
     * 1. Argument validation: check whether timeout is negative.
     * 2. Get or create the Channel: use computeIfAbsent to ensure the Channel exists.
     * 3. Timeout receive: use withTimeoutOrNull to receive a value within the given time.
     * 4. Return result: return null on timeout, otherwise the received value.
     *
     * Timeout mechanism:
     * - Uses withTimeoutOrNull to implement timeout control.
     * - If a value arrives within timeout, returns immediately.
     * - If timeout elapses without a value, returns null instead of throwing.
     *
     * Return value:
     * - non-null: a value was successfully received.
     * - null: timed out or no value was present in the Channel.
     *
     * Use cases:
     * - Scenarios requiring timeout-bounded waiting.
     * - Avoiding indefinite suspension.
     * - Implementing timeout-retry mechanisms.
     *
     * Notes:
     * - timeout must be >= 0; otherwise IllegalArgumentException is thrown.
     * - timeout is measured in milliseconds.
     * - Timeout does not throw; it simply returns null.
     *
     * @param key the queue's key
     * @param timeout timeout in milliseconds; must be >= 0
     * @return the received value, or null on timeout
     * @throws IllegalArgumentException if timeout is negative
     */
    suspend fun poll(key: K, timeout: Long): V? {
        require(timeout >= 0) { "Timeout must not be negative" }
        val ch = map.computeIfAbsent(key) { Channel(Channel.UNLIMITED) }
        // Use withTimeoutOrNull; returns null on timeout.
        return withTimeoutOrNull(timeout) {
            ch.receive()
        }
    }

}
