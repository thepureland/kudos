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
     * 一个简单的可序列化数据类，用于测试 clone、序列化/反序列化功能。
     */
    data class Person(val name: String, val age: Int) : Serializable

    @Test
    fun clone_WithNonNullSerializable_ReturnsDeepCopy() {
        val original = Person("Alice", 30)
        val copy = SerializationKit.clone(original)

        // 两个对象不应该是同一个引用，但内容相等
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
        // 这个类本身实现了 java.io.Serializable，但是它内部有一个 Thread（不可序列化）
        class BadHolder(val thread: Thread) : Serializable

        val bad = BadHolder(Thread.currentThread())

        // 在克隆时，会尝试将整个对象写到字节流里，因为 Thread 本身不可序列化，
        // 所以这里会抛出 org.apache.commons.lang3.SerializationException。
        assertFailsWith<SerializationException> {
            SerializationKit.clone(bad)
        }
    }

    @Test
    fun serializeToByteArray_AndDeserialize_WorksCorrectly() {
        val person = Person("Bob", 25)
        // 先将对象序列化为 ByteArray
        val bytes = SerializationKit.serialize(person)
        assertTrue(bytes.isNotEmpty())

        // 再将字节数组反序列化为对象
        val deserialized = SerializationKit.deserialize(bytes) as Person
        assertEquals(person, deserialized)
    }

    @Test
    fun serializeNullToByteArray_AndDeserialize_WorksCorrectly() {
        // 序列化 null 应该生成一个合法的 byte[]（Commons-Codec 序列化了一个 null 标记）
        val bytes = SerializationKit.serialize(null)
        assertNotNull(bytes)
        // 将其反序列化后会得到 null
        val result = SerializationKit.deserialize(bytes)
        assertNull(result)
    }

    @Test
    fun deserialize_InvalidByteArray_ThrowsSerializationException() {
        // 任意非法数据（不是完整的序列化流）应抛出运行时的 SerializationException
        val bad = byteArrayOf(1, 2, 3, 4, 5)
        assertFailsWith<SerializationException> {
            SerializationKit.deserialize(bad)
        }
    }

    @Test
    fun serializeToStream_AndDeserializeFromStream_WorksCorrectly() {
        val person = Person("Carol", 40)
        val baos = ByteArrayOutputStream()

        // 1. 将对象写入到 OutputStream
        SerializationKit.serialize(person, baos)

        // 2. 从 ByteArrayOutputStream 中取出字节，构造 InputStream
        val data = baos.toByteArray()
        // 手动关闭一下 baos（可选，因为 ByteArrayOutputStream.close() 是 no-op）
        baos.close()

        // 3. 反序列化回对象
        val bais = ByteArrayInputStream(data)
        val deserialized = SerializationKit.deserialize(bais) as Person
        assertEquals(person, deserialized)

        // 4. 手动关闭一下 bais（同样是 no-op）
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
        // 构造一个包含错误数据的 InputStream
        val corruptedBytes = byteArrayOf(0x00, 0x01, 0x02, 0x03)
        val bais = ByteArrayInputStream(corruptedBytes)

        assertFailsWith<SerializationException> {
            SerializationKit.deserialize(bais)
        }
    }

    @Test
    fun serializeVariousPrimitiveWrappers_AndDeserialize() {
        // 测试对 Java 原始类型包装类 Integer 的序列化/反序列化
        val originalInt = Integer.valueOf(123)
        val bytesForInt = SerializationKit.serialize(originalInt)
        val deserializedInt = SerializationKit.deserialize(ByteArrayInputStream(bytesForInt)) as Int
        assertEquals(123, deserializedInt)

        // 测试对 String 的序列化/反序列化
        val originalStr = "Hello, 世界"
        val baosStr = ByteArrayOutputStream()
        SerializationKit.serialize(originalStr, baosStr)
        // 不再断言写入抛异常，因为 ByteArrayOutputStream.close() 是 no-op
        baosStr.close()

        val baisStr = ByteArrayInputStream(baosStr.toByteArray())
        val deserializedStr = SerializationKit.deserialize(baisStr) as String
        assertEquals(originalStr, deserializedStr)
    }

    @Test
    fun serializeObjectWithTransientField_TransientFieldNotRestored() {
        // 测试带 transient 字段的对象
        data class WithTransient(val keep: String, @Transient val skip: String) : Serializable

        val original = WithTransient("keepValue", "skipValue")
        val bytes = SerializationKit.serialize(original)
        val restored = SerializationKit.deserialize(bytes) as WithTransient

        // transient 字段 skip 在反序列化时应该为默认值 null
        assertEquals("keepValue", restored.keep)
        assertNull(restored.skip)
    }

    @Test
    fun serializeLargeObject_PerformanceSmoke() {
        // 简单地确保对一个较大的 List 序列化不会爆内存或立即崩溃
        val largeList = List(10000) { Person("Name$it", it) }
        val start = System.currentTimeMillis()
        val bytes = SerializationKit.serialize(largeList as Serializable?)
        val elapsed1 = System.currentTimeMillis() - start
        assertTrue(bytes.isNotEmpty())
        // 关注于功能正确性，而非具体耗时，只做一个极限检查（例如不能超过 5 秒）
        assertTrue(elapsed1 < 5000, "序列化过慢: $elapsed1 ms")

        val start2 = System.currentTimeMillis()
        @Suppress("UNCHECKED_CAST")
        val deserialized = SerializationKit.deserialize(bytes) as List<Person>
        val elapsed2 = System.currentTimeMillis() - start2
        assertEquals(10000, deserialized.size)
        assertEquals(largeList[1234], deserialized[1234])
        assertTrue(elapsed2 < 5000, "反序列化过慢: $elapsed2 ms")
    }

}