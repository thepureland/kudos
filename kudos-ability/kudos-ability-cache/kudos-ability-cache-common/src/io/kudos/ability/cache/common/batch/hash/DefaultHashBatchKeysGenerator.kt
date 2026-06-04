package io.kudos.ability.cache.common.batch.hash

import io.kudos.ability.cache.common.batch.keyvalue.IKeysGenerator
import io.kudos.context.support.Consts
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

/**
 * Default Hash batch cache key generator: after excluding parameters per [HashBatchCacheableByPrimary.ignoreParamIndexes],
 * generates the id list (i.e. key list) using the same rules as [io.kudos.ability.cache.common.batch.keyvalue.DefaultKeysGenerator].
 *
 * Typical usage: for a method like `getByIds(ids: List<String>): Map<String, E?>`, keys = ids.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
class DefaultHashBatchKeysGenerator : IKeysGenerator {

    override fun generate(target: Any?, function: KFunction<*>?, vararg params: Any): List<String> {
        validParamType(*params)
        val paramIndexes = getParamIndexes(function, *params)
        // Typical usage: exactly one parameter that is an id collection; expand directly to the key list to avoid a Cartesian product and JVM non-Collection type issues
        if (paramIndexes.size == 1) {
            return when (val single = params[paramIndexes[0]]) {
                is Collection<*> -> single.map { it.toString() }
                is Array<*> -> single.map { it.toString() }
                else -> listOf(single.toString())
            }
        }
        val totalCount = calTotalCount(function, *params)
        return generateKeys(function, totalCount, *params)
    }

    override fun getDelimiter(): String = Consts.CACHE_KEY_DEFAULT_DELIMITER

    override fun getParamIndexes(function: KFunction<*>?, vararg params: Any): List<Int> {
        val ignoreParamIndexes =
            if (function == null) intArrayOf()
            else function.findAnnotation<HashBatchCacheableByPrimary>()?.ignoreParamIndexes ?: intArrayOf()
        return params.indices.filter { it !in ignoreParamIndexes }
    }

    /**
     * 守卫入参类型：Hash 批量缓存的 key 生成依赖统一的 `Array<*>` 类型，
     * 原始数组（IntArray / CharArray 等）的反射读取行为不同，会导致 keys 错位——直接 [error] 抛错。
     *
     * @param params 待检查入参
     * @throws IllegalStateException 入参中出现原始数组类型
     * @author K
     * @since 1.0.0
     */
    private fun validParamType(vararg params: Any) {
        params.forEach {
            if (it is IntArray || it is CharArray || it is ByteArray || it is ShortArray || it is LongArray ||
                it is FloatArray || it is DoubleArray || it is BooleanArray
            ) {
                error("If Hash batch cache method parameters are arrays, please use Array<Any>; IntArray, CharArray, etc. are not supported!")
            }
        }
    }

    /**
     * 计算批量 key 的笛卡尔积总数：把每个集合/数组型参数的 size 相乘。
     *
     * 单值参数计 1，不影响乘积。`ignoreParamIndexes` 标记的列已通过 [getParamIndexes] 排除。
     *
     * @param function 业务方法
     * @param params 入参
     * @return 笛卡尔积大小
     * @author K
     * @since 1.0.0
     */
    private fun calTotalCount(function: KFunction<*>?, vararg params: Any): Int {
        var totalCount = 1
        val paramIndexes = getParamIndexes(function, *params)
        params.forEachIndexed { index, it ->
            if (index in paramIndexes) {
                totalCount *= when (it) {
                    is Collection<*> -> it.size
                    is Array<*> -> it.size
                    else -> 1
                }
            }
        }
        return totalCount
    }

    /**
     * 按笛卡尔积语义把多个 collection/array 参数展开成 totalCount 条 key。
     *
     * 算法：每个集合按"组复制次数" (`totalCount / size`) 横向复制；调用方按 index % size 取段拼出每条 key。
     * 这是经典的笛卡尔积"小步快走"实现，避免一次性生成全笛卡尔后再投影的内存峰值。
     *
     * @param function 业务方法
     * @param totalCount 笛卡尔积总数（由 [calTotalCount] 算得）
     * @param params 入参
     * @return 拍扁后的 key 字符串列表
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateKeys(function: KFunction<*>?, totalCount: Int, vararg params: Any): List<String> {
        val keys = mutableListOf<List<Any>>()
        val paramIndexes = getParamIndexes(function, *params)
        for (index in paramIndexes) {
            val it = params[index]
            val parts = mutableListOf<Any>()
            when (it) {
                is Collection<*> if it.isNotEmpty() -> {
                    val groupCount = totalCount / it.size
                    repeat(groupCount) { parts.addAll(listOf(it)) }
                }

                is Array<*> if it.isNotEmpty() -> {
                    val groupCount = totalCount / it.size
                    repeat(groupCount) { parts.addAll(listOf(it)) }
                }

                is Collection<*> -> { /* parts stays empty */ }
                is Array<*> -> { /* parts stays empty */ }
                else -> repeat(totalCount) { parts.add(it) }
            }
            keys.add(parts)
        }
        val delimiter = getDelimiter()
        return (0 until totalCount).map { index ->
            keys.joinToString(delimiter) { seg -> seg[index].toString() }
        }
    }
}
