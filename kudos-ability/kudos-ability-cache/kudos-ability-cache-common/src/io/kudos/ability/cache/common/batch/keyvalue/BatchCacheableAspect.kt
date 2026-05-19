package io.kudos.ability.cache.common.batch.keyvalue

import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.lang.string.toType
import io.kudos.base.logger.LogFactory
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
 * 批量缓存的切面
 *
 * @author K
 * @since 1.0.0
 */
@Aspect
@Component
@Lazy(false)
@ConditionalOnBean(MixCacheManager::class)
@Order(10) // 批量 Cacheable，比单条 Cacheable(0) 稍后；理论与单条互斥，但显式区分便于调试组合场景。
class BatchCacheableAspect {

    /**
     * 定义切入点
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.cache.common.batch.keyvalue.BatchCacheable)")
    fun cut() {
        // do nothing
    }

    /**
     * 环绕通知
     *
     * @return Map(缓存key, 缓存值)
     * @author K
     * @since 1.0.0
     */
    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Map<String, Any?> {
        // 拿到方法定义的注解信息
        val function = (joinPoint.signature as MethodSignature).method.kotlinFunction!!
        val batchCacheable = function.findAnnotation<BatchCacheable>()!!

        // 校验约束
        val cacheName = validate(joinPoint, function, batchCacheable)

        // 得到所有缓存key
        val cachedData = linkedMapOf<String, Any?>() // 已缓存的数据。Map<缓存key, 缓存值>
        val keys = getAllCacheKeys(joinPoint, function, batchCacheable)
        keys.forEach { cachedData[it] = null } // 保证顺序

        // 读取缓存中存在的数据
        readCachedData(keys, cacheName, batchCacheable, cachedData)

        // 没有在缓存中的(参数为集合的，要踢除缓存中读到的部分)，从@BatchCacheable标注的方法读
        val noExistKeys = cachedData.filterValues { it == null }.keys
        val uncachedData = readUncachedData(noExistKeys, joinPoint, function, batchCacheable) // 未缓存的数据。Map<缓存key, 缓存值>

        // 校验底层方法返回的 key 集合是否与缺失 key 集合一致；不一致一般是 keysGenerator 与 method 之间约定有 bug，
        // 旧实现会"静默地少返回 / 多返回"，调用方很难发现。这里仅记日志，不抛错（避免触发缓存一致性以外的失败）。
        if (uncachedData != null) {
            val returnedKeys = uncachedData.keys
            val missing = noExistKeys - returnedKeys
            val extra = returnedKeys - noExistKeys
            if (missing.isNotEmpty() || extra.isNotEmpty()) {
                log.warn(
                    "BatchCacheable 方法返回的 key 集合与缺失集合不一致 cacheName={0} missing={1} extra={2}",
                    cacheName, missing, extra
                )
            }
        }

        // 缓存从@BatchCacheable标注的方法读取(未缓存)的数据(注意：已存在的缓存并不会被更新)
        uncachedData?.forEach { (k, v) -> KeyValueCacheKit.putIfAbsent(cacheName, k, v) }

        // 组装两部分数据：缓存中读取的和刚加载的，并作为@BatchCacheable标注的方法的返回值返回
        if (uncachedData != null) {
            cachedData.forEach { (k, v) -> if (v == null) cachedData[k] = uncachedData[k] }
        }

        return cachedData.filterValues { it != null } // value为null的踢除
    }

    /**
     * 校验约束
     *
     * @param joinPoint 切入点
     * @param function BatchCacheable所标注的方法
     * @param batchCacheable BatchCacheable注解
     * @return 缓存名称
     * @author K
     * @since 1.0.0
     */
    private fun validate(joinPoint: ProceedingJoinPoint, function: KFunction<*>, batchCacheable: BatchCacheable): String {
        val clazz = joinPoint.target::class
        val cacheConfig = clazz.findAnnotation<CacheConfig>()
        val cacheName = batchCacheable.cacheNames.firstOrNull()
            ?: cacheConfig?.cacheNames?.firstOrNull()
            ?: error("cacheNames未设置！请在类${clazz}上通过@CacheConfig指定，或在方法${function}上通过@BatchCacheable指定！")

        check(Map::class.isSuperclassOf(function.returnType.classifier as KClass<*>)) {
            "类${clazz}中，@BatchCacheable标注的方法【${function}】，其返回值类型必须是Map！"
        }
        return cacheName
    }

    private fun getKeysGenerator(batchCacheable: BatchCacheable): IKeysGenerator =
        (SpringKit.getBeanOrNull(batchCacheable.keysGenerator) as? IKeysGenerator)
            ?: DefaultKeysGenerator()

    /**
     * 得到所有缓存key
     *
     * @param joinPoint 切入点
     * @param function BatchCacheable所标注的方法
     * @param batchCacheable BatchCacheable注解
     * @return List(缓存key)
     * @author K
     * @since 1.0.0
     */
    private fun getAllCacheKeys(
        joinPoint: ProceedingJoinPoint, function: KFunction<*>, batchCacheable: BatchCacheable
    ): List<String> {
        val keysGenerator = getKeysGenerator(batchCacheable)
        return keysGenerator.generate(joinPoint.target, function, *joinPoint.args)
    }

    /**
     * 读取缓存中存在的数据
     *
     * @param keys List(缓存key)
     * @param batchCacheable BatchCacheable注解
     * @param result Map(缓存key，缓存值)
     * @author K
     * @since 1.0.0
     */
    private fun readCachedData(
        keys: List<String>, cacheName: String, batchCacheable: BatchCacheable, result: MutableMap<String, Any?>
    ) {
        keys.forEach { key ->
            //TODO 缓存不存在时，怎么防止缓存击穿
            KeyValueCacheKit.getValue(cacheName, key, batchCacheable.valueClass)
                ?.let { result[key] = it }
        }
    }

    /**
     * 读取未在缓存中的数据
     *
     * 没有在缓存中的(参数为集合的，要踢除缓存中读到的部分)，从@BatchCacheable标注的方法读
     *
     * @param result 已缓存的数据，Map(缓存key，缓存值)
     * @param joinPoint 切入点
     * @param function BatchCacheable所标注的方法
     * @param batchCacheable BatchCacheable注解
     * @return 未在缓存中的数据，Map(缓存key，缓存值)
     * @author K
     * @since 1.0.0
     */
    private fun readUncachedData(
        noExistKeys: Set<String>, joinPoint: ProceedingJoinPoint, function: KFunction<*>,
        batchCacheable: BatchCacheable
    ): Map<String, Any?>? {
        if (noExistKeys.isEmpty()) return null
        val keysGenerator = getKeysGenerator(batchCacheable)
        val delimiter = keysGenerator.getDelimiter()
        val paramIndexes = keysGenerator.getParamIndexes(function, *joinPoint.args)
        val parameterTypes = (joinPoint.signature as MethodSignature).parameterTypes
        // 旧实现直接 `joinPoint.args[index] = params` 改原数组：AOP 后续读 joinPoint.args 会拿到被改的值，
        // 是难调试的"隐式副作用"。这里改成在拷贝上替换，不污染上游 args 数组。
        val newArgs: Array<Any?> = joinPoint.args.copyOf()
        parameterTypes.forEachIndexed { index, clazz ->
            val paramValue = newArgs[index]
            if (index in paramIndexes && (paramValue is Collection<*> || paramValue is Array<*>)) {
                val segIndex = paramIndexes.indexOf(index) // 在key中分段索引
                val firstElemValue: Any? = when (paramValue) {
                    is Collection<*> -> paramValue.firstOrNull()
                    is Array<*> -> paramValue.firstOrNull()
                    else -> null
                }
                // 旧实现的 `firstElemValue!!::class`：空集合/数组时直接 NPE。这里给一个明确错误。
                requireNotNull(firstElemValue) {
                    "无法从空集合/数组推断元素类型：参数 index=$index，建议调用方过滤掉空入参再进入 @BatchCacheable。"
                }
                val elemType = firstElemValue::class
                // 旧实现按固定 delimiter split：若 key 段内含相同字符就拆错。这里依然按段取，但作为已知约束
                // 由 keysGenerator.getDelimiter 提供"足够不会撞"的分隔符。后续要彻底治理需要在 keysGenerator
                // 这一层做转义，本轮范围之外。
                val elemValues = noExistKeys.map { key -> key.split(delimiter)[segIndex].toType(elemType) }
                // 下面这一组 `as List<X>` 是确定安全的：elemValues 里每一项都是 `toType(elemType)` 产物，
                // elemType 即原入参集合元素的实际 KClass。这里只是把 List<Any> 转成强类型 List 再 toTypedArray()。
                @Suppress("UNCHECKED_CAST")
                val params: Any? = when (clazz.kotlin) {
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
                    else -> null
                }
                newArgs[index] = params
            }
        }
        val proceeded = joinPoint.proceed(newArgs)
        // 入口 [validate] 已经检查了方法返回类型必须是 Map，但泛型参数被擦除：
        // - 如果方法签名是 Map<Int, X>，proceed 返回 Map<Int, X>，强转成 Map<String, X> 在 JVM 层不会立刻爆，
        //   而是在下游 `cachedData[key]` 使用 String key 取值时静默 miss → 用户看到的是"缓存功能失效"。
        // 显式检查 result 非空 + 至少抽样验证 key 是 String，给出有意义的错误信息。
        require(proceeded is Map<*, *>) {
            "@BatchCacheable 方法返回值必须是 Map，实际是 ${proceeded?.let { it::class.qualifiedName } ?: "null"}"
        }
        proceeded.keys.firstOrNull { it != null && it !is String }?.let { wrongKey ->
            error(
                "@BatchCacheable 方法返回的 Map 中包含非 String key（如 $wrongKey: ${wrongKey::class.qualifiedName}），" +
                    "切面用字符串 cacheKey 进行匹配，请把返回 Map 的 key 类型改为 String。"
            )
        }
        @Suppress("UNCHECKED_CAST")
        return proceeded as Map<String, Any?>
    }

    companion object {
        private val log = LogFactory.getLog(BatchCacheableAspect::class)
    }

}