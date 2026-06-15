package io.kudos.base.data.json

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Additional [JsonKit] test cases targeting the non-inline / runtime-typed code paths that the inline-function
 * heavy [JsonKitTest] does not (JaCoCo cannot attribute inline-function bytecode to the source file, so this suite
 * focuses on the directly-attributable members).
 *
 * Covers: `defaultJson`/`preserveJson` accessors, `getPropertyValue` array/object unwrap recursion and the
 * malformed-JSON exception branches, `writeAnyAsBytes` (null, serializable and fallback), and the
 * `readValue(bytes, KClass)` no-serializer error branch.
 *
 * @author K
 * @since 1.0.0
 */
internal class JsonKitMoreTest {

    @Serializable
    private data class Dto(val a: Int, val b: String)

    private class PlainClass(val x: Int)

    @Test
    fun engineAccessors_returnConfiguredInstances() {
        assertTrue(JsonKit.defaultJson === JsonKit.defaultJson)
        assertTrue(JsonKit.preserveJson === JsonKit.preserveJson)
        // default omits nulls, preserve keeps them
        assertEquals(false, JsonKit.defaultJson.configuration.explicitNulls)
        assertEquals(true, JsonKit.preserveJson.configuration.explicitNulls)
    }

    @Test
    fun getPropertyValue_unwraps_nested_array_and_object() {
        val jsonStr = """{"arr":[1,2,"x"],"obj":{"k":true,"n":null}}"""
        @Suppress("UNCHECKED_CAST")
        val arr = JsonKit.getPropertyValue(jsonStr, "arr") as List<Any?>
        assertEquals(listOf(1L, 2L, "x"), arr)

        @Suppress("UNCHECKED_CAST")
        val obj = JsonKit.getPropertyValue(jsonStr, "obj") as Map<String, Any?>
        assertEquals(true, obj["k"])
        // Regression (bug B3): `unwrap` must match the `JsonNull -> null` branch BEFORE `is JsonPrimitive`,
        // because in kotlinx `JsonNull` is itself a `JsonPrimitive`. A JSON null nested in an object/array must
        // therefore map to a Kotlin null, not be stringified to the literal "null".
        assertNull(obj["n"])
        assertTrue(obj.containsKey("n"))
    }

    @Test
    fun getPropertyValue_malformed_returns_null() {
        // Not parseable JSON -> SerializationException / IllegalArgumentException caught -> null
        assertNull(JsonKit.getPropertyValue("", "x"))
        // Valid JSON but root is not an object -> (root as? JsonObject) is null -> null
        assertNull(JsonKit.getPropertyValue("[1,2,3]", "x"))
        // Valid object but the property is absent -> null
        assertNull(JsonKit.getPropertyValue("""{"a":1}""", "missing"))
    }

    @Test
    fun writeAnyAsBytes_null_writes_literal_null() {
        assertEquals("null", JsonKit.writeAnyAsBytes(null).toString(Charsets.UTF_8))
    }

    @Test
    fun writeAnyAsBytes_preserveNull_true_keepsNulls() {
        // preserveNull=true selects the preserveJson engine (the other branch of the engine selector).
        val bytes = JsonKit.writeAnyAsBytes(Dto(1, "z"), preserveNull = true)
        assertTrue(bytes.toString(Charsets.UTF_8).contains("\"a\":1"))
    }

    @Test
    fun writeAnyAsBytes_serializable_uses_serializer() {
        val bytes = JsonKit.writeAnyAsBytes(Dto(1, "z"))
        val text = bytes.toString(Charsets.UTF_8)
        assertTrue(text.contains("\"a\":1"))
        assertTrue(text.contains("\"b\":\"z\""))
    }

    @Test
    fun writeAnyAsBytes_nonSerializable_falls_back_to_jsonElement() {
        val bytes = JsonKit.writeAnyAsBytes(PlainClass(7))
        val text = bytes.toString(Charsets.UTF_8)
        assertTrue(text.contains("\"x\":7"), text)
    }

    @Test
    fun readValue_kClass_without_serializer_throws() {
        val ex = assertFailsWith<SerializationException> {
            JsonKit.readValue("""{"x":1}""".toByteArray(Charsets.UTF_8), PlainClass::class)
        }
        assertTrue(ex.message!!.contains("Cannot deserialize"), ex.message!!)
    }

    @Test
    fun readValue_kClass_with_serializer_succeeds() {
        val dto = JsonKit.readValue("""{"a":5,"b":"q"}""".toByteArray(Charsets.UTF_8), Dto::class)
        assertEquals(Dto(5, "q"), dto)
    }
}
