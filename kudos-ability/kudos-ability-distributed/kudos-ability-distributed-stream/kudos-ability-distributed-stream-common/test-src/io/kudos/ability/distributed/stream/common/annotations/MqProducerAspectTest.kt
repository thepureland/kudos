package io.kudos.ability.distributed.stream.common.annotations

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class MqProducerAspectTest {

    @Test
    fun selectPayload_usesConfiguredParameterIndex() {
        val args = arrayOf<Any?>("ignored", TestPayload("selected"))

        assertEquals(TestPayload("selected"), MqProducerAspect.selectPayload(args, 1))
    }

    @Test
    fun selectPayload_returnsNullWhenIndexIsOutOfBounds() {
        val args = arrayOf<Any?>("only")

        assertNull(MqProducerAspect.selectPayload(args, 2))
    }

    private data class TestPayload(val value: String)
}
