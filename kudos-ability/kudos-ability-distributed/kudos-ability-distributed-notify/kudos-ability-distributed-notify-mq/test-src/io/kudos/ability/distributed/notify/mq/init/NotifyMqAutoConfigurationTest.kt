package io.kudos.ability.distributed.notify.mq.init

import com.alibaba.fastjson2.JSONObject
import io.kudos.ability.distributed.notify.common.api.INotifyListener
import io.kudos.ability.distributed.notify.common.init.properties.NotifyCommonProperties
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.common.support.NotifyListenerItem
import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import org.springframework.messaging.support.GenericMessage
import java.io.Serializable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals


internal class NotifyMqAutoConfigurationTest {

    @Test
    fun mqNotify_doesNotFallbackToDefaultNamespaceByDefault() {
        val notifyType = "notify.mq.no-default-fallback"
        val defaultListener = CountingListener(notifyType)
        NotifyListenerItem.put(NotifyListenerItem.DEFAULT_NAMESPACE, notifyType, defaultListener)

        val consumer = NotifyMqAutoConfiguration()
            .applyNotifyCommonProperties(
                NotifyCommonProperties().apply {
                    listenerNamespace = "app-a"
                }
            )
            .mqNotify()

        consumer.accept(GenericMessage(StreamMessageVo(notifyPayload(notifyType))))

        assertEquals(0, defaultListener.count.get())
    }

    @Test
    fun mqNotify_fallsBackToDefaultNamespaceWhenExplicitlyEnabled() {
        val notifyType = "notify.mq.explicit-default-fallback"
        val defaultListener = CountingListener(notifyType)
        NotifyListenerItem.put(NotifyListenerItem.DEFAULT_NAMESPACE, notifyType, defaultListener)

        val consumer = NotifyMqAutoConfiguration()
            .applyNotifyCommonProperties(
                NotifyCommonProperties().apply {
                    listenerNamespace = "app-b"
                    fallbackToDefaultNamespace = true
                }
            )
            .mqNotify()

        consumer.accept(GenericMessage(StreamMessageVo(notifyPayload(notifyType))))

        assertEquals(1, defaultListener.count.get())
    }

    private fun NotifyMqAutoConfiguration.applyNotifyCommonProperties(
        properties: NotifyCommonProperties
    ): NotifyMqAutoConfiguration {
        val field = NotifyMqAutoConfiguration::class.java.getDeclaredField("notifyCommonProperties")
        field.isAccessible = true
        field.set(this, properties)
        return this
    }

    private fun notifyPayload(notifyType: String) = JSONObject().apply {
        this["notifyType"] = notifyType
        this["messageBody"] = "payload"
    }

    private class CountingListener(private val type: String) : INotifyListener {
        val count = AtomicInteger()

        override fun notifyType(): String = type

        override fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>) {
            count.incrementAndGet()
        }
    }

}
