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
 * test for MsgReceiverGroupCacheEntry
 *
 * Covers property accessors (incl. Boolean active/builtIn), the IIdEntity contract,
 * Serializable round-trip and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiverGroupCacheEntryTest {

    private fun sample() = MsgReceiverGroupCacheEntry(
        id = "g-1",
        receiverGroupTypeDictCode = "dept",
        defineTable = "tbl",
        nameColumn = "name",
        remark = "r",
        active = true,
        builtIn = false,
        createUserId = "cu",
        createUserName = "cn",
        createTime = LocalDateTime.of(2026, 1, 1, 0, 0),
        updateUserId = "uu",
        updateUserName = "un",
        updateTime = LocalDateTime.of(2026, 1, 2, 0, 0),
    )

    @Test
    fun properties() {
        val e = sample()
        assertEquals("g-1", e.id)
        assertEquals("dept", e.receiverGroupTypeDictCode)
        assertEquals("tbl", e.defineTable)
        assertEquals("name", e.nameColumn)
        assertEquals("r", e.remark)
        assertEquals(true, e.active)
        assertEquals(false, e.builtIn)
        assertEquals("cu", e.createUserId)
        assertEquals("cn", e.createUserName)
        assertEquals("uu", e.updateUserId)
        assertEquals("un", e.updateUserName)
        assertEquals(LocalDateTime.of(2026, 1, 2, 0, 0), e.updateTime)
        assertTrue(e is IIdEntity<*>)
        assertTrue(e is Serializable)
    }

    @Test
    fun nullableFieldsAcceptNull() {
        val e = MsgReceiverGroupCacheEntry("g-2", null, null, null, null, null, null, null, null, null, null, null, null)
        assertEquals("g-2", e.id)
        assertNull(e.active)
        assertNull(e.builtIn)
        assertNull(e.remark)
    }

    @Test
    fun dataClassContract() {
        val a = sample()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("g-1"))
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
