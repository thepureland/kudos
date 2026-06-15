package io.kudos.ms.msg.common.template.vo

import io.kudos.base.model.contract.entity.IIdEntity
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
 * test for MsgTemplateCacheEntry
 *
 * Covers property accessors (incl. Boolean defaultActive), the IIdEntity contract,
 * Serializable round-trip and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgTemplateCacheEntryTest {

    private fun sample() = MsgTemplateCacheEntry(
        id = "t-1",
        sendTypeDictCode = "s",
        eventTypeDictCode = "e",
        msgTypeDictCode = "m",
        receiverGroupCode = "rg",
        localeDictCode = "en",
        title = "Title \${name}",
        content = "Content \${name}",
        defaultActive = true,
        defaultTitle = "DefaultTitle",
        defaultContent = "DefaultContent",
        tenantId = "tn",
    )

    @Test
    fun properties() {
        val e = sample()
        assertEquals("t-1", e.id)
        assertEquals("s", e.sendTypeDictCode)
        assertEquals("e", e.eventTypeDictCode)
        assertEquals("m", e.msgTypeDictCode)
        assertEquals("rg", e.receiverGroupCode)
        assertEquals("en", e.localeDictCode)
        assertEquals("Title \${name}", e.title)
        assertEquals("Content \${name}", e.content)
        assertEquals(true, e.defaultActive)
        assertEquals("DefaultTitle", e.defaultTitle)
        assertEquals("DefaultContent", e.defaultContent)
        assertEquals("tn", e.tenantId)
        assertTrue(e is IIdEntity<*>)
        assertTrue(e is Serializable)
    }

    @Test
    fun nullableFieldsAcceptNull() {
        val e = MsgTemplateCacheEntry("t-2", null, null, null, null, null, null, null, null, null, null, null)
        assertEquals("t-2", e.id)
        assertNull(e.defaultActive)
        assertNull(e.title)
        assertNull(e.tenantId)
    }

    @Test
    fun dataClassContract() {
        val a = sample()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(defaultActive = false))
        assertTrue(a.toString().contains("t-1"))
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
