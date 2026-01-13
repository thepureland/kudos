package io.kudos.base.lang.collections

import java.io.PrintStream
import java.util.*

/**
 * kotlin.Map扩展函数
 *
 * @author K
 * @since 1.0.0
 */


/**
 * 把Map转换为二维数组，每行两个元素，按顺序分别为map的key和value
 *
 * @return 二维数组
 * @author K
 * @since 1.0.0
 */
fun Map<*, *>.toArrOfArr(): Array<Array<Any?>> =
    if (this.isEmpty()) {
        emptyArray()
    } else {
        // entries.toList() 保证 Map 顺序，mapIndexed 构建二维 Array
        this.entries
            .map { (k, v) -> arrayOf(k, v) }
            .toTypedArray()
    }



/**
 * 是否包含子map(所有key和value都相等)
 *
 * @param <K> Key
 * @param V Value
 * @param subMap 子map，为null或空返回false
 * @return 所有key和value都相等时返回true，反之返回false
 * @author K
 * @since 1.0.0
 */
fun <K, V> Map<*, *>.containsAll(subMap: Map<K, V>): Boolean {
    if (this.isEmpty() || subMap.isEmpty()) return false
    for ((k, v) in subMap) {
        if (!this.containsKey(k)
            || (v == null && this[k] != null)
            || (v != null && this[k] != v)) {
            return false
        }
    }
    return true
}


// Conversion methods
// -------------------------------------------------------------------------

/**
 * 将ResourceBundle转为Map
 *
 * @return HashMap
 * @author K
 * @since 1.0.0
 */
fun ResourceBundle.toMap(): MutableMap<String, Any?> {
    val result = mutableMapOf<String, Any?>()
    for (key in this.keySet()) {
        result[key] = this.getObject(key)
    }
    return result
}


//region Printing

/**
 * 将指定的Map的内容分行打印
 * 该方法打印出Map的良好格式的字符串描述。
 * 每个Map实体将打印出key和value。当值为Map时，该行为将递归。
 * 该方法不是线程安全的。您必须手动同步该类或请求的流。
 *
 * @param out   打印要输出的流,
 * @param label 要使用的标签, 可以为null. 为null该标签将不被输出. 它经常代表bean(或类似)的属性名
 * @author K
 * @since 1.0.0
 */
fun Map<*, *>.verbosePrint(out: PrintStream, label: Any?)
    = verbosePrintInternal(out, label, this, mutableListOf(), debug = false)

/**
 * 将指定的Map的内容分行打印
 * 该方法打印出Map的良好格式的字符串描述。
 * 每个Map实体将打印出key、value和类名。当值为Map时，该行为将递归。
 * 该方法不是线程安全的。您必须手动同步该类或请求的流。
 *
 * @param out   打印要输出的流
 * @param label 要使用的标签, 可以为null. 为null该标签将不被输出. 它经常代表bean(或类似)的属性名
 * @author K
 * @since 1.0.0
 */
fun Map<*, *>.debugPrint(out: PrintStream, label: Any?)
    = verbosePrintInternal(out, label, this, mutableListOf(), debug = true)

//endregion Printing

/**
 * 反转Map。返回一个指定Map的key和value对换过的新HashMap。
 * 该方法假设要反转的Map是定义良好的。如果输入的map有多个
 * 相同值映射到不同key的实体，返回的map将只映射其中一个key
 * 到该value，但是具体是哪一个key是不确定的。
 *
 * @param K Key
 * @param V Value
 * @return 一个包含反转后的数据的新HashMap
 * @author K
 * @since 1.0.0
 */
fun <K, V> Map<K, V>.invertMap(): Map<V, K> {
    val result = mutableMapOf<V, K>()
    for ((k, v) in this) {
        result[v] = k
    }
    return result
}


/**
 * 递归打印Map结构
 * 
 * 以树形结构递归打印Map的内容，支持嵌套Map和循环引用检测。
 * 
 * 工作流程：
 * 1. 空值处理：如果map为null，直接打印"null"并返回
 * 2. 打印标签：如果提供了label，打印标签和等号
 * 3. 开始打印：打印左大括号，开始当前Map的内容
 * 4. 加入lineage：将当前Map加入lineage，用于循环引用检测
 * 5. 遍历条目：
 *    - 如果value是Map且不在lineage中：递归打印
 *    - 如果value是Map但在lineage中：打印循环引用标识
 *    - 如果value不是Map：打印值（debug模式下包含类型信息）
 * 6. 移除lineage：打印完成后从lineage中移除当前Map
 * 7. 结束打印：打印右大括号，结束当前Map
 * 
 * 循环引用检测：
 * - 使用lineage列表追踪已访问的Map
 * - 如果value是Map且已在lineage中，说明发生循环引用
 * - 打印"(this Map)"表示指向自身
 * - 打印"(ancestor[idx] Map)"表示指向祖先Map
 * 
 * 缩进机制：
 * - 根据lineage.size决定缩进级别
 * - 每层缩进2个空格
 * - 形成树形结构的视觉效果
 * 
 * Debug模式：
 * - 如果debug=true，会输出每个值的类型信息
 * - 格式：value (类型名)
 * - 有助于调试和类型检查
 * 
 * 输出格式：
 * ```
 * label = {
 *   key1 => value1
 *   key2 => {
 *     nestedKey => nestedValue
 *   }
 *   key3 => (this Map)  // 循环引用
 * }
 * ```
 * 
 * 注意事项：
 * - 使用lineage避免无限递归
 * - 递归深度没有限制，但实际受JVM栈深度限制
 * - 循环引用会被检测并标记，不会导致栈溢出
 * 
 * @param out 输出流，用于打印内容
 * @param label 本次打印的标签，用于标识当前Map，可以为null
 * @param map 要打印的Map，可以为null
 * @param lineage 已进入递归打印的上层Map列表，用于检测循环引用
 * @param debug 如果为true，在打印每个非Map值时输出其类型名；否则仅输出值本身
 */
private fun verbosePrintInternal(
    out: PrintStream,
    label: Any?,
    map: Map<*, *>?,
    lineage: MutableList<Map<*, *>>,
    debug: Boolean
) {
    // 根据当前递归深度决定缩进空格数，每层两个空格
    fun printIndent(level: Int) {
        repeat(level) { out.print("  ") }
    }

    // 1. 首先打印 label 和 map 是否为 null
    printIndent(lineage.size)
    if (map == null) {
        // 如果 Map 本身为 null，直接打印 "label = null" 或 "null"
        if (label != null) {
            out.print(label)
            out.print(" = ")
        }
        out.println("null")
        return
    }

    // 2. 如果传了 label 且 map 不为 null，就先打印 "label ="
    if (label != null) {
        printIndent(lineage.size)
        out.print(label)
        out.println(" = ")
    }

    // 3. 打开大括号，开始打印当前 Map 的内容
    printIndent(lineage.size)
    out.println("{")

    // 4. 将当前 map 加入 lineage，以便后续检测循环引用
    lineage.add(map)

    // 5. 遍历 map 中的每一个 entry
    for ((childKey, childValue) in map.entries) {
        // 如果 value 也是一个 Map，且不在 ancestry 中，就对其递归打印
        if (childValue is Map<*, *> && !lineage.contains(childValue)) {
            // 递归调用：将 childKey 作为下一层的 label，childValue 作为下层要打印的 Map
            verbosePrintInternal(
                out,
                childKey ?: "null",
                childValue,
                lineage,
                debug
            )
        } else {
            // 否则要么 value 不是 Map，要么虽然是 Map 但已经在 lineage 中（循环引用）
            printIndent(lineage.size)

            // 打印 key
            out.print(childKey)

            // 打印分隔符 " => "
            out.print(" => ")

            if (childValue is Map<*, *>) {
                // 如果 childValue 是 Map 且已经在 lineage 中，说明发生了循环引用
                val idx = lineage.indexOf(childValue)
                if (idx == 0) {
                    // 如果同一层（刚好是传进来的那个 map），表明它指向自己
                    out.println("(this Map)")
                } else {
                    // 否则，指向祖先中的某一级
                    out.println("(ancestor[$idx] Map)")
                }
            } else {
                // 普通值或 null 都走这里
                out.print(childValue)
                if (debug && childValue != null) {
                    // debug 模式下，额外输出类型信息
                    out.print(" (${childValue.javaClass.name})")
                }
                out.println()
            }
        }
    }

    // 6. 当前 Map 的所有 entries 打印完毕，从 lineage 中移除
    lineage.removeAt(lineage.lastIndex)

    // 7. 缩进并打印右括号，结束本层
    printIndent(lineage.size)
    out.println("}")
}