package io.kudos.base.lang.string

import io.kudos.base.security.CryptoKit
import io.kudos.base.security.DigestKit
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Strings
import java.math.BigDecimal
import java.math.BigInteger
import java.util.regex.Matcher
import kotlin.math.ceil
import kotlin.reflect.KClass


/**
 * kotlin.String extension functions
 *
 * @author K
 * @since 1.0.0
 */


/**
 * Capitalizes the first character.
 *
 * @return the string with its first character capitalized
 * @author K
 * @since 1.0.0
 */
fun CharSequence.capitalizeString(): String =
    this.toString().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }


/**
 * Converts a character sequence to the specified type.
 *
 * Supports converting strings to common primitive and numeric types.
 *
 * Supported types:
 * - Numeric types: Double, Int, Long, Float, Short, BigDecimal, BigInteger, Byte
 * - Boolean
 * - Char (takes the first character)
 * - String (returned as-is)
 *
 * Conversion rules:
 * 1. Numeric types: use the Kotlin standard library conversion methods (toDouble, toInt, etc.)
 * 2. BigDecimal: use toBigDecimal() for arbitrary precision.
 * 3. BigInteger: use toBigInteger() for arbitrary-length integers.
 * 4. Boolean: use toBoolean(); supports "true"/"false" strings.
 * 5. Char: takes the first character of the string.
 * 6. String: returned as-is.
 *
 * Type conversion:
 * - Uses Kotlin standard library conversion methods following standard conversion rules.
 * - Numeric conversion failure throws NumberFormatException.
 * - Boolean conversion follows the Kotlin standard ("true" -> true, otherwise false).
 *
 * Exception handling:
 * - Unsupported type: throws IllegalArgumentException.
 * - Numeric conversion failure: throws NumberFormatException (thrown by stdlib methods).
 * - Empty string to Char: throws NoSuchElementException (thrown by first()).
 *
 * Use cases:
 * - Type conversion during Excel import.
 * - Configuration file parsing.
 * - Dynamic type conversion.
 *
 * Notes:
 * - Only primitive and numeric types are supported; custom types are not.
 * - Numeric conversion follows the Kotlin standard and may throw exceptions.
 * - The Char type only takes the first character; an empty string causes an exception.
 *
 * @param T target type
 * @param returnType KClass of the target type
 * @return the converted value of the target type
 * @throws IllegalArgumentException if the target type is not supported
 * @throws NumberFormatException if numeric conversion fails
 * @throws NoSuchElementException if the string is empty and the target type is Char
 */
fun <T : Any> CharSequence.toType(returnType: KClass<out T>): T {
    val converted: Any = this.toString().run {
        when (returnType) {
            Double::class -> toDouble()
            Int::class -> toInt()
            Long::class -> toLong()
            Float::class -> toFloat()
            Short::class -> toShort()
            BigDecimal::class -> toBigDecimal()
            BigInteger::class -> toBigInteger()
            Boolean::class -> toBoolean()
            Byte::class -> toByte()
            Char::class -> toCharArray().first()
            String::class -> this
            else -> error("Unsupported type [$returnType]!")
        }
    }
    return returnType.javaObjectType.cast(converted)
}


/**
 * Finds substrings and replaces them with the specified strings (supports multiple replacement rules).
 *
 * Replaces every occurrence of each key in the map with its corresponding value.
 *
 * Workflow:
 * 1. Check the map: if it is empty, return the original string.
 * 2. Convert to arrays: convert the map's keys and values to arrays.
 * 3. Delegate to the overload: invoke the array-based version to perform the actual replacement.
 *
 * Replacement rules:
 * - Replaces all occurrences (not just the first).
 * - Multiple different substrings can be replaced simultaneously.
 * - Replacement order follows the map's iteration order.
 *
 * Notes:
 * - If the map is empty, returns the original string.
 * - Replacements are applied sequentially; later replacements may affect earlier results.
 * - Keys and values may be null, but null keys are not replaced.
 *
 * @param map replacement-rule map; key is the string to look for, value is the replacement
 * @return the resulting string
 */
fun CharSequence.replaceEach(map: Map<String?, String?>): String =
    if (map.isEmpty()) toString()
    else replaceEach(map.keys.toTypedArray(), map.values.toTypedArray())

/**
 * Converts the string to its hexadecimal representation.
 *
 * @return the converted hex-encoded string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.toHexStr(): String = String(CryptoKit.encodeHex(this.toString().toByteArray()))

/**
 * Decodes a hex-encoded string.
 *
 * @return the decoded string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.decodeHexStr(): String = String(CryptoKit.decodeHex(this.toString().toByteArray()))

/**
 * Applies MD5 hashing to the string, then hex-encodes the result.
 *
 * @param saltStr salt
 * @return the encrypted string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.toMd5HexStr(saltStr: CharSequence): String = DigestKit.getMD5(this.toString(), saltStr.toString())

/**
 * Splits the string into the given number of groups as evenly as possible.
 *
 * Splits the string into the specified number of groups; the last group may have a different length.
 *
 * Workflow:
 * 1. Validate arguments: if groupLen <= 0 or the string is empty, return an empty array.
 * 2. Compute group length: round up total length divided by group count.
 * 3. Split the string:
 *    - iterate over each group index;
 *    - compute the start and end indices of each group;
 *    - the last group contains all remaining characters.
 * 4. Return the result: the split substrings are returned as an array.
 *
 * Splitting algorithm:
 * - Group length = ceil(total length / group count).
 * - Start index of group i = i * group length.
 * - End index of group i = start index + group length (last group = total length).
 *
 * Examples:
 * - "123456".divideAverage(3) = ["12", "34", "56"] (2 chars per group)
 * - "1234567".divideAverage(3) = ["123", "456", "7"] (first two groups have 3 chars, last has 1)
 * - "".divideAverage(3) = [] (empty string)
 *
 * Edge cases:
 * - groupLen <= 0: returns an empty array.
 * - empty string: returns an empty array.
 * - groupLen > string length: each group has 1 char, and the last may be empty.
 *
 * Notes:
 * - The length of the last group may differ from the rest.
 * - Rounding up ensures all characters are assigned.
 * - When group count exceeds string length, some groups may be empty.
 *
 * @param groupLen number of groups to split into
 * @return array of split strings, or an empty array if arguments are invalid or the string is empty
 */
fun CharSequence.divideAverage(groupLen: Int): Array<String?> {
    if (groupLen <= 0 || this.isEmpty()) {
        return arrayOf()
    }
    val strLen = this.length
    val eachCount = ceil(strLen.toDouble() / groupLen).toInt()
    val groups = mutableListOf<String>()
    for (i in 0 until groupLen) {
        val beginIndex = i * eachCount
        val endIndex = if (i == groupLen - 1) { // last group
            strLen
        } else {
            beginIndex + eachCount
        }
        groups.add(this.substring(beginIndex, endIndex))
    }
    return groups.toTypedArray()
}

/**
 * Converts a "camelCase" string to an "_"-delimited string.
 *
 * <pre>
 * "".humpToUnderscore() = ""
 * " ".humpToUnderscore(false) = " "
 * "humpToUnderscore".humpToUnderscore() = "HUMP_TO_UNDERSCORE"
 * </pre>
 *
 * @param upperCase whether to convert to uppercase; true for upper, false for lower
 * @return the "_"-delimited string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.humpToUnderscore(upperCase: Boolean = true): String {
    if (isEmpty()) return ""
    val result = buildString {
        append(this@humpToUnderscore[0])
        (1 until this@humpToUnderscore.length).forEach { i ->
            if (Character.isUpperCase(this@humpToUnderscore[i])) append('_')
            append(this@humpToUnderscore[i])
        }
    }
    return if (upperCase) result.uppercase() else result.lowercase()
}

/**
 * Converts an "_"-delimited string to a "camelCase" string, e.g. HUMP_TO_Underscore -> humpToUnderscore.
 *
 * <pre>
 * "".underscoreToHump() = ""
 * " ".underscoreToHump() = " "
 * "HUMP_TO_Underscore".underscoreToHump() = "humpToUnderscore"
 * </pre>
 *
 * @return the "camelCase" string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.underscoreToHump(): String {
    if (isBlank()) return toString()
    val merged = split("_").joinToString(separator = "") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    return merged.first().lowercaseChar() + merged.substring(1)
}

/**
 * Replaces parameters in a template (enclosed in "${" and "}").
 *
 * @param paramMap parameter map
 * @return the resulting string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.fillTemplateByObjectMap(paramMap: Map<String, Any>): CharSequence =
    paramMap.entries.fold(toString()) { acc, (paramName, value) ->
        acc.replace(Regex("\\$\\{$paramName\\}"), Matcher.quoteReplacement(value.toString()))
    }

/**
 * Appends the given suffix if it is missing.
 *
 * @param suffix suffix
 * @param ignoreCase ignore case; default false
 * @return the concatenated string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.appendIfMissing(suffix: String, ignoreCase: Boolean = false): String =
    if (endsWith(suffix, ignoreCase)) toString() else "$this$suffix"


// vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
// Wrappers around org.apache.commons.lang3.StringUtils
// vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

/**
 * Tests whether the given string contains any whitespace characters.
 *
 * @return true if the string is non-empty and contains at least 1 whitespace character; false otherwise
 * @author K
 * @since 1.0.0
 */
fun CharSequence.containsWhitespace(): Boolean = StringUtils.containsWhitespace(this)


//region ContainsAny
/**
 * Tests whether this character sequence contains any of the characters in the given group searchChars.
 *
 * <pre>
 * "".containsAny(*)                  = false
 * *.containsAny([])                  = false
 * "zzabyycdxx".containsAny(['z','a']) = true
 * "zzabyycdxx".containsAny(['b','y']) = true
 * "aba".containsAny(['z'])           = false
 * </pre>
 *
 * @param searchChars character group to search for
 * @return true if any of the given characters is found; false otherwise
 * @author K
 * @since 1.0.0
 */
fun CharSequence.containsAny(vararg searchChars: Char): Boolean = StringUtils.containsAny(this, *searchChars)

/**
 * Tests whether this character sequence contains any of the characters in the given group searchChars.
 *
 * <pre>
 * "".containsAny(*)              = false
 * *.containsAny("")              = false
 * "zzabyycdxx".containsAny("za") = true
 * "zzabyycdxx".containsAny("by") = true
 * "aba".containsAny("z")          = false
 * </pre>
 *
 * @param searchChars character group to search for; may be null
 * @return true if any of the given characters is found; false otherwise
 * @author K
 * @since 1.0.0
 */
fun CharSequence.containsAny(searchChars: CharSequence?): Boolean = StringUtils.containsAny(this, searchChars)
//endregion ContainsAny

//region IndexOfAnyBut chars
/**
 * Searches the character sequence for any of the characters in searchChars and returns the index of the first character not in the group.
 *
 * <pre>
 * "".indexOfAnyBut(*)                    = -1
 * *.indexOfAnyBut([])                    = -1
 * "zzabyycdxx".indexOfAnyBut(['z', 'a']) = 3
 * "aba".indexOfAnyBut(['z'])             = 0
 * "aba".indexOfAnyBut(['a', 'b'])        = -1
 * </pre>
 *
 * @param searchChars character group to search for
 * @return the index of the first character that does not match; -1 if none is found
 * @author K
 * @since 1.0.0
 */
fun CharSequence.indexOfAnyBut(vararg searchChars: Char): Int = StringUtils.indexOfAnyBut(this, *searchChars)

/**
 * Searches the character sequence for any of the characters in searchChars and returns the index of the first character not in the group.
 *
 * <pre>
 * "".indexOfAnyBut(*)              = -1
 * *.indexOfAnyBut("")              = -1
 * "zzabyycdxx".indexOfAnyBut("za") = 3
 * "zzabyycdxx".indexOfAnyBut("")   = -1
 * "aba".indexOfAnyBut("ab")        = -1
 * </pre>
 *
 * @param searchChars character group to search for
 * @return the index of the first character that does not match; -1 if none is found
 * @author K
 * @since 1.0.0
 */
fun CharSequence.indexOfAnyBut(searchChars: CharSequence): Int = StringUtils.indexOfAnyBut(this, searchChars)
//endregion IndexOfAnyBut chars

//region ContainsOnly
/**
 * Tests whether the character sequence consists solely of characters in valid.
 *
 * <pre>
 * "".containsOnly(*)         = true
 * "ab".containsOnly('')      = false
 * "abab".containsOnly('abc') = true
 * "ab1".containsOnly('abc')  = false
 * "abz".containsOnly('abc')  = false
 * </pre>
 *
 * @param valid the valid character group
 * @return true if the sequence consists only of characters from valid (or is empty); false otherwise
 * @author K
 * @since 1.0.0
 */
fun CharSequence.containsOnly(vararg valid: Char): Boolean = StringUtils.containsOnly(this, *valid)

/**
 * Tests whether the character sequence consists solely of characters in validChars.
 *
 * <pre>
 * "".containsOnly(*)         = true
 * "ab".containsOnly("")      = false
 * "abab".containsOnly("abc") = true
 * "ab1".containsOnly("abc")  = false
 * "abz".containsOnly("abc")  = false
 * </pre>
 *
 * @param validChars the valid character group
 * @return true if the sequence consists only of characters from validChars (or is empty); false otherwise
 * @author K
 * @since 1.0.0
 */
fun CharSequence.containsOnly(validChars: String?): Boolean = StringUtils.containsOnly(this, validChars)
//endregion ContainsOnly

//region ContainsNone
/**
 * Tests whether the character sequence contains none of the characters in searchChars.
 *
 * <pre>
 * "".containsNone(*)         = true
 * "ab".containsNone('')      = true
 * "abab".containsNone('xyz') = true
 * "ab1".containsNone('xyz')  = true
 * "abz".containsNone('xyz')  = false
 * </pre>
 *
 * @param searchChars the invalid character group
 * @return true if no character from searchChars is contained (or the sequence is empty); false if any character is contained
 * @author K
 * @since 1.0.0
 */
fun CharSequence.containsNone(vararg searchChars: Char): Boolean = StringUtils.containsNone(this, *searchChars)

/**
 * Tests whether the character sequence contains none of the characters in the invalidChars string.
 *
 * <pre>
 * "".containsNone(*)         = true
 * "ab".containsNone("")      = true
 * "abab".containsNone("xyz") = true
 * "ab1".containsNone("xyz")  = true
 * "abz".containsNone("xyz")  = false
 * </pre>
 *
 * @param invalidChars the invalid character group
 * @return true if no character from invalidChars is contained (or the sequence is empty); false if any character is contained
 * @author K
 * @since 1.0.0
 */
fun CharSequence.containsNone(invalidChars: String): Boolean = StringUtils.containsNone(this, invalidChars)
//endregion ContainsNone


//region Left/Right/Mid
/**
 * Returns the leftmost len characters of the string.
 *
 * <pre>
 * *.left(-ve)     = ""
 * "".left(*)      = ""
 * "abc".left(0)   = ""
 * "abc".left(2)   = "ab"
 * "abc".left(4)   = "abc"
 * </pre>
 *
 * @param len length of the substring
 * @return the leftmost substring; returns the empty string if the source is empty or len is negative
 * @author K
 * @since 1.0.0
 */
fun CharSequence.left(len: Int): String? = StringUtils.leftPad(this.toString(), len)

/**
 * Returns the rightmost len characters of the string.
 *
 * <pre>
 * *.right(-ve)     = ""
 * "".right(*)      = ""
 * "abc".right(0)   = ""
 * "abc".right(2)   = "bc"
 * "abc".right(4)   = "abc"
 * </pre>
 *
 * @param len length of the substring
 * @return the rightmost substring; returns the empty string if the source is empty or len is negative
 * @author K
 * @since 1.0.0
 */
fun CharSequence.right(len: Int): String? = StringUtils.right(this.toString(), len)

/**
 * Returns len characters of the string starting from position pos.
 *
 * <pre>
 * *.mid(*, -ve)     = ""
 * "".mid(0, *)      = ""
 * "abc".mid(0, 2)   = "ab"
 * "abc".mid(0, 4)   = "abc"
 * "abc".mid(2, 4)   = "c"
 * "abc".mid(4, 2)   = ""
 * "abc".mid(-2, 2)  = "ab"
 * </pre>
 *
 * @param pos starting position; negatives are treated as 0
 * @param len length of the substring
 * @return len characters starting at pos; returns the empty string if the source is empty or len is negative
 * @author K
 * @since 1.0.0
 */
fun CharSequence.mid(pos: Int, len: Int): String? = StringUtils.mid(this.toString(), pos, len)
//endregion Left/Right/Mid

//region Substring between
/**
 * Returns the substring nested between two identical tag strings.
 *
 * <pre>
 * "".substringBetween("")             = ""
 * "".substringBetween("tag")          = ""
 * "tagabctag".substringBetween("")    = ""
 * "tagabctag".substringBetween("tag") = "abc"
 * </pre>
 *
 * @param tag the string surrounding the substring
 * @return the substring; returns empty string if not found or if tag is empty
 * @author K
 * @since 1.0.0
 */
fun CharSequence.substringBetween(tag: String): String = StringUtils.substringBetween(this.toString(), tag) ?: ""

/**
 * Returns the substring nested between the strings open and close; returns the first match.
 *
 * <pre>
 * "wx[b]yz".substringBetween("[", "]") = "b"
 * "".substringBetween("", "")          = ""
 * "".substringBetween("", "]")         = ""
 * "".substringBetween("[", "]")        = ""
 * "yabcz".substringBetween("", "")     = ""
 * "yabcz".substringBetween("y", "z")   = "abc"
 * "yabczyabcz".substringBetween("y", "z")   = "abc"
 * </pre>
 *
 * @param open  string before the substring
 * @param close string after the substring
 * @return the substring; returns empty string if not found or if open/close is empty
 * @author K
 * @since 1.0.0
 */
fun CharSequence.substringBetween(open: String, close: CharSequence): String =
    StringUtils.substringBetween(this.toString(), open, close.toString()) ?: ""

/**
 * Returns all substrings nested between the strings open and close.
 *
 * <pre>
 * "[a][b][c]".substringsBetween("[", "]") = ["a","b","c"]
 * "".substringsBetween("[", "]")          = []
 * </pre>
 *
 * @param open  string marking the start of the substring
 * @param close string marking the end of the substring
 * @return array of substrings; empty array if none is found
 * @author K
 * @since 1.0.0
 */
fun CharSequence.substringsBetween(open: String, close: CharSequence): Array<String> =
    StringUtils.substringsBetween(this.toString(), open, close.toString())
//endregion Substring between

//region Splitting

/**
 * Splits the string based on character type (as returned by `java.lang.Character.getType(char)`). Multiple consecutive characters of the same type are returned together as one group.
 *
 * <pre>
 * "".splitByCharacterType()           = []
 * "ab de fg".splitByCharacterType()   = ["ab", " ", "de", " ", "fg"]
 * "ab   de fg".splitByCharacterType() = ["ab", "   ", "de", " ", "fg"]
 * "ab:cd:ef".splitByCharacterType()   = ["ab", ":", "cd", ":", "ef"]
 * "number5".splitByCharacterType()    = ["number", "5"]
 * "fooBar".splitByCharacterType()     = ["foo", "B", "ar"]
 * "foo200Bar".splitByCharacterType()  = ["foo", "200", "B", "ar"]
 * "ASFRules".splitByCharacterType()   = ["ASFR", "ules"]
 * </pre>
 *
 * @return array of substrings
 * @author K
 * @since 1.0.0
 */
fun CharSequence.splitByCharacterType(): Array<String> = StringUtils.splitByCharacterTypeCamelCase(this.toString())

/**
 * Splits the string based on character type (as returned by `java.lang.Character.getType(char)`).
 * Multiple consecutive characters of the same type are returned as one group. Exception: when an uppercase letter is immediately followed by a lowercase letter (camel case), the uppercase letter is grouped with the lowercase letters.
 *
 * <pre>
 * "".splitByCharacterTypeCamelCase()           = []
 * "ab de fg".splitByCharacterTypeCamelCase()   = ["ab", " ", "de", " ", "fg"]
 * "ab   de fg".splitByCharacterTypeCamelCase() = ["ab", "   ", "de", " ", "fg"]
 * "ab:cd:ef".splitByCharacterTypeCamelCase()   = ["ab", ":", "cd", ":", "ef"]
 * "number5".splitByCharacterTypeCamelCase()    = ["number", "5"]
 * "fooBar".splitByCharacterTypeCamelCase()     = ["foo", "Bar"]
 * "foo200Bar".splitByCharacterTypeCamelCase()  = ["foo", "200", "Bar"]
 * "ASFRules".splitByCharacterTypeCamelCase()   = ["ASF", "Rules"]
 * </pre>
 *
 * @return array of substrings
 * @author K
 * @since 1.0.0
 */
fun CharSequence.splitByCharacterTypeCamelCase(): Array<String>? = StringUtils.splitByCharacterType(this.toString())
//endregion Splitting

/**
 * Removes all whitespace characters.
 *
 * <pre>
 * "".deleteWhitespace()           = ""
 * "abc".deleteWhitespace()        = "abc"
 * "   ab  c  ".deleteWhitespace() = "abc"
 * </pre>
 *
 * @return the string with no whitespace characters
 * @author K
 * @since 1.0.0
 */
fun CharSequence.deleteWhitespace(): String = StringUtils.deleteWhitespace(this.toString())

//region Remove

/**
 * Removes the substring from the beginning of the main string (case-insensitive); otherwise returns the original string.
 *
 * <pre>
 * "".removeStartIgnoreCase(*)        = ""
 * *.removeStartIgnoreCase(null)      = *
 * "www.domain.com".removeStartIgnoreCase("www.")   = "domain.com"
 * "www.domain.com".removeStartIgnoreCase("WWW.")   = "domain.com"
 * "domain.com".removeStartIgnoreCase("www.")       = "domain.com"
 * "www.domain.com".removeStartIgnoreCase("domain") = "www.domain.com"
 * "abc".removeStartIgnoreCase("")    = "abc"
 * </pre>
 *
 * @param remove the substring to remove; may be null; returns the source string if null or empty
 * @return the string after removing the leading substring
 * @author K
 * @since 1.0.0
 */
fun CharSequence.removePrefixIgnoreCase(remove: CharSequence?): String? =
    Strings.CS.removeStart(this.toString(), remove?.toString() ?: "")

/**
 * Removes the substring from the end of the main string (case-insensitive); otherwise returns the original string.
 *
 * <pre>
 * "".removeEndIgnoreCase(*)        = ""
 * *.removeEndIgnoreCase(null)      = *
 * "www.domain.com".removeEndIgnoreCase(".com.")  = "www.domain.com"
 * "www.domain.com".removeEndIgnoreCase(".com")   = "www.domain"
 * "www.domain.com".removeEndIgnoreCase("domain") = "www.domain.com"
 * "abc".removeEndIgnoreCase("")    = "abc"
 * "www.domain.com".removeEndIgnoreCase(".COM") = "www.domain")
 * "www.domain.COM".removeEndIgnoreCase(".com") = "www.domain")
 * </pre>
 *
 * @param remove the substring to remove; may be null; returns the source string if null or empty
 * @return the string after removing the trailing substring
 * @author K
 * @since 1.0.0
 */
fun CharSequence.removeSuffixIgnoreCase(remove: CharSequence?): String? =
    Strings.CI.removeEnd(this.toString(), remove?.toString() ?: "")

//endregion Remove

//region Replacing

/**
 * Finds substrings and replaces them with the specified string, with a cap on replacement count.
 *
 * <pre>
 * "".replace(*, *, *)           = ""
 * "any".replace(null, *, *)     = "any"
 * "any".replace(*, null, *)     = "any"
 * "any".replace("", *, *)       = "any"
 * "any".replace(*, *, 0)        = "any"
 * "abaa".replace("a", null, -1) = "abaa"
 * "abaa".replace("a", "", -1)   = "b"
 * "abaa".replace("a", "z", 0)   = "abaa"
 * "abaa".replace("a", "z", 1)   = "zbaa"
 * "abaa".replace("a", "z", 2)   = "zbza"
 * "abaa".replace("a", "z", -1)  = "zbzz"
 * </pre>
 *
 * @param searchString string to search for; returns the source string if null or empty
 * @param replacement  replacement string; returns the source string if null
 * @param max          maximum number of replacements
 * @return the resulting string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.replace(searchString: CharSequence?, replacement: CharSequence?, max: Int): String =
    Strings.CS.replace(this.toString(), searchString?.toString() ?: "", replacement?.toString() ?: "", max)

/**
 * Finds substrings and replaces them with the specified strings (supports multiple replacement rules).
 *
 * Replaces every occurrence of each search string with the corresponding replacement string, based on the correspondence between the two arrays.
 *
 * Workflow:
 * 1. Argument handling: convert the CharSequence arrays to String arrays; null values become empty strings.
 * 2. Delegate to the utility: invoke Apache Commons Lang's replaceEach to perform the replacement.
 * 3. Return result: return the resulting string.
 *
 * Replacement rules:
 * - searchList[i] is replaced by replacementList[i].
 * - Replaces all occurrences (not just the first).
 * - Replacements are applied in array order.
 *
 * Array correspondence:
 * - Both arrays must have the same length (except for null or empty arrays).
 * - Null arrays are treated as empty.
 * - Null array elements are converted to empty strings.
 *
 * Edge cases:
 * - If both arrays are null or empty, returns the original string.
 * - If searchList is empty, returns the original string.
 * - If replacementList is null, returns the original string.
 *
 * Notes:
 * - Both arrays must have the same length; otherwise IllegalArgumentException is thrown.
 * - Replacements are applied sequentially; later replacements may affect earlier results.
 * - Uses Apache Commons Lang's implementation to ensure correctness.
 *
 * @param searchList array of strings to search for; may be null
 * @param replacementList array of replacement strings, parallel to searchList; may be null
 * @return the resulting string
 * @throws IllegalArgumentException if the two arrays have different lengths
 */
fun CharSequence.replaceEach(
    searchList: Array<out CharSequence?>?,
    replacementList: Array<out CharSequence?>?
): String {
    val sList = searchList?.map { it?.toString() ?: "" } ?: emptyList()
    val rList = replacementList?.map { it?.toString() ?: "" } ?: emptyList()
    return StringUtils.replaceEach(this.toString(), sList.toTypedArray(), rList.toTypedArray())
}


/**
 * Repeatedly finds substrings and replaces every occurrence with the corresponding string, supporting multiple replacement rules.
 *
 * <pre>
 * "".replaceEachRepeatedly(*, *) = ""
 * "aba".replaceEachRepeatedly(null, null) = "aba"
 * "aba".replaceEachRepeatedly(String[0], null) = "aba"
 * "aba".replaceEachRepeatedly(null, String[0]) = "aba"
 * "aba".replaceEachRepeatedly(String[]{"a"}, null, *) = "aba"
 * "aba".replaceEachRepeatedly(String[]{"a"}, String[]{""}) = "b"
 * "aba".replaceEachRepeatedly(String[]{null}, String[]{"a"}) = "aba"
 * "abcde".replaceEachRepeatedly(String[]{"ab", "d"}, String[]{"w", "t"}) = "wcte"
 * "abcde".replaceEachRepeatedly(String[]{"ab", "d"}, String[]{"d", "t"}) = "tcte"
 * "abcde".replaceEachRepeatedly(String[]{"ab", "d"}, String[]{"d", "ab"}) = IllegalStateException
 * </pre>
 *
 * @param searchList      array of strings to search for; returns the source string if null
 * @param replacementList array of replacement strings, parallel to searchList. Returns the source string if null
 * @return the resulting string
 * @throws IllegalStateException    if an infinite loop is detected
 * @throws IllegalArgumentException if the two arrays have different lengths (null or empty arrays are allowed)
 * @author K
 * @since 1.0.0
 */
fun CharSequence.replaceEachRepeatedly(
    searchList: Array<out CharSequence?>?,
    replacementList: Array<out CharSequence>?
): String {
    val sList = searchList?.map { it?.toString() ?: "" } ?: emptyList()
    val rList = replacementList?.map { it.toString() } ?: emptyList()
    return StringUtils.replaceEachRepeatedly(this.toString(), sList.toTypedArray(), rList.toTypedArray())
}

//endregion Replacing

/**
 * Removes line terminators ("\n", "\r", "\r\n") from the end of the string.
 *
 * <pre>
 * "".chomp()            = ""
 * "abc \r".chomp()      = "abc "
 * "abc\n".chomp()       = "abc"
 * "abc\r\n".chomp()     = "abc"
 * "abc\r\n\r\n".chomp() = "abc\r\n"
 * "abc\n\r".chomp()     = "abc\n"
 * "abc\n\rabc".chomp()  = "abc\n\rabc"
 * "\r".chomp()          = ""
 * "\n".chomp()          = ""
 * "\r\n".chomp()        = ""
 * </pre>
 *
 * @return the processed string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.chomp(): String = StringUtils.chomp(this.toString())

/**
 * Removes the last character of the string (if the string ends with "\r\n", both characters are removed).
 *
 * <pre>
 * "".chop()            = ""
 * "abc \r".chop()      = "abc "
 * "abc\n".chop()       = "abc"
 * "abc\r\n".chop()     = "abc"
 * "abc".chop()         = "ab"
 * "abc\nabc".chop()    = "abc\nab"
 * "a".chop()           = ""
 * "\r".chop()          = ""
 * "\n".chop()          = ""
 * "\r\n".chop()        = ""
 * </pre>
 *
 * @return the processed string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.chop(): String = StringUtils.chop(this.toString())

/**
 * Joins a string with itself the specified number of times, separated by the given separator.
 *
 * <pre>
 * "".repeat(null, 0)   = ""
 * "".repeat("", 2)     = ""
 * "".repeat("x", 3)    = "xxx"
 * "?".repeat(", ", 3)  = "?, ?, ?"
 * </pre>
 *
 * @param separator separator string; treated as empty if null
 * @param repeat    number of repetitions; negatives are treated as 0
 * @return the concatenated string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.repeatAndSeparate(separator: CharSequence?, repeat: Int): String? =
    StringUtils.repeat(this.toString(), separator?.toString(), repeat)

//region Centering

/**
 * Pads the source string on the left and right with the specified character to the given length.
 *
 * <pre>
 * "".center(4, ' ')     = "    "
 * "ab".center(-1, ' ')  = "ab"
 * "ab".center(4, ' ')   = " ab"
 * "abcd".center(2, ' ') = "abcd"
 * "a".center(4, ' ')    = " a  "
 * "a".center(4, 'y')    = "yayy"
 * </pre>
 *
 * @param size required length; negatives are treated as 0; less than the source length returns the source string
 * @param padChar character used for padding
 * @return the padded string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.center(size: Int, padChar: Char): String? = StringUtils.center(this.toString(), size, padChar)

/**
 * Pads the source string on the left and right with the specified string to the given length.
 *
 * <pre>
 * "".center(4, " ")     = "    "
 * "ab".center(-1, " ")  = "ab"
 * "ab".center(4, " ")   = " ab"
 * "abcd".center(2, " ") = "abcd"
 * "a".center(4, " ")    = " a  "
 * "a".center(4, "yz")   = "yayz"
 * "abc".center(7, null) = "  abc  "
 * "abc".center(7, "")   = "  abc  "
 * </pre>
 *
 * @param size   required length; negatives are treated as 0; less than the source length returns the source string
 * @param padStr string used for padding
 * @return the padded string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.center(size: Int, padStr: CharSequence?): String? =
    StringUtils.center(this.toString(), size, padStr?.toString() ?: "")

//endregion Centering

//region Case conversion

/**
 * Lowercases the first character of the source string.
 *
 * <pre>
 * "".uncapitalize()    = ""
 * "Cat".uncapitalize() = "cat"
 * "CAT".uncapitalize() = "cAT"
 * </pre>
 *
 * @return the string with its first character lowercased
 * @author K
 * @since 1.0.0
 */
fun CharSequence.uncapitalize(): String = StringUtils.uncapitalize(this.toString())

/**
 * Swaps the case of letters in the source string: uppercase to lowercase and vice versa.
 *
 * <pre>
 * "".swapCase()                   = ""
 * "The dog has a BONE".swapCase() = "tHE DOG HAS A bone"
 * </pre>
 *
 * @return the resulting string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.swapCase(): String = StringUtils.swapCase(this.toString())

//endregion Case conversion

/**
 * Counts the occurrences of a substring in the source string.
 *
 * <pre>
 * "".countMatches(*)         = 0
 * "abba".countMatches(null)  = 0
 * "abba".countMatches("")    = 0
 * "abba".countMatches("a")   = 2
 * "abba".countMatches("ab")  = 1
 * "abba".countMatches("xxx") = 0
 * </pre>
 *
 * @param sub the substring; may be null
 * @return the number of occurrences
 * @author K
 * @since 1.0.0
 */
fun CharSequence.countMatches(sub: CharSequence?): Int = StringUtils.countMatches(this, sub)

//region Character Tests
/**
 * Tests whether the character sequence contains only Unicode letters.
 *
 * <pre>
 * "".isAlpha()     = false
 * "  ".isAlpha()   = false
 * "abc".isAlpha()  = true
 * "ab2c".isAlpha() = false
 * "ab-c".isAlpha() = false
 * </pre>
 *
 * @return true if the sequence contains only Unicode letters
 * @author K
 * @since 1.0.0
 */
fun CharSequence.isAlpha(): Boolean = StringUtils.isAlpha(this)

/**
 * Tests whether the character sequence contains only Unicode letters or spaces.
 *
 * <pre>
 * "".isAlphaSpace()     = true
 * "  ".isAlphaSpace()   = true
 * "abc".isAlphaSpace()  = true
 * "ab c".isAlphaSpace() = true
 * "ab2c".isAlphaSpace() = false
 * "ab-c".isAlphaSpace() = false
 * </pre>
 *
 * @return true if the sequence contains only Unicode letters or spaces
 * @author K
 * @since 1.0.0
 */
fun CharSequence.isAlphaSpace(): Boolean = StringUtils.isAlphaSpace(this)

/**
 * Tests whether the character sequence contains only Unicode letters or digits.
 *
 * <pre>
 * "".isAlphanumeric()     = false
 * "  ".isAlphanumeric()   = false
 * "abc".isAlphanumeric()  = true
 * "ab c".isAlphanumeric() = false
 * "ab2c".isAlphanumeric() = true
 * "ab-c".isAlphanumeric() = false
 * </pre>
 *
 * @return true if the sequence contains only Unicode letters or digits
 * @author K
 * @since 1.0.0
 */
fun CharSequence.isAlphanumeric(): Boolean = StringUtils.isAlpha(this)

/**
 * Tests whether the character sequence contains only Unicode letters, spaces, or digits.
 *
 * <pre>
 * "".isAlphanumericSpace()     = true
 * "  ".isAlphanumericSpace()   = true
 * "abc".isAlphanumericSpace()  = true
 * "ab c".isAlphanumericSpace() = true
 * "ab2c".isAlphanumericSpace() = true
 * "ab-c".isAlphanumericSpace() = false
 * </pre>
 *
 * @return true if the sequence contains only Unicode letters, spaces, or digits
 * @author K
 * @since 1.0.0
 */
fun CharSequence.isAlphanumericSpace(): Boolean = StringUtils.isAlphanumeric(this)

/**
 * Tests whether the character sequence contains only printable ASCII characters.
 *
 * <pre>
 * "".isAsciiPrintable()       = true
 * " ".isAsciiPrintable()      = true
 * "Ceki".isAsciiPrintable()   = true
 * "ab2c".isAsciiPrintable()   = true
 * "!ab-c~".isAsciiPrintable() = true
 * " ".isAsciiPrintable() = true
 * "!".isAsciiPrintable() = true
 * "~".isAsciiPrintable() = true
 * "".isAsciiPrintable() = false
 * "Ceki Gülcü".isAsciiPrintable() = false
 * </pre>
 *
 * @return true if every character is in the range 32-126
 * @author K
 * @since 1.0.0
 */
fun CharSequence.isAsciiPrintable(): Boolean = StringUtils.isAsciiPrintable(this)

/**
 * Tests whether the character sequence contains only Unicode digits. Decimal fractions are not Unicode digits.
 *
 * <pre>
 * "".isNumeric()     = false
 * "  ".isNumeric()   = false
 * "123".isNumeric()  = true
 * "12 3".isNumeric() = false
 * "ab2c".isNumeric() = false
 * "12-3".isNumeric() = false
 * "12.3".isNumeric() = false
 * </pre>
 *
 * @return true if the sequence contains only Unicode digits
 * @author K
 * @since 1.0.0
 */
fun CharSequence.isNumeric(): Boolean = StringUtils.isNumeric(this)

/**
 * Tests whether the character sequence contains only Unicode digits or spaces. Decimal fractions are not Unicode digits.
 *
 * <pre>
 * "".isNumericSpace()     = true
 * "  ".isNumericSpace()   = true
 * "123".isNumericSpace()  = true
 * "12 3".isNumericSpace() = true
 * "ab2c".isNumericSpace() = false
 * "12-3".isNumericSpace() = false
 * "12.3".isNumericSpace() = false
 * </pre>
 *
 * @return true if the sequence contains only Unicode digits or spaces
 * @author K
 * @since 1.0.0
 */
fun CharSequence.isNumericSpace(): Boolean = StringUtils.isNumeric(this)

/**
 * Tests whether the character sequence contains only whitespace characters.
 *
 * <pre>
 * "".isWhitespace()     = true
 * "  ".isWhitespace()   = true
 * "abc".isWhitespace()  = false
 * "ab2c".isWhitespace() = false
 * "ab-c".isWhitespace() = false
 * </pre>
 *
 * @return true if the sequence contains only whitespace characters
 * @author K
 * @since 1.0.0
 */
fun CharSequence.isWhitespace(): Boolean = StringUtils.isWhitespace(this)

/**
 * Tests whether the character sequence contains only lowercase letters.
 *
 * <pre>
 * "".isAllLowerCase()     = false
 * "  ".isAllLowerCase()   = false
 * "abc".isAllLowerCase()  = true
 * "abC".isAllLowerCase() = false
 * </pre>
 *
 * @return true if the sequence contains only lowercase letters
 * @author K
 * @since 1.0.0
 */
fun CharSequence.isAllLowerCase(): Boolean = StringUtils.isAllLowerCase(this)

/**
 * Tests whether the character sequence contains only uppercase letters.
 *
 * <pre>
 * "".isAllUpperCase()     = false
 * "  ".isAllUpperCase()   = false
 * "ABC".isAllUpperCase()  = true
 * "aBC".isAllUpperCase() = false
 * </pre>
 *
 * @return true if the sequence contains only uppercase letters
 * @author K
 * @since 1.0.0
 */
fun CharSequence.isAllUpperCase(): Boolean = StringUtils.isAllUpperCase(this)

//endregion Character Tests

/**
 * Reverses the string by separators; substrings between separators are treated as wholes (not themselves reversed).
 *
 * <pre>
 * "".reverseDelimited(*)        = ""
 * "a.b.c".reverseDelimited('x') = "a.b.c"
 * "a.b.c".reverseDelimited(".") = "c.b.a"
 * "java.lang.String".reverseDelimited(".") = "String.lang.java"
 * </pre>
 *
 * @param separatorChar the separator
 * @return the reversed string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.reverseDelimited(separatorChar: Char): String? =
    StringUtils.reverseDelimited(this.toString(), separatorChar)

//region Abbreviating
/**
 * Abbreviates a string.
 * Rules:
 *  * If the length of `str` is less than `maxWidth`, returns `str`.
 *  * Otherwise abbreviates as: `(substring(str, 0, max-3) + "...")`.
 *  * If `maxWidth` is less than `4`, throws
 * `IllegalArgumentException`.
 *  * Never returns a string with length greater than `maxWidth`.
 *
 * <pre>
 * "".abbreviate(4)        = ""
 * "abcdefg".abbreviate(6) = "abc..."
 * "abcdefg".abbreviate(7) = "abcdefg"
 * "abcdefg".abbreviate(8) = "abcdefg"
 * "abcdefg".abbreviate(4) = "a..."
 * "abcdefg".abbreviate(3) = IllegalArgumentException
 * </pre>
 *
 * @param maxWidth maximum length of the returned string; must be >= 4
 * @return the abbreviated string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.abbreviate(maxWidth: Int): String? = StringUtils.abbreviate(this.toString(), maxWidth)

/**
 * Abbreviates a string similarly to `abbreviate(int)`, but with a configurable left-edge offset.
 * The left-edge offset does not necessarily require the leftmost character to appear at the leftmost position of the resulting string,
 * or right after the ellipsis; it must, however, appear somewhere in the resulting string.
 *
 * <pre>
 * "".abbreviate(0, 4)                  = ""
 * "abcdefghijklmno".abbreviate(-1, 10) = "abcdefg..."
 * "abcdefghijklmno".abbreviate(0, 10)  = "abcdefg..."
 * "abcdefghijklmno".abbreviate(1, 10)  = "abcdefg..."
 * "abcdefghijklmno".abbreviate(4, 10)  = "abcdefg..."
 * "abcdefghijklmno".abbreviate(5, 10)  = "...fghi..."
 * "abcdefghijklmno".abbreviate(6, 10)  = "...ghij..."
 * "abcdefghijklmno".abbreviate(8, 10)  = "...ijklmno"
 * "abcdefghijklmno".abbreviate(10, 10) = "...ijklmno"
 * "abcdefghijklmno".abbreviate(12, 10) = "...ijklmno"
 * "abcdefghij".abbreviate(0, 3)        = IllegalArgumentException
 * "abcdefghij".abbreviate(5, 6)        = IllegalArgumentException
 * </pre>
 *
 * @param offset   left-edge offset of the source string
 * @param maxWidth maximum length of the returned string; must be >= 4
 * @return the abbreviated string
 * @throws IllegalArgumentException if the resulting length is less than 4
 * @author K
 * @since 1.0.0
 */
fun CharSequence.abbreviate(offset: Int, maxWidth: Int): String? =
    StringUtils.abbreviate(this.toString(), offset, maxWidth)

/**
 * Replaces the middle characters of the source string with the specified string in order to abbreviate the source string to the specified length.
 * Abbreviation occurs only when all of the following conditions are satisfied:
 *  Neither the source string nor the replacement string is null or empty.
 *  The specified target length is less than the source string length.
 *  The specified target length is greater than 0.
 *  The abbreviated string has enough length to accommodate the replacement string plus the first and last characters.
 * Otherwise, the result is the source string unchanged.
 *
 * <pre>
 * "abc".abbreviateMiddle(null, 0)      = "abc"
 * "abc".abbreviateMiddle(".", 0)      = "abc"
 * "abc".abbreviateMiddle(".", 3)      = "abc"
 * "abcdef".abbreviateMiddle(".", 4)     = "ab.f"
 * </pre>
 *
 * @param middle string used to replace the middle characters; may be null, in which case no replacement occurs
 * @param length maximum length of the returned string
 * @return the abbreviated string
 * @author K
 * @since 1.0.0
 */
fun CharSequence.abbreviateMiddle(middle: CharSequence?, length: Int): String? =
    StringUtils.abbreviateMiddle(this.toString(), middle?.toString(), length)

//endregion Abbreviating

//region Difference
/**
 * Compares two strings and returns the part where they differ (more precisely, the remainder of the second string starting from the point where it begins to differ from the first).
 *
 * <pre>
 * "".difference("") = ""
 * "".difference("abc") = "abc"
 * "abc".difference("") = ""
 * "abc".difference("abc") = ""
 * "ab".difference("abxyz") = "xyz"
 * "abcde".difference("abxyz") = "xyz"
 * "abcde".difference("xyz") = "xyz"
 * </pre>
 *
 * @param str2 the second string
 * @return the differing part of the two strings; an empty string if they are the same
 * @author K
 * @since 1.0.0
 */
fun CharSequence.difference(str2: CharSequence): String? = StringUtils.difference(this.toString(), str2.toString())

/**
 * Compares two strings and returns the index where they begin to differ.
 *
 * <pre>
 * "".indexOfDifference("") = -1
 * "".indexOfDifference("abc") = 0
 * "abc".indexOfDifference("") = 0
 * "abc".indexOfDifference("abc") = -1
 * "ab".indexOfDifference("abxyz") = 2
 * "abcde".indexOfDifference("abxyz") = 2
 * "abcde".indexOfDifference("xyz") = 0
 * </pre>
 *
 * @param cs2 the first string; may be null
 * @return the index at which they begin to differ; -1 if the two strings are the same
 * @author K
 * @since 1.0.0
 */
fun CharSequence.indexOfDifference(cs2: CharSequence): Int = StringUtils.indexOfDifference(this, cs2)

/**
 * Compares each string in the array and returns the index where they begin to differ.
 *
 * <pre>
 * {}.indexOfDifference() = -1
 * {"abc"}.indexOfDifference() = -1
 * {null, null}.indexOfDifference() = -1
 * {"", ""}.indexOfDifference() = -1
 * {"", null}.indexOfDifference() = 0
 * {"abc", null, null}.indexOfDifference() = 0
 * {null, null, "abc"}.indexOfDifference() = 0
 * {"", "abc"}.indexOfDifference() = 0
 * {"abc", ""}.indexOfDifference() = 0
 * {"abc", "abc"}.indexOfDifference() = -1
 * {"abc", "a"}.indexOfDifference() = 1
 * {"ab", "abxyz"}.indexOfDifference() = 2
 * {"abcde", "abxyz"}.indexOfDifference() = 2
 * {"abcde", "xyz"}.indexOfDifference() = 0
 * {"xyz", "abcde"}.indexOfDifference() = 0
 * {"i am a machine", "i am a robot"}.indexOfDifference() = 7
 * </pre>
 *
 * @return the index at which they begin to differ; -1 if all strings are the same
 * @author K
 * @since 1.0.0
 */
fun Array<out CharSequence?>.indexOfDifference(): Int = StringUtils.indexOfDifference(*this)
//endregion Difference

/**
 * Compares each string in the array and returns the common prefix they share.
 *
 * <pre>
 * {}.getCommonPrefix() = ""
 * {"abc"}.getCommonPrefix() = "abc"
 * {null, null}.getCommonPrefix() = ""
 * {"", ""}.getCommonPrefix() = ""
 * {"", null}.getCommonPrefix() = ""
 * {"abc", null, null}.getCommonPrefix() = ""
 * {null, null, "abc"}.getCommonPrefix() = ""
 * {"", "abc"}.getCommonPrefix() = ""
 * {"abc", ""}.getCommonPrefix() = ""
 * {"abc", "abc"}.getCommonPrefix() = "abc"
 * {"abc", "a"}.getCommonPrefix() = "a"
 * {"ab", "abxyz"}.getCommonPrefix() = "ab"
 * {"abcde", "abxyz"}.getCommonPrefix() = "ab"
 * {"abcde", "xyz"}.getCommonPrefix() = ""
 * {"xyz", "abcde"}.getCommonPrefix() = ""
 * {"i am a machine", "i am a robot"}.getCommonPrefix() = "i am a "
 * </pre>
 *
 * @return the common prefix
 * @author K
 * @since 1.0.0
 */
fun Array<out CharSequence?>.getCommonPrefix(): String {
    val array = this.map { it?.toString() }.toTypedArray()
    return StringUtils.getCommonPrefix(*array)
}

////region Misc
///**
// * Compares the "distance" (similarity) between two strings. This "distance" is the number of deletions, insertions and substitutions required to transform the source string into the target string.
// *
// * <pre>
// * "".getLevenshteinDistance(,"")               = 0
// * "".getLevenshteinDistance(,"a")              = 1
// * "aaapppp".getLevenshteinDistance("")       = 7
// * "frog".getLevenshteinDistance("fog")       = 1
// * "fly".getLevenshteinDistance("ant")        = 3
// * "elephant".getLevenshteinDistance("hippo") = 7
// * "hippo".getLevenshteinDistance("elephant") = 7
// * "hippo".getLevenshteinDistance("zzzzzzzz") = 8
// * "hello".getLevenshteinDistance("hallo")    = 1
// * </pre>
// *
// * @param t the second string
// * @return the distance
// * @throws IllegalArgumentException if either argument is null
// * @author K
// * @since 1.0.0
// */
//fun CharSequence.getLevenshteinDistance(t: CharSequence): Int = LevenshteinDistance().apply(this, t)
//
///**
// * If the "distance" (similarity) between two strings is less than or equal to the given threshold, returns the distance; otherwise returns -1.
// * The "distance" is the number of deletions, insertions and substitutions required to transform the source string into the target string.
// *
// * <pre>
// * *.getLevenshteinDistance(*, -1)               = IllegalArgumentException
// * "".getLevenshteinDistance(,"", 0)               = 0
// * "aaapppp".getLevenshteinDistance("", 8)       = 7
// * "aaapppp".getLevenshteinDistance("", 7)       = 7
// * "aaapppp".getLevenshteinDistance("", 6))      = -1
// * "elephant".getLevenshteinDistance("hippo", 7) = 7
// * "elephant".getLevenshteinDistance("hippo", 6) = -1
// * "hippo".getLevenshteinDistance("elephant", 7) = 7
// * "hippo".getLevenshteinDistance("elephant", 6) = -1
// * </pre>
// *
// *
// * @param t         the second string
// * @param threshold the target upper bound; must not be negative
// * @return the distance, or -1
// * @throws IllegalArgumentException if the threshold is negative
// * @author K
// * @since 1.0.0
// */
//fun CharSequence.getLevenshteinDistance(t: CharSequence, threshold: Int): Int =
//    LevenshteinDistance(threshold).apply(this, t)
////endregion Misc

/**
 * Tests whether the string starts with any of the given values (case-insensitive).
 *
 * <pre>
 * "abcxyz".startsWithAny(null)     = false
 * abcxyz".startsWithAny({""}) = false
 * abcxyz".startsWithAny({"abc"}) = true
 * abcxyz".startsWithAny({null, "xyz", "abc"}) = true
 * </pre>
 *
 * @param searchStrings group of prefixes to match
 * @return true if any of them is a prefix of the string (case-insensitive)
 * @author K
 * @since 1.0.0
 */
fun CharSequence.startsWithAny(vararg searchStrings: CharSequence?): Boolean =
    Strings.CS.startsWithAny(this, *searchStrings)

/**
 * Tests whether the string ends with any of the given values (case-insensitive).
 *
 * <pre>
 * "abcxyz".endsWithAny(null)     = false
 * "abcxyz".endsWithAny(String[] {""}) = true
 * "abcxyz".endsWithAny(String[] {"xyz"}) = true
 * "abcxyz".endsWithAny(String[] {null, "xyz", "abc"}) = true
 * </pre>
 *
 * @param searchStrings group of suffixes to match
 * @return true if any of them is a suffix of the string (case-insensitive)
 * @author K
 * @since 1.0.0
 */
fun CharSequence.endsWithAny(vararg searchStrings: CharSequence?): Boolean =
    Strings.CS.endsWithAny(this, *searchStrings)

/**
 * Normalizes whitespace by removing leading and trailing whitespace and replacing runs of whitespace characters with a single space.
 *
 * @return the resulting string
 * @see .trim
 * @see [
 * http://www.w3.org/TR/xpath/.function-normalize-space](http://www.w3.org/TR/xpath/.function-normalize-space)
 * @author K
 * @since 1.0.0
 */
fun CharSequence.normalizeSpace(): String = StringUtils.normalizeSpace(this.toString())

// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
// Wrappers around org.apache.commons.lang3.StringUtils
// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
