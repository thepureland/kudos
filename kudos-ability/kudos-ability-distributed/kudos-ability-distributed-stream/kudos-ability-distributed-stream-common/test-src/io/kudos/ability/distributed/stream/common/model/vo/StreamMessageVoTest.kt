package io.kudos.ability.distributed.stream.common.model.vo

import io.kudos.base.lang.SerializationKit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests for [StreamMessageVo]: both constructors, null data, data mutation and the JDK
 * serialization round trip the stream wire format relies on.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class StreamMessageVoTest {

    @Test
    fun defaultConstructor_leavesDataNullAndAllowsMutation() {
        val vo = StreamMessageVo<String>()
        assertNull(vo.data)

        vo.data = "later"
        assertEquals("later", vo.data)
    }

    @Test
    fun dataConstructor_acceptsValueAndNull() {
        assertEquals("payload", StreamMessageVo("payload").data)
        assertNull(StreamMessageVo<String>(null).data)
    }

    @Test
    fun jdkSerialization_roundTripsTheMessage() {
        val source = StreamMessageVo("wire-data")

        val restored = SerializationKit.deserialize(SerializationKit.serialize(source))

        assertEquals("wire-data", assertIs<StreamMessageVo<*>>(restored).data)
    }

}
