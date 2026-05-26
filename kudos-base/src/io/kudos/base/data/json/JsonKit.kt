package io.kudos.base.data.json

import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * JSON utility class (based on kotlinx.serialization).
 *
 * Implementation split across three files:
 * - [JsonAdapters]: two [Json] instances plus LocalDate/Time/DateTime serializers
 * - [JsonFallbackEncoder]: reflection-based fallback that converts arbitrary objects to [JsonElement]
 *   when the class lacks `@Serializable`
 * - This file: the public API facade
 *
 * Notes:
 * 1. Supports Kotlin @Serializable data classes as well as plain classes.
 * 2. Deserialization ignores unknown fields; nulls are omitted by default.
 * 3. To preserve nulls, set preserveNull = true.
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
object JsonKit {

    /** Logger. */
    @PublishedApi
    internal val log = LogFactory.getLog(this::class)

    /** Default Json engine: ignores unknown fields, omits nulls, supports LocalDate. */
    val defaultJson: Json get() = JsonAdapters.defaultJson

    /** Json engine that preserves null values. */
    val preserveJson: Json get() = JsonAdapters.preserveJson

    /**
     * Registers an additional [SerializersModule], primarily for open polymorphism.
     *
     * Sealed hierarchies are handled automatically by kotlinx and need no registration --
     * `@Serializable sealed class Base` + `@Serializable class Sub : Base()` can be passed to [toJson] directly.
     *
     * Open polymorphism (non-sealed) must be registered here:
     *
     * ```kotlin
     * JsonKit.registerSerializersModule(SerializersModule {
     *     polymorphic(Animal::class) {
     *         subclass(Dog::class)
     *         subclass(Cat::class)
     *     }
     * })
     * ```
     *
     * Note: call this during application start-up, before the first JSON serialization. Swapping at
     * runtime may expose decoding-in-progress to a different module and behavior is undefined.
     */
    fun registerSerializersModule(module: SerializersModule) {
        JsonAdapters.registerSerializersModule(module)
    }

    /**
     * Returns the value of the given property name from the JSON string.
     *
     * @param jsonStr JSON string to parse
     * @param propertyName property name
     * @return the property value, or null if the property is missing or an error occurs
     */
    fun getPropertyValue(jsonStr: String, propertyName: String): Any? = try {
        val root = JsonAdapters.defaultJson.parseToJsonElement(jsonStr)
        val elem = (root as? JsonObject)?.get(propertyName)
        elem?.let { unwrap(elem) }
    } catch (e: SerializationException) {
        log.error(e)
        null
    } catch (e: IllegalArgumentException) {
        log.error(e)
        null
    }

    /**
     * Formats a simple JSON string into a display-friendly string (strips braces, quotes, and any trailing comma).
     *
     * @param simpleJsonStr simple JSON string (e.g. {"A":"b","B":'b'}); returns an empty string when blank
     * @return display string (e.g. A:b, B:b)
     */
    fun jsonToDisplay(simpleJsonStr: String): String = simpleJsonStr
        .takeIf { it.isNotBlank() }
        ?.removePrefix("{")
        ?.removeSuffix("}")
        ?.replace("[\"']".toRegex(), "")
        ?.removeSuffix(",")
        ?: ""

    /**
     * Deserializes the JSON string into the specified type.
     *
     * @param T target type
     * @param json JSON string
     * @return an instance of the target type, or null on error
     */
    inline fun <reified T> fromJson(json: String): T? = try {
        JsonAdapters.defaultJson.decodeFromString<T>(json)
    } catch (e: SerializationException) {
        log.error(e)
        null
    } catch (e: IllegalArgumentException) {
        log.error(e)
        null
    }

    /**
     * Serializes the object into a JSON string.
     *
     * Rules:
     * 1) If the class is `@Serializable` (or its serializer is resolvable from the module), use the corresponding KSerializer.
     * 2) Otherwise recursively convert common types (Map/List/array/enum/java.time/data class) to JsonElement
     *    via [JsonFallbackEncoder].
     * 3) If still unsupported, throw SerializationException asking the caller to add @Serializable or use a data class.
     *
     * @param obj object to serialize; may be a regular object, a Collection, or an array; an empty collection returns "[]"
     * @param preserveNull whether to keep null values; defaults to false
     * @return the serialized JSON string; an empty string when obj is null or on error
     */
    inline fun <reified T> toJson(obj: T, preserveNull: Boolean = false): String {
        if (obj == null) return ""
        return try {
            val engine = if (preserveNull) JsonAdapters.preserveJson else JsonAdapters.defaultJson
            engine.encodeToString<T>(obj)
        } catch (_: SerializationException) {
            // Trigger the pure-kotlinx fallback.
            val engine = if (preserveNull) JsonAdapters.preserveJson else JsonAdapters.defaultJson
            val elem = JsonFallbackEncoder.encodeAnyToJsonElement(engine, obj as Any?)
            engine.encodeToString(JsonElement.serializer(), elem)
        } catch (e: IllegalArgumentException) {
            log.error(e)
            ""
        } catch (e: RuntimeException) {
            log.error(e)
            ""
        }
    }

    /**
     * Outputs data in JSONP format.
     *
     * @param functionName function name
     * @param obj object to serialize; its JSON form is used as the function argument
     * @param preserveNull whether to keep null values; defaults to false
     * @return the JSONP string
     */
    inline fun <reified T> toJsonP(functionName: String, obj: T, preserveNull: Boolean = false): String =
        "${functionName}(${toJson<T>(obj, preserveNull)})"

    /**
     * Updates the matching properties of the given bean from values in the JSON string when the JSON
     * contains only a subset of the bean's properties.
     *
     * @param T bean type
     * @param jsonStr JSON string
     * @param obj bean to update
     * @return the updated bean, or null on failure
     */
    inline fun <reified T : Any> updateBean(jsonStr: String, obj: T): T? = try {
        val decoded: T = JsonAdapters.defaultJson.decodeFromString<T>(jsonStr)
        BeanKit.copyProperties(decoded, obj)
        obj
    } catch (e: SerializationException) {
        log.error(e)
        null
    } catch (e: IllegalArgumentException) {
        log.error(e)
        null
    }

    /**
     * Compile-time-known type variant (preferred):
     * uses the KSerializer<T> provided by the compiler to encode to JSON and convert to UTF-8 bytes.
     *
     * @param data           object to serialize
     * @param preserveNull   whether to keep null fields
     * @return the byte array
     */
    inline fun <reified T> writeValueAsBytes(data: T, preserveNull: Boolean = false): ByteArray {
        val engine = if (preserveNull) JsonAdapters.preserveJson else JsonAdapters.defaultJson
        val jsonString = try {
            engine.encodeToString(serializer<T>(), data)
        } catch (_: SerializationException) {
            val elem = JsonFallbackEncoder.encodeAnyToJsonElement(engine, data as Any?)
            engine.encodeToString(JsonElement.serializer(), elem)
        }
        return jsonString.toByteArray(StandardCharsets.UTF_8)
    }

    /**
     * Runtime-typed variant (Any):
     * looks up a serializer from the runtime type; on failure recursively converts to JsonElement.
     *
     * @param data           object to serialize (null allowed)
     * @param preserveNull   whether to keep null fields
     * @return the byte array
     */
    fun writeAnyAsBytes(data: Any?, preserveNull: Boolean = false): ByteArray {
        val engine = if (preserveNull) JsonAdapters.preserveJson else JsonAdapters.defaultJson

        if (data == null) return "null".toByteArray(StandardCharsets.UTF_8)

        val jsonString = JsonFallbackEncoder.findKSerializer(engine, data)?.let { ser ->
            JsonFallbackEncoder.encodeWithSerializer(engine, ser, data)
        } ?: run {
            val elem = JsonFallbackEncoder.encodeAnyToJsonElement(engine, data)
            engine.encodeToString(JsonElement.serializer(), elem)
        }
        return jsonString.toByteArray(StandardCharsets.UTF_8)
    }

    /**
     * Deserialization: compile-time-known target type.
     *
     * Equivalent to Jackson's `readValue(bytes, T.class)`, but based on kotlinx.serialization.
     *
     * Behavior:
     * - Uses the same `defaultJson` configuration as encoding (e.g. ignoreUnknownKeys=true).
     * - Assumes the input is UTF-8 encoded JSON bytes.
     * - Requires `T` (and its nested types) to have available serializers:
     *   - annotated with `@Serializable`; or
     *   - part of a `sealed` polymorphic hierarchy that is `@Serializable`; or
     *   - registered in `SerializersModule` (open polymorphism).
     *
     * @param bytes  UTF-8 encoded JSON byte array
     * @return       the deserialized instance of T
     * @throws kotlinx.serialization.SerializationException when the JSON structure does not match or no serializer can be found
     */
    inline fun <reified T> readValue(bytes: ByteArray): T {
        val engine: Json = JsonAdapters.defaultJson
        val text = bytes.toString(Charsets.UTF_8)
        return engine.decodeFromString(serializer<T>(), text)
    }

    /**
     * Deserialization: only `KClass<T>` is available at runtime (no generic parameters).
     *
     * Use when:
     * - The target type is **not** generic (e.g. `User`) and the class has an available serializer
     *   (`@Serializable` or automatic via sealed polymorphism).
     * - If the target is a **generic** type such as `List<User>` or `Map<String, User>`, use `readValue(bytes, kType)` instead.
     *
     * @param bytes   UTF-8 encoded JSON byte array
     * @param kClass  `KClass<T>` of the target type (without generic info)
     * @return        the deserialized instance of T
     * @throws kotlinx.serialization.SerializationException
     *         when no `@Serializable` or other usable serializer is found, or the target structure does not match the JSON.
     */
    @OptIn(InternalSerializationApi::class)
    fun <T : Any> readValue(bytes: ByteArray, kClass: KClass<T>): T {
        val engine: Json = JsonAdapters.defaultJson
        val text = bytes.toString(Charsets.UTF_8)

        val ser = kClass.serializerOrNull()
            ?: throw SerializationException(
                "Cannot deserialize to ${kClass.qualifiedName}: the class lacks a serializer or is a generic type. " +
                        "For List<T>/Map<K,V> and similar, use readValue(bytes, kType) instead."
            )

        val decoded = engine.decodeFromString(ser, text)
        return kClass.java.cast(decoded)
    }

    /**
     * Deserialization: full runtime type information via `KType` (supports generics).
     *
     * Use when:
     * - The target type is **generic**, such as `List<User>`, `Map<String, User>`, or `Array<Foo>`.
     *
     * @param bytes  UTF-8 encoded JSON byte array
     * @param kType  `KType` of the target type (can be obtained from `typeOf<List<User>>()` etc.)
     * @return       the deserialized object instance (cast at the call site)
     * @throws kotlinx.serialization.SerializationException
     *         when the JSON structure does not match or a participating type lacks a serializer
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun readValue(bytes: ByteArray, kType: KType): Any? {
        val engine: Json = JsonAdapters.defaultJson
        val text = bytes.toString(Charsets.UTF_8)
        val ser: KSerializer<Any?> = serializer(kType)
        return engine.decodeFromString(ser, text)
    }

    /**
     * Recursively converts a [JsonElement] back to Kotlin primitive types (used by [getPropertyValue]).
     *
     * Type-resolution order (matching [JsonPrimitive] parsing): Boolean -> Long -> Int -> Double -> String content.
     * That is, the string "100" is parsed as Long rather than kept as String -- this helper is intended only for
     * dynamic/scripting scenarios. For type-sensitive deserialization use [readValue].
     */
    private fun unwrap(elem: JsonElement): Any? = when (elem) {
        is JsonPrimitive -> elem.booleanOrNull
            ?: elem.longOrNull
            ?: elem.intOrNull
            ?: elem.doubleOrNull
            ?: elem.content

        is JsonArray -> elem.map { unwrap(it) }
        is JsonObject -> elem.mapValues { unwrap(it.value) }
        JsonNull -> null
    }
}
