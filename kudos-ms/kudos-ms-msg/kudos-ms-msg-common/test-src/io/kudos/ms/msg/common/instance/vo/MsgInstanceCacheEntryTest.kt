package io.kudos.ms.msg.common.instance.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgInstanceCacheEntry
 *
 * Covers required/nullable property accessors, the IIdEntity.id contract, Serializable round-trip,
 * and the data-class equals/hashCode/copy/toString contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgInstanceCacheEntryTest {

    private fun sample() = MsgInstanceCacheEntry(
        id = "i-1",
        localeDictCode = "zh_CN",
        title = "标题",
        content = "内容",
        templateId = "t-1",
        sendTypeDictCode = "s1",
        eventTypeDictCode = "e1",
        msgTypeDictCode = "m1",
        validTimeStart = LocalDateTime.of(2026, 1, 1, 0, 0),
        validTimeEnd = LocalDateTime.of(2026, 12, 31, 23, 59),
        tenantId = "tn-1",
    )

    @Test
    fun properties() {
        val e = sample()
        assertEquals("i-1", e.id)
        assertEquals("zh_CN", e.localeDictCode)
        assertEquals("标题", e.title)
        assertEquals("内容", e.content)
        assertEquals("t-1", e.templateId)
        assertEquals("s1", e.sendTypeDictCode)
        assertEquals("e1", e.eventTypeDictCode)
        assertEquals("m1", e.msgTypeDictCode)
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), e.validTimeStart)
        assertEquals(LocalDateTime.of(2026, 12, 31, 23, 59), e.validTimeEnd)
        assertEquals("tn-1", e.tenantId)
        assertTrue(e is IIdEntity<*>)
        assertTrue(e is Serializable)
    }

    @Test
    fun nullableFieldsAcceptNull() {
        val e = MsgInstanceCacheEntry(
            id = "i-2", localeDictCode = null, title = null, content = null, templateId = null,
            sendTypeDictCode = null, eventTypeDictCode = null, msgTypeDictCode = null,
            validTimeStart = null, validTimeEnd = null, tenantId = null,
        )
        assertEquals("i-2", e.id)
        assertNull(e.localeDictCode)
        assertNull(e.title)
        assertNull(e.validTimeStart)
        assertNull(e.tenantId)
    }

    @Test
    fun dataClassContract() {
        val a = sample()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(title = "other"))
        assertTrue(a.toString().contains("i-1"))
    }

    @Test
    fun serializableRoundTrip() {
        val a = sample()
        val bytes = java.io.ByteArrayOutputStream().use { bo ->
            java.io.ObjectOutputStream(bo).use { it.writeObject(a) }
            bo.toByteArray()
        }
        val restored = java.io.ObjectInputStream(java.io.ByteArrayInputStream(bytes)).use { it.readObject() }
        assertEquals(a, restored)
        assertNotSame(a, restored)
    }

}
