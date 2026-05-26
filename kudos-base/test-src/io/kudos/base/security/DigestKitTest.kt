package io.kudos.base.security

import io.kudos.base.lang.string.EncodeKit
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import kotlin.test.*
import kotlin.test.DefaultAsserter.assertNotNull


/**
 * test for DigestKit
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class DigestKitTest {

    @Test
    fun sha1() {
        val input = "user"
        val salt = DigestKit.generateSalt(8) // random salt

        assertEquals(
            "12dea96fec20593566ab75692c9949596833adc9", EncodeKit.encodeHex(DigestKit.sha1(input.toByteArray()))
        )

        println(EncodeKit.encodeHex(DigestKit.sha1(input.toByteArray(), salt)))
        println(EncodeKit.encodeHex(DigestKit.sha1(input.toByteArray(), salt, 1024)))
    }

    companion object {
        private const val EXPECTED_SHA1_HELLO = "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d"
        private const val EXPECTED_MD5_SALT_HELLO = "06decc8b095724f80103712c235586be"
        private const val EXPECTED_MD5_HELLO      = "5d41402abc4b2a76b9719d911017c592"
    }

    @Test
    fun testGetMD5_withSalt() {
        // Test the String version with salt
        val md5Hex = DigestKit.getMD5("hello", "salt")
        assertEquals(EXPECTED_MD5_SALT_HELLO, md5Hex, "MD5(plaintext + salt) should match the expected value")
    }

    @Test
    fun testGetMD5_byteArray_nullOrEmpty() {
        // original is null -> should return null
        val resultNull = DigestKit.getMD5(null, "anySalt")
        assertNull(resultNull, "when original is null, should return null")

        // original is an empty byte array -> should also return null
        val emptyBytes = ByteArray(0)
        val resultEmpty = DigestKit.getMD5(emptyBytes, "salt")
        assertNull(resultEmpty, "when original is an empty byte array, should return null")
    }

    @Test
    fun testGetMD5_byteArray_withoutSalt() {
        // original is not null but salt is null -> equivalent to no salt
        val originalBytes = "hello".toByteArray(Charsets.UTF_8)
        val md5HexNoSalt = DigestKit.getMD5(originalBytes, null)
        assertNotNull(md5HexNoSalt, "when original is non-empty and salt is null, should not return null")
        assertEquals(EXPECTED_MD5_HELLO, md5HexNoSalt, "MD5(plaintext) should match the no-salt expected value")
    }

    @Test
    fun testIsMatchMD5_withCorrectSalt() {
        // Normal match: MD5 computed from the same plaintext and salt equals the given md5Str
        val md5Hex = DigestKit.getMD5("hello", "salt")
        assertTrue(DigestKit.isMatchMD5("hello", "salt", md5Hex), "correct plaintext + salt should match md5Str")
    }

    @Test
    fun testIsMatchMD5_withoutSaltHistoricalData() {
        // When the salted match fails but the unsalted match succeeds, should also return true
        // (backward compatibility with historical unsalted data).
        val md5NoSalt = DigestKit.getMD5("hello", "") // essentially MD5("hello")
        // Intentionally pass a wrong salt "wrong" so the first attempt fails; the second attempt with empty salt succeeds.
        assertTrue(DigestKit.isMatchMD5("hello", "wrong", md5NoSalt),
            "first attempt with salt 'wrong' fails, second with empty salt succeeds, overall should return true")
    }

    @Test
    fun testMd5_inputStream() {
        // Use ByteArrayInputStream to simulate a file input stream
        val data = "abcdefg".toByteArray(Charsets.UTF_8)
        val expectedMd5Bytes = MessageDigest.getInstance(DigestKit.MD5).digest(data)
        val resultBytes = DigestKit.md5(ByteArrayInputStream(data))
        assertTrue(expectedMd5Bytes.contentEquals(resultBytes), "input-stream MD5 should match the expected value")
    }

    @Test
    fun testSha1_byteArray_noSalt() {
        // Plain SHA-1("hello")
        val result = DigestKit.sha1("hello".toByteArray(Charsets.UTF_8))
        // Compare after converting result to hex
        val hex = EncodeKit.encodeHex(result)
        assertEquals(EXPECTED_SHA1_HELLO, hex, "SHA-1(hello) should match the expected value")
    }

    @Test
    fun testSha1_byteArray_withSaltAndIterations() {
        // Verify SHA-1 with salt and multiple iterations
        val input = "hello".toByteArray(Charsets.UTF_8)
        val salt = "salt".toByteArray(Charsets.UTF_8)
        val iterations = 3

        // Manual computation: first iteration result1 = SHA1(salt || input)
        val md = MessageDigest.getInstance(DigestKit.SHA1)
        md.update(salt)
        val first = md.digest(input)
        // Second iteration result2 = SHA1(result1)
        md.reset()
        val second = md.digest(first)
        // Third iteration result3 = SHA1(result2)
        md.reset()
        val third = md.digest(second)

        val expectedHex = EncodeKit.encodeHex(third)
        val actual = DigestKit.sha1(input, salt, iterations)
        val actualHex = EncodeKit.encodeHex(actual)

        assertEquals(expectedHex, actualHex, "SHA-1 with salt and $iterations iterations should match the manual computation")
    }

    @Test
    fun testSha1_inputStream() {
        // Use ByteArrayInputStream to test file SHA-1
        val data = "kotlinTest".toByteArray(Charsets.UTF_8)
        // Pre-compute SHA-1("kotlinTest")
        val expectedBytes = MessageDigest.getInstance(DigestKit.SHA1).digest(data)
        val resultBytes = DigestKit.sha1(ByteArrayInputStream(data))
        assertTrue(expectedBytes.contentEquals(resultBytes), "input-stream SHA-1 should match the expected value")
    }

    @Test
    fun testGenerateSalt_validLength() {
        // Test generating salts of various lengths; two calls should produce different results
        val salt1 = DigestKit.generateSalt(16)
        val salt2 = DigestKit.generateSalt(16)
        assertEquals(16, salt1.size, "generated salt length should be 16")
        assertEquals(16, salt2.size, "generated salt length should be 16")
        assertFalse(salt1.contentEquals(salt2), "multiple calls to generateSalt(16) should yield different random results")
    }

    @Test
    fun testGenerateSalt_invalidLength_throws() {
        // numBytes <= 0 should throw IllegalArgumentException (triggered by Validate.isTrue)
        assertFailsWith<IllegalArgumentException> {
            DigestKit.generateSalt(0)
        }
        assertFailsWith<IllegalArgumentException> {
            DigestKit.generateSalt(-5)
        }
    }

    @Test
    fun testDigest_privateMethod_behavior_equivalence() {
        // Verify digest(ByteArray, algorithm, null, 1) is equivalent to manual MD5/SHA1
        val data = "digestTest".toByteArray(Charsets.UTF_8)

        // Test MD5
        val expectedMd5 = MessageDigest.getInstance(DigestKit.MD5).digest(data)
        val actualMd5 = DigestKit.digest(data, DigestKit.MD5, null, 1)
        assertTrue(expectedMd5.contentEquals(actualMd5), "DigestKit.digest MD5 should match MessageDigest")

        // Test SHA-1
        val expectedSha1 = MessageDigest.getInstance(DigestKit.SHA1).digest(data)
        val actualSha1 = DigestKit.digest(data, DigestKit.SHA1, null, 1)
        assertTrue(expectedSha1.contentEquals(actualSha1), "DigestKit.digest SHA-1 should match MessageDigest")
    }

}