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
 * Json 工具类 (基于 kotlinx.serialization)
 *
 * 实现拆分为三个文件：
 * - [JsonAdapters]：两个 [Json] 实例 + LocalDate/Time/DateTime 序列化器
 * - [JsonFallbackEncoder]：当类没有 `@Serializable` 时，用反射把任意对象转 [JsonElement] 的兜底
 * - 本文件：对外的公开 API 门面
 *
 * 注意事项：
 * 1. 支持 Kotlin @Serializable 数据类与普通类
 * 2. 反序列化时忽略多余字段，默认不输出 null
 * 3. 如需保留 null，请设置 preserveNull = true
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
object JsonKit {

    /** 日志记录器 */
    @PublishedApi
    internal val log = LogFactory.getLog(this::class)

    /** 默认 Json 引擎：忽略未知字段，不输出 null，支持 LocalDate */
    val defaultJson: Json get() = JsonAdapters.defaultJson

    /** 保留 null 值的 Json 引擎 */
    val preserveJson: Json get() = JsonAdapters.preserveJson

    /**
     * 注册额外的 [SerializersModule]，主要用于 polymorphic（开放多态）。
     *
     * Sealed 层级 kotlinx 已自动处理，无需注册——`@Serializable sealed class Base` +
     * `@Serializable class Sub : Base()` 直接 [toJson] 即可。
     *
     * Open 多态（非 sealed）必须通过这里注册：
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
     * 注意：应在应用启动阶段、首次走 JSON 序列化之前调用。运行中切换可能让正在进行的
     * 解码看到不同模块，行为不定义。
     */
    fun registerSerializersModule(module: SerializersModule) {
        JsonAdapters.registerSerializersModule(module)
    }

    /**
     * 返回 json 串中指定属性名的属性值
     *
     * @param jsonStr 待解析的 json 串
     * @param propertyName 属性名
     * @return 属性值，如果找不到属性或出错，则返回 null
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
        JsonAdapters.defaultJson.decodeFromString<T>(json)
    } catch (e: SerializationException) {
        log.error(e)
        null
    } catch (e: IllegalArgumentException) {
        log.error(e)
        null
    }

    /**
     * 序列化，将对象转为 json 串
     *
     * 规则：
     * 1) 若类上有 @Serializable（或在 module 中可解析到），直接用对应 KSerializer。
     * 2) 否则对常见类型(Map/List/数组/枚举/Java Time/data class)递归转 JsonElement（[JsonFallbackEncoder]）。
     * 3) 仍无法处理则抛出 SerializationException，提示加 @Serializable 或改成 data class。
     *
     * @param obj 要序列化的对象，可以是一般对象，也可以是 Collection 或数组，如果集合为空集合, 返回"[]"
     * @param preserveNull 是否保留 null 值，默认为否
     * @return 序列化后的 json 串，obj为null或出错时返回空串
     */
    inline fun <reified T> toJson(obj: T, preserveNull: Boolean = false): String {
        if (obj == null) return ""
        return try {
            val engine = if (preserveNull) JsonAdapters.preserveJson else JsonAdapters.defaultJson
            engine.encodeToString<T>(obj)
        } catch (_: SerializationException) {
            // 触发纯 kotlinx 兜底
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
     * 编译期已知类型版本（推荐优先使用）：
     * 直接通过编译器提供的 KSerializer<T> 编码为 JSON，再转 UTF-8 字节。
     *
     * @param data           待序列化对象
     * @param preserveNull   是否保留 null 字段
     * @return 字节数组
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
     * 运行期才知道类型（Any 版本）：
     * 尝试从运行时类型获取序列化器；若失败则递归转为 JsonElement。
     *
     * @param data           待序列化对象（允许为 null）
     * @param preserveNull   是否保留 null 字段
     * @return 字节数组
     */
    fun writeAnyAsBytes(data: Any?, preserveNull: Boolean = false): ByteArray {
        val engine = if (preserveNull) JsonAdapters.preserveJson else JsonAdapters.defaultJson

        if (data == null) return "null".toByteArray(StandardCharsets.UTF_8)

        val ser = JsonFallbackEncoder.findKSerializer(engine, data)
        val jsonString = if (ser != null) {
            JsonFallbackEncoder.encodeWithSerializer(engine, ser, data)
        } else {
            val elem = JsonFallbackEncoder.encodeAnyToJsonElement(engine, data)
            engine.encodeToString(JsonElement.serializer(), elem)
        }
        return jsonString.toByteArray(StandardCharsets.UTF_8)
    }

    /**
     * 反序列化：编译期已知目标类型。
     *
     * 等价于 Jackson 的 `readValue(bytes, T.class)`，但基于 kotlinx.serialization。
     *
     * 行为说明：
     * - 使用与编码一致的 `defaultJson` 配置（如：ignoreUnknownKeys=true）。
     * - 假设输入是 UTF-8 编码的 JSON 字节。
     * - 需要 `T`（及其嵌套类型）具备可用的序列化器：
     *   - 标注 `@Serializable`；或
     *   - 属于 `sealed` 多态层级并 `@Serializable`；或
     *   - 在 `SerializersModule` 中已注册（开放多态）。
     *
     * @param bytes  UTF-8 编码的 JSON 字节数组
     * @return       反序列化后的 T 实例
     * @throws kotlinx.serialization.SerializationException 当 JSON 结构不匹配或找不到序列化器时抛出
     */
    inline fun <reified T> readValue(bytes: ByteArray): T {
        val engine: Json = JsonAdapters.defaultJson
        val text = bytes.toString(Charsets.UTF_8)
        return engine.decodeFromString(serializer<T>(), text)
    }

    /**
     * 反序列化：运行期只有 `KClass<T>`（不包含泛型参数）。
     *
     * 适用场景：
     * - 目标类型 **不是** 泛型（例如 `User`），且类本身有可用序列化器（`@Serializable` 或 sealed 多态自动）。
     * - 如果目标是 `List<User>` / `Map<String, User>` 这类 **带泛型** 的类型，请使用 `readValue(bytes, kType)`。
     *
     * @param bytes   UTF-8 编码的 JSON 字节数组
     * @param kClass  目标类型的 `KClass<T>`（不含泛型信息）
     * @return        反序列化后的 T 实例
     * @throws kotlinx.serialization.SerializationException
     *         找不到 `@Serializable` 等可用序列化器；或目标结构与 JSON 不匹配。
     */
    @OptIn(InternalSerializationApi::class)
    fun <T : Any> readValue(bytes: ByteArray, kClass: KClass<T>): T {
        val engine: Json = JsonAdapters.defaultJson
        val text = bytes.toString(Charsets.UTF_8)

        val ser = kClass.serializerOrNull()
            ?: throw SerializationException(
                "无法反序列化到 ${kClass.qualifiedName}：该类缺少序列化器或为泛型类型。" +
                        "如需支持 List<T>/Map<K,V> 等，请改用 readValue(bytes, kType)。"
            )

        val decoded = engine.decodeFromString(ser, text)
        return kClass.java.cast(decoded)
    }

    /**
     * 反序列化：运行期携带完整类型信息的 `KType`（支持泛型）。
     *
     * 适用场景：
     * - 需要反序列化到 `List<User>`、`Map<String, User>`、`Array<Foo>` 等 **带泛型** 的目标类型。
     *
     * @param bytes  UTF-8 编码的 JSON 字节数组
     * @param kType  目标类型的 `KType`（可由 `typeOf<List<User>>()` 等获得）
     * @return       反序列化得到的对象实例（需在调用处 cast）
     * @throws kotlinx.serialization.SerializationException
     *         当 JSON 结构不匹配或某些参与类型缺少序列化器时抛出
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun readValue(bytes: ByteArray, kType: KType): Any? {
        val engine: Json = JsonAdapters.defaultJson
        val text = bytes.toString(Charsets.UTF_8)
        val ser: KSerializer<Any?> = serializer(kType)
        return engine.decodeFromString(ser, text)
    }

    /**
     * 递归把 [JsonElement] 转回 Kotlin 基础类型（用于 [getPropertyValue]）。
     *
     * 类型优先级（按 [JsonPrimitive] 解析顺序）：Boolean → Long → Int → Double → String content。
     * 即字符串 "100" 会优先被解析为 Long 而非保留为 String——本工具仅适用于动态/脚本场景，
     * 类型敏感的反序列化请走 [readValue]。
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
