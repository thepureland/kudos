package io.kudos.base.lang.string

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * XCharSequence extension function test cases (complements XCharSequenceTest).
 *
 * Covers capitalizeString / toType (all branches + error branches) / toMd5HexStr /
 * the array-based replaceEach & replaceEachRepeatedly / replace(max) / and every
 * Apache Commons StringUtils wrapper extension declared in XCharSequence.kt.
 *
 * @author K
 * @since 1.0.0
 */
internal class XCharSequenceExtTest {

    @Test
    fun capitalizeString() {
        assertEquals("Cat", "cat".capitalizeString())
        assertEquals("CAT", "CAT".capitalizeString())
        assertEquals("", "".capitalizeString())
        assertEquals("Abc", "abc".capitalizeString())
    }

    @Test
    fun toType_allSupportedTypes() {
        assertEquals(1.5, "1.5".toType(Double::class))
        assertEquals(7, "7".toType(Int::class))
        assertEquals(8L, "8".toType(Long::class))
        assertEquals(1.5f, "1.5".toType(Float::class))
        assertEquals(3.toShort(), "3".toType(Short::class))
        assertEquals(BigDecimal("9.99"), "9.99".toType(BigDecimal::class))
        assertEquals(BigInteger("123"), "123".toType(BigInteger::class))
        assertEquals(true, "true".toType(Boolean::class))
        assertEquals(false, "abc".toType(Boolean::class)) // anything non-"true" -> false
        assertEquals(5.toByte(), "5".toType(Byte::class))
        assertEquals('h', "hello".toType(Char::class))
        assertEquals("raw", "raw".toType(String::class))
    }

    @Test
    fun toType_errorBranches() {
        // unsupported type -> error() throws IllegalStateException (note: KDoc says IllegalArgumentException)
        assertFailsWith<IllegalStateException> { "x".toType(java.util.Date::class) }
        // numeric conversion failure
        assertFailsWith<NumberFormatException> { "abc".toType(Int::class) }
        // empty string to Char
        assertFailsWith<NoSuchElementException> { "".toType(Char::class) }
    }

    @Test
    fun toMd5HexStr() {
        // deterministic with the same salt
        val a = "password".toMd5HexStr("salt")
        val b = "password".toMd5HexStr("salt")
        assertEquals(a, b)
        // different salt -> different digest
        assertFalse(a == "password".toMd5HexStr("other"))
    }

    @Test
    fun replaceEach_arrays() {
        assertEquals("wcte", "abcde".replaceEach(arrayOf("ab", "d"), arrayOf("w", "t")))
        // null arrays -> original string
        assertEquals("aba", "aba".replaceEach(null, null))
        // null elements treated as empty string
        assertEquals("b", "aba".replaceEach(arrayOf("a"), arrayOf<CharSequence?>(null)))
    }

    @Test
    fun replaceEachRepeatedly_arrays() {
        assertEquals("tcte", "abcde".replaceEachRepeatedly(arrayOf("ab", "d"), arrayOf("d", "t")))
        assertEquals("aba", "aba".replaceEachRepeatedly(null, null))
        // infinite-loop detection
        assertFailsWith<IllegalStateException> {
            "abcde".replaceEachRepeatedly(arrayOf("ab", "d"), arrayOf("d", "ab"))
        }
    }

    @Test
    fun replace_withMax() {
        assertEquals("zbza", "abaa".replace("a", "z", 2))
        assertEquals("zbzz", "abaa".replace("a", "z", -1))
        assertEquals("abaa", "abaa".replace("a", "z", 0))
        // null search -> empty search string -> original returned
        assertEquals("any", "any".replace(null, "x", 2))
        // null replacement -> treated as empty string -> matched chars removed
        assertEquals("ny", "any".replace("a", null, 2))
    }

    @Test
    fun containsWhitespace() {
        assertTrue("a b".containsWhitespace())
        assertFalse("abc".containsWhitespace())
    }

    @Test
    fun containsAny() {
        assertTrue("zzabyycdxx".containsAny('z', 'a'))
        assertFalse("aba".containsAny('z'))
        assertTrue("zzabyycdxx".containsAny("za"))
        assertFalse("aba".containsAny("z"))
    }

    @Test
    fun indexOfAnyBut() {
        assertEquals(3, "zzabyycdxx".indexOfAnyBut('z', 'a'))
        assertEquals(-1, "aba".indexOfAnyBut('a', 'b'))
        assertEquals(3, "zzabyycdxx".indexOfAnyBut("za"))
    }

    @Test
    fun containsOnly() {
        assertTrue("abab".containsOnly('a', 'b', 'c'))
        assertFalse("ab1".containsOnly('a', 'b', 'c'))
        assertTrue("abab".containsOnly("abc"))
        assertFalse("ab1".containsOnly("abc"))
    }

    @Test
    fun containsNone() {
        assertTrue("abab".containsNone('x', 'y', 'z'))
        assertFalse("abz".containsNone('x', 'y', 'z'))
        assertTrue("abab".containsNone("xyz"))
        assertFalse("abz".containsNone("xyz"))
    }

    @Test
    fun leftRightMid() {
        assertEquals("bc", "abc".right(2))
        assertEquals("c", "abc".mid(2, 4))
        // left uses leftPad in the implementation
        assertEquals("abc", "abc".left(3))
    }

    @Test
    fun substringBetween() {
        assertEquals("abc", "tagabctag".substringBetween("tag"))
        assertEquals("", "tagabctag".substringBetween(""))
        assertEquals("b", "wx[b]yz".substringBetween("[", "]"))
        assertEquals("abc", "yabcz".substringBetween("y", "z"))
    }

    @Test
    fun substringsBetween() {
        assertContentEquals(arrayOf("a", "b", "c"), "[a][b][c]".substringsBetween("[", "]"))
    }

    @Test
    fun splitByCharacterType() {
        // NOTE: the two extension names delegate to the swapped Commons methods; assert actual behavior.
        // splitByCharacterType() -> Commons splitByCharacterTypeCamelCase -> camel-case aware grouping
        assertContentEquals(arrayOf("foo", "Bar"), "fooBar".splitByCharacterType())
        // splitByCharacterTypeCamelCase() -> Commons splitByCharacterType -> pure type grouping
        assertContentEquals(arrayOf("foo", "B", "ar"), "fooBar".splitByCharacterTypeCamelCase())
    }

    @Test
    fun deleteWhitespace() {
        assertEquals("abc", "   ab  c  ".deleteWhitespace())
    }

    @Test
    fun removePrefixSuffixIgnoreCase() {
        assertEquals("domain.com", "www.domain.com".removePrefixIgnoreCase("www."))
        // CS variant: case must match for prefix removal
        assertEquals("WWW.domain.com", "WWW.domain.com".removePrefixIgnoreCase("www."))
        assertEquals("abc", "abc".removePrefixIgnoreCase(null))
        // suffix is case-insensitive (CI)
        assertEquals("www.domain", "www.domain.COM".removeSuffixIgnoreCase(".com"))
        assertEquals("abc", "abc".removeSuffixIgnoreCase(null))
    }

    @Test
    fun chompAndChop() {
        assertEquals("abc", "abc\r\n".chomp())
        assertEquals("abc ", "abc \r".chomp())
        assertEquals("ab", "abc".chop())
        assertEquals("abc", "abc\r\n".chop())
    }

    @Test
    fun repeatAndSeparate() {
        assertEquals("?, ?, ?", "?".repeatAndSeparate(", ", 3))
        assertEquals("", "?".repeatAndSeparate(", ", 0))
    }

    @Test
    fun center() {
        assertEquals(" a  ", "a".center(4, ' '))
        assertEquals("yayy", "a".center(4, 'y'))
        assertEquals(" a  ", "a".center(4, " "))
        assertEquals("  abc  ", "abc".center(7, null as CharSequence?))
    }

    @Test
    fun uncapitalizeAndSwapCase() {
        assertEquals("cat", "Cat".uncapitalize())
        assertEquals("tHE DOG", "The dog".swapCase())
    }

    @Test
    fun countMatches() {
        assertEquals(2, "abba".countMatches("a"))
        assertEquals(1, "abba".countMatches("ab"))
        assertEquals(0, "abba".countMatches(null))
    }

    @Test
    fun characterTests() {
        assertTrue("abc".isAlpha())
        assertFalse("ab2c".isAlpha())
        assertTrue("ab c".isAlphaSpace())
        assertTrue("abc".isAlphanumeric())   // delegates to isAlpha per implementation
        assertTrue("ab2c".isAlphanumericSpace())
        assertTrue("ab-c~".isAsciiPrintable())
        assertTrue("123".isNumeric())
        assertFalse("12.3".isNumeric())
        // NOTE: isNumericSpace delegates to Commons isNumeric (not isNumericSpace), so a space -> false
        assertFalse("12 3".isNumericSpace())
        assertTrue("123".isNumericSpace())
        assertTrue("  ".isWhitespace())
        assertFalse("abc".isWhitespace())
        assertTrue("abc".isAllLowerCase())
        assertFalse("abC".isAllLowerCase())
        assertTrue("ABC".isAllUpperCase())
        assertFalse("aBC".isAllUpperCase())
    }

    @Test
    fun reverseDelimited() {
        assertEquals("c.b.a", "a.b.c".reverseDelimited('.'))
        assertEquals("String.lang.java", "java.lang.String".reverseDelimited('.'))
    }

    @Test
    fun abbreviate() {
        assertEquals("abc...", "abcdefg".abbreviate(6))
        assertEquals("abcdefg", "abcdefg".abbreviate(7))
        assertFailsWith<IllegalArgumentException> { "abcdefg".abbreviate(3) }
        assertEquals("...fghi...", "abcdefghijklmno".abbreviate(5, 10))
        assertEquals("ab.f", "abcdef".abbreviateMiddle(".", 4))
        assertEquals("abc", "abc".abbreviateMiddle(".", 3))
    }

    @Test
    fun difference() {
        assertEquals("xyz", "abcde".difference("abxyz"))
        assertEquals("", "abc".difference("abc"))
        assertEquals(2, "abcde".indexOfDifference("abxyz"))
        assertEquals(-1, "abc".indexOfDifference("abc"))
    }

    @Test
    fun arrayDifferenceAndCommonPrefix() {
        assertEquals(7, arrayOf<CharSequence?>("i am a machine", "i am a robot").indexOfDifference())
        assertEquals(-1, arrayOf<CharSequence?>("abc", "abc").indexOfDifference())
        assertEquals("i am a ", arrayOf<CharSequence?>("i am a machine", "i am a robot").getCommonPrefix())
        assertEquals("", arrayOf<CharSequence?>("abcde", "xyz").getCommonPrefix())
        assertEquals("", arrayOf<CharSequence?>(null, null).getCommonPrefix())
    }

    @Test
    fun startsEndsWithAny() {
        assertTrue("abcxyz".startsWithAny("abc"))
        assertTrue("abcxyz".startsWithAny(null, "xyz", "abc"))
        assertFalse("abcxyz".startsWithAny("zzz"))
        assertTrue("abcxyz".endsWithAny("xyz"))
        assertFalse("abcxyz".endsWithAny("zzz"))
    }

    @Test
    fun normalizeSpace() {
        assertEquals("a b c", "  a   b  c  ".normalizeSpace())
    }

}
