package io.kudos.ability.distributed.stream.common.init

import io.kudos.ability.distributed.stream.common.annotations.MqProducerAspect
import io.kudos.ability.distributed.stream.common.biz.SysMqFailMsgService
import io.kudos.ability.distributed.stream.common.handler.StreamGlobalExceptionHandler
import io.kudos.ability.distributed.stream.common.handler.StreamProducerExceptionHandler
import io.kudos.ability.distributed.stream.common.init.properties.StreamAsyncSendExecutorProperties
import io.kudos.ability.distributed.stream.common.init.properties.StreamBindingVerifyProperties
import io.kudos.ability.distributed.stream.common.support.StreamMessageConverter
import io.kudos.ability.distributed.stream.common.support.StreamProducerFailHandlerProcessor
import io.kudos.ability.distributed.stream.common.support.StreamProducerHelper
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.ObjectProvider
import org.springframework.cloud.stream.config.BindingProperties
import org.springframework.cloud.stream.config.BindingServiceProperties
import org.springframework.integration.channel.DirectChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [StreamCommonConfiguration]:
 * - every @Bean factory method returns a non-null instance of the documented type
 * - streamAsyncSendExecutor applies the pool sizing from [StreamAsyncSendExecutorProperties]
 * - streamBindingVerifier branches: disabled, no required bindings configured (incl. blank-only),
 *   all bindings present, missing bindings with failOnMissing=true (fails fast with the binding
 *   name in the message) and failOnMissing=false (warn only), absent BindingServiceProperties
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class StreamCommonConfigurationTest {

    private val config = StreamCommonConfiguration()

    @Test
    fun beanFactoryMethods_returnExpectedTypes() {
        assertNotNull(config.streamAsyncSendExecutorProperties())
        assertNotNull(config.streamBindingVerifyProperties())
        assertNotNull(config.streamProducerLimitProperties())
        assertIs<MqProducerAspect>(config.mqProducerAspect())
        assertIs<StreamGlobalExceptionHandler>(config.streamGlobalExceptionHandler())
        assertIs<StreamProducerExceptionHandler>(config.streamProducerExceptionHandler())
        assertIs<StreamProducerFailHandlerProcessor>(config.streamProducerFailHandlerProcessor())
        assertIs<StreamProducerHelper>(config.streamProducerHelper())
        assertIs<StreamMessageConverter>(config.customMessageConverter())
        assertIs<DirectChannel>(config.mqProducerChannel())
        val dao = config.streamExceptionMsgDao()
        assertIs<SysMqFailMsgService>(config.streamExceptionBiz(dao))
    }

    @Test
    fun streamAsyncSendExecutor_appliesConfiguredPoolSettings() {
        val props = StreamAsyncSendExecutorProperties().apply {
            corePoolSize = 2
            maxPoolSize = 4
            queueCapacity = 8
            threadNamePrefix = "test-async-"
        }

        val executor = config.streamAsyncSendExecutor(props)
        try {
            assertEquals(2, executor.corePoolSize)
            assertEquals(4, executor.maxPoolSize)
            assertEquals(8, executor.queueCapacity)
            assertEquals("test-async-", executor.threadNamePrefix)
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun streamBindingVerifier_skipsWhenDisabled() {
        val verifier = config.streamBindingVerifier(
            StreamBindingVerifyProperties().apply { enabled = false },
            providerOf(null)
        )

        // Must not throw even though no bindings are available.
        verifier.afterPropertiesSet()
    }

    @Test
    fun streamBindingVerifier_warnsWhenNoRequiredBindingsConfigured() {
        val verifier = config.streamBindingVerifier(
            StreamBindingVerifyProperties().apply { enabled = true },
            providerOf(bindingProps("a-out-0"))
        )

        verifier.afterPropertiesSet()
    }

    @Test
    fun streamBindingVerifier_treatsBlankRequiredBindingsAsUnconfigured() {
        val verifier = config.streamBindingVerifier(
            StreamBindingVerifyProperties().apply {
                enabled = true
                requiredProducerBindings = listOf("", "   ")
            },
            providerOf(bindingProps("a-out-0"))
        )

        verifier.afterPropertiesSet()
    }

    @Test
    fun streamBindingVerifier_passesWhenAllRequiredBindingsPresent() {
        val verifier = config.streamBindingVerifier(
            StreamBindingVerifyProperties().apply {
                enabled = true
                requiredProducerBindings = listOf("a-out-0", "b-out-0")
            },
            providerOf(bindingProps("a-out-0", "b-out-0"))
        )

        verifier.afterPropertiesSet()
    }

    @Test
    fun streamBindingVerifier_failsFastOnMissingBindingWhenConfigured() {
        val verifier = config.streamBindingVerifier(
            StreamBindingVerifyProperties().apply {
                enabled = true
                failOnMissing = true
                requiredProducerBindings = listOf("missing-out-0")
            },
            providerOf(bindingProps("a-out-0"))
        )

        val e = assertFailsWith<IllegalStateException> { verifier.afterPropertiesSet() }
        assertTrue(e.message!!.contains("missing-out-0"), "message should name the missing binding: ${e.message}")
    }

    @Test
    fun streamBindingVerifier_onlyWarnsOnMissingBindingByDefault() {
        val verifier = config.streamBindingVerifier(
            StreamBindingVerifyProperties().apply {
                enabled = true
                failOnMissing = false
                requiredProducerBindings = listOf("missing-out-0")
            },
            providerOf(bindingProps("a-out-0"))
        )

        verifier.afterPropertiesSet()
    }

    @Test
    fun streamBindingVerifier_treatsAbsentBindingServicePropertiesAsNoBindings() {
        val verifier = config.streamBindingVerifier(
            StreamBindingVerifyProperties().apply {
                enabled = true
                failOnMissing = true
                requiredProducerBindings = listOf("missing-out-0")
            },
            providerOf(null)
        )

        assertFailsWith<IllegalStateException> { verifier.afterPropertiesSet() }
    }

    private fun providerOf(props: BindingServiceProperties?): ObjectProvider<BindingServiceProperties> {
        @Suppress("UNCHECKED_CAST")
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<BindingServiceProperties>
        `when`(provider.ifAvailable).thenReturn(props)
        return provider
    }

    private fun bindingProps(vararg names: String): BindingServiceProperties =
        BindingServiceProperties().apply {
            bindings = names.associateWith { BindingProperties() }
        }

}
