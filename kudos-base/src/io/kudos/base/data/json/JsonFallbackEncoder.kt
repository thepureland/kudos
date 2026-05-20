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
 * 反射兜底编码器：当类没有 `@Serializable` 时，把任意对象递归转成 [JsonElement]。
 *
 * 支持的类型：
 * 1. 基本类型：String / Boolean / Number / Char → [JsonPrimitive]
 * 2. 枚举：使用 `.name` → [JsonPrimitive]
 * 3. java.time：LocalDate / LocalTime / LocalDateTime → 走 contextual 序列化器
 * 4. Map / Iterable / Array 及各种原始类型数组 → [JsonArray]
 * 5. data class：按主构造参数顺序提取属性 → [JsonObject]（保证输出顺序稳定）
 * 6. Java Bean：使用 [Introspector] 提取属性 → [JsonObject]
 *
 * 性能：data class 属性访问器和 Java Bean read method 都用 [ConcurrentHashMap] 缓存反射结果，
 * 同一个类只反射一次。
 *
 * 设计为 [JsonKit] 的实现细节。声明为 `@PublishedApi internal` 让 [JsonKit] 的 inline 函数能跨文件调用。
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
@PublishedApi
internal object JsonFallbackEncoder {

    /**
     * data class 属性的“名字 + 读取闭包”。
     * 读取闭包优先用 Java getter；不可用时退回 Kotlin 反射，避免每次都走完整反射链。
     *
     * @property name 属性名
     * @property read 读取属性值的闭包；失败返回 null 而非抛出
     */
    private data class DataClassPropertyAccessor(
        val name: String,
        val read: (Any) -> Any?
    )

    /** 通过反射拿到的 `Json.encodeToString(SerializationStrategy, T)` 方法句柄，避开重载歧义 */
    private val encodeToStringMethod: Method by lazy {
        Json::class.java.methods.firstOrNull {
            it.name == "encodeToString" &&
                it.parameterCount == 2 &&
                SerializationStrategy::class.java.isAssignableFrom(it.parameterTypes[0])
        } ?: error("Json.encodeToString(serializer, value) 方法不存在")
    }
    /** 通过反射拿到的 `Json.encodeToJsonElement(SerializationStrategy, T)` 方法句柄 */
    private val encodeToJsonElementMethod: Method by lazy {
        Json::class.java.methods.firstOrNull {
            it.name == "encodeToJsonElement" &&
                it.parameterCount == 2 &&
                SerializationStrategy::class.java.isAssignableFrom(it.parameterTypes[0])
        } ?: error("Json.encodeToJsonElement(serializer, value) 方法不存在")
    }
    /** data class 属性访问器缓存，按类缓存，避免每次序列化都遍历构造器与成员属性 */
    private val dataClassPropertyCache = ConcurrentHashMap<KClass<*>, List<DataClassPropertyAccessor>>()
    /** Java Bean 读取方法缓存，避免每次序列化都执行 [Introspector.getBeanInfo] */
    private val javaBeanReadMethodCache = ConcurrentHashMap<Class<*>, List<Pair<String, Method>>>()

    /**
     * 递归把任意对象转成 [JsonElement]。null → [JsonNull]。
     * 标准 `@Serializable` 路径处理不了时的兜底入口。
     */
    @PublishedApi
    internal fun encodeAnyToJsonElement(json: Json, v: Any?): JsonElement {
        encodeFastPath(json, v)?.let { return it }
        return encodeComplexObject(json, requireNotNull(v))
    }

    /**
     * 按 contextual → 直连 [serializerOrNull] 的顺序，为运行时对象查找 [KSerializer]。
     * 找不到返回 null（由调用方决定是否走 [encodeAnyToJsonElement] 兜底）。
     */
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    internal fun findKSerializer(json: Json, value: Any): KSerializer<*>? {
        val k = value::class
        json.serializersModule.getContextual(k)?.let { return it }
        k.serializerOrNull()?.let { return it }
        // 多态序列化：当前未启用——如果未来在 SerializersModule 里注册了 polymorphic 基类，
        // 可在此处补 json.serializersModule.getPolymorphic(Base::class, value)
        return null
    }

    /** 用反射调用 [Json.encodeToString] 的双参版本（带显式 serializer） */
    internal fun encodeWithSerializer(json: Json, serializer: KSerializer<*>, value: Any): String {
        return encodeToStringMethod.invoke(json, serializer, value) as? String
            ?: error("Json.encodeToString 返回值类型异常")
    }

    /**
     * 反射调用 `Json.encodeToJsonElement(serializer, value)`，主要给 LocalDate/Time 等 contextual 类型用。
     *
     * @param json 配置好的 Json 引擎
     * @param serializer 显式的序列化器
     * @param value 待编码对象
     * @return 编码后的 [JsonElement]
     * @author K
     * @since 1.0.0
     */
    private fun encodeWithSerializerToElement(json: Json, serializer: KSerializer<*>, value: Any): JsonElement {
        return encodeToJsonElementMethod.invoke(json, serializer, value) as? JsonElement
            ?: error("Json.encodeToJsonElement 返回值类型异常")
    }

    /**
     * 快速分支：常见的基本类型 / 时间类型 / 集合 / 数组直接编码为 [JsonElement]，
     * 没有命中则返回 null 让上层走 [encodeComplexObject] 复杂分支。
     *
     * @param json 配置好的 Json 引擎
     * @param value 待编码对象，允许 null
     * @return 命中快速分支返回对应 [JsonElement]，否则 null
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
                    ?: throw SerializationException("缺少 ${value::class.qualifiedName} 的序列化器")
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
     * 复杂分支：data class 走主构造参数顺序提取属性；其它对象退回 Java Bean 反射；
     * 均无法处理时抛出 [SerializationException] 提示加 @Serializable。
     *
     * @param json 配置好的 Json 引擎
     * @param value 非空待编码对象
     * @return 对应的 [JsonObject]
     * @throws SerializationException 类型既不是 data class、也无法按 Java Bean 反射时
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
                "无法序列化 ${k.qualifiedName}：请添加 @Serializable，或改为 data class，" +
                    "或在外层改用可序列化的 DTO/JsonElement/基础类型集合"
            )
    }

    /**
     * 把任意 [Iterable] 递归编码为 [JsonArray]。
     *
     * @param json 配置好的 Json 引擎
     * @param value 待编码集合
     * @return 编码后的 [JsonArray]
     * @author K
     * @since 1.0.0
     */
    private fun encodeIterable(json: Json, value: Iterable<*>): JsonArray =
        JsonArray(value.map { encodeAnyToJsonElement(json, it) })

    /**
     * 把对象数组递归编码为 [JsonArray]。
     *
     * @param json 配置好的 Json 引擎
     * @param value 待编码对象数组
     * @return 编码后的 [JsonArray]
     * @author K
     * @since 1.0.0
     */
    private fun encodeObjectArray(json: Json, value: Array<*>): JsonArray =
        JsonArray(value.map { encodeAnyToJsonElement(json, it) })

    /**
     * 原始类型数组的统一编码模板：按下标读取并通过 [producer] 转为 [JsonElement]。
     * inline 是为了避开数百次 lambda 调用的装箱开销。
     *
     * @param size 数组长度
     * @param producer 把下标映射为 [JsonElement] 的闭包
     * @return 编码后的 [JsonArray]
     * @author K
     * @since 1.0.0
     */
    private inline fun encodePrimitiveArray(size: Int, producer: (Int) -> JsonElement): JsonArray =
        JsonArray(List(size, producer))

    /**
     * 把 [Map] 编码为 [JsonObject]：null key 转 `"null"`，非字符串 key 走 [Any.toString]。
     * 与标准 kotlinx 行为略有差异，仅用于无 `@Serializable` 类型的兜底。
     *
     * @param json 配置好的 Json 引擎
     * @param value 待编码的 Map
     * @return 编码后的 [JsonObject]
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
     * 通过 [Introspector] 提取 Java Bean 风格的属性并编码为 [JsonObject]。
     * 自动跳过 [Object] 自带属性（如 class），属性顺序由 Introspector 给出（一般按 getter 名字字典序）。
     *
     * @param json 配置好的 Json 引擎
     * @param bean 待编码对象
     * @return 编码后的 [JsonObject]
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
     * 把任意条目集合（Map.Entry、Pair、属性元组等）按 keyOf/valueOf 投影为 [JsonObject]。
     * 使用 [LinkedHashMap] 保证输出顺序与输入一致；为复用性把投影写成 inline 以避免闭包开销。
     *
     * @param T 条目类型
     * @param json 配置好的 Json 引擎
     * @param size 预分配的容量提示
     * @param entries 待编码条目
     * @param keyOf 从条目提取 JSON 字段名的闭包
     * @param valueOf 从条目提取字段值的闭包
     * @return 编码后的 [JsonObject]
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
     * 返回 data class 的属性访问器，按主构造器参数顺序在前、其它成员属性在后。
     * 这样输出的 JSON 字段顺序与源码声明一致，diff 友好。结果按类缓存。
     *
     * @param kClass data class 的 KClass
     * @return 属性访问器列表
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
     * 为单个属性构造 [DataClassPropertyAccessor]。
     * 优先使用 Java getter 反射（更快），失败时退回 Kotlin 反射调用 [KProperty1.get]。
     *
     * @param name 属性名
     * @param prop Kotlin 反射的属性引用
     * @return 属性访问器
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
