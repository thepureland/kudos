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
 * 集中放置 [JsonKit] 用到的两个 [Json] 实例和共享的序列化器。
 *
 * 设计为 [JsonKit] 的实现细节，避免外部直接依赖此对象——通过 [JsonKit.defaultJson] /
 * [JsonKit.preserveJson] 访问。声明为 `@PublishedApi internal` 是为了让 [JsonKit] 的
 * inline 函数能跨文件访问。
 *
 * @author K
 * @since 1.0.0
 */
@PublishedApi
internal object JsonAdapters {

    /**
     * LocalDate / LocalTime / LocalDateTime 的序列化模块，供 [defaultJson] 与 [preserveJson] 共用，
     * 避免配置漂移。
     */
    private val javaTimeSerializersModule = SerializersModule {
        contextual(LocalDate::class, LocalDateSerializer)
        contextual(LocalTime::class, LocalTimeSerializer)
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }

    /**
     * 用户通过 [registerSerializersModule] 注册的额外 SerializersModule。
     * 主要用于 polymorphic 配置——sealed 层级 kotlinx 自动处理无需注册；
     * open 多态需要在这里显式 `polymorphic(Base) { subclass(...) }`。
     */
    private val customSerializersModules = mutableListOf<SerializersModule>()

    /**
     * Json 实例构建用的锁。所有 [registerSerializersModule] 注册与 lazy 构建在同一把锁下，
     * 保证：注册一定对后续读可见、Json 实例对每个状态最多构建一次。
     */
    private val rebuildLock = Any()

    @Volatile private var cachedDefaultJson: Json? = null
    @Volatile private var cachedPreserveJson: Json? = null

    /** 默认 Json 引擎：忽略未知字段，不输出 null，支持 LocalDate */
    @PublishedApi
    internal val defaultJson: Json
        get() {
            cachedDefaultJson?.let { return it }
            return synchronized(rebuildLock) {
                cachedDefaultJson ?: buildJson(explicitNulls = false).also { cachedDefaultJson = it }
            }
        }

    /** 保留 null 值 Json 引擎 */
    @PublishedApi
    internal val preserveJson: Json
        get() {
            cachedPreserveJson?.let { return it }
            return synchronized(rebuildLock) {
                cachedPreserveJson ?: buildJson(explicitNulls = true).also { cachedPreserveJson = it }
            }
        }

    /**
     * 注册额外 SerializersModule（典型用法：polymorphic 注册）。
     *
     * 注册后已缓存的 Json 实例会被丢弃，下一次访问 [defaultJson] / [preserveJson]
     * 时按新配置重建。**应当在应用启动阶段、首次走 JSON 序列化之前调用**——
     * 在运行中切换可能让正在进行的解码遇到不同模块，行为不定义。
     */
    fun registerSerializersModule(module: SerializersModule) {
        synchronized(rebuildLock) {
            customSerializersModules.add(module)
            cachedDefaultJson = null
            cachedPreserveJson = null
        }
    }

    /**
     * 把内建 java.time module 与用户注册的所有自定义 module 合并后构造 [Json] 实例。
     * `explicitNulls` 控制 "JSON 中显式 null 时是否退回属性默认值"，由两个缓存路径（default / preserve）共用本方法。
     *
     * @param explicitNulls true 保留显式 null；false 退回默认值
     * @return 配置完毕的 [Json]
     * @author K
     * @since 1.0.0
     */
    private fun buildJson(explicitNulls: Boolean): Json {
        // 把内建的 java.time module 和用户注册的所有 module 合并起来
        val combinedModule = SerializersModule {
            include(javaTimeSerializersModule)
            customSerializersModules.forEach { include(it) }
        }
        return Json {
            encodeDefaults = true
            // JSON 中显式 null 且属性有默认值时，用默认值替代（如 "id":null -> id=""）
            coerceInputValues = true
            serializersModule = combinedModule
            ignoreUnknownKeys = true
            this.explicitNulls = explicitNulls
        }
    }
}

// ============================================================
// 时间类型序列化器：支持 ISO 字符串或 epoch 毫秒数字
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
