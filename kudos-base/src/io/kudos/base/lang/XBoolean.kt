package io.kudos.base.lang

/**
 * kotlin.Boolean扩展函数
 *
 * @author K
 * @since 1.0.0
 */


/**
 * 将boolean转化为int, 0当作false
 *
 * <pre>
 * true.toInt()  = 1
 * false.toInt() = 0
 * </pre>
 *
 * @return `true`返回1, `false`返回0
 * @author K
 * @since 1.0.0
 */
fun Boolean.toInt(): Int = if (this) 1 else 0


/**
 * 将Boolean转化为String, 返回`'true'`, `'false'`
 *
 * <pre>
 * true.toStringTrueFalse()  = "true"
 * false.toStringTrueFalse() = "false"
 * </pre>
 *
 * @return `'true'`, `'false'`
 * @author K
 * @since 1.0.0
 */
fun Boolean.toStringTrueFalse(): String = toString()

/**
 * 将Boolean转化为String, 返回`'on'`, `'off'`
 *
 * <pre>
 * true.toStringTrueFalse()  = "on"
 * false.toStringTrueFalse() = "off"
 * </pre>
 *
 * @return `'on'`, `'off'`
 * @author K
 * @since 1.0.0
 */
fun Boolean.toStringOnOff(): String = if (this) "on" else "off"

/**
 * 将Boolean转化为String, 返回`'yes'`, `'no'`
 *
 * <pre>
 * true.toStringTrueFalse()  = "yes"
 * false.toStringTrueFalse() = "no"
 * </pre>
 *
 * @return `'yes'`, `'no'`
 * @author K
 * @since 1.0.0
 */
fun Boolean.toStringYesNo(): String = if (this) "yes" else "no"

/**
 * 将Boolean转化为String, 返回输入的某个匹配的字符串
 *
 * <pre>
 * true.toString("true", "false")   = "true"
 * false.toString("true", "false")  = "false"
 * </pre>
 *
 * @param trueString 代表 `true`的值(大小写敏感), 可以为 `null`
 * @param falseString 代表 `false`的值(大小写敏感), 可以为 `null`
 * @return 输入的某个匹配的字符串
 * @author K
 * @since 1.0.0
 */
fun Boolean.toString(trueString: String?, falseString: String?): String? =
    if (this) trueString else falseString

/**
 * 对一组boolean进行逻辑与操作
 *
 * <pre>
 * [true, true].and()         = true
 * [false, false].and()       = false
 * [true, false].and()        = false
 * [true, true, false].and()  = false
 * [true, true, true].and()   = true
 * </pre>
 *
 * @return 逻辑与操作的结果
 * @throws IllegalArgumentException 如果 `array` 为空.
 * @author K
 * @since 1.0.0
 */
fun Array<Boolean>.and(): Boolean {
    require(isNotEmpty()) { "Boolean 数组不能为空" }
    // 只要有一个 false，则结果为 false
    return all { it }
}


/**
 * 对一组boolean进行逻辑或操作
 *
 * <pre>
 * [true, true].or()          = true
 * [false, false].or()        = false
 * [true, false].or()         = true
 * [true, true, false].or()   = true
 * [true, true, true].or()    = true
 * [false, false, false].or() = false
 * </pre>
 *
 * @return 逻辑或操作的结果
 * @throws IllegalArgumentException 如果 `array` 为空.
 * @author K
 * @since 1.0.0
 */
fun Array<Boolean>.or(): Boolean {
    require(isNotEmpty()) { "Boolean 数组不能为空" }
    // 只要有一个 true，则结果为 true
    return any { it }

}

/**
 * 对一组boolean进行逻辑异或操作
 *
 * <pre>
 * [true, true].xor()   = false
 * [false, false].xor() = false
 * [true, false].xor()  = true
 * [true, true].xor()   = false
 * [false, false].xor() = false
 * [true, false].xor()  = true
 * </pre>
 *
 * @return 逻辑异或操作的结果
 * @throws IllegalArgumentException 如果 `array` 为空.
 * @author K
 * @since 1.0.0
 */
fun Array<Boolean>.xor(): Boolean {
    require(this.isNotEmpty()) { "Boolean 数组不能为空" }
    // 统计 true 的个数，看是否为奇数
    val trueCount = count { it }
    return trueCount % 2 == 1
}