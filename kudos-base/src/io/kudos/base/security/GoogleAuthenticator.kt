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
 * Google Authenticator dynamic verifier.
 *
 * @since 1.0.0
 */
class GoogleAuthenticator {
    /**
     * Radius of "time windows" allowed during TOTP verification (each window is 30 seconds).
     * The default of 3 allows clock drift of 3 windows on either side; Google docs recommend a maximum of 17.
     */
    var windowSize = 3 // default 3 - max 17 (from google docs); maximum allowed time drift

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
        /** Byte length of the shared secret (per Google pam recommendation); about 16 chars when Base32-encoded. */
        const val SECRET_SIZE = 10
        /** Initialization seed for SecureRandom (Base64-encoded), used to derive the key generator. */
        const val SEED = "g8GjEvTbW5oVSV7avLBdwIHqGlUYNzKFI7izOF8GwLDVKs2m0QN7vxRs2im5MDaNCWGmcD2rvcZx"
        /** Random number generator algorithm name. */
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

        /**
         * Compute the 6-digit dynamic code for a given time window per RFC 6238 (TOTP) / RFC 4226 (HOTP).
         *
         * Key detail: calling `toLong()` on `hash[i]` directly will sign-extend; we must first do `toInt() and 0xFF`
         * to assemble an unsigned byte. Otherwise, when a byte is >= 0x80, the high bits get polluted and the
         * resulting code will not match standard clients (Google Authenticator / Authy / 1Password, etc.).
         *
         * @param key the decoded shared secret
         * @param t the current time-window number (milliseconds / 1000 / 30)
         * @return the 6-digit integer dynamic code (0..999999)
         * @throws NoSuchAlgorithmException if the current JDK does not support HmacSHA1
         * @throws InvalidKeyException if the key cannot be used to initialize HmacSHA1
         */
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
            // RFC 6238 dynamic truncation: take 4 bytes starting from `offset` as a 31-bit unsigned integer.
            // Note: `hash[i].toLong()` sign-extends; must first do `.toInt() and 0xFF` to obtain an unsigned byte before assembling.
            // The previous form `hash[i].and(0xFF.toByte()).toLong()` is wrong because `0xFF.toByte()` is -1 (still a signed byte),
            // byte AND yields the byte itself, and `toLong` still sign-extends -- so bytes >= 0x80 pollute the high bits,
            // producing codes that do not match standard TOTP apps (Google Authenticator / Authy / 1Password, etc.).
            // See the PassportServiceTest.currentTotpCode comment for details on the bug.
            val truncatedHash: Long =
                ((hash[offset].toInt() and 0x7F).toLong() shl 24) or
                ((hash[offset + 1].toInt() and 0xFF).toLong() shl 16) or
                ((hash[offset + 2].toInt() and 0xFF).toLong() shl 8) or
                (hash[offset + 3].toInt() and 0xFF).toLong()
            return (truncatedHash % 1000000L).toInt()
        }
    }
}