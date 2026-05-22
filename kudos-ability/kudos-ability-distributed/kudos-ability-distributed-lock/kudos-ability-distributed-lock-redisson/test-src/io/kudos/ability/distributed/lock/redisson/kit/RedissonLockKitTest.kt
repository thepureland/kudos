package io.kudos.ability.distributed.lock.redisson.kit

import io.kudos.ability.distributed.lock.redisson.locker.RedissonLocker
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * [RedissonLockKit] 全局配置与 named locker 路由测试。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class RedissonLockKitTest {

    @AfterTest
    fun tearDown() {
        RedissonLockKit.clearCachedLockers()
        RedissonLockKit.setLockKeyPrefix(RedissonLockKit.DEFAULT_LOCK_KEY_PREFIX)
    }

    @Test
    fun lock_usesConfiguredKeyPrefix() {
        val client = RecordingRedissonClient()
        RedissonLockKit.setLockKeyPrefix("APP::")
        RedissonLockKit.bindLocker(locker(client))

        val result = RedissonLockKit.lock("order:1")

        assertSame(client.lock, result)
        assertEquals("APP::order:1", client.lastLockName)
    }

    @Test
    fun lock_canUseNamedLocker() {
        val defaultClient = RecordingRedissonClient()
        val reportClient = RecordingRedissonClient()
        RedissonLockKit.bindLocker(locker(defaultClient))
        RedissonLockKit.bindLocker(locker(reportClient), "reportLocker")

        RedissonLockKit.lock("order:2", "reportLocker")

        assertEquals(null, defaultClient.lastLockName)
        assertEquals("REDISSON::order:2", reportClient.lastLockName)
    }

    /**
     * 记录 getLock 调用参数的 RedissonClient stub。
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class RecordingRedissonClient {
        var lastLockName: String? = null

        val lock: RLock = proxy(RLock::class.java) { method, _ ->
            when (method.name) {
                "tryLock" -> true
                else -> defaultValue(method.returnType)
            }
        }

        val proxy: RedissonClient = proxy(RedissonClient::class.java) { method, args ->
            when (method.name) {
                "getLock" -> {
                    lastLockName = args?.get(0) as String
                    lock
                }

                else -> defaultValue(method.returnType)
            }
        }
    }

    companion object {
        private fun locker(client: RecordingRedissonClient): RedissonLocker =
            RedissonLocker().apply {
                setPrivateField("redissonClient", client.proxy)
            }

        private fun Any.setPrivateField(name: String, value: Any?) {
            val field = this::class.java.getDeclaredField(name)
            field.isAccessible = true
            field.set(this, value)
        }

        private fun <T> proxy(type: Class<T>, handler: (Method, Array<Any?>?) -> Any?): T =
            type.cast(
                Proxy.newProxyInstance(
                    type.classLoader,
                    arrayOf(type),
                    InvocationHandler { _, method, args -> handler(method, args) }
                )
            )

        private fun defaultValue(returnType: Class<*>): Any? =
            when (returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Byte.TYPE -> 0.toByte()
                java.lang.Short.TYPE -> 0.toShort()
                java.lang.Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Float.TYPE -> 0f
                java.lang.Double.TYPE -> 0.0
                java.lang.Character.TYPE -> 0.toChar()
                java.lang.Void.TYPE -> null
                TimeUnit::class.java -> TimeUnit.SECONDS
                else -> null
            }
    }

}
