package io.kudos.ms.msg.common.send.vo

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgDispatchEvent
 *
 * Covers required vs nullable fields, set/collection handling, Serializable round-trip
 * and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgDispatchEventTest {

    private fun sample() = MsgDispatchEvent(
        sendId = "s-1",
        instanceId = "i-1",
        publishMethodDictCode = "email",
        receiverIds = setOf("u-1", "u-2"),
        renderedTitle = "Hello",
        renderedContent = "World",
        localeDictCode = "en_US",
        tenantId = "tn-1",
    )

    @Test
    fun properties() {
        val e = sample()
        assertEquals("s-1", e.sendId)
        assertEquals("i-1", e.instanceId)
        assertEquals("email", e.publishMethodDictCode)
        assertEquals(setOf("u-1", "u-2"), e.receiverIds)
        assertEquals("Hello", e.renderedTitle)
        assertEquals("World", e.renderedContent)
        assertEquals("en_US", e.localeDictCode)
        assertEquals("tn-1", e.tenantId)
        assertTrue(e is Serializable)
    }

    @Test
    fun localeMayBeNullAndReceiverIdsMayBeEmpty() {
        val e = MsgDispatchEvent("s-1", "i-1", "sms", emptySet(), "", "", null, "tn-1")
        assertNull(e.localeDictCode)
        assertTrue(e.receiverIds.isEmpty())
        assertEquals("", e.renderedTitle)
        assertEquals("", e.renderedContent)
    }

    @Test
    fun dataClassContract() {
        val a = sample()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(receiverIds = setOf("u-3")))
        assertTrue(a.toString().contains("s-1"))
    }

    @Test
    fun serializableRoundTrip() {
        val a = sample()
        val bytes = ByteArrayOutputStream().use { bo ->
            ObjectOutputStream(bo).use { it.writeObject(a) }
            bo.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        assertEquals(a, restored)
        assertNotSame(a, restored)
    }

}
