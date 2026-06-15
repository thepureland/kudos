package io.kudos.ability.distributed.stream.rocketmq.init.properties

import io.kudos.context.kit.SpringKit
import org.mockito.Mockito
import org.springframework.context.ApplicationContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [RocketMqProperties] unit tests. Covers:
 * - field defaults (nameSrvAddr null, saveException true, deserialization filter empty)
 * - mutability of all properties (including Unicode / empty-string values)
 * - companion `instance` accessor resolving the bean through [SpringKit]
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class RocketMqPropertiesTest {

    @Test
    fun defaults() {
        val props = RocketMqProperties()
        assertNull(props.nameSrvAddr)
        assertTrue(props.saveException)
        assertEquals("", props.batchConsumerDeserializationFilter)
    }

    @Test
    fun propertiesAreMutable() {
        val props = RocketMqProperties()
        props.nameSrvAddr = "名稱伺服器.example.com:9876"
        props.saveException = false
        props.batchConsumerDeserializationFilter = "java.base/*;!*"

        assertEquals("名稱伺服器.example.com:9876", props.nameSrvAddr)
        assertFalse(props.saveException)
        assertEquals("java.base/*;!*", props.batchConsumerDeserializationFilter)

        props.nameSrvAddr = null
        assertNull(props.nameSrvAddr)
    }

    @Test
    fun instance_resolvesBeanFromSpringContext() {
        val expected = RocketMqProperties()
        val ctx = Mockito.mock(ApplicationContext::class.java)
        Mockito.`when`(ctx.getBean(RocketMqProperties::class.java)).thenReturn(expected)

        val previousCtx = try {
            SpringKit.applicationContext
        } catch (_: IllegalStateException) {
            null
        }
        SpringKit.applicationContext = ctx
        try {
            assertSame(expected, RocketMqProperties.instance)
        } finally {
            SpringKit.applicationContext = previousCtx
        }
    }

}
