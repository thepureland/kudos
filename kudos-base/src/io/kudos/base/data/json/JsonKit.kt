package io.kudos.base.data.json

import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import java.beans.Introspector
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import java.time.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaMethod

/**
 * Json 工具类 (基于 kotlinx.serialization)
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
    private data class DataClassPropertyAccessor(
        val name: String,
        val read: (Any) -> Any?
    )

    /** 日志记录器 */
    val log = LogFactory.getLog(this)

    private val encodeToStringMethod: Method by lazy {
        Json::class.java.methods.firstOrNull {
            it.name == "encodeToString" &&
                it.parameterCount == 2 &&
                SerializationStrategy::class.java.isAssignableFrom(it.parameterTypes[0])
        } ?: error("Json.encodeToString(serializer, value) 方法不存在")
    }
    private val encodeToJsonElementMethod: Method by lazy {
        Json::class.java.methods.firstOrNull {
            it.name == "encodeToJsonElement" &&
                it.parameterCount == 2 &&
                SerializationStrategy::class.java.isAssignableFrom(it.parameterTypes[0])
        } ?: error("Json.encodeToJsonElement(serializer, value) 方法不存在")
    }
    private val dataClassPropertyCache = ConcurrentHashMap<KClass<*>, List<DataClassPropertyAccessor>>()
    private val javaBeanReadMethodCache = ConcurrentHashMap<Class<*>, List<Pair<String, Method>>>()

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
        defaultJson.decodeFromString<T>(json)
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
    } catch (e: IllegalArgumentException) {
        log.error(e)
        ""
    } catch (e: RuntimeException) {
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
        val engine = if (preserveNull) preserveJson else defaultJson
        val jsonString = try {
            // 1) 优先走标准路径（@Serializable / sealed 多态）
            engine.encodeToString(serializer<T>(), data)
        } catch (_: SerializationException) {
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

        val ser = findKSerializer(engine, data)
        val jsonString = if (ser != null) {
            // 找到序列化器：标准编码
            encodeToStringWithSerializer(engine, ser, data)
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

        val decoded = engine.decodeFromString(ser, text)
        return kClass.java.cast(decoded)
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
    @OptIn(ExperimentalSerializationApi::class)
    fun readValue(bytes: ByteArray, kType: KType): Any? {
        val engine: Json = defaultJson
        val text = bytes.toString(Charsets.UTF_8)
        val ser: KSerializer<Any?> = serializer(kType)
        return engine.decodeFromString(ser, text)
    }


    /**
     * 递归将JsonElement转为Kotlin类型
     * 
     * 将JsonElement递归转换为对应的Kotlin类型，支持基本类型、集合和对象。
     * 
     * 工作流程：
     * 1. JsonPrimitive：按优先级尝试转换为Boolean、Long、Int、Double，否则返回字符串内容
     * 2. JsonArray：递归转换每个元素，返回List
     * 3. JsonObject：递归转换每个值，返回Map
     * 4. JsonNull：返回null
     * 
     * 类型转换优先级：
     * - Boolean：优先尝试解析为布尔值
     * - Long：其次尝试解析为长整型
     * - Int：再次尝试解析为整型
     * - Double：然后尝试解析为浮点数
     * - String：最后返回字符串内容
     * 
     * 递归处理：
     * - 数组和对象会递归处理其内部元素
     * - 确保嵌套结构被正确转换
     * 
     * @param elem JsonElement对象
     * @return 转换后的Kotlin对象，可能是基本类型、List、Map或null
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


    /**
     * 查找KSerializer序列化器
     * 
     * 按优先级从三个位置查找序列化器：上下文序列化器、直连序列化器、多态序列化器。
     * 
     * 查找顺序：
     * 1. 上下文序列化器：从SerializersModule的contextual序列化器中查找
     *    - 适用于已注册的上下文类型（如LocalDate、LocalTime、LocalDateTime）
     *    - 通过getContextual方法获取
     * 2. 直连序列化器：从类本身查找序列化器
     *    - 适用于标注@Serializable的类
     *    - 适用于sealed层级子类（自动生成序列化器）
     *    - 通过serializerOrNull方法获取
     * 3. 多态序列化器：从多态序列化器中查找（当前未实现）
     *    - 适用于开放多态场景
     *    - 需要配置polymorphic序列化器
     * 
     * 返回值：
     * - 找到序列化器：返回KSerializer<Any>
     * - 未找到：返回null
     * 
     * 使用场景：
     * - 运行时序列化未知类型
     * - 兜底序列化机制
     * - 支持常见类型的自动序列化
     * 
     * 注意事项：
     * - 使用@OptIn注解启用内部API
     * - 类型转换使用@Suppress抑制警告
     * - 多态序列化器需要额外配置
     * 
     * @param json Json配置对象，包含SerializersModule
     * @param value 待序列化的对象
     * @return KSerializer序列化器，如果未找到返回null
     */
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    private fun findKSerializer(json: Json, value: Any): KSerializer<*>? {
        val k = value::class
        // 1) 尝试上下文序列化器（你已经为 LocalDate/Time/DateTime 注册了 contextual）
        json.serializersModule.getContextual(k)?.let { return it }

        // 2) 尝试直连（@Serializable 或 sealed 层级子类）
        k.serializerOrNull()?.let { return it }

        // 3) 尝试多态（如果你之后在 module 里配了 polymorphic）
        //   例：json.serializersModule.getPolymorphic(Base::class, value)
        //   这里不知道你的基类是谁，先不默认写死；如果你有开放多态，可在此处补充。
        return null
    }

    private fun encodeToStringWithSerializer(json: Json, serializer: KSerializer<*>, value: Any): String {
        return encodeToStringMethod.invoke(json, serializer, value) as? String
            ?: error("Json.encodeToString 返回值类型异常")
    }

    private fun encodeToJsonElementWithSerializer(json: Json, serializer: KSerializer<*>, value: Any): JsonElement {
        return encodeToJsonElementMethod.invoke(json, serializer, value) as? JsonElement
            ?: error("Json.encodeToJsonElement 返回值类型异常")
    }



    /**
     * 将任意对象递归转换为JsonElement（兜底序列化）
     * 
     * 当标准序列化失败时，使用此方法作为兜底方案，支持常见类型的递归序列化。
     * 
     * 支持的类型：
     * 1. 基本类型：String、Boolean、Number、Char → JsonPrimitive
     * 2. 枚举：使用枚举的name → JsonPrimitive
     * 3. java.time：LocalDate、LocalTime、LocalDateTime → 使用已注册的序列化器
     * 4. Map：递归转换key和value → JsonObject
     * 5. 集合和数组：
     *    - Iterable、Array → JsonArray（递归转换元素）
     *    - 基本类型数组（IntArray、LongArray等）→ JsonArray（直接转换）
     * 6. data class：按主构造参数顺序提取属性 → JsonObject
     * 7. Java Bean：使用JavaBeans Introspector提取属性 → JsonObject
     * 
     * data class处理：
     * - 优先按主构造参数顺序提取属性，保持输出稳定
     * - 然后补充不在主构造中的属性
     * - 使用反射获取属性值，递归转换
     * 
     * Java Bean处理：
     * - 使用Introspector.getBeanInfo获取属性描述符
     * - 调用getter方法获取属性值
     * - 递归转换属性值
     * 
     * 异常处理：
     * - 如果所有方式都无法序列化，抛出SerializationException
     * - 异常信息会提示添加@Serializable或改为data class
     * 
     * 注意事项：
     * - 这是兜底方案，性能不如标准序列化
     * - 使用反射，可能有性能开销
     * - 建议优先使用@Serializable标注
     * 
     * @param json Json配置对象
     * @param v 待序列化的对象，可以为null
     * @return 转换后的JsonElement
     * @throws SerializationException 如果无法序列化该类型
     */
    fun _encodeAnyToJsonElement(json: Json, v: Any?): JsonElement {
        encodeFastPath(json, v)?.let { return it }
        return encodeComplexObject(json, requireNotNull(v))
    }

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
                    ?: throw SerializationException("缺少 ${value::class.qualifiedName} 的序列化器")
                encodeToJsonElementWithSerializer(json, ser, value)
            }
            is Map<*, *> -> encodeMapToJsonObject(json, value)
            is Iterable<*> -> encodeIterable(json, value)
            is Array<*> -> encodeObjectArray(json, value)
            is IntArray -> encodeIntArray(value)
            is LongArray -> encodeLongArray(value)
            is ShortArray -> encodeShortArray(value)
            is ByteArray -> encodeByteArray(value)
            is DoubleArray -> encodeDoubleArray(value)
            is FloatArray -> encodeFloatArray(value)
            is BooleanArray -> encodeBooleanArray(value)
            is CharArray -> encodeCharArray(value)
            else -> null
        }
    }

    private fun encodeIterable(json: Json, value: Iterable<*>): JsonArray {
        val items = if (value is Collection<*>) {
            ArrayList<JsonElement>(value.size)
        } else {
            ArrayList<JsonElement>()
        }
        value.forEach { items.add(_encodeAnyToJsonElement(json, it)) }
        return JsonArray(items)
    }

    private fun encodeObjectArray(json: Json, value: Array<*>): JsonArray {
        val items = ArrayList<JsonElement>(value.size)
        value.forEach { items.add(_encodeAnyToJsonElement(json, it)) }
        return JsonArray(items)
    }

    private inline fun encodePrimitiveArray(size: Int, producer: (Int) -> JsonElement): JsonArray {
        val items = ArrayList<JsonElement>(size)
        for (i in 0 until size) {
            items.add(producer(i))
        }
        return JsonArray(items)
    }

    private fun encodeIntArray(value: IntArray): JsonArray {
        return encodePrimitiveArray(value.size) { idx -> JsonPrimitive(value[idx]) }
    }

    private fun encodeLongArray(value: LongArray): JsonArray {
        return encodePrimitiveArray(value.size) { idx -> JsonPrimitive(value[idx]) }
    }

    private fun encodeShortArray(value: ShortArray): JsonArray {
        return encodePrimitiveArray(value.size) { idx -> JsonPrimitive(value[idx].toInt()) }
    }

    private fun encodeByteArray(value: ByteArray): JsonArray {
        return encodePrimitiveArray(value.size) { idx -> JsonPrimitive(value[idx].toInt()) }
    }

    private fun encodeDoubleArray(value: DoubleArray): JsonArray {
        return encodePrimitiveArray(value.size) { idx -> JsonPrimitive(value[idx]) }
    }

    private fun encodeFloatArray(value: FloatArray): JsonArray {
        return encodePrimitiveArray(value.size) { idx -> JsonPrimitive(value[idx].toDouble()) }
    }

    private fun encodeBooleanArray(value: BooleanArray): JsonArray {
        return encodePrimitiveArray(value.size) { idx -> JsonPrimitive(value[idx]) }
    }

    private fun encodeCharArray(value: CharArray): JsonArray {
        return encodePrimitiveArray(value.size) { idx -> JsonPrimitive(value[idx].toString()) }
    }

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
        val beanElem = runCatching { javaBeanToJsonElement(json, value) }.getOrNull()
        if (beanElem != null) return beanElem
        throw SerializationException(
            "无法序列化 ${k.qualifiedName}：请添加 @Serializable，或改为 data class，" +
                "或在外层改用可序列化的 DTO/JsonElement/基础类型集合"
        )
    }

    /**
     * 将Java Bean转换为JsonElement
     * 
     * 使用JavaBeans Introspector提取Bean的属性，并递归转换为JsonElement。
     * 
     * 工作流程：
     * 1. 获取Bean信息：使用Introspector.getBeanInfo获取属性描述符
     * 2. 设置停止类：截止到Any.class，避免包含"class"属性
     * 3. 遍历属性：遍历所有属性描述符
     * 4. 提取属性值：
     *    - 获取readMethod（getter方法）
     *    - 设置方法可访问（isAccessible = true）
     *    - 调用getter方法获取属性值
     * 5. 递归转换：调用_encodeAnyToJsonElement递归转换属性值
     * 6. 构建JsonObject：将属性名和转换后的值组成JsonObject
     * 
     * JavaBeans规范：
     * - 属性必须有对应的getter方法（getXxx或isXxx）
     * - 属性名从getter方法名推导（去掉get/is前缀，首字母小写）
     * - 使用Introspector自动发现属性
     * 
     * 属性过滤：
     * - 只包含有getter方法的属性
     * - 不包含"class"属性（通过设置停止类实现）
     * - 跳过无法访问的属性
     * 
     * 异常处理：
     * - 如果getter方法调用失败，属性值设为null
     * - 使用runCatching捕获异常，不影响其他属性的处理
     * 
     * 注意事项：
     * - 使用反射，可能有性能开销
     * - 需要确保getter方法可访问
     * - 嵌套对象会递归转换
     * 
     * @param json Json配置对象
     * @param bean Java Bean对象
     * @return 转换后的JsonObject
     */
    private fun javaBeanToJsonElement(json: Json, bean: Any): JsonElement {
        val readMethods = javaBeanReadMethodCache.getOrPut(bean.javaClass) {
            Introspector.getBeanInfo(bean.javaClass, Any::class.java).propertyDescriptors.mapNotNull { pd ->
                val read = pd.readMethod ?: return@mapNotNull null
                val name = pd.name ?: return@mapNotNull null
                try { read.isAccessible = true } catch (_: Throwable) {}
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

    private inline fun <T> encodeObjectEntries(
        json: Json,
        size: Int,
        entries: Iterable<T>,
        keyOf: (T) -> String,
        valueOf: (T) -> Any?
    ): JsonObject {
        val encoded = LinkedHashMap<String, JsonElement>(size)
        entries.forEach { entry ->
            encoded[keyOf(entry)] = _encodeAnyToJsonElement(json, valueOf(entry))
        }
        return JsonObject(encoded)
    }

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

    @Suppress("UNCHECKED_CAST")
    private fun buildDataClassPropertyAccessor(
        name: String,
        prop: KProperty1<out Any, *>
    ): DataClassPropertyAccessor {
        try { prop.isAccessible = true } catch (_: Throwable) {}
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
