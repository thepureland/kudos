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
 * Polymorphic serialization test cases.
 *
 * Covers two kinds of polymorphism:
 * - Sealed hierarchies: kotlinx handles them automatically, no JsonKit registration required.
 * - Open polymorphism: requires registration via [JsonKit.registerSerializersModule].
 *
 * @author K
 * @since 1.0.0
 */
internal class JsonKitPolymorphicTest {

    // ============================================================
    // Sealed: calling toJson directly should emit polymorphic JSON (with a type field)
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
        // Use the compile-time-known type List<Shape> so kotlinx takes the sealed polymorphism path
        val shapes: List<Shape> = listOf(Shape.Circle(1.0), Shape.Square(2.0))
        val json = JsonKit.toJson(shapes)

        // Should contain both type identifiers; exact format determined by kotlinx (default type field name is "type")
        assertTrue(json.contains("\"type\":\"circle\""), "Should contain circle discriminator: $json")
        assertTrue(json.contains("\"type\":\"square\""), "Should contain square discriminator: $json")
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
    // Open polymorphism: requires registerSerializersModule
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
     * The registered module is globally shared and should not be re-registered across cases — done via companion + one-time init.
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
        assertTrue(json.contains("\"type\":\"dog\""), "Should contain dog discriminator: $json")
        assertTrue(json.contains("\"type\":\"cat\""), "Should contain cat discriminator: $json")
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
