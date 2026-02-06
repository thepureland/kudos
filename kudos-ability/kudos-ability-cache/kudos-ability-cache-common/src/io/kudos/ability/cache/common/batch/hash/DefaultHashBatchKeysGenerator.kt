package io.kudos.ability.cache.common.batch.hash

import io.kudos.ability.cache.common.batch.keyvalue.IKeysGenerator
import io.kudos.context.support.Consts
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

/**
 * 默认的 Hash 批量缓存 key 生成器：从方法参数中按 [HashBatchCacheableByPrimary.ignoreParamIndexes] 排除后，
 * 用与 [io.kudos.ability.cache.common.batch.keyvalue.DefaultKeysGenerator] 相同的规则生成 id 列表（即 key 列表）。
 *
 * 典型用法：方法形如 `getByIds(ids: List<String>): Map<String, E?>`，则 keys = ids。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
class DefaultHashBatchKeysGenerator : IKeysGenerator {

    override fun generate(target: Any?, function: KFunction<*>?, vararg params: Any): List<String> {
        validParamType(*params)
        val paramIndexes = getParamIndexes(function, *params)
        // 典型用法：仅一个参数且为 id 集合，直接展开为 key 列表，避免笛卡尔积且避免 JVM 下非 Collection 类型问题
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

    private fun validParamType(vararg params: Any) {
        params.forEach {
            if (it is IntArray || it is CharArray || it is ByteArray || it is ShortArray || it is LongArray ||
                it is FloatArray || it is DoubleArray || it is BooleanArray
            ) {
                error("Hash 批量缓存方法参数如是数组，请使用 Array<Any>，不支持 IntArray、CharArray 等类型！")
            }
        }
    }

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

                is Collection<*> -> { /* parts 保持空 */ }
                is Array<*> -> { /* parts 保持空 */ }
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
