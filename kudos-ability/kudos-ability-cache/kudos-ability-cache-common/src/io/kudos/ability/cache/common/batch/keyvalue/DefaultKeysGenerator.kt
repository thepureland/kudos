package io.kudos.ability.cache.common.batch.keyvalue

import io.kudos.context.support.Consts
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

/**
 * 默认的批量缓存key生成器
 *
 *
 * 适用条件：
 *
 * 1.缓存方法参数，除集合与数组外，其他类型将被toString()
 *
 * 2.数组需要用Array<Int>、Array<String>等形式，不要使用IntArray、CharArray等形式
 *
 * 3.不支持无参
 *
 *
 * key的组装规则为：
 *
 * 1.由除ignoreParamIndexes指定的参数索引外的所有参数组成
 *
 * 2.各部分以半角冒号分隔
 *
 * 3.各部分顺序同参数顺序
 *
 * 4.总的key个数为各参数元素个数的积，如各参数分别为："1", listOf(2,3,4), arrayOf("5","6"),7，那么组装完的key共6个，分别为：
 *   "1:2:5:7", "1:3:6:7", "1:4:5:7", "1:2:6:7", "1:3:5:7", "1:4:6:7"
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
     * 校验参数的类型
     *
     * @param params 方法参数
     * @author K
     * @since 1.0.0
     */
    private fun validParamType(vararg params: Any) {
        params.forEach {
            if (it is IntArray || it is CharArray || it is ByteArray || it is ShortArray || it is LongArray ||
                it is FloatArray || it is DoubleArray || it is BooleanArray
            ) {
                error("缓存方法参数如果是数组，请使用Array<Any>类型，不支持IntArray、CharArray等类型！")
            }
        }
    }

    /**
     * 计算key的总个数
     *
     * @param params 方法参数
     * @return key的总数
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
     * 生成批量缓存的key列表
     * 
     * 根据方法参数生成所有可能的缓存key组合，支持集合、数组和普通参数。
     * 
     * 工作流程：
     * 1. 过滤参数：根据ignoreParamIndexes过滤掉需要忽略的参数
     * 2. 扩展参数：将每个参数扩展为totalCount大小的列表
     *    - 集合/数组：重复添加元素，直到列表大小为totalCount
     *    - 普通参数：重复添加相同值，直到列表大小为totalCount
     * 3. 组合key：遍历每个索引位置，将各参数列表的对应元素组合成一个key
     * 4. 返回结果：返回所有组合后的key列表
     * 
     * 扩展算法：
     * - 集合/数组：计算需要重复的次数（groupCount = totalCount / 元素个数）
     *   然后重复添加整个集合/数组，直到列表大小为totalCount
     * - 普通参数：直接重复添加totalCount次
     * 
     * 组合规则：
     * - 每个key由各参数在相同索引位置的元素组成
     * - 元素之间使用分隔符（delimiter）连接
     * - 例如：参数1的第i个元素 + 分隔符 + 参数2的第i个元素 + ...
     * 
     * 示例：
     * - 参数："1", listOf(2,3), arrayOf("a","b")
     * - totalCount = 1 * 2 * 2 = 4
     * - 扩展后：
     *   * 参数1: ["1", "1", "1", "1"]
     *   * 参数2: [2, 3, 2, 3]（重复2次）
     *   * 参数3: ["a", "a", "b", "b"]（重复2次）
     * - 生成的key: ["1:2:a", "1:3:a", "1:2:b", "1:3:b"]
     * 
     * 注意事项：
     * - 参数必须支持toString()方法
     * - 集合/数组为空时，不会添加任何元素
     * - 生成的key数量等于totalCount（各参数元素个数的乘积）
     * - 使用分隔符连接，最后会去除末尾的分隔符
     * 
     * @param function 目标方法，用于获取ignoreParamIndexes配置
     * @param totalCount 要生成的key总数（各参数元素个数的乘积）
     * @param params 方法参数，可能包含集合、数组或普通值
     * @return 生成的key列表，数量等于totalCount
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateKeys(function: KFunction<*>?, totalCount: Int, vararg params: Any): List<String> {
        val keys = mutableListOf<List<Any>>() // List<List<key同一分段的部分>>
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