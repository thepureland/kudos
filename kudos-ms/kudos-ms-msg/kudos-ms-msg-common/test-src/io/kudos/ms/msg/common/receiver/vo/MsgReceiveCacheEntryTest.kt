package io.kudos.ms.msg.common.receiver.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgReceiveCacheEntry
 *
 * Covers property accessors, the IIdEntity contract, Serializable round-trip and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveCacheEntryTest {

    private fun sample() = MsgReceiveCacheEntry(
        id = "r-1",
        receiverId = "u-1",
        sendId = "s-1",
        receiveStatusDictCode = "11",
        createTime = LocalDateTime.of(2026, 1, 1, 0, 0),
        updateTime = LocalDateTime.of(2026, 1, 2, 0, 0),
        tenantId = "tn-1",
    )

    @Test
    fun properties() {
        val e = sample()
        assertEquals("r-1", e.id)
        assertEquals("u-1", e.receiverId)
        assertEquals("s-1", e.sendId)
        assertEquals("11", e.receiveStatusDictCode)
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), e.createTime)
        assertEquals(LocalDateTime.of(2026, 1, 2, 0, 0), e.updateTime)
        assertEquals("tn-1", e.tenantId)
        assertTrue(e is IIdEntity<*>)
        assertTrue(e is Serializable)
    }

    @Test
    fun nullableFieldsAcceptNull() {
        val e = MsgReceiveCacheEntry("r-2", null, null, null, null, null, null)
        assertEquals("r-2", e.id)
        assertNull(e.receiverId)
        assertNull(e.createTime)
        assertNull(e.tenantId)
    }

    @Test
    fun dataClassContract() {
        val a = sample()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(receiverId = "u-2"))
        assertTrue(a.toString().contains("r-1"))
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
