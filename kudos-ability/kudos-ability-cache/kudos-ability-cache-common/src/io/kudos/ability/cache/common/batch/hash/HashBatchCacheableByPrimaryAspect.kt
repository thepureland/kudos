package io.kudos.ability.cache.common.batch.hash

import io.kudos.ability.cache.common.batch.keyvalue.IKeysGenerator
import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.lang.string.toType
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.context.kit.SpringKit
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.cache.annotation.CacheConfig
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.kotlinFunction

/**
 * [HashBatchCacheableByPrimary] 切面：先按一批主属性（id）查 Hash 缓存（findByIds），未命中的再调方法，结果 saveBatch 回写（可带副属性索引）并合并返回。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Aspect
@Component
@Lazy(false)
@ConditionalOnBean(MixHashCacheManager::class)
@Order(10) // 与 [BatchCacheableAspect] 同序：批量 Cacheable 类切面统一标 10。
class HashBatchCacheableByPrimaryAspect {

    @Pointcut("@annotation(io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary)")
    fun cut() {}

    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Map<String, Any?> {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val function = method.kotlinFunction ?: return proceedAsStringAnyMap(joinPoint)
        val ann = function.findAnnotation<HashBatchCacheableByPrimary>() ?: return proceedAsStringAnyMap(joinPoint)

        val cacheName = resolveCacheName(joinPoint, ann) ?: return proceedAsStringAnyMap(joinPoint)
        validateReturnType(function, ann)

        val keysGenerator = getKeysGenerator(ann)
        val keys = keysGenerator.generate(joinPoint.target, function, *joinPoint.args)
        if (keys.isEmpty()) return emptyMap()

        val cachedData = linkedMapOf<String, Any?>()
        keys.forEach { cachedData[it] = null }

        val hashCache = HashCacheKit.getHashCache(cacheName)
        if (KeyValueCacheKit.isCacheActive(cacheName)) {
            @Suppress("UNCHECKED_CAST")
            val entityClass = ann.entityClass as KClass<out IIdEntity<Any?>>
            val list = hashCache.findByIds(cacheName, keys, entityClass)
            list.forEach { e ->
                val tid = e.id?.toString()?.trim() ?: return@forEach
                val slot = cachedData.keys.find { it.trim() == tid } ?: tid
                cachedData[slot] = e
            }
        }

        val uncachedKeys = cachedData.filterValues { it == null }.keys.toList()
        if (uncachedKeys.isEmpty()) return cachedData.filterValues { it != null }

        val uncachedMap = readUncachedData(uncachedKeys, joinPoint, function, keysGenerator)
            ?: return cachedData.filterValues { it != null }

        if (KeyValueCacheKit.isCacheActive(cacheName) && KeyValueCacheKit.isWriteInTime(cacheName)) {
            val toSave = uncachedMap.values.filterIsInstance<IIdEntity<*>>().filter { it.id != null }
            if (toSave.isNotEmpty()) {
                val filterable = ann.filterableProperties.toSet()
                val sortable = ann.sortableProperties.toSet()
                @Suppress("UNCHECKED_CAST")
                hashCache.saveBatch(cacheName, toSave as List<IIdEntity<Any?>>, filterable, sortable)
            }
        }

        uncachedMap.forEach { (k, v) -> cachedData[k] = v }
        return cachedData.filterValues { it != null }
    }

    /**
     * 解析 cache 名：注解 cacheNames > 类上 `@CacheConfig.cacheNames` > null。
     * @author K
     * @since 1.0.0
     */
    private fun resolveCacheName(joinPoint: ProceedingJoinPoint, ann: HashBatchCacheableByPrimary): String? {
        if (ann.cacheNames.isNotEmpty()) return ann.cacheNames.first()
        val cacheConfig = joinPoint.target::class.findAnnotation<CacheConfig>()
        if (cacheConfig != null && cacheConfig.cacheNames.isNotEmpty()) return cacheConfig.cacheNames.first()
        return null
    }

    /**
     * 校验目标方法返回值必须是 [Map]，否则切面拼装"批量结果"会出错。
     * 不合规直接抛错让开发期立即发现。
     *
     * @throws IllegalStateException 返回值不是 Map 时
     * @author K
     * @since 1.0.0
     */
    private fun validateReturnType(function: KFunction<*>, ann: HashBatchCacheableByPrimary) {
        if (!Map::class.isSuperclassOf(function.returnType.classifier as KClass<*>)) {
            error("@HashBatchCacheableByPrimary 标注的方法【${function}】返回值类型必须是 Map！")
        }
    }

    /**
     * 解析 [HashBatchCacheableByPrimary.keysGenerator] 指定的 keys 生成器；缺失时降级到 [DefaultHashBatchKeysGenerator]。
     * @author K
     * @since 1.0.0
     */
    private fun getKeysGenerator(ann: HashBatchCacheableByPrimary): IKeysGenerator =
        (SpringKit.getBeanOrNull(ann.keysGenerator) as? IKeysGenerator)
            ?: DefaultHashBatchKeysGenerator()

    /**
     * `joinPoint.proceed()` 后调 [validatedMap] 强校验返回值为 `Map<String, Any?>`。
     * 抽出来是为了让 around 主流程更紧凑。
     * @author K
     * @since 1.0.0
     */
    private fun proceedAsStringAnyMap(joinPoint: ProceedingJoinPoint): Map<String, Any?> =
        validatedMap(joinPoint.proceed())

    /**
     * 校验 proceed 的返回值符合 `Map<String, Any?>`：返回类型擦除后任何 Map 都能通过强转，
     * 实际 key 类型不对会在下游"按字符串 key 取值"时静默 miss。这里把校验集中起来。
     */
    private fun validatedMap(proceeded: Any?): Map<String, Any?> {
        if (proceeded !is Map<*, *>) {
            error("HashBatchCacheableByPrimary 期望方法返回 Map<String, Any?>，实际返回: ${proceeded?.let { it::class.qualifiedName } ?: "null"}")
        }
        require(proceeded.keys.all { it == null || it is String }) {
            "HashBatchCacheableByPrimary 期望方法返回 Map<String, Any?>，但检测到非 String key。"
        }
        @Suppress("UNCHECKED_CAST")
        return proceeded as Map<String, Any?>
    }

    /**
     * 把缓存未命中的 key 列表反算出"该走 DB 的入参"，再 proceed 到目标方法回源。
     *
     * 核心难点：业务方法的入参可能是 `Collection<T>` / `Array<T>`，且每个缓存 key 是多段组合
     * （例：`tenantId:userId`），需要：
     * 1. 把每个 noExistKey 按 [IKeysGenerator.getDelimiter] 拆段
     * 2. 取第 `paramIndex` 段，用集合中第一个元素的类型作样本调 [toType] 反序列化回原类型
     * 3. 按原参数声明类型（List/Set/Array<X>）重新装回集合并替换 args[paramIndex]
     *
     * 不能直接传 String 列表——目标方法的签名可能要求 `List<Long>` / `Array<Int>` 等具体类型，
     * 不做类型还原会在 reflection invoke 时抛 ClassCastException。
     *
     * @param noExistKeys 缓存中未命中的 key 列表（已格式化为 `seg1:seg2:...`）
     * @param joinPoint AOP 切入点（拿原始 args）
     * @param function 目标 KFunction（拿参数索引元信息）
     * @param keysGenerator key 生成器（提供 delimiter + paramIndexes 反查）
     * @return 重新跑目标方法后的 key→value map；null 表示无需回源
     * @author K
     * @since 1.0.0
     */
    private fun readUncachedData(
        noExistKeys: List<String>,
        joinPoint: ProceedingJoinPoint,
        function: KFunction<*>,
        keysGenerator: IKeysGenerator
    ): Map<String, Any?>? {
        val delimiter = keysGenerator.getDelimiter()
        val paramIndexes = keysGenerator.getParamIndexes(function, *joinPoint.args)
        val parameterTypes = (joinPoint.signature as MethodSignature).parameterTypes
        val args = joinPoint.args.copyOf()

        paramIndexes.forEachIndexed { segIdx, paramIndex ->
            val paramValue = args[paramIndex]
            if (paramValue is Collection<*> || paramValue is Array<*>) {
                val elemValues = noExistKeys.map { key ->
                    val seg = key.split(delimiter)
                    val segStr = if (segIdx < seg.size) seg[segIdx] else key
                    val first = when (paramValue) {
                        is Collection<*> -> paramValue.first()
                        else -> (paramValue as Array<*>).first()
                    }
                    val sample = requireNotNull(first) { "批量缓存参数集合中存在 null 元素，无法推断类型。" }
                    segStr.toType(sample::class)
                }
                val clazz = parameterTypes[paramIndex].kotlin
                // 下面 `as List<X>` 是确定安全的：elemValues 来自 `toType(sample::class)`，元素类型已对齐。
                @Suppress("UNCHECKED_CAST")
                args[paramIndex] = when (clazz) {
                    List::class, Collection::class -> elemValues
                    Set::class -> elemValues.toSet()
                    Array<String>::class -> (elemValues as List<String>).toTypedArray()
                    Array<Char>::class -> (elemValues as List<Char>).toTypedArray()
                    Array<Boolean>::class -> (elemValues as List<Boolean>).toTypedArray()
                    Array<Byte>::class -> (elemValues as List<Byte>).toTypedArray()
                    Array<Short>::class -> (elemValues as List<Short>).toTypedArray()
                    Array<Int>::class -> (elemValues as List<Int>).toTypedArray()
                    Array<Long>::class -> (elemValues as List<Long>).toTypedArray()
                    Array<Float>::class -> (elemValues as List<Float>).toTypedArray()
                    Array<Double>::class -> (elemValues as List<Double>).toTypedArray()
                    Array<BigDecimal>::class -> (elemValues as List<BigDecimal>).toTypedArray()
                    Array<BigInteger>::class -> (elemValues as List<BigInteger>).toTypedArray()
                    else -> elemValues
                }
            }
        }

        // 走与 [proceedAsStringAnyMap] 一样的强校验路径，避免 `as? Map<String, Any?>` 在 key 类型错时静默返回错误结构。
        return validatedMap(joinPoint.proceed(args))
    }
}
