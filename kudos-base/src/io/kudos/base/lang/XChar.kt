package io.kudos.base.lang

/**
 * kotlin.Char扩展函数
 *
 * @author K
 * @since 1.0.0
 */



/**
 * 将char转化为其Unicode编码的字符串
 *
 * <pre>
 * ' '.unicodeEscaped() = "\u0020"
 * 'A'.unicodeEscaped() = "\u0041"
 * </pre>
 *
 * @return 字符对应Unicode编码的字符串
 * @author K
 * @since 1.0.0
 */
fun Char.unicodeEscaped(): String = String.format("\\u%04x", code)


/**
 * 检测给定的char是否为7位的ASCII码
 *
 * <pre>
 * 'a'.isAscii()  = true
 * 'A'.isAscii()  = true
 * '3'.isAscii()  = true
 * '-'.isAscii()  = true
 * '\n'.isAscii() = true
 * </pre>
 *
 * @return true: 如果ASCII码值小于128
 * @author K
 * @since 1.0.0
 */
fun Char.isAscii(): Boolean = code < 128

/**
 * 检测给定的char是否为7位可打印的ASCII码
 *
 * <pre>
 * 'a'.isAsciiPrintable()  = true
 * 'A'.isAsciiPrintable()  = true
 * '3'.isAsciiPrintable()  = true
 * '-'.isAsciiPrintable()  = true
 * '\n'.isAsciiPrintable() = false
 * </pre>
 *
 * @return true: 如果ASCII码值介于32和126之间
 * @author K
 * @since 1.0.0
 */
fun Char.isAsciiPrintable(): Boolean = code in 32..126

/**
 * 检测给定的char是否为7位ASCII码的控制字符
 *
 * <pre>
 * 'a'.isAsciiControl()  = false
 * 'A'.isAsciiControl()  = false
 * '3'.isAsciiControl()  = false
 * '-'.isAsciiControl()  = false
 * '\n'.isAsciiControl() = true
 * </pre>
 *
 * @return true: 如果ASCII码值介于32和127之间
 * @author K
 * @since 1.0.0
 */
fun Char.isAsciiControl(): Boolean = code < 32 || code == 127

/**
 * 检测给定的char是否为7位ASCII码的字母
 *
 * <pre>
 * 'a'.isAsciiAlpha()  = true
 * 'A'.isAsciiAlpha()  = true
 * '3'.isAsciiAlpha()  = false
 * '-'.isAsciiAlpha()  = false
 * '\n'.isAsciiAlpha() = false
 * </pre>
 *
 * @return true: 如果ASCII码值介于65和90之间(大写字母)或97和122之间(小写字母)
 * @author K
 * @since 1.0.0
 */
fun Char.isAsciiAlpha(): Boolean = code in 65..90 || code in 97..122

/**
 * 检测给定的char是否为7位ASCII码的大写字母
 *
 * <pre>
 * 'a'.isAsciiAlphaUpper()  = false
 * 'A'.isAsciiAlphaUpper()  = true
 * '3'.isAsciiAlphaUpper()  = false
 * '-'.isAsciiAlphaUpper()  = false
 * '\n'.isAsciiAlphaUpper() = false
 * </pre>
 *
 * @return true: 如果ASCII码值介于65和90
 * @author K
 * @since 1.0.0
 */
fun Char.isAsciiAlphaUpper(): Boolean = code in 65..90

/**
 * 检测给定的char是否为7位ASCII码的大写字母
 *
 * <pre>
 * 'a'.isAsciiAlphaLower()  = true
 * 'A'.isAsciiAlphaLower()  = false
 * '3'.isAsciiAlphaLower()  = false
 * '-'.isAsciiAlphaLower()  = false
 * '\n'.isAsciiAlphaLower() = false
 * </pre>
 *
 * @return true: 如果ASCII码值介于97和122之间
 * @author K
 * @since 1.0.0
 */
fun Char.isAsciiAlphaLower(): Boolean = code in 97..122

/**
 * 检测给定的char是否为7位ASCII码的数字
 *
 * <pre>
 * 'a'.isAsciiNumeric()  = false
 * 'A'.isAsciiNumeric()  = false
 * '3'.isAsciiNumeric()  = true
 * '-'.isAsciiNumeric()  = false
 * '\n'.isAsciiNumeric() = false
 * </pre>
 *
 * @return true: 如果ASCII码值介于48和57之间
 * @author K
 * @since 1.0.0
 */
fun Char.isAsciiNumeric(): Boolean = code in 48..57

/**
 * 检测给定的char是否为7位ASCII码的字母或数字
 *
 * <pre>
 * 'a'.isAsciiAlphanumeric()  = true
 * 'A'.isAsciiAlphanumeric()  = true
 * '3'.isAsciiAlphanumeric()  = true
 * '-'.isAsciiAlphanumeric()  = false
 * '\n'.isAsciiAlphanumeric() = false
 * </pre>
 *
 * @return true: 如果ASCII码值介于48和57之间(数字)或65和90之间(大写字母)或97和122之间(小写字母)
 * @author K
 * @since 1.0.0
 */
fun Char.isAsciiAlphanumeric(): Boolean = isAsciiAlpha() || isAsciiNumeric()