package io.kudos.base.lang.string

import org.apache.commons.codec.DecoderException
import java.nio.charset.StandardCharsets
import kotlin.test.*

/**
 * test for EncodeKit
 *
 * @author ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class EncodeKitTest {

    //========================
    // Tests for Hex encoding
    //========================

    @Test
    fun encodeHex_EmptyArray_ReturnsEmptyString() {
        val empty = ByteArray(0)
        val encoded = EncodeKit.encodeHex(empty)
        assertEquals("", encoded)
    }

    @Test
    fun encodeHex_NonEmptyArray_CorrectHexString() {
        // bytes: 0x00, 0x7F, 0xAB, 0xFF
        val input = byteArrayOf(0x00, 0x7F.toByte(), 0xAB.toByte(), 0xFF.toByte())
        val encoded = EncodeKit.encodeHex(input)
        // Expected lowercase hex: "007fabff"
        assertEquals("007fabff", encoded)
    }

    @Test
    fun decodeHex_ValidHexString_ReturnsOriginalBytes() {
        val original = byteArrayOf(0x10, 0x20, 0x30, 0x40.toByte(), 0x50.toByte())
        val hex = EncodeKit.encodeHex(original)
        val decoded = EncodeKit.decodeHex(hex)
        assertTrue(original.contentEquals(decoded))
    }

    @Test
    fun decodeHex_EmptyString_ReturnsEmptyArray() {
        val decoded = EncodeKit.decodeHex("")
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun decodeHex_InvalidLength_ThrowsDecoderException() {
        // odd-length hex string
        assertFailsWith<DecoderException> { EncodeKit.decodeHex("ABC") }
    }

    @Test
    fun decodeHex_NonHexCharacter_ThrowsDecoderException() {
        // contains 'G'
        assertFailsWith<DecoderException> { EncodeKit.decodeHex("0a0g") }
    }

    //=============================
    // Tests for Base64 encoding
    //=============================

    @Test
    fun encodeBase64_EmptyArray_ReturnsEmptyString() {
        val encoded = EncodeKit.encodeBase64(ByteArray(0))
        assertEquals("", encoded)
    }

    @Test
    fun encodeBase64_DecodeCycle_ReturnsOriginal() {
        val text = "Kotlin Base64 Test!"
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        val encoded = EncodeKit.encodeBase64(bytes)
        // Base64 string should not contain whitespace
        assertFalse(encoded.contains("\n") || encoded.contains(" "))
        val decoded = EncodeKit.decodeBase64(encoded)
        assertEquals(text, decoded.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun decodeBase64_InvalidInput_DoesNotThrow() {
        // 对于非法 Base64 字符，Decode 不应抛出异常
        EncodeKit.decodeBase64("invalid***")
    }

    //=====================================
    // Tests for URL-safe Base64 encoding
    //=====================================

    @Test
    fun encodeUrlSafeBase64_ReservedCharacters_Replaced() {
        // Construct bytes that produce '+' and '/' in standard Base64
        val input = byteArrayOf(0xFB.toByte(), 0xEF.toByte()) // 0xFBEF -> "+-8"
        val standard = java.util.Base64.getEncoder().encodeToString(input)
        assertTrue(standard.contains("+") || standard.contains("/"))
        val urlSafe = EncodeKit.encodeUrlSafeBase64(input)
        assertFalse(urlSafe.contains("+"))
        assertFalse(urlSafe.contains("/"))
        // Decoding URL-safe via decodeBase64 should still work
        val decoded = EncodeKit.decodeBase64(urlSafe)
        assertTrue(input.contentEquals(decoded))
    }

    @Test
    fun decodeBase64_UrlSafeInput_Works() {
        val original = "URL safe test"
        val bytes = original.toByteArray(StandardCharsets.UTF_8)
        val urlSafe = EncodeKit.encodeUrlSafeBase64(bytes)
        val decoded = EncodeKit.decodeBase64(urlSafe)
        assertEquals(original, decoded.toString(StandardCharsets.UTF_8))
    }

    //==========================
    // Tests for Base62 encoding
    //==========================

    @Test
    fun encodeBase62_EmptyArray_ReturnsEmptyString() {
        val result = EncodeKit.encodeBase62(ByteArray(0))
        assertEquals("", result)
    }

    @Test
    fun encodeBase62_ByteValues_MapToCorrectCharacters() {
        // Byte 0x00 -> 0 % 62 = 0 -> '0'
        // Byte 0x3D -> 61 % 62 = 61 -> 'z'
        // Byte 0x3E -> 62 -> 62 % 62 = 0 -> '0'
        val input = byteArrayOf(0x00, 0x3D.toByte(), 0x3E.toByte())
        val encoded = EncodeKit.encodeBase62(input)
        assertEquals("0z0", encoded)
    }

    @Test
    fun encodeBase62_RandomBytes_DeterministicMapping() {
        val input = byteArrayOf(0x7F, 0x80.toByte(), 0xFF.toByte())
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val expected = """
            ${chars[(0x7F and 0xFF) % chars.length]}
            ${chars[(0x80 and 0xFF) % chars.length]}
            ${chars[(0xFF and 0xFF) % chars.length]}
        """.trimIndent().replace("\n", "")
        assertEquals(expected, EncodeKit.encodeBase62(input))
    }

    //===========================
    // Tests for URL encoding
    //===========================

    @Test
    fun urlEncode_SimpleStrings_CorrectlyEncoded() {
        val original = "hello world"
        val encoded = EncodeKit.urlEncode(original)
        // space -> '+'
        assertTrue(encoded.contains("hello+world") || encoded.contains("hello%20world"))
    }

    @Test
    fun urlDecode_EncodedStrings_ReturnsOriginal() {
        val original = "Kotlin & Encode/Decode?"
        val encoded = EncodeKit.urlEncode(original)
        val decoded = EncodeKit.urlDecode(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun urlEncode_ReservedAndUnicode_CorrectlyEncodedAndDecoded() {
        val original = "a+b/c?d=e&f=©"
        val encoded = EncodeKit.urlEncode(original)
        // Check that special chars are percent-encoded
        assertTrue(encoded.contains("%2B") && encoded.contains("%2F") && encoded.contains("%3D"))
        val decoded = EncodeKit.urlDecode(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun urlDecode_InvalidPercentSequence_ThrowsException() {
        // URLDecoder.decode will throw IllegalArgumentException on invalid percent
        assertFailsWith<IllegalArgumentException> { EncodeKit.urlDecode("%E0%") }
    }

}