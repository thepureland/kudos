package io.kudos.base.security

import io.kudos.base.lang.string.EncodeKit
import org.junit.jupiter.api.Assertions.assertArrayEquals
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.*


/**
 * test for CryptoKit
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class CryptoKitTest {

    @Test
    fun isMacValid() {
        val input = "foo message"

        //key可为任意字符串
        //byte[] key = "a foo key".getBytes();

        //key可为任意字符串
        //byte[] key = "a foo key".getBytes();
        val key = CryptoKit.generateHmacSha1Key()
        assertEquals(20, key.size.toLong())

        val macResult = CryptoKit.hmacSha1(input.toByteArray(), key)
        println("hmac-sha1 key in hex      :" + EncodeKit.encodeHex(key))
        println("hmac-sha1 in hex result   :" + EncodeKit.encodeHex(macResult))

        assert(CryptoKit.isMacValid(macResult, input.toByteArray(), key))
    }

    // -------- HMAC-SHA1 Tests --------

    @Test
    fun testHmacSha1MatchesJavaMac() {
        val input = "The quick brown fox jumps over the lazy dog".toByteArray(StandardCharsets.UTF_8)
        val key = "secret-key-123".toByteArray(StandardCharsets.UTF_8)

        // Compute expected via Java's Mac directly
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val expected = mac.doFinal(input)

        val actual = CryptoKit.hmacSha1(input, key)
        assertArrayEquals(expected, actual, "hmacSha1 output should match Java's Mac HmacSHA1")
    }

    @Test
    fun testIsMacValidReturnsTrueAndFalse() {
        val input = "DataToSign".toByteArray(StandardCharsets.UTF_8)
        val key = "another-secret".toByteArray(StandardCharsets.UTF_8)

        val signature = CryptoKit.hmacSha1(input, key)
        assertTrue(CryptoKit.isMacValid(signature, input, key), "isMacValid should return true when HMAC matches")

        // Modify one byte of signature
        val badSignature = signature.copyOf()
        badSignature[0] = badSignature[0].inc()
        assertFalse(CryptoKit.isMacValid(badSignature, input, key), "isMacValid should return false when HMAC does not match")
    }

    @Test
    fun testGenerateHmacSha1KeyLengthAndRandomness() {
        val key1 = CryptoKit.generateHmacSha1Key()
        val key2 = CryptoKit.generateHmacSha1Key()

        assertEquals(20, key1.size, "HMAC-SHA1 key length must be 20 bytes")
        assertEquals(20, key2.size, "HMAC-SHA1 key length must be 20 bytes")
        // Very small chance of collision; this check ensures keys are not trivially identical
        assertFalse(key1.contentEquals(key2), "Two generated HMAC-SHA1 keys should usually not be identical")
    }

    // -------- AES Encryption/Decryption Tests --------

    @Test
    fun testAesEncryptDecryptWithExplicitPassword() {
        val password = "password123"
        val plaintext = "HelloKudosWorld"

        // 1. 用显式密码加密成十六进制字符串
        val cipherHex = CryptoKit.aesEncrypt(plaintext, password)
        assertNotNull(cipherHex)
        assertTrue(cipherHex.isNotBlank(), "Cipher text hex should not be blank")

        // 2. 将十六进制字符串解码为密文字节数组
        val cipherBytes = EncodeKit.decodeHex(cipherHex)

        // 3. 用 aesDecrypt(ByteArray, password) 解码回原始明文字节
        val decryptedBytes = CryptoKit.aesDecrypt(cipherBytes, password)
        assertArrayEquals(
            plaintext.toByteArray(StandardCharsets.UTF_8),
            decryptedBytes,
            "aesDecrypt(ByteArray, password) should return original plaintext bytes"
        )

        // 4. 再直接调用 aesDecrypt(String, password) 得到明文字符串
        val roundTrip = CryptoKit.aesDecrypt(cipherHex, password)
        assertEquals(plaintext, roundTrip, "aesDecrypt(hexString, password) should return original plaintext string")

        val roundTripResult = CryptoKit.tryAesDecrypt(cipherHex, password)
        assertTrue(roundTripResult.isSuccess, "tryAesDecrypt should succeed for valid input")
        assertEquals(plaintext, roundTripResult.getOrThrow(), "tryAesDecrypt should return original plaintext string")
    }

    @Test
    fun testTryAesDecryptReturnsFailureOnInvalidHex() {
        val result = CryptoKit.tryAesDecrypt("zz", "password123")
        assertTrue(result.isFailure, "tryAesDecrypt should return failure for invalid hex input")

        // 兼容旧API：失败时仍返回空字符串
        assertEquals("", CryptoKit.aesDecrypt("zz", "password123"))
    }

    @Test
    fun testAesDecryptCanFallbackLegacySha1PrngCiphertext() {
        val password = "legacy-password"
        val plaintext = "legacy-compatible-content"
        val legacyCipherHex = legacyAesEncryptToHex(plaintext, password)

        val decrypted = CryptoKit.aesDecrypt(legacyCipherHex, password)
        assertEquals(plaintext, decrypted, "aesDecrypt should fallback to legacy SHA1PRNG-derived key data")
    }

    @Test
    fun testAesDecryptLegacyCiphertextWithWrongPasswordReturnsEmpty() {
        val password = "legacy-password"
        val legacyCipherHex = legacyAesEncryptToHex("legacy-compatible-content", password)

        val decrypted = CryptoKit.aesDecrypt(legacyCipherHex, "wrong-password")
        assertEquals("", decrypted, "legacy ciphertext with wrong password should keep compatibility and return empty")
    }

    @Test
    fun testAesDecryptLegacyCiphertextWithHtbUpperHexCanFallback() {
        val password = "_HTBlegacy-password"
        val plaintext = "legacy-htb-content"
        val legacyCipherHexUpper = legacyAesEncryptToHex(plaintext, password).uppercase()

        val decrypted = CryptoKit.aesDecrypt(legacyCipherHexUpper, password)
        assertEquals(plaintext, decrypted, "HTB uppercase hex should still fallback to legacy decryption correctly")
    }

    @Test
    fun testAesEncryptDecryptWithHtbPrefixPassword() {
        val password = "_HTBsuperSecret"
        val plaintext = "CaseSensitive123"

        // Encrypt; result must be uppercase hex because password starts with "_HTB"
        val cipherHex = CryptoKit.aesEncrypt(plaintext, password)
        assertTrue(cipherHex.all { it.isDigit() || it in 'A'..'F' }, "Cipher hex must be uppercase when password starts with HTB")

        // Decrypt via aesDecrypt(String, password) path
        val decrypted = CryptoKit.aesDecrypt(cipherHex, password)
        assertEquals(plaintext, decrypted, "Decrypting HTB-encrypted hex should return original plaintext")
    }

    @Test
    fun testAesEncryptDecryptWithDefaultKey() {
        val plaintext = "DefaultKeyTest"
        // aesEncrypt(input) adds PREFIX before the hex
        val cipherWithPrefix = CryptoKit.aesEncrypt(plaintext)
        assertTrue(cipherWithPrefix.startsWith("┼"), "aesEncrypt(input) should prepend the special PREFIX '┼'")
        cipherWithPrefix.removePrefix("┼")

        // Decrypt via aesDecrypt(fullString)
        val roundTrip = CryptoKit.aesDecrypt(cipherWithPrefix)
        assertEquals(plaintext, roundTrip, "aesDecrypt on PREFIX+hex should return the original plaintext")

        // Also test that passing a non-prefixed string returns itself (legacy path)
        val legacy = "NotEncryptedString"
        assertEquals(legacy, CryptoKit.aesDecrypt(legacy), "aesDecrypt on a non-prefixed string should return it unchanged")
    }

    @Test
    fun testAesEncryptDecryptEmptyString() {
        val password = "emptyTest"
        val emptyPlain = ""

        // 使用显式密码加密，得到十六进制字符串
        val cipherHex = CryptoKit.aesEncrypt(emptyPlain, password)
        // 解密时直接调用 aesDecrypt(String, password)——它会自动把 hex 字符串 decode 回字节，再做解密
        val roundTripExplicit = CryptoKit.aesDecrypt(cipherHex, password)
        assertEquals("", roundTripExplicit, "Decrypting encryption of empty string should yield empty string")

        // 使用默认 key 的重载
        val cipherWithPrefix = CryptoKit.aesEncrypt(emptyPlain)
        val roundTripDefault = CryptoKit.aesDecrypt(cipherWithPrefix)
        assertEquals("", roundTripDefault, "Default-key decrypt of empty input should yield empty string")
    }

    @Test
    fun testGenerateIVLengthAndRandomness() {
        val iv1 = CryptoKit.generateIV()
        val iv2 = CryptoKit.generateIV()
        assertEquals(16, iv1.size, "IV must be 16 bytes")
        assertEquals(16, iv2.size, "IV must be 16 bytes")
        assertFalse(iv1.contentEquals(iv2), "Two generated IVs should not be identical")
    }

    // -------- Hex Encoding/Decoding Tests --------

    @Test
    fun testEncodeHexDecodeHexInverse() {
        val random = java.util.Random(12345)
        repeat(10) {
            val length = random.nextInt(20) + 1
            val bytes = ByteArray(length).apply { random.nextBytes(this) }

            val hexChars = CryptoKit.encodeHex(bytes)
            assertEquals(length * 2, hexChars.size, "Hex char array length should be twice the byte array length")

            val back = CryptoKit.decodeHex(hexChars.concatToString().toByteArray(Charsets.UTF_8))
            assertArrayEquals(bytes, back, "Decoding hex-encoded bytes must recover original byte array")
        }
    }

    @Test
    fun testEncodeHexProducesLowercase() {
        // Single byte 0xAB => hex should be "ab"
        val single = byteArrayOf(0xAB.toByte())
        val hexChars = CryptoKit.encodeHex(single)
        val hexString = hexChars.concatToString()
        assertEquals("ab", hexString, "encodeHex should produce lowercase hex digits for input 0xAB")
    }

    @Test
    fun testDecodeHexThrowsOnInvalidInput() {
        val invalidHexBytes = "zz".toByteArray(Charsets.UTF_8)
        assertFailsWith<NumberFormatException> {
            CryptoKit.decodeHex(invalidHexBytes)
        }
    }

    private fun legacyAesEncryptToHex(input: String, password: String): String {
        val generator = KeyGenerator.getInstance("AES")
        val secureRandom = SecureRandom.getInstance("SHA1PRNG")
        secureRandom.setSeed(password.toByteArray(StandardCharsets.UTF_8))
        generator.init(128, secureRandom)
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, generator.generateKey())
        val encrypted = cipher.doFinal(input.toByteArray(StandardCharsets.UTF_8))
        return EncodeKit.encodeHex(encrypted)
    }

}