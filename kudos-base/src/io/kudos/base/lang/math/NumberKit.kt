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
    /**
     * 将字符串转换为Number对象
     * 
     * 支持多种数字格式：十六进制、十进制整数、浮点数、科学计数法。
     * 
     * 工作流程：
     * 1. 空值处理：如果输入为null，直接返回null
     * 2. 去除空白：去除字符串首尾空白字符
     * 3. 空字符串检查：如果去除空白后为空，抛出异常
     * 4. 转换为小写：便于统一处理十六进制标识
     * 5. 格式判断：
     *    - 十六进制：以"0x"或"-0x"开头
     *    - 浮点数：包含小数点或科学计数法（e/E）
     *    - 整数：其他情况
     * 
     * 支持的格式：
     * 1. 十六进制：
     *    - 正数："0xFF" → 255
     *    - 负数："-0x1A" → -26
     *    - 使用BigInteger解析，支持任意精度
     * 2. 浮点数：
     *    - 普通小数："3.14" → BigDecimal
     *    - 科学计数法："1.23e10" → BigDecimal
     *    - 使用BigDecimal解析，保证精度
     * 3. 整数：
     *    - 普通整数："123" → BigInteger
     *    - 支持以0开头（不再识别为八进制，统一按十进制处理）
     *    - 使用BigInteger解析，支持任意精度
     * 
     * 精度保证：
     * - 整数使用BigInteger，支持任意长度的整数
     * - 浮点数使用BigDecimal，保证精度不丢失
     * - 避免使用基本类型导致的精度问题
     * 
     * 异常处理：
     * - 空字符串：抛出NumberFormatException
     * - 格式错误：抛出NumberFormatException，包含详细错误信息
     * - 所有异常都包含原始字符串，便于调试
     * 
     * 注意事项：
     * - 十六进制标识不区分大小写（0x、0X都可以）
     * - 科学计数法不区分大小写（e、E都可以）
     * - 以0开头的数字不再识别为八进制，统一按十进制处理
     * - 返回BigInteger或BigDecimal，需要根据实际需求转换
     * 
     * @param str 待转换的字符串，可以为null
     * @return Number对象（BigInteger或BigDecimal），如果输入为null则返回null
     * @throws NumberFormatException 如果字符串格式不正确或为空
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
                } catch (ex: NumberFormatException) {
                    throw NumberFormatException("无法将 \"$s\" 转换为 BigDecimal: ${ex.message}")
                }
            }

            else -> {
                // 普通整数，尝试用 BigInteger 解析
                try {
                    // 支持以 0 开头的八进制? Java 不再默认识别八进制，统一用 BigInteger 十进制
                    BigInteger(s)
                } catch (ex: NumberFormatException) {
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
        } catch (_: NumberFormatException) {
            false
        }
    }

}

