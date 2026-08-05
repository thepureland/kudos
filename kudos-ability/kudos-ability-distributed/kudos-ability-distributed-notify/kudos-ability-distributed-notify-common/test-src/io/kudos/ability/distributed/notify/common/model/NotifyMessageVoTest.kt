package io.kudos.ability.distributed.notify.common.model

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [NotifyMessageVo].
 *
 * Covers all three constructors (defaults, body-only, type+body), property mutability,
 * Unicode / empty-string notifyType values, and a Java-serialization round trip
 * (the class is [java.io.Serializable] and is transmitted across processes by MQ).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class NotifyMessageVoTest {

    @Test
    fun noArgConstructor_defaultsNotifyTypeToBlank() {
        val message = NotifyMessageVo<String>()

        assertTrue(message.notifyType.isBlank())
    }

    @Test
    fun bodyOnlyConstructor_defaultsNotifyTypeToBlank() {
        val message = NotifyMessageVo("payload")

        assertTrue(message.notifyType.isBlank())
        assertEquals("payload", message.messageBody)
    }

    @Test
    fun typeAndBodyConstructor_setsBothFields() {
        val message = NotifyMessageVo("cache.invalidate", "payload")

        assertEquals("cache.invalidate", message.notifyType)
        assertEquals("payload", message.messageBody)
    }

    @Test
    fun noArgConstructor_messageBodyDefaultsToNull() {
        val message = NotifyMessageVo<String>()

        assertNull(message.messageBody)
    }

    @Test
    fun properties_areMutableAfterConstruction() {
        val message = NotifyMessageVo<String>()

        message.notifyType = "config.refresh"
        message.messageBody = "new-body"

        assertEquals("config.refresh", message.notifyType)
        assertEquals("new-body", message.messageBody)

        message.messageBody = null
        assertNull(message.messageBody)
    }

    @Test
    fun notifyType_acceptsUnicodeAndEmptyString() {
        val message = NotifyMessageVo("缓存失效·テスト-θ", "payload")
        assertEquals("缓存失效·テスト-θ", message.notifyType)

        message.notifyType = ""
        assertTrue(message.notifyType.isBlank())
    }

    @Test
    fun javaSerialization_roundTripPreservesFields() {
        val original = NotifyMessageVo("cache.evict.中文", "body-内容")

        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(original) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use {
            @Suppress("UNCHECKED_CAST")
            it.readObject() as NotifyMessageVo<String>
        }

        assertEquals("cache.evict.中文", restored.notifyType)
        assertEquals("body-内容", restored.messageBody)
    }

}
