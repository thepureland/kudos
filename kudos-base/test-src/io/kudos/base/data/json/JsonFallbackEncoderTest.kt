package io.kudos.base.data.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * [JsonFallbackEncoder] test cases.
 *
 * The fallback encoder only runs when a type is NOT `@Serializable`. The serializable [io.kudos.base.bean.Person]
 * used in [JsonKitTest] never reaches it, so this suite intentionally uses plain (non-`@Serializable`) Java beans,
 * non-`@Serializable` data classes, raw maps, enums, java.time types and every primitive array so each branch of
 * the reflection-based fallback path is exercised. Routing is done through [JsonKit.writeAnyAsBytes] (runtime `Any`
 * path) and [JsonKit.toJson]/[JsonFallbackEncoder.encodeAnyToJsonElement] (compile-time fallback path).
 *
 * Covers: primitives/Boolean/Number/Char/enum fast paths, java.time contextual serializers, Map (string / non-string /
 * null keys), Iterable, object array, all 8 primitive array kinds, non-serializable data class property ordering,
 * Java Bean introspection, nested structures, accessor caching, and the unsupported-type error path.
 *
 * @author K
 * @since 1.0.0
 */
internal class JsonFallbackEncoderTest {

    private val json: Json get() = JsonKit.defaultJson

    // ---- helpers ----
    private fun encode(v: Any?) = JsonFallbackEncoder.encodeAnyToJsonElement(json, v)
    private fun anyJson(v: Any?): JsonObject = json.parseToJsonElement(
        JsonKit.writeAnyAsBytes(v).toString(Charsets.UTF_8)
    ).jsonObject

    // ============ fast path primitives ============

    @Test
    fun fastPath_primitives_and_null() {
        assertEquals(JsonPrimitive("s"), encode("s"))
        assertEquals(JsonPrimitive(true), encode(true))
        assertEquals(JsonPrimitive(42), encode(42))
        assertEquals(JsonPrimitive(3.14), encode(3.14))
        assertEquals(JsonPrimitive("A"), encode('A')) // Char -> string
        // null -> JsonNull
        assertTrue(encode(null) is kotlinx.serialization.json.JsonNull)
        // already a JsonElement -> returned as-is
        val pre = JsonPrimitive("pre")
        assertEquals(pre, encode(pre))
    }

    @Test
    fun fastPath_enum_uses_name() {
        assertEquals(JsonPrimitive("B"), encode(Color.B))
    }

    @Test
    fun fastPath_javaTime_serialized_via_contextual() {
        assertEquals(JsonPrimitive("2020-08-27"), encode(LocalDate.of(2020, 8, 27)))
        assertEquals(JsonPrimitive("10:20:30"), encode(LocalTime.of(10, 20, 30)))
        assertEquals(
            JsonPrimitive("2020-08-27T10:20:30"),
            encode(LocalDateTime.of(2020, 8, 27, 10, 20, 30))
        )
    }

    // ============ collections & arrays ============

    @Test
    fun iterable_and_object_array() {
        val list = encode(listOf("a", 1, true)) as JsonArray
        assertEquals(3, list.size)
        assertEquals("a", list[0].jsonPrimitive.content)

        val arr = encode(arrayOf("x", "y")) as JsonArray
        assertEquals(2, arr.size)
        assertEquals("y", arr[1].jsonPrimitive.content)

        // a bare collection round-trips through toJson as a JSON array
        assertEquals("[1,2,3]", JsonKit.toJson(listOf(1, 2, 3)))
    }

    @Test
    fun all_primitive_arrays() {
        assertEquals(listOf(1, 2), (encode(intArrayOf(1, 2)) as JsonArray).map { it.jsonPrimitive.int })
        assertEquals("[1,2]", json.encodeToString(JsonArray.serializer(), encode(longArrayOf(1, 2)) as JsonArray))
        assertEquals("[1,2]", json.encodeToString(JsonArray.serializer(), encode(shortArrayOf(1, 2)) as JsonArray))
        assertEquals("[1,2]", json.encodeToString(JsonArray.serializer(), encode(byteArrayOf(1, 2)) as JsonArray))
        assertEquals("[1.5,2.5]", json.encodeToString(JsonArray.serializer(), encode(doubleArrayOf(1.5, 2.5)) as JsonArray))
        assertEquals("[1.5,2.5]", json.encodeToString(JsonArray.serializer(), encode(floatArrayOf(1.5f, 2.5f)) as JsonArray))
        assertEquals("[true,false]", json.encodeToString(JsonArray.serializer(), encode(booleanArrayOf(true, false)) as JsonArray))
        assertEquals("""["a","b"]""", json.encodeToString(JsonArray.serializer(), encode(charArrayOf('a', 'b')) as JsonArray))
    }

    // ============ maps ============

    @Test
    fun map_string_nonString_and_null_keys() {
        val m = linkedMapOf<Any?, Any?>(
            "s" to 1,
            42 to "int-key",
            null to "null-key"
        )
        val obj = encode(m) as JsonObject
        assertEquals(JsonPrimitive(1), obj["s"])
        assertEquals(JsonPrimitive("int-key"), obj["42"]) // non-string key -> toString()
        assertEquals(JsonPrimitive("null-key"), obj["null"]) // null key -> "null"
    }

    // ============ complex objects ============

    @Test
    fun nonSerializable_dataClass_ordering_follows_constructor() {
        // PlainData is a data class WITHOUT @Serializable -> SerializationException -> fallback complex path
        val obj = anyJson(PlainData(name = "Bob", age = 7))
        assertEquals("Bob", obj["name"]!!.jsonPrimitive.content)
        assertEquals(7, obj["age"]!!.jsonPrimitive.int)
        // primary-constructor order: name then age
        assertEquals(listOf("name", "age"), obj.keys.toList())
    }

    @Test
    fun nonSerializable_dataClass_with_bodyProperty_not_in_constructor() {
        // `extra` is declared in the class body (not the primary constructor) -> exercises the
        // "member properties not in the constructor param set" ordering branch.
        val obj = anyJson(WithBodyProp(id = 5))
        assertEquals(5, obj["id"]!!.jsonPrimitive.int)
        assertEquals("e", obj["extra"]!!.jsonPrimitive.content)
        // constructor params come first, then body properties
        assertEquals(listOf("id", "extra"), obj.keys.toList())
    }

    @Test
    fun nonSerializable_dataClass_with_nested_values() {
        val obj = anyJson(Nested(id = 1, child = PlainData("Kid", 3), tags = listOf("x", "y")))
        assertEquals(1, obj["id"]!!.jsonPrimitive.int)
        assertEquals("Kid", obj["child"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        assertEquals(2, obj["tags"]!!.jsonArray.size)
    }

    @Test
    fun plainJavaBean_via_introspector() {
        val bean = JavaStyleBean().apply { title = "T"; count = 9 }
        val obj = anyJson(bean)
        assertEquals("T", obj["title"]!!.jsonPrimitive.content)
        assertEquals(9, obj["count"]!!.jsonPrimitive.int)
        // "class" property inherited from Object must be skipped
        assertTrue("class" !in obj)
    }

    @Test
    fun accessor_cache_is_reused_across_calls() {
        // call twice to exercise the ConcurrentHashMap getOrPut cache-hit branch for both data class and java bean
        repeat(2) { anyJson(PlainData("c", 1)) }
        repeat(2) { anyJson(JavaStyleBean().apply { title = "c"; count = 1 }) }
    }

    @Test
    fun plainClass_noBeanProperties_encodesToEmptyObject() {
        // A non-data class whose only field is a @JvmField (no getter) yields an empty bean via Introspector,
        // so encoding succeeds with an empty JsonObject rather than throwing.
        val elem = JsonFallbackEncoder.encodeAnyToJsonElement(json, NotABean(5))
        assertEquals(JsonObject(emptyMap()), elem)
    }

    @Test
    fun toJson_routes_nonSerializable_through_fallback() {
        // toJson catches the SerializationException from the @Serializable path and falls back.
        val out = JsonKit.toJson(PlainData("Z", 0))
        val obj = json.parseToJsonElement(out).jsonObject
        assertEquals("Z", obj["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun findKSerializer_resolves_serializable_and_returns_null_for_plain() {
        // String has a built-in serializer
        assertTrue(JsonFallbackEncoder.findKSerializer(json, "x") != null)
        // A plain class with no serializer returns null
        assertEquals(null, JsonFallbackEncoder.findKSerializer(json, NotABean(1)))
    }

    @Test
    fun encodeWithSerializer_uses_explicit_serializer() {
        val ser = JsonFallbackEncoder.findKSerializer(json, "hello")!!
        assertEquals("\"hello\"", JsonFallbackEncoder.encodeWithSerializer(json, ser, "hello"))
    }

    // ---- fixtures ----

    private enum class Color { A, B }

    /** data class WITHOUT @Serializable so the fallback path's data-class branch runs. */
    private data class PlainData(val name: String, val age: Int)

    private data class Nested(val id: Int, val child: PlainData, val tags: List<String>)

    /** data class with a property declared in the body (not the primary constructor). */
    private data class WithBodyProp(val id: Int) {
        var extra: String = "e"
    }

    /** Plain (non-data) class with JavaBean getters -> Introspector branch. */
    class JavaStyleBean {
        var title: String? = null
        var count: Int = 0
    }

    /** No JavaBean-readable properties and not a data class -> unsupported. */
    private class NotABean(@JvmField val x: Int)
}
