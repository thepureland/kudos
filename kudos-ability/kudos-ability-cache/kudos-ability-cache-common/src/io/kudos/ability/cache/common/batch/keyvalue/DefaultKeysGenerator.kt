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
 * Key composition rules:
 *
 * 1. Composed of all parameters except those at indexes specified by ignoreParamIndexes.
 *
 * 2. Parts are separated by a half-width colon.
 *
 * 3. Parts follow the parameter order.
 *
 * 4. The total number of keys equals the product of element counts of each parameter. For example, given parameters
 *    "1", listOf(2,3,4), arrayOf("5","6"), 7, the resulting 6 keys are:
 *    "1:2:5:7", "1:3:6:7", "1:4:5:7", "1:2:6:7", "1:3:5:7", "1:4:6:7"
 *
 * @author K
 * @since 1.0.0
 */
class DefaultKeysGenerator : IKeysGenerator {

    override fun generate(target: Any?, function: KFunction<*>?, vararg params: Any): List<String> {
        validParamType(*params)
        val totalCount = calTotalCount(*params)
        return generateKeys(function, totalCount, *params)
    }

    override fun getDelimiter(): String = Consts.CACHE_KEY_DEFAULT_DELIMITER

    override fun getParamIndexes(function: KFunction<*>?, vararg params: Any): List<Int> {
        var ignoreParamIndexes =
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
     * Calculates the total number of keys.
     *
     * @param params method parameters
     * @return total number of keys
     * @author K
     * @since 1.0.0
     */
    private fun calTotalCount(vararg params: Any): Int {
        var totalCount = 1
        params.forEach {
            val count = when (it) {
                is Collection<*> -> it.size
                is Array<*> -> it.size
                else -> 1
            }
            totalCount *= count
        }
        return totalCount
    }

    /**
     * Generates the list of batch cache keys.
     *
     * Produces all possible cache key combinations from method parameters, supporting collections, arrays and plain values.
     *
     * Workflow:
     * 1. Filter parameters: exclude those specified by ignoreParamIndexes.
     * 2. Expand parameters: expand each parameter into a list of size totalCount.
     *    - Collection/array: repeatedly append elements until list size reaches totalCount.
     *    - Plain parameter: repeatedly append the same value until list size reaches totalCount.
     * 3. Combine keys: for each index, join the corresponding element from each parameter list into a single key.
     * 4. Return result: return the list of all composed keys.
     *
     * Expansion algorithm:
     * - Collection/array: compute repeat count (groupCount = totalCount / element count),
     *   then append the whole collection/array repeatedly until size equals totalCount.
     * - Plain parameter: append totalCount copies directly.
     *
     * Composition rules:
     * - Each key consists of elements from each parameter at the same index position.
     * - Elements are joined by a delimiter.
     * - For example: param1[i] + delimiter + param2[i] + ...
     *
     * Example:
     * - Parameters: "1", listOf(2,3), arrayOf("a","b")
     * - totalCount = 1 * 2 * 2 = 4
     * - After expansion:
     *   * Param 1: ["1", "1", "1", "1"]
     *   * Param 2: [2, 3, 2, 3] (repeated twice)
     *   * Param 3: ["a", "a", "b", "b"] (repeated twice)
     * - Generated keys: ["1:2:a", "1:3:a", "1:2:b", "1:3:b"]
     *
     * Notes:
     * - Parameters must support toString().
     * - Empty collections/arrays contribute no elements.
     * - The number of generated keys equals totalCount (product of element counts).
     * - The trailing delimiter is trimmed from each composed key.
     *
     * @param function target method, used to read ignoreParamIndexes configuration
     * @param totalCount total number of keys to generate (product of element counts)
     * @param params method parameters, may contain collections, arrays or plain values
     * @return list of generated keys, with size equal to totalCount
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateKeys(function: KFunction<*>?, totalCount: Int, vararg params: Any): List<String> {
        val keys = mutableListOf<List<Any>>() // List<List<parts at the same key segment>>
        val paramIndexes = getParamIndexes(function, *params)
        params.filterIndexed { index, _ -> index in paramIndexes }.forEach {
            val parts = mutableListOf<Any>()
            when (it) {
                is Collection<*> -> {
                    if (it.isNotEmpty()) {
                        val groupCount = totalCount / it.size
                        (0 until groupCount).forEach { group ->
                            parts.addAll(it as Collection<Any>)
                        }
                    }
                }
                is Array<*> -> {
                    if (it.isNotEmpty()) {
                        val groupCount = totalCount / it.size
                        (0 until groupCount).forEach { group ->
                            parts.addAll(it as Array<Any>)
                        }
                    }
                }
                else -> {
                    (0 until totalCount).forEach { group ->
                        parts.add(it)
                    }
                }
            }
            keys.add(parts)
        }

        val result = mutableListOf<String>()
        val delimiter = getDelimiter()
        for (index in 0 until totalCount) {
            val sb = StringBuilder()
            keys.forEachIndexed { segIndex, _ -> sb.append(keys[segIndex][index]).append(delimiter) }
            result.add(sb.substring(0, sb.length - delimiter.length))
        }
        return result
    }

}