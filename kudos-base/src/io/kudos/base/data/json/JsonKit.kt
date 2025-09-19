package io.kudos.base.data.json

import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import java.beans.Introspector
import java.nio.charset.StandardCharsets
import java.time.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

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
        elem?.let { unwrap(elem) }
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
     * 规则：
     * 1) 若类上有 @Serializable（或在 module 中可解析到），直接用对应 KSerializer。
     * 2) 否则对常见类型(Map/List/数组/枚举/Java Time/data class)递归转 JsonElement。
     * 3) 仍无法处理则抛出 SerializationException，提示加 @Serializable 或改成 data class。
     *
     * @param obj 要序列化的对象，可以是一般对象，也可以是 Collection 或数组，如果集合为空集合, 返回"[]"
     * @param preserveNull 是否保留 null 值，默认为否
     * @return 序列化后的 json 串，出错时返回空串
     */
    inline fun <reified T> toJson(obj: T, preserveNull: Boolean = false): String = try {
        val engine = if (preserveNull) preserveJson else defaultJson
        engine.encodeToString<T>(obj)
    } catch (_: SerializationException) {
        // 触发纯 kotlinx 兜底
        val engine = if (preserveNull) preserveJson else defaultJson
        val elem = _encodeAnyToJsonElement(engine, obj as Any?)
        engine.encodeToString(JsonElement.serializer(), elem)
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
     * 编译期已知类型版本（推荐优先使用）：
     * 直接通过编译器提供的 KSerializer<T> 编码为 JSON，再转 UTF-8 字节。
     *
     * @param data           待序列化对象
     * @param preserveNull   是否保留 null 字段
     * @return 字节数组
     */
    inline fun <reified T> writeValueAsBytes(data: T, preserveNull: Boolean = false): ByteArray {
        val engine = if (preserveNull) preserveJson else defaultJson
        val jsonString = try {
            // 1) 优先走标准路径（@Serializable / sealed 多态）
            engine.encodeToString(serializer<T>(), data)
        } catch (e: SerializationException) {
            // 2) 兜底：将对象递归转为 JsonElement，再编码（纯 kotlinx，不引入 Jackson）
            val elem = _encodeAnyToJsonElement(engine, data as Any?)
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
        val engine = if (preserveNull) preserveJson else defaultJson

        // 顶层即为 null：输出 "null"
        if (data == null) return "null".toByteArray(StandardCharsets.UTF_8)

        val ser: KSerializer<Any>? = findKSerializer(engine, data)
        val jsonString = if (ser != null) {
            // 找到序列化器：标准编码
            engine.encodeToString(ser, data)
        } else {
            // 未找到序列化器：递归转 JsonElement 再编码
            val elem = _encodeAnyToJsonElement(engine, data)
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
     * @sample
     *   val user: User = readValue<User>(bytes)
     */
    inline fun <reified T> readValue(bytes: ByteArray): T {
        val engine: Json = defaultJson
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
     * 行为说明：
     * - 使用与编码一致的 `defaultJson` 配置。
     * - 假设输入是 UTF-8 编码的 JSON 字节。
     * - 仅当 `kClass.serializerOrNull()` 能拿到直连序列化器时可用；否则抛出异常并提示改用 `KType` 版本。
     *
     * @param bytes   UTF-8 编码的 JSON 字节数组
     * @param kClass  目标类型的 `KClass<T>`（不含泛型信息）
     * @return        反序列化后的 T 实例
     * @throws kotlinx.serialization.SerializationException
     *         - 找不到 `@Serializable` 等可用序列化器；
     *         - 或目标本身/其属性结构与 JSON 不匹配。
     * @sample
     *   val user: User = readValue(bytes, User::class)
     */
    @OptIn(InternalSerializationApi::class)
    fun <T : Any> readValue(bytes: ByteArray, kClass: KClass<T>): T {
        val engine: Json = defaultJson
        val text = bytes.toString(Charsets.UTF_8)

        val ser = kClass.serializerOrNull()
            ?: throw SerializationException(
                "无法反序列化到 ${kClass.qualifiedName}：该类缺少序列化器或为泛型类型。" +
                        "如需支持 List<T>/Map<K,V> 等，请改用 readValue(bytes, kType)。"
            )

        @Suppress("UNCHECKED_CAST")
        return engine.decodeFromString(ser as KSerializer<T>, text)
    }

    /**
     * 反序列化：运行期携带完整类型信息的 `KType`（支持泛型）。
     *
     * 适用场景：
     * - 需要反序列化到 `List<User>`、`Map<String, User>`、`Array<Foo>` 等 **带泛型** 的目标类型。
     * - `kotlinx.serialization` 可以通过 `serializer(kType)` 为任意嵌套泛型生成/获取序列化器，
     *   前提是涉及到的具体类型都有可用序列化器（`@Serializable` / sealed 多态 / 已注册）。
     *
     * 行为说明：
     * - 使用与编码一致的 `defaultJson` 配置。
     * - 假设输入是 UTF-8 编码的 JSON 字节。
     * - 返回值类型为 `Any?`，请在调用处进行显式强转。
     *
     * @param bytes  UTF-8 编码的 JSON 字节数组
     * @param kType  目标类型的 `KType`（可由 `typeOf<List<User>>()` 等获得）
     * @return       反序列化得到的对象实例（需在调用处 cast）
     * @throws kotlinx.serialization.SerializationException
     *         当 JSON 结构不匹配或某些参与类型缺少序列化器时抛出
     * @sample
     *   val listType: KType = typeOf<List<User>>()
     *   val users: List<User> = readValue(bytes, listType) as List<User>
     */
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    fun readValue(bytes: ByteArray, kType: KType): Any? {
        val engine: Json = defaultJson
        val text = bytes.toString(Charsets.UTF_8)
        val ser: KSerializer<Any?> = kotlinx.serialization.serializer(kType)
        return engine.decodeFromString(ser, text)
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


    /** 优先从上下文/多态/直连三处查找 KSerializer（都找不到返回 null） */
    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    private fun findKSerializer(json: Json, value: Any): KSerializer<Any>? {
        val k = value::class
        // 1) 尝试上下文序列化器（你已经为 LocalDate/Time/DateTime 注册了 contextual）
        json.serializersModule.getContextual(k)?.let { return it as KSerializer<Any> }

        // 2) 尝试直连（@Serializable 或 sealed 层级子类）
        k.serializerOrNull()?.let { return it as KSerializer<Any> }

        // 3) 尝试多态（如果你之后在 module 里配了 polymorphic）
        //   例：json.serializersModule.getPolymorphic(Base::class, value)
        //   这里不知道你的基类是谁，先不默认写死；如果你有开放多态，可在此处补充。
        return null
    }



    /**
     * 纯 kotlinx 的“Any 兜底”：把任意常见结构递归转为 JsonElement。
     * - data class：按主构造参数顺序取属性（尽量稳定输出）
     * - Map/List/数组/基本类型/Enum/java.time：分别处理
     * - 其他无法处理的类型会抛出 SerializationException，提示显式建模
     */
    fun _encodeAnyToJsonElement(json: Json, v: Any?): JsonElement {
        return when (v) {
            null -> JsonNull
            is JsonElement -> v

            // 基础类型
            is String -> JsonPrimitive(v)
            is Boolean -> JsonPrimitive(v)
            is Number -> JsonPrimitive(v)
            is Char -> JsonPrimitive(v.toString())

            // Enum：用 name，稳定
            is Enum<*> -> JsonPrimitive(v.name)

            // java.time：走已注册的 contextual
            is LocalDate, is LocalTime, is LocalDateTime -> {
                val ser = findKSerializer(json, v)
                    ?: throw SerializationException("缺少 ${v::class.qualifiedName} 的序列化器")
                json.encodeToJsonElement(ser, v)
            }

            // Map（建议 String 键；非 String 键用 toString 并给出风险提醒）
            is Map<*, *> -> {
                val entries = buildMap<String, JsonElement> {
                    for ((k, value) in v) {
                        val key = when (k) {
                            null -> "null"
                            is String -> k
                            else -> k.toString() // 如需更强稳定性可自行规范化
                        }
                        put(key, _encodeAnyToJsonElement(json, value))
                    }
                }
                JsonObject(entries)
            }

            // Iterable / 数组
            is Iterable<*> -> JsonArray(v.map { _encodeAnyToJsonElement(json, it) })
            is Array<*> -> JsonArray(v.map { _encodeAnyToJsonElement(json, it) })
            is IntArray -> JsonArray(v.map { JsonPrimitive(it) })
            is LongArray -> JsonArray(v.map { JsonPrimitive(it) })
            is ShortArray -> JsonArray(v.map { JsonPrimitive(it.toInt()) })
            is ByteArray -> JsonArray(v.map { JsonPrimitive(it.toInt()) })
            is DoubleArray -> JsonArray(v.map { JsonPrimitive(it) })
            is FloatArray -> JsonArray(v.map { JsonPrimitive(it.toDouble()) })
            is BooleanArray -> JsonArray(v.map { JsonPrimitive(it) })
            is CharArray -> JsonArray(v.map { JsonPrimitive(it.toString()) })

            else -> {
                val k = v::class

                // 如果这是一个 data class：按“主构造参数顺序”取属性，尽量与 kotlinx 默认顺序一致
                if (k.isData) {
                    val ctor = k.primaryConstructor
                    val propsByName = k.memberProperties.associateBy { it.name }

                    val orderedPairs = buildList {
                        // 先主构造参数顺序
                        ctor?.parameters?.forEach { p ->
                            val name = p.name ?: return@forEach
                            val prop = propsByName[name] ?: return@forEach
                            try { prop.isAccessible = true } catch (_: Throwable) { }
                            val pv = runCatching { prop.getter.call(v) }.getOrNull()
                            add(name to _encodeAnyToJsonElement(json, pv))
                        }
                        // 再补上不在主构造里的属性（如有）
                        for ((name, prop) in propsByName) {
                            if (ctor?.parameters?.any { it.name == name } == true) continue
                            try { prop.isAccessible = true } catch (_: Throwable) { }
                            val pv = runCatching { prop.getter.call(v) }.getOrNull()
                            add(name to _encodeAnyToJsonElement(json, pv))
                        }
                    }

                    JsonObject(linkedMapOf<String, JsonElement>().apply {
                        orderedPairs.forEach { (n, e) -> put(n, e) }
                    })
                } else {
                    // 尝试按 Java Bean 反射兜底
                    val beanElem = runCatching { javaBeanToJsonElement(json, v) }.getOrNull()
                    if (beanElem != null) return beanElem

                    // 仍然兜不住，再给出明确提示
                    throw SerializationException(
                        "无法序列化 ${k.qualifiedName}：请添加 @Serializable，或改为 data class，" +
                                "或在外层改用可序列化的 DTO/JsonElement/基础类型集合"
                    )
                }
            }
        }
    }

    /**
     * 将普通 Java Bean（有 getXxx() 访问器）转换为 JsonElement。
     * - 使用 JavaBeans Introspector，截止到 Object.class，因此不会包含 "class" 属性
     * - 递归地调用 encodeAnyToJsonElement 处理嵌套
     */
    private fun javaBeanToJsonElement(json: Json, bean: Any): JsonElement {
        val info = Introspector.getBeanInfo(bean.javaClass, Object::class.java)
        val props = info.propertyDescriptors
        val ordered = LinkedHashMap<String, JsonElement>(props.size)
        for (pd in props) {
            val read = pd.readMethod ?: continue
            val name = pd.name ?: continue
            try { read.isAccessible = true } catch (_: Throwable) {}
            val value = runCatching { read.invoke(bean) }.getOrNull()
            ordered[name] = _encodeAnyToJsonElement(json, value)
        }
        return JsonObject(ordered)
    }



}
