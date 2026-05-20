package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyListener
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertNull


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

    private class TestListener(private val type: String) : INotifyListener {
        override fun notifyType(): String = type
        override fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>) {}
    }

}
