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

    private data class DataClassPropertyAccessor(
        val name: String,
        val read: (Any) -> Any?
    )

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

    private fun encodeWithSerializerToElement(json: Json, serializer: KSerializer<*>, value: Any): JsonElement {
        return encodeToJsonElementMethod.invoke(json, serializer, value) as? JsonElement
            ?: error("Json.encodeToJsonElement 返回值类型异常")
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

    private fun encodeIterable(json: Json, value: Iterable<*>): JsonArray {
        val items = if (value is Collection<*>) {
            ArrayList<JsonElement>(value.size)
        } else {
            ArrayList<JsonElement>()
        }
        value.forEach { items.add(encodeAnyToJsonElement(json, it)) }
        return JsonArray(items)
    }

    private fun encodeObjectArray(json: Json, value: Array<*>): JsonArray {
        val items = ArrayList<JsonElement>(value.size)
        value.forEach { items.add(encodeAnyToJsonElement(json, it)) }
        return JsonArray(items)
    }

    private inline fun encodePrimitiveArray(size: Int, producer: (Int) -> JsonElement): JsonArray {
        val items = ArrayList<JsonElement>(size)
        for (i in 0 until size) {
            items.add(producer(i))
        }
        return JsonArray(items)
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
            encoded[keyOf(entry)] = encodeAnyToJsonElement(json, valueOf(entry))
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
