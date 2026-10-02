package io.kudos.ability.distributed.stream.common.support

import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.base.lang.SerializationKit
import java.io.InvalidClassException
import java.io.ObjectInputStream
import java.io.Serializable
import kotlin.test.*

internal class StreamMessageSerializationTest {
    private class Unexpected : Serializable {
        private fun readObject(input: ObjectInputStream) {
            executed = true
            input.defaultReadObject()
        }
        companion object { var executed = false }
    }

    @Test fun rejectsUnknownClassBeforeReadObjectRuns() {
        Unexpected.executed = false
        val bytes = SerializationKit.serialize(StreamMessageVo(Unexpected()))
        assertFailsWith<InvalidClassException> { StreamMessageSerialization().deserialize(bytes) }
        assertFalse(Unexpected.executed)
    }

    @Test fun enforcesExpectedRootType() {
        assertFailsWith<IllegalArgumentException> {
            StreamMessageSerialization().deserialize(SerializationKit.serialize("text"), StreamMessageVo::class.java)
        }
    }

    @Test fun acceptsBuiltinPayloadWithoutCustomTypes() {
        val value = StreamMessageSerialization().deserialize(SerializationKit.serialize(StreamMessageVo("text")))
        assertEquals("text", (value as StreamMessageVo<*>).data)
    }

    @Test fun acceptsBoundedBuiltinCollections() {
        val body = hashMapOf("names" to arrayListOf("alice", "bob"))
        val restored = StreamMessageSerialization().deserialize(SerializationKit.serialize(StreamMessageVo(body)))
        assertEquals(body, (restored as StreamMessageVo<*>).data)
    }

    @Test fun rejectsOversizedMessagesAndArrays() {
        val policy = StreamMessageSerialization().apply { maxBytes = 64 }
        assertFailsWith<IllegalArgumentException> { policy.deserialize(ByteArray(65)) }
        policy.maxBytes = 1024
        policy.maxArrayLength = 4
        assertFailsWith<InvalidClassException> { policy.deserialize(SerializationKit.serialize(ByteArray(5))) }
    }

    @Test fun rejectsDeepObjectGraphs() {
        var value: Any = "leaf"
        repeat(30) { value = StreamMessageVo(value) }
        assertFailsWith<InvalidClassException> { StreamMessageSerialization().deserialize(SerializationKit.serialize(value as Serializable)) }
    }
}
