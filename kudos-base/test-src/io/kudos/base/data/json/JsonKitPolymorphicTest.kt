package io.kudos.base.data.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Polymorphic 序列化测试用例
 *
 * 覆盖两种多态：
 * - Sealed 层级——kotlinx 自动处理，无需通过 JsonKit 注册
 * - Open 多态——需要通过 [JsonKit.registerSerializersModule] 注册
 *
 * @author K
 * @since 1.0.0
 */
internal class JsonKitPolymorphicTest {

    // ============================================================
    // Sealed：直接 toJson 应输出多态 JSON（带 type 字段）
    // ============================================================

    @Serializable
    sealed class Shape {
        @Serializable
        @SerialName("circle")
        data class Circle(val r: Double) : Shape()

        @Serializable
        @SerialName("square")
        data class Square(val side: Double) : Shape()
    }

    @Test
    fun sealedPolymorphismWorksWithoutRegistration() {
        // 用编译期已知类型 List<Shape> 让 kotlinx 走 sealed 多态路径
        val shapes: List<Shape> = listOf(Shape.Circle(1.0), Shape.Square(2.0))
        val json = JsonKit.toJson(shapes)

        // 应包含两个 type 标识；具体格式由 kotlinx 决定（type 字段名默认是 "type"）
        assertTrue(json.contains("\"type\":\"circle\""), "应含 circle discriminator：$json")
        assertTrue(json.contains("\"type\":\"square\""), "应含 square discriminator：$json")
        assertTrue(json.contains("\"r\":1.0"))
        assertTrue(json.contains("\"side\":2.0"))
    }

    @Test
    fun sealedPolymorphismRoundTrip() {
        val original: List<Shape> = listOf(Shape.Circle(3.14), Shape.Square(4.2))
        val json = JsonKit.toJson(original)
        val restored = JsonKit.fromJson<List<Shape>>(json)
        assertEquals(original, restored)
    }

    // ============================================================
    // Open 多态：需要 registerSerializersModule 注册
    // ============================================================

    @Serializable
    abstract class Animal {
        abstract val name: String
    }

    @Serializable
    @SerialName("dog")
    data class Dog(override val name: String, val tricks: Int) : Animal()

    @Serializable
    @SerialName("cat")
    data class Cat(override val name: String, val livesLeft: Int) : Animal()

    /**
     * 注册的 module 全局共享，多 case 之间不重复注册——用 companion + 一次性 init。
     */
    companion object {
        init {
            JsonKit.registerSerializersModule(SerializersModule {
                polymorphic(Animal::class) {
                    subclass(Dog::class)
                    subclass(Cat::class)
                }
            })
        }
    }

    @Test
    fun openPolymorphismWorksAfterRegistration() {
        val animals: List<Animal> = listOf(Dog("Rex", 3), Cat("Whiskers", 9))
        val json = JsonKit.toJson(animals)
        assertTrue(json.contains("\"type\":\"dog\""), "应含 dog discriminator：$json")
        assertTrue(json.contains("\"type\":\"cat\""), "应含 cat discriminator：$json")
        assertTrue(json.contains("\"name\":\"Rex\""))
        assertTrue(json.contains("\"livesLeft\":9"))
    }

    @Test
    fun openPolymorphismRoundTrip() {
        val original: List<Animal> = listOf(Dog("Rex", 3), Cat("Whiskers", 9))
        val json = JsonKit.toJson(original)
        val restored = JsonKit.fromJson<List<Animal>>(json)
        assertEquals(original, restored)
    }
}
