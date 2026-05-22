package io.kudos.ability.distributed.stream.common.annotations

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [MqProducerAspect] payload 参数选择测试。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
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

    /**
     * producer payload 选择测试 DTO。
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private data class TestPayload(val value: String)
}
