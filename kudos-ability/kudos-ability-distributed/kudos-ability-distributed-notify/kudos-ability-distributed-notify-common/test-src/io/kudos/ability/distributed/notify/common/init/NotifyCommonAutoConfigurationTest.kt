package io.kudos.ability.distributed.notify.common.init

import io.kudos.ability.distributed.notify.common.api.INotifyListener
import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.init.properties.NotifyCommonProperties
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.common.support.NotifyListenerItem
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.mock.env.MockEnvironment
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [NotifyCommonAutoConfiguration] bean factory methods (direct instantiation, no Spring container).
 *
 * Covers:
 *  - `notifyCommonProperties` produces a properties instance with documented defaults
 *  - `notifyListenerBeanPostProcessor` namespace resolution priority:
 *      explicit listenerNamespace > spring.application.name > [NotifyListenerItem.DEFAULT_NAMESPACE],
 *      including the blank-namespace fallback branch
 *  - `notifyTool` wires the provider's `ifAvailable` producer (present and absent)
 *  - `notifyProducerAvailabilityVerifier` fail-fast matrix:
 *      (failOnMissingProducer x producer present/absent) -> only true+absent throws
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class NotifyCommonAutoConfigurationTest {

    private val config = NotifyCommonAutoConfiguration()

    /** Minimal [ObjectProvider] fake; only the two methods the production code may touch are implemented. */
    private fun providerOf(producer: INotifyProducer?) = object : ObjectProvider<INotifyProducer> {
        override fun getObject(): INotifyProducer =
            producer ?: throw NoSuchBeanDefinitionException(INotifyProducer::class.java)

        override fun getIfAvailable(): INotifyProducer? = producer
    }

    private class FakeProducer : INotifyProducer {
        var invocations = 0
        override fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean {
            invocations++
            return true
        }
    }

    private class TestListener(private val type: String) : INotifyListener {
        override fun notifyType(): String = type
        override fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>) {}
    }

    /** Registers a probe listener through the processor and returns the namespace it actually landed in. */
    private fun registeredNamespace(processor: BeanPostProcessor, type: String, candidates: List<String>): String? {
        val listener = TestListener(type)
        processor.postProcessAfterInitialization(listener, "probeListener")
        return candidates.firstOrNull { NotifyListenerItem.get(it, type) === listener }
    }

    // ---- notifyCommonProperties ----

    @Test
    fun notifyCommonProperties_returnsInstanceWithDefaults() {
        val properties = config.notifyCommonProperties()

        assertNotNull(properties)
        assertFalse(properties.failOnMissingProducer)
        assertNull(properties.listenerNamespace)
        assertFalse(properties.fallbackToDefaultNamespace)
    }

    // ---- notifyListenerBeanPostProcessor namespace resolution ----

    @Test
    fun beanPostProcessor_explicitNamespaceWins() {
        val properties = NotifyCommonProperties().apply { listenerNamespace = "explicit-ns" }
        val environment = MockEnvironment().withProperty("spring.application.name", "env-app")

        val processor = config.notifyListenerBeanPostProcessor(properties, environment)

        val ns = registeredNamespace(
            processor, "notify.autoconfig.explicit",
            listOf("explicit-ns", "env-app", NotifyListenerItem.DEFAULT_NAMESPACE)
        )
        assertEquals("explicit-ns", ns)
    }

    @Test
    fun beanPostProcessor_blankNamespaceFallsBackToApplicationName() {
        val properties = NotifyCommonProperties().apply { listenerNamespace = "   " }
        val environment = MockEnvironment().withProperty("spring.application.name", "env-app-blank")

        val processor = config.notifyListenerBeanPostProcessor(properties, environment)

        val ns = registeredNamespace(
            processor, "notify.autoconfig.blank",
            listOf("env-app-blank", NotifyListenerItem.DEFAULT_NAMESPACE)
        )
        assertEquals("env-app-blank", ns)
    }

    @Test
    fun beanPostProcessor_nullNamespaceFallsBackToApplicationName() {
        val properties = NotifyCommonProperties() // listenerNamespace = null
        val environment = MockEnvironment().withProperty("spring.application.name", "env-app-null")

        val processor = config.notifyListenerBeanPostProcessor(properties, environment)

        val ns = registeredNamespace(
            processor, "notify.autoconfig.null",
            listOf("env-app-null", NotifyListenerItem.DEFAULT_NAMESPACE)
        )
        assertEquals("env-app-null", ns)
    }

    @Test
    fun beanPostProcessor_noNamespaceNoAppName_fallsBackToDefaultNamespace() {
        val properties = NotifyCommonProperties()
        val environment = MockEnvironment() // no spring.application.name

        val processor = config.notifyListenerBeanPostProcessor(properties, environment)

        val ns = registeredNamespace(
            processor, "notify.autoconfig.default",
            listOf(NotifyListenerItem.DEFAULT_NAMESPACE)
        )
        assertEquals(NotifyListenerItem.DEFAULT_NAMESPACE, ns)
    }

    // ---- notifyTool ----

    @Test
    fun notifyTool_withAvailableProducer_delegatesToIt() {
        val producer = FakeProducer()
        val tool = config.notifyTool(providerOf(producer), NotifyCommonProperties())

        assertTrue(tool.notify(NotifyMessageVo("notify.autoconfig.tool", "payload")))
        assertEquals(1, producer.invocations)
    }

    @Test
    fun notifyTool_withoutProducer_degradesToFalse() {
        val tool = config.notifyTool(providerOf(null), NotifyCommonProperties())

        assertFalse(tool.notify(NotifyMessageVo("notify.autoconfig.tool.missing", "payload")))
    }

    // ---- notifyProducerAvailabilityVerifier ----

    @Test
    fun verifier_failFastEnabledAndProducerMissing_throws() {
        val properties = NotifyCommonProperties().apply { failOnMissingProducer = true }
        val verifier = config.notifyProducerAvailabilityVerifier(providerOf(null), properties)

        val ex = assertFailsWith<IllegalStateException> { verifier.afterPropertiesSet() }
        assertTrue(ex.message!!.contains("No INotifyProducer implementation found"))
        assertTrue(ex.message!!.contains("fail-on-missing-producer"))
    }

    @Test
    fun verifier_failFastEnabledAndProducerPresent_passes() {
        val properties = NotifyCommonProperties().apply { failOnMissingProducer = true }
        val verifier = config.notifyProducerAvailabilityVerifier(providerOf(FakeProducer()), properties)

        verifier.afterPropertiesSet() // must not throw
    }

    @Test
    fun verifier_failFastDisabled_passesEvenWithoutProducer() {
        val verifier = config.notifyProducerAvailabilityVerifier(providerOf(null), NotifyCommonProperties())

        verifier.afterPropertiesSet() // must not throw
    }

}
