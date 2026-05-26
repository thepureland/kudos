package io.kudos.base.data.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializerOrNull
import java.beans.Introspector
import java.lang.reflect.Method
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaMethod

/**
 * Reflection-based fallback encoder: recursively converts any object into a [JsonElement] when the
 * class is not annotated with `@Serializable`.
 *
 * Supported types:
 * 1. Primitives: String / Boolean / Number / Char -> [JsonPrimitive]
 * 2. Enums: encoded via `.name` -> [JsonPrimitive]
 * 3. java.time: LocalDate / LocalTime / LocalDateTime -> contextual serializers
 * 4. Map / Iterable / Array and all primitive arrays -> [JsonArray]
 * 5. data class: properties extracted in primary-constructor order -> [JsonObject] (stable output order)
 * 6. Java Bean: properties extracted via [Introspector] -> [JsonObject]
 *
 * Performance: data-class property accessors and Java Bean read methods are cached in a
 * [ConcurrentHashMap], so reflection runs only once per class.
 *
 * Treated as an implementation detail of [JsonKit]. Declared as `@PublishedApi internal` so the
 * inline functions in [JsonKit] can call it across files.
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
@PublishedApi
internal object JsonFallbackEncoder {

    /**
     * Data-class property accessor: a name plus a read closure.
     * The closure prefers the Java getter; if unavailable, it falls back to Kotlin reflection,
     * avoiding a full reflection chain on every call.
     *
     * @property name property name
     * @property read closure that reads the property value; returns null instead of throwing on failure
     */
    private data class DataClassPropertyAccessor(
        val name: String,
        val read: (Any) -> Any?
    )

    /** Reflected handle for `Json.encodeToString(SerializationStrategy, T)`, used to avoid overload ambiguity. */
    private val encodeToStringMethod: Method by lazy {
        Json::class.java.methods.firstOrNull {
            it.name == "encodeToString" &&
                it.parameterCount == 2 &&
                SerializationStrategy::class.java.isAssignableFrom(it.parameterTypes[0])
        } ?: error("Json.encodeToString(serializer, value) method not found")
    }
    /** Reflected handle for `Json.encodeToJsonElement(SerializationStrategy, T)`. */
    private val encodeToJsonElementMethod: Method by lazy {
        Json::class.java.methods.firstOrNull {
            it.name == "encodeToJsonElement" &&
                it.parameterCount == 2 &&
                SerializationStrategy::class.java.isAssignableFrom(it.parameterTypes[0])
        } ?: error("Json.encodeToJsonElement(serializer, value) method not found")
    }
    /** Cache of data-class property accessors keyed by class, to avoid walking constructors and properties on each call. */
    private val dataClassPropertyCache = ConcurrentHashMap<KClass<*>, List<DataClassPropertyAccessor>>()
    /** Cache of Java Bean read methods, to avoid running [Introspector.getBeanInfo] on every serialization. */
    private val javaBeanReadMethodCache = ConcurrentHashMap<Class<*>, List<Pair<String, Method>>>()

    /**
     * Recursively converts any object to a [JsonElement]. null -> [JsonNull].
     * Fallback entry point for cases the standard `@Serializable` path cannot handle.
     */
    @PublishedApi
    internal fun encodeAnyToJsonElement(json: Json, v: Any?): JsonElement {
        encodeFastPath(json, v)?.let { return it }
        return encodeComplexObject(json, requireNotNull(v))
    }

    /**
     * Looks up a [KSerializer] for a runtime object, trying contextual first and then [serializerOrNull].
     * Returns null when none is found (callers decide whether to fall back to [encodeAnyToJsonElement]).
     */
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    internal fun findKSerializer(json: Json, value: Any): KSerializer<*>? {
        val k = value::class
        json.serializersModule.getContextual(k)?.let { return it }
        k.serializerOrNull()?.let { return it }
        // Polymorphic serialization: currently disabled. If a polymorphic base class is later registered
        // in the SerializersModule, add json.serializersModule.getPolymorphic(Base::class, value) here.
        return null
    }

    /** Reflectively invokes the two-argument form of [Json.encodeToString] (with an explicit serializer). */
    internal fun encodeWithSerializer(json: Json, serializer: KSerializer<*>, value: Any): String {
        return encodeToStringMethod.invoke(json, serializer, value) as? String
            ?: error("Json.encodeToString returned an unexpected type")
    }

    /**
     * Reflectively invokes `Json.encodeToJsonElement(serializer, value)`, primarily for contextual types
     * such as LocalDate / LocalTime.
     *
     * @param json configured Json engine
     * @param serializer explicit serializer
     * @param value object to encode
     * @return the encoded [JsonElement]
     * @author K
     * @since 1.0.0
     */
    private fun encodeWithSerializerToElement(json: Json, serializer: KSerializer<*>, value: Any): JsonElement {
        return encodeToJsonElementMethod.invoke(json, serializer, value) as? JsonElement
            ?: error("Json.encodeToJsonElement returned an unexpected type")
    }

    /**
     * Fast path: common primitives / time types / collections / arrays are encoded directly to
     * [JsonElement]; on a miss, returns null so the caller can take the complex path
     * [encodeComplexObject].
     *
     * @param json configured Json engine
     * @param value object to encode; null allowed
     * @return the matching [JsonElement] on hit, otherwise null
     * @author K
     * @since 1.0.0
     */
    private fun encodeFastPath(json: Json, value: Any?): JsonElement? {
        return when (value) {
            null -> JsonNull
            is JsonElement -> value
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Char -> JsonPrimitive(value.toString())
            is Enum<*> -> JsonPrimitive(value.name)
            is LocalDate, is LocalTime, is LocalDateTime -> {
                val ser = findKSerializer(json, value)
                    ?: throw SerializationException("Missing serializer for ${value::class.qualifiedName}")
                encodeWithSerializerToElement(json, ser, value)
            }
            is Map<*, *> -> encodeMapToJsonObject(json, value)
            is Iterable<*> -> encodeIterable(json, value)
            is Array<*> -> encodeObjectArray(json, value)
            is IntArray -> encodePrimitiveArray(value.size) { JsonPrimitive(value[it]) }
            is LongArray -> encodePrimitiveArray(value.size) { JsonPrimitive(value[it]) }
            is ShortArray -> encodePrimitiveArray(value.size) { JsonPrimitive(value[it].toInt()) }
            is ByteArray -> encodePrimitiveArray(value.size) { JsonPrimitive(value[it].toInt()) }
            is DoubleArray -> encodePrimitiveArray(value.size) { JsonPrimitive(value[it]) }
            is FloatArray -> encodePrimitiveArray(value.size) { JsonPrimitive(value[it].toDouble()) }
            is BooleanArray -> encodePrimitiveArray(value.size) { JsonPrimitive(value[it]) }
            is CharArray -> encodePrimitiveArray(value.size) { JsonPrimitive(value[it].toString()) }
            else -> null
        }
    }

    /**
     * Complex path: data classes extract properties in primary-constructor order; other objects fall
     * back to Java Bean reflection. If neither applies, a [SerializationException] is thrown asking the
     * caller to add @Serializable.
     *
     * @param json configured Json engine
     * @param value non-null object to encode
     * @return the resulting [JsonObject]
     * @throws SerializationException when the type is neither a data class nor reflectable as a Java Bean
     * @author K
     * @since 1.0.0
     */
    private fun encodeComplexObject(json: Json, value: Any): JsonElement {
        val k = value::class
        if (k.isData) {
            val orderedProperties = getOrderedDataClassProperties(k)
            return encodeObjectEntries(
                json = json,
                size = orderedProperties.size,
                entries = orderedProperties,
                keyOf = { it.name },
                valueOf = { it.read(value) }
            )
        }
        return runCatching { javaBeanToJsonElement(json, value) }.getOrNull()
            ?: throw SerializationException(
                "Cannot serialize ${k.qualifiedName}: add @Serializable, convert to a data class, " +
                    "or wrap in a serializable DTO/JsonElement/collection of basic types"
            )
    }

    /**
     * Recursively encodes any [Iterable] as a [JsonArray].
     *
     * @param json configured Json engine
     * @param value collection to encode
     * @return the encoded [JsonArray]
     * @author K
     * @since 1.0.0
     */
    private fun encodeIterable(json: Json, value: Iterable<*>): JsonArray =
        JsonArray(value.map { encodeAnyToJsonElement(json, it) })

    /**
     * Recursively encodes an object array as a [JsonArray].
     *
     * @param json configured Json engine
     * @param value object array to encode
     * @return the encoded [JsonArray]
     * @author K
     * @since 1.0.0
     */
    private fun encodeObjectArray(json: Json, value: Array<*>): JsonArray =
        JsonArray(value.map { encodeAnyToJsonElement(json, it) })

    /**
     * Unified template for primitive-array encoding: reads by index and maps each element to a
     * [JsonElement] via [producer]. Marked inline to avoid the boxing overhead of hundreds of lambda
     * invocations.
     *
     * @param size array length
     * @param producer closure that maps an index to a [JsonElement]
     * @return the encoded [JsonArray]
     * @author K
     * @since 1.0.0
     */
    private inline fun encodePrimitiveArray(size: Int, producer: (Int) -> JsonElement): JsonArray =
        JsonArray(List(size, producer))

    /**
     * Encodes a [Map] as a [JsonObject]: null keys become `"null"`, non-string keys are converted via
     * [Any.toString]. This differs slightly from standard kotlinx behavior and is intended only for the
     * fallback path for types lacking `@Serializable`.
     *
     * @param json configured Json engine
     * @param value map to encode
     * @return the encoded [JsonObject]
     * @author K
     * @since 1.0.0
     */
    private fun encodeMapToJsonObject(json: Json, value: Map<*, *>): JsonObject {
        return encodeObjectEntries(
            json = json,
            size = value.size,
            entries = value.entries,
            keyOf = { entry ->
                when (val key = entry.key) {
                    null -> "null"
                    is String -> key
                    else -> key.toString()
                }
            },
            valueOf = { entry -> entry.value }
        )
    }

    /**
     * Extracts Java Bean style properties via [Introspector] and encodes them as a [JsonObject].
     * Skips [Object]-inherited properties (such as class). Property order is determined by
     * Introspector (typically alphabetical by getter name).
     *
     * @param json configured Json engine
     * @param bean object to encode
     * @return the encoded [JsonObject]
     * @author K
     * @since 1.0.0
     */
    private fun javaBeanToJsonElement(json: Json, bean: Any): JsonElement {
        val readMethods = javaBeanReadMethodCache.getOrPut(bean.javaClass) {
            Introspector.getBeanInfo(bean.javaClass, Any::class.java).propertyDescriptors.mapNotNull { pd ->
                val read = pd.readMethod ?: return@mapNotNull null
                val name = pd.name ?: return@mapNotNull null
                runCatching { read.isAccessible = true }
                name to read
            }
        }
        return encodeObjectEntries(
            json = json,
            size = readMethods.size,
            entries = readMethods,
            keyOf = { it.first },
            valueOf = { runCatching { it.second.invoke(bean) }.getOrNull() }
        )
    }

    /**
     * Projects any iterable of entries (Map.Entry, Pair, property tuples, ...) into a [JsonObject] via
     * keyOf/valueOf. Uses [LinkedHashMap] to preserve input order in the output; the projection is inline
     * to avoid closure overhead.
     *
     * @param T entry type
     * @param json configured Json engine
     * @param size capacity hint for pre-allocation
     * @param entries entries to encode
     * @param keyOf closure that extracts the JSON field name from an entry
     * @param valueOf closure that extracts the field value from an entry
     * @return the encoded [JsonObject]
     * @author K
     * @since 1.0.0
     */
    private inline fun <T> encodeObjectEntries(
        json: Json,
        size: Int,
        entries: Iterable<T>,
        keyOf: (T) -> String,
        valueOf: (T) -> Any?
    ): JsonObject {
        val encoded = LinkedHashMap<String, JsonElement>(size)
        entries.forEach { entry ->
            encoded[keyOf(entry)] = encodeAnyToJsonElement(json, valueOf(entry))
        }
        return JsonObject(encoded)
    }

    /**
     * Returns a data class's property accessors, with primary-constructor parameters first and other
     * member properties after. This keeps JSON field order aligned with source declaration order, which
     * is diff-friendly. Results are cached by class.
     *
     * @param kClass KClass of the data class
     * @return list of property accessors
     * @author K
     * @since 1.0.0
     */
    private fun getOrderedDataClassProperties(kClass: KClass<*>): List<DataClassPropertyAccessor> {
        return dataClassPropertyCache.getOrPut(kClass) {
            val ctorParamNames = kClass.primaryConstructor?.parameters?.mapNotNull { it.name } ?: emptyList()
            val ctorParamNameSet = ctorParamNames.toSet()
            val propsByName = kClass.memberProperties.associateBy { it.name }
            val ordered = mutableListOf<DataClassPropertyAccessor>()

            ctorParamNames.forEach { name ->
                val prop = propsByName[name] ?: return@forEach
                ordered.add(buildDataClassPropertyAccessor(name, prop))
            }
            propsByName.forEach { (name, prop) ->
                if (name !in ctorParamNameSet) {
                    ordered.add(buildDataClassPropertyAccessor(name, prop))
                }
            }
            ordered
        }
    }

    /**
     * Builds a [DataClassPropertyAccessor] for a single property.
     * Prefers Java getter reflection (faster) and falls back to Kotlin's [KProperty1.get] on failure.
     *
     * @param name property name
     * @param prop Kotlin reflection property reference
     * @return the property accessor
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildDataClassPropertyAccessor(
        name: String,
        prop: KProperty1<out Any, *>
    ): DataClassPropertyAccessor {
        runCatching { prop.isAccessible = true }
        val javaGetter = runCatching { prop.getter.javaMethod }.getOrNull()?.also { method ->
            runCatching { method.isAccessible = true }
        }
        return if (javaGetter != null) {
            DataClassPropertyAccessor(name) { instance ->
                runCatching { javaGetter.invoke(instance) }.getOrNull()
            }
        } else {
            val safeProp = prop as KProperty1<Any, *>
            DataClassPropertyAccessor(name) { instance ->
                runCatching { safeProp.get(instance) }.getOrNull()
            }
        }
    }
}
