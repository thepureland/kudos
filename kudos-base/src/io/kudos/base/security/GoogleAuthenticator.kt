package io.kudos.base.security

import org.apache.commons.codec.binary.Base32
import org.apache.commons.codec.binary.Base64
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

/**
 * 谷歌动态验证器
 *
 * @since 1.0.0
 */
class GoogleAuthenticator {
    var windowSize = 3 // default 3 - max 17 (from google docs)最多可偏移的时间

        /**
         * set the windows size. This is an integer value representing the number of 30 second windows
         * we allow
         * The bigger the window, the more tolerant of clock skew we are.
         * @param s window size - must be >=1 and <=17. Other values are ignored
         */
        set(s) {
            if (s in 1..17) {
                field = s
            }
        }

    /**
     * Check the code entered by the user to see if it is valid
     * @param secret The users secret.
     * @param code The code displayed on the users device
     * @param timeMsec The time in msec (System.currentTimeMillis() for example)
     * @return
     */
    fun checkCode(secret: String, code: Long, timeMsec: Long): Boolean {
        val codec = Base32()
        val decodedKey = codec.decode(secret)
        // convert unix msec time into a 30 second "window"
        // this is per the TOTP spec (see the RFC for details)
        val t = timeMsec / 1000L / 30L
        // Window is used to check codes generated in the near past.
        // You can use this value to tune how far you're willing to go.
        for (i in -windowSize..windowSize) {
            val hash: Long = try {
                verifyCode(decodedKey, t + i).toLong()
            } catch (e: NoSuchAlgorithmException) {
                throw IllegalStateException("TOTP algorithm not available", e)
            } catch (e: InvalidKeyException) {
                throw IllegalStateException("Invalid TOTP secret key", e)
            }
            if (hash == code) {
                return true
            }
        }
        // The validation code is invalid.
        return false
    }

    companion object {
        // taken from Google pam docs - we probably don't need to mess with these
        const val SECRET_SIZE = 10
        const val SEED = "g8GjEvTbW5oVSV7avLBdwIHqGlUYNzKFI7izOF8GwLDVKs2m0QN7vxRs2im5MDaNCWGmcD2rvcZx"
        const val RANDOM_NUMBER_ALGORITHM = "SHA1PRNG"

        /**
         * Generate a random secret key. This must be saved by the server and associated with the
         * users account to verify the code displayed by Google Authenticator.
         * The user must register this secret on their device.
         * @return secret key
         */
        fun generateSecretKey(): String? {
            var sr: SecureRandom?
            try {
                sr = SecureRandom.getInstance(RANDOM_NUMBER_ALGORITHM)
                sr.setSeed(Base64.decodeBase64(SEED))
                val buffer = sr.generateSeed(SECRET_SIZE)
                val codec = Base32()
                val bEncodedKey = codec.encode(buffer)
                return String(bEncodedKey)
            } catch (_: NoSuchAlgorithmException) {
                // should never occur... configuration error
            }
            return null
        }

        /**
         * Return a URL that generates and displays a QR barcode. The user scans this bar code with the
         * Google Authenticator application on their smartphone to register the auth code. They can also
         * manually enter the
         * secret if desired
         * @param user user id (e.g. fflinstone)
         * @param host host or system that the code is for (e.g. myapp.com)
         * @param secret the secret that was previously generated for this user
         * @return the URL for the QR code to scan
         */
        fun getQRBarcodeURL(user: String?, host: String?, secret: String?): String {
            val format =
                "https://www.google.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=otpauth://totp/%s@%s%%3Fsecret%%3D%s"
            return String.format(format, user, host, secret)
        }

        @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
        internal fun verifyCode(key: ByteArray, t: Long): Int {
            val data = ByteArray(8)
            var value = t
            run {
                var i = 8
                while (i-- > 0) {
                    data[i] = value.toByte()
                    value = value ushr 8
                }
            }
            val signKey = SecretKeySpec(key, "HmacSHA1")
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(signKey)
            val hash = mac.doFinal(data)
            val offset: Int = hash[20 - 1].and(0xF).toInt()
            // RFC 6238 dynamic truncation：取 offset 起 4 字节作为 31-bit 无符号整数。
            // 注意：`hash[i].toLong()` 会做符号扩展，必须先 `.toInt() and 0xFF` 转成无符号字节再拼。
            // 之前的写法 `hash[i].and(0xFF.toByte()).toLong()` 中 `0xFF.toByte()` 是 -1（仍是有符号字节），
            // byte AND 等于 byte 本身，再 toLong 仍然 sign-extend——导致 byte ≥ 0x80 时上位被污染、
            // 与所有标准 TOTP App（Google Authenticator / Authy / 1Password 等）算出的码不一致。
            // bug 详见 PassportServiceTest.currentTotpCode 注释。
            val truncatedHash: Long =
                ((hash[offset].toInt() and 0x7F).toLong() shl 24) or
                ((hash[offset + 1].toInt() and 0xFF).toLong() shl 16) or
                ((hash[offset + 2].toInt() and 0xFF).toLong() shl 8) or
                (hash[offset + 3].toInt() and 0xFF).toLong()
            return (truncatedHash % 1000000L).toInt()
        }
    }
}