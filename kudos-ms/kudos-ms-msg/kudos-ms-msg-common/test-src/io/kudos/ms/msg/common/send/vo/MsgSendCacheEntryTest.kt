package io.kudos.ms.msg.common.send.vo

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
 * test for MsgSendCacheEntry
 *
 * Covers property accessors (incl. Int counts), the IIdEntity contract, Serializable round-trip
 * and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendCacheEntryTest {

    private fun sample() = MsgSendCacheEntry(
        id = "s-1",
        receiverGroupTypeDictCode = "dept",
        receiverGroupId = "g-1",
        instanceId = "i-1",
        msgTypeDictCode = "m",
        localeDictCode = "en",
        sendStatusDictCode = "11",
        createTime = LocalDateTime.of(2026, 1, 1, 0, 0),
        updateTime = LocalDateTime.of(2026, 1, 2, 0, 0),
        successCount = 10,
        failCount = 2,
        jobId = "job-1",
        tenantId = "tn-1",
    )

    @Test
    fun properties() {
        val e = sample()
        assertEquals("s-1", e.id)
        assertEquals("dept", e.receiverGroupTypeDictCode)
        assertEquals("g-1", e.receiverGroupId)
        assertEquals("i-1", e.instanceId)
        assertEquals("m", e.msgTypeDictCode)
        assertEquals("en", e.localeDictCode)
        assertEquals("11", e.sendStatusDictCode)
        assertEquals(10, e.successCount)
        assertEquals(2, e.failCount)
        assertEquals("job-1", e.jobId)
        assertEquals("tn-1", e.tenantId)
        assertTrue(e is IIdEntity<*>)
        assertTrue(e is Serializable)
    }

    @Test
    fun nullableFieldsAcceptNull() {
        val e = MsgSendCacheEntry("s-2", null, null, null, null, null, null, null, null, null, null, null, null)
        assertEquals("s-2", e.id)
        assertNull(e.successCount)
        assertNull(e.failCount)
        assertNull(e.jobId)
    }

    @Test
    fun dataClassContract() {
        val a = sample()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(successCount = 99))
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
