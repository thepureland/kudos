package io.kudos.base.lang

/**
 * kotlin.Boolean extension functions.
 *
 * @author K
 * @since 1.0.0
 */


/**
 * Convert a boolean to an int; 0 is treated as false.
 *
 * <pre>
 * true.toInt()  = 1
 * false.toInt() = 0
 * </pre>
 *
 * @return 1 for `true`, 0 for `false`
 * @author K
 * @since 1.0.0
 */
fun Boolean.toInt(): Int = if (this) 1 else 0


/**
 * Convert a Boolean to a String, returning `'true'` or `'false'`.
 *
 * <pre>
 * true.toStringTrueFalse()  = "true"
 * false.toStringTrueFalse() = "false"
 * </pre>
 *
 * @return `'true'` or `'false'`
 * @author K
 * @since 1.0.0
 */
fun Boolean.toStringTrueFalse(): String = toString()

/**
 * Convert a Boolean to a String, returning `'on'` or `'off'`.
 *
 * <pre>
 * true.toStringTrueFalse()  = "on"
 * false.toStringTrueFalse() = "off"
 * </pre>
 *
 * @return `'on'` or `'off'`
 * @author K
 * @since 1.0.0
 */
fun Boolean.toStringOnOff(): String = if (this) "on" else "off"

/**
 * Convert a Boolean to a String, returning `'yes'` or `'no'`.
 *
 * <pre>
 * true.toStringTrueFalse()  = "yes"
 * false.toStringTrueFalse() = "no"
 * </pre>
 *
 * @return `'yes'` or `'no'`
 * @author K
 * @since 1.0.0
 */
fun Boolean.toStringYesNo(): String = if (this) "yes" else "no"

/**
 * Convert a Boolean to a String, returning one of the supplied matching strings.
 *
 * <pre>
 * true.toString("true", "false")   = "true"
 * false.toString("true", "false")  = "false"
 * </pre>
 *
 * @param trueString the value representing `true` (case-sensitive), may be `null`
 * @param falseString the value representing `false` (case-sensitive), may be `null`
 * @return one of the supplied matching strings
 * @author K
 * @since 1.0.0
 */
fun Boolean.toString(trueString: String?, falseString: String?): String? =
    if (this) trueString else falseString

/**
 * Perform a logical AND over an array of booleans.
 *
 * <pre>
 * [true, true].and()         = true
 * [false, false].and()       = false
 * [true, false].and()        = false
 * [true, true, false].and()  = false
 * [true, true, true].and()   = true
 * </pre>
 *
 * @return the result of the logical AND
 * @throws IllegalArgumentException if `array` is empty.
 * @author K
 * @since 1.0.0
 */
fun Array<Boolean>.and(): Boolean {
    require(isNotEmpty()) { "Boolean array must not be empty" }
    // Result is false as long as any element is false
    return all { it }
}


/**
 * Perform a logical OR over an array of booleans.
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
 * @return the result of the logical OR
 * @throws IllegalArgumentException if `array` is empty.
 * @author K
 * @since 1.0.0
 */
fun Array<Boolean>.or(): Boolean {
    require(isNotEmpty()) { "Boolean array must not be empty" }
    // Result is true as long as any element is true
    return any { it }

}

/**
 * Perform a logical XOR over an array of booleans.
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
 * @return the result of the logical XOR
 * @throws IllegalArgumentException if `array` is empty.
 * @author K
 * @since 1.0.0
 */
fun Array<Boolean>.xor(): Boolean {
    require(this.isNotEmpty()) { "Boolean array must not be empty" }
    // Count true values and check whether the count is odd
    val trueCount = count { it }
    return trueCount % 2 == 1
}
