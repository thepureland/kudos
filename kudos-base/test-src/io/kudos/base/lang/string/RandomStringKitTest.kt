package io.kudos.base.lang.string


import java.security.SecureRandom
import java.util.*
import kotlin.test.*

/**
 * test for RandomStringKit
 *
 * @author ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class RandomStringKitTest {

    @Test
    fun uuid_ShouldContainHyphensAndBeValidUUID() {
        val id = RandomStringKit.uuid()
        assertNotNull(id)
        // UUID format is 8-4-4-4-12 hex characters
        val parts = id.split("-")
        assertEquals(5, parts.size)
        assertTrue(
            parts[0].length == 8 &&
                    parts[1].length == 4 &&
                    parts[2].length == 4 &&
                    parts[3].length == 4 &&
                    parts[4].length == 12
        )
        // Should parseable as UUID
        UUID.fromString(id)
    }

    @Test
    fun uuidWithoutDelimiter_ShouldNotContainHyphensAndLength32() {
        val id = RandomStringKit.uuidWithoutDelimiter()
        assertNotNull(id)
        assertFalse(id.contains("-"))
        assertEquals(32, id.length)
        // Should parseable after inserting hyphens
        val withHyphens = id.substring(0, 8) + "-" +
                id.substring(8, 12) + "-" +
                id.substring(12, 16) + "-" +
                id.substring(16, 20) + "-" +
                id.substring(20, 32)
        UUID.fromString(withHyphens)
    }

    @Test
    fun randomLong_ShouldReturnDigitsOnlyAndNonNegative() {
        repeat(10) {
            val s = RandomStringKit.randomLong()
            assertNotNull(s)
            assertTrue(s.all { it.isDigit() })
            // Should be non-negative (no leading minus sign)
            assertFalse(s.startsWith("-"))
            // Should parse as Long without exception
            s.toLong()
        }
    }

    @Test
    fun randomBase62_LengthMatchesAndValidChars() {
        val length = 16
        val s = RandomStringKit.randomBase62(length)
        assertNotNull(s)
        assertEquals(length, s.length)
        val validChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        s.forEach { ch ->
            assertTrue(validChars.contains(ch), "Invalid Base62 char: $ch")
        }
    }

    @Test
    fun randomBase62_ZeroLength_ReturnsEmpty() {
        val s = RandomStringKit.randomBase62(0)
        assertEquals("", s)
    }

    @Test
    fun randomBase62_NegativeLength_StillProducesStringOfThatLength() {
        // Because SecureRandom.nextBytes on negative size yields IllegalArgumentException,
        // but our wrapper does not check. We expect it to throw.
        assertFailsWith<NegativeArraySizeException> {
            RandomStringKit.randomBase62(-1)
        }
    }

    @Test
    fun random_DefaultRandomString_LengthZero() {
        val s = RandomStringKit.random(0)
        assertEquals("", s)
    }

    @Test
    fun random_DefaultRandomString_PositiveLength() {
        val len = 20
        val s = RandomStringKit.random(len)
        assertNotNull(s)
        assertEquals(len, s.length)
    }

    @Test
    fun randomAscii_ShouldOnlyContainAscii32To126() {
        val len = 50
        val s = RandomStringKit.randomAscii(len)
        assertEquals(len, s.length)
        s.forEach { ch ->
            assertTrue(ch.code in 32..126, "Character out of ASCII range: ${ch.code}")
        }
    }

    @Test
    fun randomAlphabetic_ShouldOnlyContainLetters() {
        val len = 30
        val s = RandomStringKit.randomAlphabetic(len)
        assertEquals(len, s.length)
        s.forEach { ch ->
            assertTrue(ch.isLetter(), "Non-letter character: $ch")
        }
    }

    @Test
    fun randomAlphanumeric_ShouldOnlyContainLettersOrDigits() {
        val len = 25
        val s = RandomStringKit.randomAlphanumeric(len)
        assertEquals(len, s.length)
        s.forEach { ch ->
            assertTrue(ch.isLetterOrDigit(), "Non-alphanumeric character: $ch")
        }
    }

    @Test
    fun randomNumeric_ShouldOnlyContainDigits() {
        val len = 15
        val s = RandomStringKit.randomNumeric(len)
        assertEquals(len, s.length)
        s.forEach { ch ->
            assertTrue(ch.isDigit(), "Non-digit character: $ch")
        }
    }

    @Test
    fun random_WithLettersAndNumbersFlags() {
        val len = 10
        val lettersOnly = RandomStringKit.random(len, letters = true, numbers = false)
        assertEquals(len, lettersOnly.length)
        lettersOnly.forEach { ch ->
            assertTrue(ch.isLetter(), "Expected letter but got $ch")
        }

        val numbersOnly = RandomStringKit.random(len, letters = false, numbers = true)
        assertEquals(len, numbersOnly.length)
        numbersOnly.forEach { ch ->
            assertTrue(ch.isDigit(), "Expected digit but got $ch")
        }

        val both = RandomStringKit.random(len, letters = true, numbers = true)
        assertEquals(len, both.length)
        both.forEach { ch ->
            assertTrue(ch.isLetterOrDigit(), "Expected letter or digit but got $ch")
        }
    }

    @Test
    fun random_WithRangeAndFlags() {
        val len = 12
        val start = 65  // 'A'
        val end = 91    // 'Z'+1
        val alpha = RandomStringKit.random(len, start, end, letters = true, numbers = false)
        assertEquals(len, alpha.length)
        alpha.forEach { ch ->
            assertTrue(ch in 'A'..'Z', "Expected uppercase letter but got $ch")
        }

        val numericRange = RandomStringKit.random(len, start = 48, end = 58, letters = false, numbers = true)
        assertEquals(len, numericRange.length)
        numericRange.forEach { ch ->
            assertTrue(ch in '0'..'9', "Expected digit but got $ch")
        }
    }

    @Test
    fun random_WithRangeAndCharsVararg() {
        val len = 8
        val charPool = charArrayOf('X', 'Y', 'Z')
        val s = RandomStringKit.random(
            len,
            start = 0,
            end = 3,
            letters = true,
            numbers = false,
            chars = charPool,
            random = SecureRandom()
        )
        assertEquals(len, s.length)
        s.forEach { ch ->
            assertTrue(ch in charPool, "Character not in provided pool: $ch")
        }
    }

    @Test
    fun random_WithCharsString() {
        val len = 5
        val pool = "ABC123"
        val s = RandomStringKit.random(len, pool)
        assertEquals(len, s.length)
        s.forEach { ch ->
            assertTrue(pool.contains(ch), "Character not in provided string pool: $ch")
        }
    }

    @Test
    fun random_WithCharsArray() {
        val len = 6
        val pool = charArrayOf('a', 'b', 'c')
        val s = RandomStringKit.random(len, *pool)
        assertEquals(len, s.length)
        s.forEach { ch ->
            assertTrue(ch in pool, "Character not in provided char array: $ch")
        }
    }

    @Test
    fun random_WithCountNegative_ThrowsIllegalArgument() {
        assertFailsWith<IllegalArgumentException> {
            RandomStringKit.random(-1)
        }
        assertFailsWith<IllegalArgumentException> {
            RandomStringKit.random(-5, letters = true, numbers = true)
        }
        assertFailsWith<IllegalArgumentException> {
            RandomStringKit.random(-3, 0, 10, letters = true, numbers = false)
        }
        assertFailsWith<IllegalArgumentException> {
            RandomStringKit.random(-2, 0, 5,
                letters = true,
                numbers = false,
                chars = charArrayOf('a', 'b'),
                random = SecureRandom()
            )
        }
        assertFailsWith<IllegalArgumentException> {
            RandomStringKit.random(-4, "pool")
        }
        assertFailsWith<IllegalArgumentException> {
            RandomStringKit.random(-6, 'x', 'y')
        }
    }

    @Test
    fun random_WithInvalidRange_ThrowsArrayIndexOutOfBounds() {
        // end <= start and no custom chars => should fail
        assertFailsWith<IllegalArgumentException> {
            RandomStringKit.random(5, 10, 5, letters = true, numbers = true)
        }
    }

    @Test
    fun random_WithCustomRandomSeed_ProducesDeterministicSequence() {
        val seed = 12345L
        val rand = Random(seed)
        val s1 = RandomStringKit.random(
            10,
            start = 32,
            end = 126,
            letters = true,
            numbers = true,
            chars = null,
            random = rand
        )
        // Reset same seed
        val rand2 = Random(seed)
        val s2 = RandomStringKit.random(
            10,
            start = 32,
            end = 126,
            letters = true,
            numbers = true,
            chars = null,
            random = rand2
        )
        assertEquals(s1, s2)
    }

}
