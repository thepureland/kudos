package io.kudos.base.lang.math

import java.math.BigDecimal
import java.math.BigInteger

/**
 * Number utility.
 *
 * @author K
 * @since 1.0.0
 */
object NumberKit {


    /**
     * Convert a string to a Number.
     * First, the trailing type qualifiers `'f','F','d','D','l','L'` of the value are checked.
     * If one is found, the method starts from the specified type and tries successively larger types until it finds one capable of representing the value.
     * If no type specifier is found, the method checks for a decimal point and then tries types from smallest to largest,
     * Integer -> BigInteger, Float -> BigDecimal.
     * A string starting with `0x` or `-0x` (in either case) is interpreted as a hexadecimal integer.
     * A string starting with `0` is interpreted as octal.
     * If the argument is `null`, `null` is returned.
     * This method does not trim the input string.
     * For example, a string containing leading or trailing whitespace will throw a NumberFormatException.
     *
     * @param str the string form of the number, may be null
     * @return the numeric value represented by the string, returns `null` if `null` is passed in
     * @throws NumberFormatException if the string cannot be converted
     * @author K
     * @since 1.0.0
     */
    /**
     * Convert a string to a Number object.
     *
     * Supports a variety of numeric formats: hexadecimal, decimal integer, floating point, and scientific notation.
     *
     * Workflow:
     * 1. Null handling: if the input is null, return null directly
     * 2. Trim: strip leading and trailing whitespace
     * 3. Empty check: if the trimmed string is empty, throw an exception
     * 4. Lowercase: normalize for hex prefix handling
     * 5. Format detection:
     *    - Hexadecimal: starts with "0x" or "-0x"
     *    - Floating point: contains a decimal point or scientific notation (e/E)
     *    - Integer: otherwise
     *
     * Supported formats:
     * 1. Hexadecimal:
     *    - Positive: "0xFF" -> 255
     *    - Negative: "-0x1A" -> -26
     *    - Parsed with BigInteger, supports arbitrary precision
     * 2. Floating point:
     *    - Normal decimal: "3.14" -> BigDecimal
     *    - Scientific notation: "1.23e10" -> BigDecimal
     *    - Parsed with BigDecimal to preserve precision
     * 3. Integer:
     *    - Normal integer: "123" -> BigInteger
     *    - Leading-zero values are no longer treated as octal; they are parsed as decimal
     *    - Parsed with BigInteger, supports arbitrary precision
     *
     * Precision guarantees:
     * - Integers use BigInteger and support integers of arbitrary length
     * - Floating-point numbers use BigDecimal to preserve precision
     * - Avoids precision issues caused by primitive types
     *
     * Exception handling:
     * - Empty string: throws NumberFormatException
     * - Format error: throws NumberFormatException with detailed error message
     * - All exceptions include the original string for easier debugging
     *
     * Notes:
     * - The hex prefix is case-insensitive (both 0x and 0X work)
     * - Scientific notation is case-insensitive (both e and E work)
     * - Numbers with a leading zero are no longer recognized as octal; they are parsed as decimal
     * - Returns BigInteger or BigDecimal; convert as needed
     *
     * @param str the string to convert, may be null
     * @return a Number object (BigInteger or BigDecimal); returns null if the input is null
     * @throws NumberFormatException if the string is malformed or empty
     */
    fun createNumber(str: String?): Number? {
        if (str == null) return null
        // Check for hexadecimal first (may carry a sign)
        val s = str.trim()
        if (s.isEmpty()) throw NumberFormatException("Empty string cannot be converted to a number")
        val lower = s.lowercase()
        return when {
            lower.startsWith("0x") -> {
                // For example, "0xFF" -> 255
                BigInteger(lower.substring(2), 16)
            }

            lower.startsWith("-0x") -> {
                // For example, "-0x1A" -> -26
                BigInteger(lower.substring(3), 16).negate()
            }
            // Check for a decimal point or scientific notation
            s.contains('.') || lower.contains('e') -> {
                // Parse directly with BigDecimal
                try {
                    BigDecimal(s)
                } catch (ex: NumberFormatException) {
                    throw NumberFormatException("Cannot convert \"$s\" to BigDecimal: ${ex.message}")
                }
            }

            else -> {
                // Plain integer, try parsing with BigInteger
                try {
                    // Support leading 0 as octal? Java no longer recognizes octal by default; parse as decimal with BigInteger
                    BigInteger(s)
                } catch (ex: NumberFormatException) {
                    throw NumberFormatException("Cannot convert \"$s\" to an integer: ${ex.message}")
                }
            }
        }
    }


    /**
     * Check whether the specified string contains only digit characters.
     * Returns `false` for `null` or an empty string.
     *
     * @param str the string to check
     * @return `true` if the specified string contains only Unicode digit characters
     * @author K
     * @since 1.0.0
     */
    fun isDigits(str: String?): Boolean {
        if (str.isNullOrEmpty()) return false
        // Treat as a number only if every character is in 0..9
        return str.all { it in '0'..'9' }
    }

    /**
     * Check whether the specified string represents a valid Java numeric value.
     * Valid numbers include hexadecimal values prefixed with `0x`, scientific notation,
     * and values ending with a type qualifier (e.g., 123L).
     * Returns `false` for `null` or an empty string.
     *
     * @param str the string to check
     * @return `true` if the specified string is a properly formatted number
     * @author K
     * @since 1.0.0
     */
    fun isNumber(str: String?): Boolean {
        if (str.isNullOrEmpty()) return false
        val s = str.trim()
        if (s.isEmpty()) return false

        // Check hexadecimal
        val lower = s.lowercase()
        if (lower.startsWith("0x") || lower.startsWith("-0x")) {
            // At least one hex character must follow the prefix
            val hexPart = if (lower.startsWith("0x")) lower.substring(2) else lower.substring(3)
            if (hexPart.isEmpty()) return false
            return hexPart.all { it in '0'..'9' || it in 'a'..'f' }
        }

        // For decimal point or scientific notation, try to parse with BigDecimal
        return try {
            BigDecimal(s)
            true
        } catch (_: NumberFormatException) {
            false
        }
    }

}

