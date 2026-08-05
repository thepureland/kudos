package io.kudos.ability.distributed.stream.common.init.properties

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the stream configuration property holders: documented default values and mutability of
 * [StreamAsyncSendExecutorProperties], [StreamBindingVerifyProperties] and
 * [StreamProducerLimitProperties].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class StreamPropertiesTest {

    @Test
    fun asyncSendExecutorProperties_haveDocumentedDefaultsAndAreMutable() {
        val props = StreamAsyncSendExecutorProperties()
        assertEquals(32, props.corePoolSize)
        assertEquals(128, props.maxPoolSize)
        assertEquals(1024, props.queueCapacity)
        assertEquals("stream-async-", props.threadNamePrefix)

        props.corePoolSize = 1
        props.maxPoolSize = 2
        props.queueCapacity = 3
        props.threadNamePrefix = "x-"
        assertEquals(1, props.corePoolSize)
        assertEquals(2, props.maxPoolSize)
        assertEquals(3, props.queueCapacity)
        assertEquals("x-", props.threadNamePrefix)
    }

    @Test
    fun bindingVerifyProperties_haveDocumentedDefaultsAndAreMutable() {
        val props = StreamBindingVerifyProperties()
        assertFalse(props.enabled)
        assertFalse(props.failOnMissing)
        assertTrue(props.requiredProducerBindings.isEmpty())

        props.enabled = true
        props.failOnMissing = true
        props.requiredProducerBindings = listOf("a-out-0")
        assertTrue(props.enabled)
        assertTrue(props.failOnMissing)
        assertEquals(listOf("a-out-0"), props.requiredProducerBindings)
    }

    @Test
    fun producerLimitProperties_haveDocumentedDefaultsAndAreMutable() {
        val props = StreamProducerLimitProperties()
        assertFalse(props.enabled)
        assertEquals(1024, props.maxInFlight)
        assertEquals(0L, props.acquireTimeoutMillis)

        props.enabled = true
        props.maxInFlight = 7
        props.acquireTimeoutMillis = 99L
        assertTrue(props.enabled)
        assertEquals(7, props.maxInFlight)
        assertEquals(99L, props.acquireTimeoutMillis)
    }

}
