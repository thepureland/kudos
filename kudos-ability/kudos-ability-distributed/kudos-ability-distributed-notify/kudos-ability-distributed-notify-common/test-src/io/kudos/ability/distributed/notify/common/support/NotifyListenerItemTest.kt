package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyListener
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import java.io.Serializable
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertNull

/**
 * Unit tests for [NotifyListenerItem].
 *
 * Covers:
 *  - blank namespace fallback to [NotifyListenerItem.DEFAULT_NAMESPACE] on both `put` and `get`
 *  - namespace isolation (same type registered in different namespaces never cross-resolves)
 *  - the 2-arg `put` / 1-arg `get` default-namespace shortcuts
 *  - null result when the namespace exists but the key is missing, and when the namespace itself is missing
 *  - last-write-wins overwrite semantics for duplicate (namespace, key) registrations
 *  - concurrent registration (the registry declares thread safety via two-level ConcurrentHashMap)
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class NotifyListenerItemTest {

    @Test
    fun put_blankNamespaceFallsBackToDefaultNamespace() {
        val listener = TestListener("notify.test.blank")

        NotifyListenerItem.put("   ", listener.notifyType(), listener)

        assertSame(listener, NotifyListenerItem.get(listener.notifyType()))
    }

    @Test
    fun get_namespaceIsolation() {
        val type = "notify.test.namespace"
        val listenerA = TestListener(type)
        val listenerB = TestListener(type)

        NotifyListenerItem.put("app-a", type, listenerA)
        NotifyListenerItem.put("app-b", type, listenerB)

        assertSame(listenerA, NotifyListenerItem.get("app-a", type))
        assertSame(listenerB, NotifyListenerItem.get("app-b", type))
        assertNull(NotifyListenerItem.get("app-c", type))
    }

    @Test
    fun put_shortcutOverloadRegistersUnderDefaultNamespace() {
        val type = "notify.test.shortcut"
        val listener = TestListener(type)

        NotifyListenerItem.put(type, listener)

        assertSame(listener, NotifyListenerItem.get(NotifyListenerItem.DEFAULT_NAMESPACE, type))
        assertSame(listener, NotifyListenerItem.get(type))
    }

    @Test
    fun get_blankNamespaceFallsBackToDefaultNamespace() {
        val type = "notify.test.get.blank"
        val listener = TestListener(type)

        NotifyListenerItem.put(type, listener) // registered under default namespace

        assertSame(listener, NotifyListenerItem.get("   ", type))
        assertSame(listener, NotifyListenerItem.get("", type))
    }

    @Test
    fun get_existingNamespaceMissingKey_returnsNull() {
        val namespace = "notify-test-ns-missing-key"
        NotifyListenerItem.put(namespace, "notify.test.present", TestListener("notify.test.present"))

        assertNull(NotifyListenerItem.get(namespace, "notify.test.absent"))
    }

    @Test
    fun get_unknownKeyInDefaultNamespace_returnsNull() {
        assertNull(NotifyListenerItem.get("notify.test.never-registered"))
    }

    @Test
    fun put_duplicateKeyInSameNamespace_lastWriteWins() {
        val type = "notify.test.overwrite"
        val first = TestListener(type)
        val second = TestListener(type)

        NotifyListenerItem.put("ns-overwrite", type, first)
        NotifyListenerItem.put("ns-overwrite", type, second)

        assertSame(second, NotifyListenerItem.get("ns-overwrite", type))
    }

    @Test
    fun put_concurrentRegistrations_allRetrievable() {
        val threadCount = 16
        val perThread = 50
        val startGate = CountDownLatch(1)
        val listeners = Array(threadCount) { t ->
            Array(perThread) { i -> TestListener("notify.test.concurrent.$t.$i") }
        }
        val threads = (0 until threadCount).map { t ->
            Thread {
                startGate.await()
                for (i in 0 until perThread) {
                    NotifyListenerItem.put("ns-concurrent-$t", "notify.test.concurrent.$t.$i", listeners[t][i])
                }
            }.apply { start() }
        }

        startGate.countDown()
        threads.forEach { it.join() }

        for (t in 0 until threadCount) {
            for (i in 0 until perThread) {
                assertSame(listeners[t][i], NotifyListenerItem.get("ns-concurrent-$t", "notify.test.concurrent.$t.$i"))
            }
        }
    }

    private class TestListener(private val type: String) : INotifyListener {
        override fun notifyType(): String = type
        override fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>) {}
    }

}
