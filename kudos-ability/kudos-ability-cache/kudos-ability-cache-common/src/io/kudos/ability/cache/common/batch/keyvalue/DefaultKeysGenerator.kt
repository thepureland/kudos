package io.kudos.ability.cache.common.batch.keyvalue

import io.kudos.context.support.Consts
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

/**
 * Default batch cache key generator.
 *
 *
 * Applicable conditions:
 *
 * 1. For cache method parameters, all types except collections and arrays will be converted via toString().
 *
 * 2. Arrays must be of form Array<Int>, Array<String>, etc. Do not use IntArray, CharArray, etc.
 *
 * 3. No-arg methods are not supported.
 *
 *
 * Key composition rules (**positional zip**, not a cartesian product):
 *
 * 1. Composed of all parameters except those at indexes specified by ignoreParamIndexes.
 *
 * 2. Parts are separated by the configured delimiter (default `::`, see [Consts.CACHE_KEY_DEFAULT_DELIMITER]).
 *
 * 3. Parts follow the parameter order.
 *
 * 4. All collection/array parameters that compose the key **must have the same length L**; the i-th key is built
 *    from the i-th element of each of them. Scalar parameters are repeated across all L keys. The number of
 *    generated keys therefore equals L. Collection/array parameters of differing lengths are rejected with an
 *    error. For example, given parameters `"1", listOf("a","b","c"), arrayOf("x","y","z")`, the 3 keys are:
 *    `"1::a::x"`, `"1::b::y"`, `"1::c::z"`.
 *
 *    This generator intentionally does **not** produce the cartesian product of the parameter values; callers that
 *    need every combination must expand the combinations themselves before the cached call.
 *
 * @author K
 * @since 1.0.0
 */
class DefaultKeysGenerator : IKeysGenerator {

    override fun generate(target: Any?, function: KFunction<*>?, vararg params: Any): List<String> {
        validParamType(*params)
        val paramIndexes = getParamIndexes(function, *params)
        val totalCount = calTotalCount(paramIndexes, *params)
        return generateKeys(paramIndexes, totalCount, *params)
    }

    override fun getDelimiter(): String = Consts.CACHE_KEY_DEFAULT_DELIMITER

    override fun getParamIndexes(function: KFunction<*>?, vararg params: Any): List<Int> {
        val ignoreParamIndexes =
            if (function == null) {
                intArrayOf()
            } else {
                val batchCacheable = function.findAnnotation<BatchCacheable>()!!
                batchCacheable.ignoreParamIndexes
            }
        return params.indices.filter { it !in ignoreParamIndexes }
    }

    /**
     * Validates parameter types.
     *
     * @param params method parameters
     * @author K
     * @since 1.0.0
     */
    private fun validParamType(vararg params: Any) {
        params.forEach {
            if (it is IntArray || it is CharArray || it is ByteArray || it is ShortArray || it is LongArray ||
                it is FloatArray || it is DoubleArray || it is BooleanArray
            ) {
                error("If cache method parameters are arrays, please use Array<Any>; primitive arrays such as IntArray, CharArray are not supported!")
            }
        }
    }

    /**
     * Calculates the total number of keys = the common length L of the participating collection/array parameters.
     *
     * Under positional-zip semantics, every collection/array parameter that composes the key must share the same
     * length; scalar parameters do not constrain L. When no collection/array parameter participates, L is 1.
     *
     * @param paramIndexes indexes of the parameters that compose the key (ignoreParamIndexes already removed)
     * @param params method parameters
     * @return the number of keys to generate
     * @throws IllegalStateException if the participating collection/array parameters have differing lengths
     * @author K
     * @since 1.0.0
     */
    private fun calTotalCount(paramIndexes: List<Int>, vararg params: Any): Int {
        val sizes = params
            .filterIndexed { index, _ -> index in paramIndexes }
            .mapNotNull {
                when (it) {
                    is Collection<*> -> it.size
                    is Array<*> -> it.size
                    else -> null
                }
            }
        if (sizes.isEmpty()) return 1
        val distinctSizes = sizes.distinct()
        check(distinctSizes.size == 1) {
            "All collection/array parameters that compose a batch cache key must have the same length " +
                "(positional zip semantics); got sizes=$sizes. This generator does not support a cartesian product."
        }
        return distinctSizes.single()
    }

    /**
     * Generates the list of batch cache keys by positional zip.
     *
     * Workflow:
     * 1. Filter parameters: keep only those in [paramIndexes] (ignoreParamIndexes already removed).
     * 2. Normalize each parameter into a column of length [totalCount]:
     *    - Collection/array: used as-is (its length already equals totalCount, enforced by [calTotalCount]).
     *    - Scalar: repeated totalCount times.
     * 3. For each row index i, join the i-th element of every column with the delimiter to form one key.
     *
     * Example:
     * - Parameters: "1", listOf("a","b"), arrayOf("x","y")  (L = 2)
     * - Columns: ["1","1"], ["a","b"], ["x","y"]
     * - Generated keys: ["1::a::x", "1::b::y"]
     *
     * Notes:
     * - Parameters must support toString().
     * - An empty collection/array (L = 0) yields an empty key list.
     * - The trailing delimiter is trimmed from each composed key.
     *
     * @param paramIndexes indexes of the parameters that compose the key
     * @param totalCount number of keys to generate (the common collection/array length)
     * @param params method parameters, may contain collections, arrays or plain values
     * @return list of generated keys, with size equal to totalCount
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateKeys(paramIndexes: List<Int>, totalCount: Int, vararg params: Any): List<String> {
        if (totalCount == 0) return emptyList()

        val columns = mutableListOf<List<Any>>() // one column per participating parameter
        params.filterIndexed { index, _ -> index in paramIndexes }.forEach {
            val column: List<Any> = when (it) {
                is Collection<*> -> (it as Collection<Any>).toList()
                is Array<*> -> (it as Array<Any>).toList()
                else -> List(totalCount) { _ -> it }
            }
            columns.add(column)
        }

        val result = mutableListOf<String>()
        val delimiter = getDelimiter()
        for (index in 0 until totalCount) {
            val sb = StringBuilder()
            columns.forEach { column -> sb.append(column[index]).append(delimiter) }
            result.add(sb.substring(0, sb.length - delimiter.length))
        }
        return result
    }

}
