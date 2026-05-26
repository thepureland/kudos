package io.kudos.base.lang

/**
 * kotlin.Char extension functions.
 *
 * @author K
 * @since 1.0.0
 */



/**
 * Convert a char to its Unicode-encoded string form.
 *
 * <pre>
 * ' '.unicodeEscaped() = " "
 * 'A'.unicodeEscaped() = "A"
 * </pre>
 *
 * @return the Unicode-encoded string of the character
 * @author K
 * @since 1.0.0
 */
fun Char.unicodeEscaped(): String = String.format("\\u%04x", code)


/**
 * Check whether the given char is a 7-bit ASCII code.
 *
 * <pre>
 * 'a'.isAscii()  = true
 * 'A'.isAscii()  = true
 * '3'.isAscii()  = true
 * '-'.isAscii()  = true
 * '\n'.isAscii() = true
 * </pre>
 *
 * @return true if the ASCII code value is less than 128
 * @author K
 * @since 1.0.0
 */
fun Char.isAscii(): Boolean = code < 128

/**
 * Check whether the given char is a 7-bit printable ASCII code.
 *
 * <pre>
 * 'a'.isAsciiPrintable()  = true
 * 'A'.isAsciiPrintable()  = true
 * '3'.isAsciiPrintable()  = true
 * '-'.isAsciiPrintable()  = true
 * '\n'.isAsciiPrintable() = false
 * </pre>
 *
 * @return true if the ASCII code value is between 32 and 126
 * @author K
 * @since 1.0.0
 */
fun Char.isAsciiPrintable(): Boolean = code in 32..126

/**
 * Check whether the given char is a 7-bit ASCII control character.
 *
 * <pre>
 * 'a'.isAsciiControl()  = false
 * 'A'.isAsciiControl()  = false
 * '3'.isAsciiControl()  = false
 * '-'.isAsciiControl()  = false
 * '\n'.isAsciiControl() = true
 * </pre>
 *
 * @return true if the ASCII code value is between 32 and 127
 * @author K
 * @since 1.0.0
 */
fun Char.isAsciiControl(): Boolean = code < 32 || code == 127

/**
 * Check whether the given char is a 7-bit ASCII letter.
 *
 * <pre>
 * 'a'.isAsciiAlpha()  = true
 * 'A'.isAsciiAlpha()  = true
 * '3'.isAsciiAlpha()  = false
 * '-'.isAsciiAlpha()  = false
 * '\n'.isAsciiAlpha() = false
 * </pre>
 *
 * @return true if the ASCII code value is between 65 and 90 (uppercase letters) or 97 and 122 (lowercase letters)
 * @author K
 * @since 1.0.0
 */
fun Char.isAsciiAlpha(): Boolean = code in 65..90 || code in 97..122

/**
 * Check whether the given char is a 7-bit ASCII uppercase letter.
 *
 * <pre>
 * 'a'.isAsciiAlphaUpper()  = false
 * 'A'.isAsciiAlphaUpper()  = true
 * '3'.isAsciiAlphaUpper()  = false
 * '-'.isAsciiAlphaUpper()  = false
 * '\n'.isAsciiAlphaUpper() = false
 * </pre>
 *
 * @return true if the ASCII code value is between 65 and 90
 * @author K
 * @since 1.0.0
 */
fun Char.isAsciiAlphaUpper(): Boolean = code in 65..90

/**
 * Check whether the given char is a 7-bit ASCII lowercase letter.
 *
 * <pre>
 * 'a'.isAsciiAlphaLower()  = true
 * 'A'.isAsciiAlphaLower()  = false
 * '3'.isAsciiAlphaLower()  = false
 * '-'.isAsciiAlphaLower()  = false
 * '\n'.isAsciiAlphaLower() = false
 * </pre>
 *
 * @return true if the ASCII code value is between 97 and 122
 * @author K
 * @since 1.0.0
 */
fun Char.isAsciiAlphaLower(): Boolean = code in 97..122

/**
 * Check whether the given char is a 7-bit ASCII digit.
 *
 * <pre>
 * 'a'.isAsciiNumeric()  = false
 * 'A'.isAsciiNumeric()  = false
 * '3'.isAsciiNumeric()  = true
 * '-'.isAsciiNumeric()  = false
 * '\n'.isAsciiNumeric() = false
 * </pre>
 *
 * @return true if the ASCII code value is between 48 and 57
 * @author K
 * @since 1.0.0
 */
fun Char.isAsciiNumeric(): Boolean = code in 48..57

/**
 * Check whether the given char is a 7-bit ASCII alphanumeric character.
 *
 * <pre>
 * 'a'.isAsciiAlphanumeric()  = true
 * 'A'.isAsciiAlphanumeric()  = true
 * '3'.isAsciiAlphanumeric()  = true
 * '-'.isAsciiAlphanumeric()  = false
 * '\n'.isAsciiAlphanumeric() = false
 * </pre>
 *
 * @return true if the ASCII code value is between 48 and 57 (digits) or 65 and 90 (uppercase letters) or 97 and 122 (lowercase letters)
 * @author K
 * @since 1.0.0
 */
fun Char.isAsciiAlphanumeric(): Boolean = isAsciiAlpha() || isAsciiNumeric()
