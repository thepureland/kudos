package io.kudos.base.lang.string

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Wraps various encoding/decoding utilities.
 *
 * <p>
 * 1. Hex/Base64 encoding via Commons-Codec
 * 2. Custom Base62 encoding
 * 3. JDK-provided URLEncoder
 * </p>
 *
 * @author K
 * @since 1.0.0
 */
object EncodeKit {

    private const val DEFAULT_URL_ENCODING = "UTF-8"
    private val BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray()

    /**
     * Hex encoding.
     *
     * @param input the byte array to hex-encode
     * @return the encoded string
     * @author K
     * @since 1.0.0
     */
    fun encodeHex(input: ByteArray): String = Hex.encodeHexString(input)

    /**
     * Hex decoding.
     *
     * @param input the hex string to decode
     * @return the decoded byte array
     * @author K
     * @since 1.0.0
     */
    fun decodeHex(input: String): ByteArray = Hex.decodeHex(input.toCharArray())

    /**
     * Base64 encoding.
     *
     * @param input the byte array to Base64-encode
     * @return the encoded string
     * @author K
     * @since 1.0.0
     */
    fun encodeBase64(input: ByteArray): String = Base64.encodeBase64String(input)

    /**
     * Base64 encoding, URL-safe (replaces the URL-unsafe characters '+' and '/' in Base64 with '-' and '_', per RFC3548).
     *
     * @param input the byte array to Base64-encode
     * @return the encoded string
     * @author K
     * @since 1.0.0
     */
    fun encodeUrlSafeBase64(input: ByteArray): String = Base64.encodeBase64URLSafeString(input)

    /**
     * Base64 decoding.
     *
     * @param input the Base64 string to decode
     * @return the decoded byte array
     * @author K
     * @since 1.0.0
     */
    fun decodeBase64(input: String): ByteArray = Base64.decodeBase64(input)

    /**
     * Base62 encoding.
     *
     * @param input the byte array to Base62-encode
     * @return the encoded string
     * @author K
     * @since 1.0.0
     */
    fun encodeBase62(input: ByteArray): String =
        String(CharArray(input.size) { BASE62[(input[it].toInt() and 0xFF) % BASE62.size] })

    /**
     * URL encoding; the encoding defaults to UTF-8.
     *
     * @param part the part to encode
     * @return the encoded string
     * @author K
     * @since 1.0.0
     */
    fun urlEncode(part: String): String = URLEncoder.encode(part, DEFAULT_URL_ENCODING)

    /**
     * URL decoding; the encoding defaults to UTF-8.
     *
     * @param part the part to decode
     * @return the decoded string
     * @author K
     * @since 1.0.0
     */
    fun urlDecode(part: String): String = URLDecoder.decode(part, DEFAULT_URL_ENCODING)

}
