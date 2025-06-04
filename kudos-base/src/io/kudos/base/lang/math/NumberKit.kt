package io.kudos.base.lang.math

import java.math.BigDecimal
import java.math.BigInteger

/**
 * 数值工具类
 *
 * @author K
 * @since 1.0.0
 */
object NumberKit {


    /**
     * 将字符串转换为Number
     * 首先，将检查给定值的结尾类型限定符`'f','F','d','D','l','L'`。
     * 如果找到，开始尝试从指定的类型逐个创建更大的类型，直到找到一个能表示该值的类型。
     * 如果一个类型说明符也没有找到，它会检查小数点，然后从小到大地尝试类型，
     * 从Integer到BigInteger，从Float的BigDecimal
     * 一个字符串以`0x` 或 `-0x`(大写或小写)开头，它将被解释为十六进制整数。
     * 以`0`开头的则被解释为八进制。
     * 如果参数为 `null` 将返回 `null`.
     * 该方法不会对输入的字符串作trim操作。
     * 如：字符串含有前导或后导空格将抛出NumberFormatException异常.
     *
     * @param str 数值的字符串形式, 可以为null
     * @return 字符串所代表的数值，为 `null` 将返回 `null`
     * @throws NumberFormatException 如果字符串不能被转换
     * @author K
     * @since 1.0.0
     */
    fun createNumber(str: String?): Number? {
        if (str == null) return null
        // 先检查是否为十六进制（可带正负号）
        val s = str.trim()
        if (s.isEmpty()) throw NumberFormatException("空字符串不能转换为数字")
        val lower = s.lowercase()
        return when {
            lower.startsWith("0x") -> {
                // 例如 "0xFF" ⇒ 255
                BigInteger(lower.substring(2), 16)
            }

            lower.startsWith("-0x") -> {
                // 例如 "-0x1A" ⇒ -26
                BigInteger(lower.substring(3), 16).negate()
            }
            // 判断是否包含小数点或科学计数法
            s.contains('.') || lower.contains('e') -> {
                // 直接用 BigDecimal 解析
                try {
                    BigDecimal(s)
                } catch (ex: Exception) {
                    throw NumberFormatException("无法将 \"$s\" 转换为 BigDecimal: ${ex.message}")
                }
            }

            else -> {
                // 普通整数，尝试用 BigInteger 解析
                try {
                    // 支持以 0 开头的八进制? Java 不再默认识别八进制，统一用 BigInteger 十进制
                    BigInteger(s)
                } catch (ex: Exception) {
                    throw NumberFormatException("无法将 \"$s\" 转换为整数: ${ex.message}")
                }
            }
        }
    }


    /**
     * 检查指定的字符串是否只包含数字字符
     * `Null` 或 空串将返回 `false`.
     *
     * @param str 待检查的字符串
     * @return `true` 指定的字符串只包含Unicode的数字字符
     * @author K
     * @since 1.0.0
     */
    fun isDigits(str: String?): Boolean {
        if (str.isNullOrEmpty()) return false
        // 只要所有字符都是 0..9，即视为数字
        return str.all { it in '0'..'9' }
    }

    /**
     * 检查指定的字符串是否只为java的数值
     * 有效的数值包括以限定符`0x`开头的十六进制数，科学记数法和
     * 以类型限定符结尾的数值（如：123L）
     * `Null` 或 空串将返回 `false`.
     *
     * @param str 待检查的字符串
     * @return `true` 如果指定的字符串为一个正确格式的数值
     * @author K
     * @since 1.0.0
     */
    fun isNumber(str: String?): Boolean {
        if (str.isNullOrEmpty()) return false
        val s = str.trim()
        if (s.isEmpty()) return false

        // 检查十六进制
        val lower = s.lowercase()
        if (lower.startsWith("0x") || lower.startsWith("-0x")) {
            // 后面至少要有一个十六进制字符
            val hexPart = if (lower.startsWith("0x")) lower.substring(2) else lower.substring(3)
            if (hexPart.isEmpty()) return false
            return hexPart.all { it in '0'..'9' || it in 'a'..'f' }
        }

        // 带小数点或科学记数法，则尝试 BigDecimal 解析
        return try {
            BigDecimal(s)
            true
        } catch (_: Exception) {
            false
        }
    }

}

