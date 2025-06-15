package io.kudos.base.data.json

import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Json 工具类 (基于 kotlinx.serialization)
 * 注意事项：
 * 1. 支持 Kotlin @Serializable 数据类与普通类
 * 2. 反序列化时忽略多余字段，默认不输出 null
 * 3. 如需保留 null，请设置 preserveNull = true
 *
 * @author ChatGPT
 * @author K
 * @since 1.0.0
 */
object JsonKit {

    /** 日志记录器 */
    val log = LogFactory.getLog(this)

    /** 默认 Json 引擎：忽略未知字段，不输出 null，支持 LocalDate */
    val defaultJson: Json = Json {
        // 序列化时包含默认值与 null
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(LocalDate::class, LocalDateSerializer)
            contextual(LocalTime::class, LocalTimeSerializer)
            contextual(LocalDateTime::class, LocalDateTimeSerializer)
        }
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /** 保留 null 值 Json 引擎 */
    val preserveJson: Json = Json {
        // 序列化时包含默认值与 null
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(LocalDate::class, LocalDateSerializer)
            contextual(LocalTime::class, LocalTimeSerializer)
            contextual(LocalDateTime::class, LocalDateTimeSerializer)
        }
        ignoreUnknownKeys = true
        explicitNulls = true
    }

    /**
     * 返回 json 串中指定属性名的属性值
     *
     * @param jsonStr 待解析的 json 串
     * @param propertyName 属性名
     * @return 属性值，如果找不到属性或出错，则返回 null
     */
    fun getPropertyValue(jsonStr: String, propertyName: String): Any? = try {
        val root = defaultJson.parseToJsonElement(jsonStr)
        val elem = (root as? JsonObject)?.get(propertyName)
        elem?.let { unwrap(elem)}
    } catch (e: Exception) {
        log.error(e)
        null
    }

    /**
     * 将简单的 Json 串格式化成页面显示的字符串(去掉花括号、引号及最后面可能的逗号)
     *
     * @param simpleJsonStr 简单的 Json 串格式化(如：{"A":"b","B":'b'}), 为空将返回空串
     * @return 页面显示的字符串(如：A:b, B:b)
     */
    fun jsonToDisplay(simpleJsonStr: String): String = simpleJsonStr
        .takeIf { it.isNotBlank() }
        ?.removePrefix("{")
        ?.removeSuffix("}")
        ?.replace("[\"']".toRegex(), "")
        ?.removeSuffix(",")
        ?: ""

    /**
     * 反序列化, 将 json 串解析为指定类型
     *
     * @param T 目标类型
     * @param json json 串
     * @return 目标类型的实例，出错时返回 null
     */
    inline fun <reified T> fromJson(json: String): T? = try {
        defaultJson.decodeFromString<T>(json)
    } catch (e: Exception) {
        log.error(e)
        null
    }

    /**
     * 序列化，将对象转为 json 串
     *
     * @param obj 要序列化的对象，可以是一般对象，也可以是 Collection 或数组，如果集合为空集合, 返回"[]"
     * @param preserveNull 是否保留 null 值，默认为否
     * @return 序列化后的 json 串，出错时返回空串
     */
    inline fun <reified T> toJson(obj: T, preserveNull: Boolean = false): String = try {
        val engine = if (preserveNull) preserveJson else defaultJson
        engine.encodeToString<T>(obj)
    } catch (e: Exception) {
        log.error(e)
        ""
    }

    /**
     * 输出 jsonP 格式的数据
     *
     * @param functionName 函数名
     * @param obj 待序列化的对象，其 json 对象将作为函数的参数
     * @param preserveNull 是否保留 null 值，默认为否
     * @return jsonP 字符串
     */
    inline fun <reified T> toJsonP(functionName: String, obj: T, preserveNull: Boolean = false): String =
        "${functionName}(${toJson<T>(obj, preserveNull)})"

    /**
     * 当 json 里含有 bean 的部分属性时，用 json 串中的值更新该 bean 的该部分属性
     *
     * @param T bean 类型
     * @param jsonStr json 串
     * @param obj 待更新的 bean
     * @return 更新后的 bean，失败时返回 null
     */
    inline fun <reified T : Any> updateBean(jsonStr: String, obj: T): T? = try {
        val decoded: T = defaultJson.decodeFromString<T>(jsonStr)
        BeanKit.copyProperties(decoded, obj)
        obj
    } catch (e: Exception) {
        log.error(e)
        null
    }

    /**
     * 递归将 JsonElement 转为 Kotlin 类型
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

    // LocalDate 序列化器，支持长整型时间戳或字符串格式
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

    // LocalTime 序列化器，支持字符串格式或时间戳（秒）
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

    // LocalDateTime 序列化器，支持字符串格式或时间戳
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

}
