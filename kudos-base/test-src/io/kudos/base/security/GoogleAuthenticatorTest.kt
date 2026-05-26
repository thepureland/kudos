package io.kudos.base.security

import org.apache.commons.codec.binary.Base32
import kotlin.test.*
import kotlin.test.DefaultAsserter.assertNotNull

/**
 * test for GoogleAuthenticator
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
class GoogleAuthenticatorTest {

    @Test
    fun testSetWindowSize_valid_and_invalid() {
        val ga = GoogleAuthenticator()

        // Default window_size = 3
        assertEquals(3, ga.windowSize, "initial window_size should be 3")

        // Set to legal values between 1 and 17
        ga.windowSize = 1
        assertEquals(1, ga.windowSize, "after setWindowSize(1), window_size should be 1")

        ga.windowSize = 17
        assertEquals(17, ga.windowSize, "after setWindowSize(17), window_size should be 17")

        // Set to illegal values (<1, >17) should not change the existing window_size
        ga.windowSize = 0
        assertEquals(17, ga.windowSize, "setWindowSize(0) is invalid; window_size should remain 17")

        ga.windowSize = 18
        assertEquals(17, ga.windowSize, "setWindowSize(18) is invalid; window_size should remain 17")
    }

    @Test
    fun testGenerateSecretKey_notNull_and_decodable() {
        // GoogleAuthenticator.generateSecretKey internally uses a fixed SEED, so the SecureRandom output is deterministic
        val secret = GoogleAuthenticator.generateSecretKey()
        assertNotNull(secret, "generateSecretKey should not return null")

        // After Base32 decoding, the byte-array length should equal SECRET_SIZE
        val codec = Base32()
        val decoded = codec.decode(secret)
        assertEquals(
            GoogleAuthenticator.SECRET_SIZE, decoded.size,
            "length of Base32.decode(generateSecretKey()) should be SECRET_SIZE = ${GoogleAuthenticator.SECRET_SIZE}"
        )
    }

    @Test
    fun testGetQRBarcodeURL_format() {
        val user = "testUser"
        val host = "example.com"
        val secret = "JBSWY3DPEHPK3PXP"  // a valid Base32 string
        val url = GoogleAuthenticator.getQRBarcodeURL(user, host, secret)

        // The formatted URL should be:
        // https://www.google.com/chart?chs=200x200&chld=M%7C0&cht=qr&chl=otpauth://totp/testUser@example.com%3Fsecret%3DJBSWY3DPEHPK3PXP
        val expected =
            "https://www.google.com/chart?chs=200x200&chld=M%7C0&cht=qr&chl=otpauth://totp/" +
                    "${user}@${host}%3Fsecret%3D${secret}"
        assertEquals(expected, url, "the URL returned by getQRBarcodeURL should match the expected format")
    }

    @Test
    fun testCheckCode_withCorrectAndIncorrectValues() {
        // 1) First use generateSecretKey to generate a "deterministic" secret
        val secret = GoogleAuthenticator.generateSecretKey()!!
        val codec = Base32()
        val decodedKey = codec.decode(secret)

        // 2) Pick a fixed instant timeMsec, e.g. 0 (the Unix epoch); the corresponding TOTP time window t = 0 / 1000 / 30 = 0
        val timeMsec = 0L
        val t = 0 / 30L

        // 3) Invoke the private verify_code(decodedKey, t) (via reflection) to generate the "correct" six-digit code
        val correctCodeObj = GoogleAuthenticator.verifyCode(decodedKey, t)
        // correctCodeObj is an integer in [0..999999]

        // 4) Create a new instance with the default window_size = 3, allowing validation in the [-3..3] window
        val ga = GoogleAuthenticator()

        // 5.1) Verify with the correct code: should return true
        val resultTrue = ga.checkCode(secret, correctCodeObj.toLong(), timeMsec)
        assertTrue(resultTrue, "using the correct code with the default window size, check_code should return true")

        // 5.2) Verify with an incorrect code: should return false
        val wrongCode = (correctCodeObj + 1) % 1000000  // build a different verification code
        val resultFalse = ga.checkCode(secret, wrongCode.toLong(), timeMsec)
        assertFalse(resultFalse, "using an incorrect code should return false")

        // 5.3) A code at an offset outside the window range should also return false
        // To avoid occasional flakiness due to a tiny-probability TOTP collision, first collect all valid in-window codes
        // and then find an out-of-window code that does not belong to the window for the assertion.
        val validWindowCodes = (-ga.windowSize..ga.windowSize)
            .map { offset -> GoogleAuthenticator.verifyCode(decodedKey, t + offset.toLong()) }
            .toSet()
        var outOfWindowT = ga.windowSize + 1L
        var outOfWindowCode = GoogleAuthenticator.verifyCode(decodedKey, outOfWindowT)
        while (outOfWindowCode in validWindowCodes) {
            outOfWindowT++
            outOfWindowCode = GoogleAuthenticator.verifyCode(decodedKey, outOfWindowT)
        }
        val resultOutOfWindow = ga.checkCode(secret, outOfWindowCode.toLong(), timeMsec)
        assertFalse(resultOutOfWindow, "a code outside window_size should return false")
    }


    @Test
    fun testVerifyCode_directInvocation_forConsistency() {
        // Verify that the private method verify_code behaves reasonably across different time windows.
        // Use a fixed key, e.g. 10 bytes of all zeros
        val key = ByteArray(10) { 0x00 }
        // At t=0 there should be a deterministic value
        val codeAt0 = GoogleAuthenticator.verifyCode(key, 0L)
        // Invoking again (same key, same t) should return the same value
        val codeAt02 = GoogleAuthenticator.verifyCode(key, 0L)
        assertEquals(codeAt0, codeAt02, "multiple calls to verify_code with the same key and t should return the same value")
        // For different t values, the code should differ (tiny-probability collisions are not tested)
        val codeAt1 = GoogleAuthenticator.verifyCode(key, 1L)
        assertNotEquals(codeAt0, codeAt1, "same key but different t: codeAt0 and codeAt1 should differ")
    }

}
