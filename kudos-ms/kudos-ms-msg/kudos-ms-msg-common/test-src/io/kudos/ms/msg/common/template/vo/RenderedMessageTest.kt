package io.kudos.ms.msg.common.template.vo

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/**
 * test for RenderedMessage
 *
 * Covers non-null title/content/paramsUsed accessors, empty-string and empty-map edge cases,
 * Serializable round-trip and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class RenderedMessageTest {

    @Test
    fun properties() {
        val m = RenderedMessage(
            title = "Hello Alice",
            content = "Your code is 1234",
            paramsUsed = mapOf("name" to "Alice", "code" to "1234"),
        )
        assertEquals("Hello Alice", m.title)
        assertEquals("Your code is 1234", m.content)
        assertEquals(2, m.paramsUsed.size)
        assertEquals("Alice", m.paramsUsed["name"])
        assertTrue(m is Serializable)
    }

    @Test
    fun emptyTitleAndContentAndParams() {
        val m = RenderedMessage("", "", emptyMap())
        assertEquals("", m.title)
        assertEquals("", m.content)
        assertTrue(m.paramsUsed.isEmpty())
    }

    @Test
    fun dataClassContract() {
        val a = RenderedMessage("t", "c", mapOf("k" to "v"))
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(title = "x"))
        assertNotEquals(a, a.copy(paramsUsed = emptyMap()))
        assertTrue(a.toString().contains("t"))
    }

    @Test
    fun serializableRoundTrip() {
        val a = RenderedMessage("t", "c", mapOf("k" to "v"))
        val bytes = ByteArrayOutputStream().use { bo ->
            ObjectOutputStream(bo).use { it.writeObject(a) }
            bo.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        assertEquals(a, restored)
        assertNotSame(a, restored)
    }

}
