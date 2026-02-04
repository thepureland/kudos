package io.kudos.ability.cache.common.batch.hash

import io.kudos.ability.cache.common.batch.keyvalue.IKeysGenerator
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.base.support.IIdEntity
import io.kudos.context.kit.SpringKit
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.cache.annotation.CacheConfig
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import io.kudos.ability.cache.common.core.MixHashCacheManager
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.kotlinFunction
import io.kudos.base.lang.string.toType
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KFunction

/**
 * [HashBatchCacheable] 切面：先按一批 id 查 Hash 缓存（findByIds），未命中的再调方法，结果 saveBatch 回写并合并返回。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Aspect
@Component
@Lazy(false)
@ConditionalOnBean(MixHashCacheManager::class)
class HashBatchCacheableAspect {

    @Pointcut("@annotation(io.kudos.ability.cache.common.batch.hash.HashBatchCacheable)")
    private fun cut() {}

    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Map<String, Any?> {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val function = method.kotlinFunction ?: return joinPoint.proceed() as Map<String, Any?>
        val ann = function.findAnnotation<HashBatchCacheable>() ?: return joinPoint.proceed() as Map<String, Any?>

        val cacheName = resolveCacheName(joinPoint, ann) ?: return joinPoint.proceed() as Map<String, Any?>
        validateReturnType(function, ann)

        val keysGenerator = getKeysGenerator(ann)
        val keys = keysGenerator.generate(joinPoint.target, function, *joinPoint.args)
        if (keys.isEmpty()) return emptyMap()

        val cachedData = linkedMapOf<String, Any?>()
        keys.forEach { cachedData[it] = null }

        val hashCache = HashCacheKit.getHashCache(cacheName)
        if (hashCache != null && CacheKit.isCacheActive(cacheName)) {
            @Suppress("UNCHECKED_CAST")
            val entityClass = ann.entityClass as KClass<out IIdEntity<Any?>>
            val list = hashCache.findByIds(cacheName, keys, entityClass)
            list.forEach { e -> e.id?.let { id -> cachedData[id.toString()] = e } }
        }

        val uncachedKeys = cachedData.filterValues { it == null }.keys.toList()
        if (uncachedKeys.isEmpty()) return cachedData.filterValues { it != null }

        val uncachedMap = readUncachedData(uncachedKeys, joinPoint, function, keysGenerator)
            ?: return cachedData.filterValues { it != null }

        if (hashCache != null && CacheKit.isCacheActive(cacheName) && CacheKit.isWriteInTime(cacheName)) {
            val toSave = uncachedMap.values.filterIsInstance<IIdEntity<*>>().filter { it.id != null }
            if (toSave.isNotEmpty()) {
                @Suppress("UNCHECKED_CAST")
                hashCache.saveBatch(cacheName, toSave as List<IIdEntity<Any?>>, emptySet(), emptySet())
            }
        }

        uncachedMap.forEach { (k, v) -> cachedData[k] = v }
        return cachedData.filterValues { it != null }
    }

    private fun resolveCacheName(joinPoint: ProceedingJoinPoint, ann: HashBatchCacheable): String? {
        if (ann.cacheNames.isNotEmpty()) return ann.cacheNames.first()
        val cacheConfig = joinPoint.target::class.findAnnotation<CacheConfig>()
        if (cacheConfig != null && cacheConfig.cacheNames.isNotEmpty()) return cacheConfig.cacheNames.first()
        return null
    }

    private fun validateReturnType(function: KFunction<*>, ann: HashBatchCacheable) {
        if (!Map::class.isSuperclassOf(function.returnType.classifier as KClass<*>)) {
            error("@HashBatchCacheable 标注的方法【${function}】返回值类型必须是 Map！")
        }
    }

    private fun getKeysGenerator(ann: HashBatchCacheable): IKeysGenerator {
        val bean = SpringKit.getBeanOrNull(ann.keysGenerator)
        if (bean != null) return bean as IKeysGenerator
        return DefaultHashBatchKeysGenerator()
    }

    @Suppress("UNCHECKED_CAST")
    private fun readUncachedData(
//        result: MutableMap<String, Any?>,
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
                    segStr.toType(first!!::class)
                }
                val clazz = parameterTypes[paramIndex].kotlin
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

        return joinPoint.proceed(args) as? Map<String, Any?>
    }
}
