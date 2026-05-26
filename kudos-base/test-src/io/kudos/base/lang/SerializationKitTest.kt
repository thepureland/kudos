package io.kudos.base.lang

import org.apache.commons.lang3.SerializationException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.Serializable
import kotlin.test.*


/**
 * test for SerializationKit
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class SerializationKitTest {

    /**
     * A simple serializable data class used to test clone, serialize, and deserialize.
     */
    data class Person(val name: String, val age: Int) : Serializable

    @Test
    fun clone_WithNonNullSerializable_ReturnsDeepCopy() {
        val original = Person("Alice", 30)
        val copy = SerializationKit.clone(original)

        // The two objects should not be the same reference, but their contents must be equal
        assertNotSame(original, copy)
        assertEquals(original, copy)
    }

    @Test
    fun clone_WithNull_ReturnsNull() {
        val result: Person? = SerializationKit.clone(null)
        assertNull(result)
    }

    @Test
    fun clone_WithNonSerializableField_ThrowsSerializationException() {
        // This class implements java.io.Serializable itself, but contains a Thread (which is not serializable)
        class BadHolder(val thread: Thread) : Serializable

        val bad = BadHolder(Thread.currentThread())

        // During cloning the whole object is written to a byte stream; because Thread itself is not serializable,
        // this throws org.apache.commons.lang3.SerializationException.
        assertFailsWith<SerializationException> {
            SerializationKit.clone(bad)
        }
    }

    @Test
    fun serializeToByteArray_AndDeserialize_WorksCorrectly() {
        val person = Person("Bob", 25)
        // Serialize the object to a ByteArray
        val bytes = SerializationKit.serialize(person)
        assertTrue(bytes.isNotEmpty())

        // Then deserialize the byte array back to an object
        val deserialized = SerializationKit.deserialize(bytes) as Person
        assertEquals(person, deserialized)
    }

    @Test
    fun serializeNullToByteArray_AndDeserialize_WorksCorrectly() {
        // Serializing null should produce a valid byte[] (Commons-Codec serializes a null marker)
        val bytes = SerializationKit.serialize(null)
        assertNotNull(bytes)
        // Deserializing it returns null
        val result = SerializationKit.deserialize(bytes)
        assertNull(result)
    }

    @Test
    fun deserialize_InvalidByteArray_ThrowsSerializationException() {
        // Arbitrary invalid data (not a complete serialization stream) should throw a runtime SerializationException
        val bad = byteArrayOf(1, 2, 3, 4, 5)
        assertFailsWith<SerializationException> {
            SerializationKit.deserialize(bad)
        }
    }

    @Test
    fun serializeToStream_AndDeserializeFromStream_WorksCorrectly() {
        val person = Person("Carol", 40)
        val baos = ByteArrayOutputStream()

        // 1. Write the object to the OutputStream
        SerializationKit.serialize(person, baos)

        // 2. Take the bytes from the ByteArrayOutputStream and build an InputStream
        val data = baos.toByteArray()
        // Close baos manually (optional, since ByteArrayOutputStream.close() is a no-op)
        baos.close()

        // 3. Deserialize back to an object
        val bais = ByteArrayInputStream(data)
        val deserialized = SerializationKit.deserialize(bais) as Person
        assertEquals(person, deserialized)

        // 4. Close bais manually (also a no-op)
        bais.close()
    }

    @Test
    fun serializeToStream_WithNullOutputStream_ThrowsNullPointerException() {
        val person = Person("Dave", 55)
        assertFailsWith<NullPointerException> {
            SerializationKit.serialize(person, null)
        }
    }

    @Test
    fun deserializeFromStream_WithNullInputStream() {
        assertFailsWith<NullPointerException> {
            SerializationKit.deserialize(null as InputStream?)
        }
    }

    @Test
    fun deserializeFromStream_WithCorruptedStream_ThrowsSerializationException() {
        // Build an InputStream containing bad data
        val corruptedBytes = byteArrayOf(0x00, 0x01, 0x02, 0x03)
        val bais = ByteArrayInputStream(corruptedBytes)

        assertFailsWith<SerializationException> {
            SerializationKit.deserialize(bais)
        }
    }

    @Test
    fun serializeVariousPrimitiveWrappers_AndDeserialize() {
        // Test serialization/deserialization of the Java primitive wrapper Integer
        val originalInt = Integer.valueOf(123)
        val bytesForInt = SerializationKit.serialize(originalInt)
        val deserializedInt = SerializationKit.deserialize(ByteArrayInputStream(bytesForInt)) as Int
        assertEquals(123, deserializedInt)

        // Test serialization/deserialization of String
        val originalStr = "Hello, 世界"
        val baosStr = ByteArrayOutputStream()
        SerializationKit.serialize(originalStr, baosStr)
        // No longer assert that writing throws, since ByteArrayOutputStream.close() is a no-op
        baosStr.close()

        val baisStr = ByteArrayInputStream(baosStr.toByteArray())
        val deserializedStr = SerializationKit.deserialize(baisStr) as String
        assertEquals(originalStr, deserializedStr)
    }

    @Test
    fun serializeObjectWithTransientField_TransientFieldNotRestored() {
        // Test an object with a transient field
        data class WithTransient(val keep: String, @Transient val skip: String) : Serializable

        val original = WithTransient("keepValue", "skipValue")
        val bytes = SerializationKit.serialize(original)
        val restored = SerializationKit.deserialize(bytes) as WithTransient

        // The transient field skip should fall back to the default null on deserialization
        assertEquals("keepValue", restored.keep)
        assertNull(restored.skip)
    }

    @Test
    fun serializeLargeObject_PerformanceSmoke() {
        // Simply ensure that serializing a relatively large List does not blow up memory or crash immediately
        val largeList = List(10000) { Person("Name$it", it) }
        val start = System.currentTimeMillis()
        val bytes = SerializationKit.serialize(largeList as Serializable?)
        val elapsed1 = System.currentTimeMillis() - start
        assertTrue(bytes.isNotEmpty())
        // Focus on correctness, not exact timing; just check an upper bound (e.g. must not exceed 5s)
        assertTrue(elapsed1 < 5000, "Serialization too slow: $elapsed1 ms")

        val start2 = System.currentTimeMillis()
        @Suppress("UNCHECKED_CAST")
        val deserialized = SerializationKit.deserialize(bytes) as List<Person>
        val elapsed2 = System.currentTimeMillis() - start2
        assertEquals(10000, deserialized.size)
        assertEquals(largeList[1234], deserialized[1234])
        assertTrue(elapsed2 < 5000, "Deserialization too slow: $elapsed2 ms")
    }

}
