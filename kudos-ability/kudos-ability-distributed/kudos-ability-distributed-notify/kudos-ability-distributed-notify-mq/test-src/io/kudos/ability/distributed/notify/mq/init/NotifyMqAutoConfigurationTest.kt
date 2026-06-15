package io.kudos.ability.distributed.notify.mq.init

import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONReader
import io.kudos.ability.distributed.notify.common.api.INotifyListener
import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.init.properties.NotifyCommonProperties
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.common.support.NotifyListenerItem
import io.kudos.ability.distributed.notify.mq.init.properties.NotifyMqProperties
import io.kudos.ability.distributed.notify.mq.producer.NotifyMqProducer
import io.kudos.ability.distributed.notify.mq.support.NotifyMqBindings
import io.kudos.ability.distributed.stream.common.annotations.MqProducerAspect
import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.ObjectProvider
import org.springframework.cloud.stream.config.BindingProperties
import org.springframework.cloud.stream.config.BindingServiceProperties
import org.springframework.core.env.Environment
import org.springframework.messaging.support.GenericMessage
import org.springframework.mock.env.MockEnvironment
import java.io.Serializable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * [NotifyMqAutoConfiguration] unit tests for MQ notification consumer configuration behavior.
 *
 * Covers: namespace resolution priority (explicit property -> spring.application.name -> default),
 * default-namespace fallback switch, listener exception swallow/rethrow, null message / null data /
 * blank notifyType short-circuits, deserialization failure swallow/rethrow, bean factory methods,
 * component name, and the producer binding verifier (warn vs fail-fast for missing aspect /
 * missing binding configuration).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
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

    @Test
    fun mqNotify_swallowsListenerExceptionByDefault() {
        val notifyType = "notify.mq.listener-error-swallowed"
        NotifyListenerItem.put("app-error-default", notifyType, ThrowingListener(notifyType))

        val consumer = NotifyMqAutoConfiguration()
            .applyNotifyCommonProperties(
                NotifyCommonProperties().apply {
                    listenerNamespace = "app-error-default"
                }
            )
            .mqNotify()

        consumer.accept(GenericMessage(StreamMessageVo(notifyPayload(notifyType))))
    }

    @Test
    fun mqNotify_rethrowsListenerExceptionWhenEnabled() {
        val notifyType = "notify.mq.listener-error-rethrow"
        NotifyListenerItem.put("app-error-rethrow", notifyType, ThrowingListener(notifyType))

        val consumer = NotifyMqAutoConfiguration()
            .applyNotifyCommonProperties(
                NotifyCommonProperties().apply {
                    listenerNamespace = "app-error-rethrow"
                }
            )
            .mqNotify(
                NotifyMqProperties().apply {
                    rethrowConsumerException = true
                }
            )

        assertFailsWith<IllegalStateException> {
            consumer.accept(GenericMessage(StreamMessageVo(notifyPayload(notifyType))))
        }
    }

    // ---------- message short-circuit branches ----------

    @Test
    fun mqNotify_ignoresNullMessage() {
        val consumer = NotifyMqAutoConfiguration().mqNotify()
        consumer.accept(null) // must not throw
    }

    @Test
    fun mqNotify_ignoresNullData() {
        val consumer = NotifyMqAutoConfiguration().mqNotify()
        consumer.accept(GenericMessage(StreamMessageVo<JSONObject>(null))) // must not throw
    }

    @Test
    fun mqNotify_ignoresBlankNotifyType() {
        val listener = CountingListener("")
        NotifyListenerItem.put(NotifyListenerItem.DEFAULT_NAMESPACE, "", listener)

        val consumer = NotifyMqAutoConfiguration().mqNotify()
        consumer.accept(GenericMessage(StreamMessageVo(notifyPayload(""))))
        consumer.accept(GenericMessage(StreamMessageVo(notifyPayload("   "))))

        assertEquals(0, listener.count.get())
    }

    @Test
    fun mqNotify_swallowsDeserializationFailureByDefault() {
        val consumer = NotifyMqAutoConfiguration().mqNotify()
        consumer.accept(GenericMessage(StreamMessageVo<JSONObject>(ExplodingJsonObject()))) // must not throw
    }

    @Test
    fun mqNotify_rethrowsDeserializationFailureWhenEnabled() {
        val consumer = NotifyMqAutoConfiguration().mqNotify(
            NotifyMqProperties().apply { rethrowConsumerException = true }
        )
        val ex = assertFailsWith<IllegalStateException> {
            consumer.accept(GenericMessage(StreamMessageVo<JSONObject>(ExplodingJsonObject())))
        }
        assertEquals("deserialization boom", ex.message)
    }

    // ---------- namespace resolution ----------

    @Test
    fun mqNotify_usesSpringApplicationNameWhenNamespacePropertyBlank() {
        val notifyType = "notify.mq.env-namespace"
        val listener = CountingListener(notifyType)
        NotifyListenerItem.put("env-app", notifyType, listener)

        val consumer = NotifyMqAutoConfiguration()
            .applyNotifyCommonProperties(NotifyCommonProperties().apply { listenerNamespace = "  " })
            .applyEnvironment(MockEnvironment().withProperty("spring.application.name", "env-app"))
            .mqNotify()

        consumer.accept(GenericMessage(StreamMessageVo(notifyPayload(notifyType))))

        assertEquals(1, listener.count.get())
    }

    @Test
    fun mqNotify_fallsBackToDefaultNamespaceWhenNothingConfigured() {
        val notifyType = "notify.mq.default-namespace"
        val listener = CountingListener(notifyType)
        NotifyListenerItem.put(NotifyListenerItem.DEFAULT_NAMESPACE, notifyType, listener)

        // no notifyCommonProperties, no environment injected
        val consumer = NotifyMqAutoConfiguration().mqNotify()
        consumer.accept(GenericMessage(StreamMessageVo(notifyPayload(notifyType))))

        assertEquals(1, listener.count.get())
    }

    @Test
    fun mqNotify_fallsBackToDefaultNamespaceWhenEnvironmentHasNoApplicationName() {
        val notifyType = "notify.mq.default-namespace-no-app-name"
        val listener = CountingListener(notifyType)
        NotifyListenerItem.put(NotifyListenerItem.DEFAULT_NAMESPACE, notifyType, listener)

        val consumer = NotifyMqAutoConfiguration()
            .applyEnvironment(MockEnvironment())
            .mqNotify()
        consumer.accept(GenericMessage(StreamMessageVo(notifyPayload(notifyType))))

        assertEquals(1, listener.count.get())
    }

    @Test
    fun mqNotify_doesNotFallbackToItselfWhenAlreadyInDefaultNamespace() {
        // fallback enabled but resolved namespace IS the default namespace and no listener registered:
        // findDefaultNamespaceListener must return null instead of looping back
        val notifyType = "notify.mq.default-namespace-unregistered"

        val consumer = NotifyMqAutoConfiguration()
            .applyNotifyCommonProperties(NotifyCommonProperties().apply { fallbackToDefaultNamespace = true })
            .mqNotify()

        consumer.accept(GenericMessage(StreamMessageVo(notifyPayload(notifyType)))) // must not throw
    }

    // ---------- bean factory methods & component name ----------

    @Test
    fun notifyMqProperties_beanHasLenientDefaults() {
        val props = NotifyMqAutoConfiguration().notifyMqProperties()
        assertFalse(props.failOnMissingProducerBinding)
        assertFalse(props.rethrowConsumerException)
    }

    @Test
    fun notifyMqProducer_beanIsMqImplementation() {
        val producer: INotifyProducer = NotifyMqAutoConfiguration().notifyMqProducer()
        assertIs<NotifyMqProducer>(producer)
        assertTrue(producer.notify(NotifyMessageVo("notify.mq.bean-producer", "body")))
    }

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ability-distributed-notify-mq", NotifyMqAutoConfiguration().getComponentName())
    }

    // ---------- producer binding verifier ----------

    @Test
    fun verifier_passesWhenAspectAndBindingPresentEvenInStrictMode() {
        verifier(
            strict = true,
            aspect = MqProducerAspect(),
            bindingServiceProperties = bindingServiceProperties(NotifyMqBindings.PRODUCER_BINDING)
        ).afterPropertiesSet() // must not throw
    }

    @Test
    fun verifier_onlyWarnsByDefaultWhenAspectMissing() {
        verifier(
            strict = false,
            aspect = null,
            bindingServiceProperties = bindingServiceProperties(NotifyMqBindings.PRODUCER_BINDING)
        ).afterPropertiesSet() // must not throw
    }

    @Test
    fun verifier_failsFastWhenAspectMissingInStrictMode() {
        val ex = assertFailsWith<IllegalStateException> {
            verifier(
                strict = true,
                aspect = null,
                bindingServiceProperties = bindingServiceProperties(NotifyMqBindings.PRODUCER_BINDING)
            ).afterPropertiesSet()
        }
        assertTrue(ex.message!!.contains("MqProducerAspect"))
    }

    @Test
    fun verifier_failsFastWhenBindingServicePropertiesMissingInStrictMode() {
        val ex = assertFailsWith<IllegalStateException> {
            verifier(strict = true, aspect = MqProducerAspect(), bindingServiceProperties = null)
                .afterPropertiesSet()
        }
        assertTrue(ex.message!!.contains(NotifyMqBindings.PRODUCER_BINDING))
    }

    @Test
    fun verifier_onlyWarnsByDefaultWhenBindingMapEmpty() {
        verifier(
            strict = false,
            aspect = MqProducerAspect(),
            bindingServiceProperties = BindingServiceProperties() // empty bindings map
        ).afterPropertiesSet() // must not throw
    }

    @Test
    fun verifier_failsFastWhenProducerBindingKeyAbsentInStrictMode() {
        val ex = assertFailsWith<IllegalStateException> {
            verifier(
                strict = true,
                aspect = MqProducerAspect(),
                bindingServiceProperties = bindingServiceProperties("someOtherBinding-out-0")
            ).afterPropertiesSet()
        }
        assertTrue(ex.message!!.contains(NotifyMqBindings.PRODUCER_BINDING))
    }

    // ---------- helpers ----------

    private fun verifier(
        strict: Boolean,
        aspect: MqProducerAspect?,
        bindingServiceProperties: BindingServiceProperties?
    ) = NotifyMqAutoConfiguration().notifyMqProducerBindingVerifier(
        NotifyMqProperties().apply { failOnMissingProducerBinding = strict },
        FixedObjectProvider(aspect),
        FixedObjectProvider(bindingServiceProperties)
    )

    private fun bindingServiceProperties(vararg bindingNames: String) =
        BindingServiceProperties().apply {
            bindings = bindingNames.associateWith { BindingProperties() }
        }

    private fun NotifyMqAutoConfiguration.applyNotifyCommonProperties(
        properties: NotifyCommonProperties
    ): NotifyMqAutoConfiguration {
        val field = NotifyMqAutoConfiguration::class.java.getDeclaredField("notifyCommonProperties")
        field.isAccessible = true
        field.set(this, properties)
        return this
    }

    private fun NotifyMqAutoConfiguration.applyEnvironment(environment: Environment): NotifyMqAutoConfiguration {
        val field = NotifyMqAutoConfiguration::class.java.getDeclaredField("environment")
        field.isAccessible = true
        field.set(this, environment)
        return this
    }

    private fun notifyPayload(notifyType: String) = JSONObject().apply {
        this["notifyType"] = notifyType
        this["messageBody"] = "payload"
    }

    /**
     * Test listener that counts the number of notifications consumed.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class CountingListener(private val type: String) : INotifyListener {
        val count = AtomicInteger()

        override fun notifyType(): String = type

        override fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>) {
            count.incrementAndGet()
        }
    }

    /**
     * Test notification listener that always throws.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class ThrowingListener(private val type: String) : INotifyListener {
        override fun notifyType(): String = type

        override fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>) {
            error("listener failed")
        }
    }

    /**
     * [ObjectProvider] fake returning a fixed instance (or behaving as "bean absent" when null).
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class FixedObjectProvider<T : Any>(private val value: T?) : ObjectProvider<T> {
        override fun getObject(): T = value ?: throw NoSuchBeanDefinitionException("absent")

        override fun getIfAvailable(): T? = value
    }

    /**
     * [JSONObject] subclass whose deserialization always fails, to exercise the consumer's
     * deserialization-failure branch deterministically.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class ExplodingJsonObject : JSONObject() {
        override fun <T> toJavaObject(clazz: Class<T>, vararg features: JSONReader.Feature): T =
            error("deserialization boom")
    }

}
