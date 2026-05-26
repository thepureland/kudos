package io.kudos.base.security

import io.kudos.base.lang.string.EncodeKit
import org.apache.commons.lang3.Validate
import java.io.InputStream
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Utility class supporting SHA-1 / MD5 message digests.
 * Returns a ByteSource that may be further encoded as Hex, Base64, or UrlSafeBase64.
 *
 * @author K
 * @since 1.0.0
 */
object DigestKit {

    /** SHA-1 algorithm name. */
    const val SHA1 = "SHA-1"
    /** MD5 algorithm name. */
    const val MD5 = "MD5"

    /** Strong random source used to generate salt. */
    private val random = SecureRandom()

    //region MD5
    /**
     * MD5-encrypts a string.
     *
     * @param original source string
     * @param salt salt
     * @return the encrypted string in hex form
     * @author K
     * @since 1.0.0
     */
    fun getMD5(original: String, salt: String): String {
        val s = salt.toByteArray()
        val md5s = digest(original.toByteArray(), MD5, s, 1)
        return EncodeKit.encodeHex(md5s)
    }

    /**
     * MD5-encrypts a string.
     *
     * @param original source string byte array
     * @param salt     salt
     * @return the encrypted string in hex form
     * @author K
     * @since 1.0.0
     */
    fun getMD5(original: ByteArray?, salt: String?): String? {
        val input = original ?: return null
        if (input.isEmpty()) {
            return null
        }
        val s = salt?.toByteArray()
        val md5s = digest(input, MD5, s, 1)
        return EncodeKit.encodeHex(md5s)
    }

    /**
     * Tests whether the MD5 encryption of a string equals a given MD5 string.
     *
     * @param str unencrypted string
     * @param salt salt
     * @param md5Str encrypted string
     * @return true: the encrypted source string equals the given MD5 string; false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isMatchMD5(str: String, salt: String, md5Str: String): Boolean =
        getMD5(str, salt) == md5Str
            // handle legacy data that was not salted previously
            || (salt.isNotBlank() && getMD5(str, "") == md5Str)

    /**
     * Computes an MD5 hash of a file.
     *
     * @param input file input stream
     * @return the hashed file byte array
     * @author K
     * @since 1.0.0
     */
    fun md5(input: InputStream): ByteArray = digest(input, MD5)

    //endregion

    //region SHA1
    /**
     * SHA-1 hashes the input string byte array.
     *
     * @param input string byte array
     * @return SHA-1 hashed byte array
     * @author K
     * @since 1.0.0
     */
    fun sha1(input: ByteArray): ByteArray = digest(input, SHA1, null, 1)

    /**
     * SHA-1 hashes the input string byte array.
     *
     * @param input string byte array
     * @param salt salt byte array
     * @return SHA-1 hashed byte array
     * @author K
     * @since 1.0.0
     */
    fun sha1(input: ByteArray, salt: ByteArray): ByteArray = digest(input, SHA1, salt, 1)

    /**
     * SHA-1 hashes the input string byte array.
     *
     * @param input string byte array
     * @param salt salt byte array
     * @param iterations iteration count
     * @return SHA-1 hashed byte array
     * @author K
     * @since 1.0.0
     */
    fun sha1(input: ByteArray, salt: ByteArray, iterations: Int): ByteArray = digest(input, SHA1, salt, iterations)

    /**
     * Computes a SHA-1 hash of a file.
     *
     * @param input file input stream
     * @return the hashed file byte array
     * @author K
     * @since 1.0.0
     */
    fun sha1(input: InputStream): ByteArray = digest(input, SHA1)

    //endregion

    /**
     * Digests a streaming input, reading in 8KB chunks to avoid loading large files entirely into memory.
     * Does not close the passed stream; the caller is responsible for closing it.
     *
     * @param input input stream
     * @param algorithm algorithm name ([SHA1] or [MD5])
     * @return digest byte array
     * @author K
     * @since 1.0.0
     */
    private fun digest(input: InputStream, algorithm: String): ByteArray {
        val messageDigest = MessageDigest.getInstance(algorithm)
        val bufferLength = 8 * 1024
        val buffer = ByteArray(bufferLength)
        var read = input.read(buffer, 0, bufferLength)
        while (read > -1) {
            messageDigest.update(buffer, 0, read)
            read = input.read(buffer, 0, bufferLength)
        }
        return messageDigest.digest()
    }

    /**
     * Hashes a string, supporting MD5 and SHA-1 algorithms.
     *
     * @param input string byte array
     * @param algorithm algorithm name, ALGORITHM_SHA1 or ALGORITHM_MD5
     * @param salt salt byte array
     * @param iterations iteration count
     * @return hashed byte array
     * @author K
     * @since 1.0.0
     */
    fun digest(input: ByteArray, algorithm: String, salt: ByteArray?, iterations: Int): ByteArray {
        val digest = MessageDigest.getInstance(algorithm)
        salt?.let(digest::update)
        var result = digest.digest(input)
        repeat(iterations - 1) {
            digest.reset()
            result = digest.digest(result)
        }
        return result
    }

    /**
     * Generates a random Byte[] for use as salt.
     *
     * @param numBytes size of the byte array
     * @return salt byte array
     * @author K
     * @since 1.0.0
     */
    fun generateSalt(numBytes: Int): ByteArray {
        Validate.isTrue(numBytes > 0, "numBytes argument must be a positive integer (1 or larger)", numBytes)
        val bytes = ByteArray(numBytes)
        random.nextBytes(bytes)
        return bytes
    }

}