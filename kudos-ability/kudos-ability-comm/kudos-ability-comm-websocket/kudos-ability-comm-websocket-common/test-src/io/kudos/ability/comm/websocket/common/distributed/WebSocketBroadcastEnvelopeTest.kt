package io.kudos.ability.comm.websocket.common.distributed

import io.kudos.ability.comm.websocket.common.distributed.WebSocketBroadcastEnvelope.TargetType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [WebSocketBroadcastEnvelope]:
 * - data-class semantics (equals / hashCode / copy / toString) including the nullable targetId;
 * - [TargetType] enum completeness and name round-trip (the flat-enum wire encoding);
 * - Java serialization round-trip (the JdkSerializationRedisSerializer fallback path documented
 *   in the class KDoc), including Unicode payloads.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class WebSocketBroadcastEnvelopeTest {

    @Test
    fun dataClassEquality_andCopy() {
        val e1 = WebSocketBroadcastEnvelope("n1", TargetType.USER, "u1", "hello")
        val e2 = WebSocketBroadcastEnvelope("n1", TargetType.USER, "u1", "hello")
        val e3 = e1.copy(targetId = null)

        assertEquals(e1, e2)
        assertEquals(e1.hashCode(), e2.hashCode())
        assertNotEquals(e1, e3)
        assertEquals("n1", e3.nodeId)
        assertEquals(TargetType.USER, e3.targetType)
        assertEquals(null, e3.targetId)
        assertEquals("hello", e3.text)
        assertTrue(e1.toString().contains("u1"), "toString should include component values")
    }

    @Test
    fun targetType_enumIsCompleteAndRoundTripsByName() {
        assertEquals(
            listOf("ALL", "USER", "TENANT", "SESSION"),
            TargetType.entries.map { it.name },
            "Wire encoding relies on these exact names; adding/renaming is a protocol change",
        )
        for (t in TargetType.entries) {
            assertEquals(t, TargetType.valueOf(t.name))
        }
    }

    @Test
    fun javaSerialization_roundTrip_preservesAllFields() {
        val original = WebSocketBroadcastEnvelope("node-中文", TargetType.SESSION, "s-🚀", "payload \n\t \"quoted\"")

        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(original) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }

        assertEquals(original, restored)
    }

    @Test
    fun javaSerialization_roundTrip_withNullTargetId() {
        val original = WebSocketBroadcastEnvelope("n", TargetType.ALL, null, "")

        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(original) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }

        assertEquals(original, restored)
    }
}
