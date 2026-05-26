package io.kudos.base.data.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.modules.SerializersModule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Centralized holder for the two [Json] instances and shared serializers used by [JsonKit].
 *
 * Treated as an implementation detail of [JsonKit]; do not depend on this object directly --
 * access it via [JsonKit.defaultJson] / [JsonKit.preserveJson]. Declared as `@PublishedApi internal`
 * so that the inline functions in [JsonKit] can reach it across files.
 *
 * @author K
 * @since 1.0.0
 */
@PublishedApi
internal object JsonAdapters {

    /**
     * Serializer module for LocalDate / LocalTime / LocalDateTime, shared by [defaultJson] and [preserveJson]
     * to avoid configuration drift.
     */
    private val javaTimeSerializersModule = SerializersModule {
        contextual(LocalDate::class, LocalDateSerializer)
        contextual(LocalTime::class, LocalTimeSerializer)
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }

    /**
     * Additional SerializersModules registered by the user via [registerSerializersModule].
     * Primarily used for polymorphic configuration -- sealed hierarchies are handled automatically by
     * kotlinx and need no registration; open polymorphism requires an explicit
     * `polymorphic(Base) { subclass(...) }` here.
     */
    private val customSerializersModules = mutableListOf<SerializersModule>()

    /**
     * Lock used when building Json instances. All [registerSerializersModule] calls and lazy builds happen
     * under the same lock, guaranteeing that registrations are visible to subsequent reads and that each
     * Json instance is built at most once per state.
     */
    private val rebuildLock = Any()

    @Volatile private var cachedDefaultJson: Json? = null
    @Volatile private var cachedPreserveJson: Json? = null

    /** Default Json engine: ignores unknown fields, omits nulls, supports LocalDate. */
    @PublishedApi
    internal val defaultJson: Json
        get() {
            cachedDefaultJson?.let { return it }
            return synchronized(rebuildLock) {
                cachedDefaultJson ?: buildJson(explicitNulls = false).also { cachedDefaultJson = it }
            }
        }

    /** Json engine that preserves null values. */
    @PublishedApi
    internal val preserveJson: Json
        get() {
            cachedPreserveJson?.let { return it }
            return synchronized(rebuildLock) {
                cachedPreserveJson ?: buildJson(explicitNulls = true).also { cachedPreserveJson = it }
            }
        }

    /**
     * Registers an additional SerializersModule (typically used for polymorphic registration).
     *
     * After registration the cached Json instances are discarded and rebuilt with the new configuration
     * on the next access to [defaultJson] / [preserveJson]. **Call this during application start-up,
     * before the first JSON serialization** -- swapping modules at runtime may let in-flight decoding
     * see different modules and the behavior is undefined.
     */
    fun registerSerializersModule(module: SerializersModule) {
        synchronized(rebuildLock) {
            customSerializersModules.add(module)
            cachedDefaultJson = null
            cachedPreserveJson = null
        }
    }

    /**
     * Builds a [Json] instance by merging the built-in java.time module with all user-registered modules.
     * `explicitNulls` controls "whether to fall back to property defaults when JSON contains an explicit
     * null"; this method is shared by both cache paths (default / preserve).
     *
     * @param explicitNulls true to preserve explicit nulls; false to fall back to defaults
     * @return the configured [Json]
     * @author K
     * @since 1.0.0
     */
    private fun buildJson(explicitNulls: Boolean): Json {
        // Merge the built-in java.time module with all user-registered modules.
        val combinedModule = SerializersModule {
            include(javaTimeSerializersModule)
            customSerializersModules.forEach { include(it) }
        }
        return Json {
            encodeDefaults = true
            // When JSON contains explicit null and the property has a default, substitute the default
            // (e.g. "id":null -> id="").
            coerceInputValues = true
            serializersModule = combinedModule
            ignoreUnknownKeys = true
            this.explicitNulls = explicitNulls
        }
    }
}

// ============================================================
// Time-type serializers: accept ISO strings or epoch-millisecond numbers
// ============================================================

private object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement()
            ?: throw SerializationException("Expected JsonDecoder for LocalDate deserialization")
        if (element is JsonPrimitive) {
            element.longOrNull?.let { epoch ->
                return Instant.ofEpochMilli(epoch)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            return LocalDate.parse(element.content)
        }
        throw SerializationException("Unexpected JSON element for LocalDate: $element")
    }
}

private object LocalTimeSerializer : KSerializer<LocalTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalTime {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement()?.jsonPrimitive
            ?: throw SerializationException("Expected LocalTime element")
        element.longOrNull?.let { epoch ->
            return Instant.ofEpochMilli(epoch)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }
        return LocalTime.parse(element.content)
    }
}

private object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDateTime {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement()?.jsonPrimitive
            ?: throw SerializationException("Expected LocalDateTime element")
        element.longOrNull?.let { epoch ->
            return Instant.ofEpochMilli(epoch)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
        }
        return LocalDateTime.parse(element.content)
    }
}
