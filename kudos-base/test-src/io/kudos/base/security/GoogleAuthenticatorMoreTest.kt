package io.kudos.base.security

import kotlin.test.*

/**
 * Extra coverage for [GoogleAuthenticator], complementing GoogleAuthenticatorTest.
 *
 * Targets the getQRBarcodeURL null-argument branch and the constants exposed on the companion.
 *
 * @author K
 * @since 1.0.0
 */
internal class GoogleAuthenticatorMoreTest {

    @Test
    fun getQRBarcodeURL_withNullArguments_formatsLiteralNull() {
        val url = GoogleAuthenticator.getQRBarcodeURL(null, null, null)
        assertEquals(
            "https://www.google.com/chart?chs=200x200&chld=M%7C0&cht=qr&chl=otpauth://totp/" +
                "null@null%3Fsecret%3Dnull",
            url
        )
    }

    @Test
    fun companionConstantsAreStable() {
        assertEquals(10, GoogleAuthenticator.SECRET_SIZE)
        assertEquals("SHA1PRNG", GoogleAuthenticator.RANDOM_NUMBER_ALGORITHM)
        assertTrue(GoogleAuthenticator.SEED.isNotBlank())
    }

    @Test
    fun checkCode_withInvalidBase32Secret_returnsFalse() {
        // Base32 decoder yields an empty/odd key; HmacSHA1 still initializes, so checkCode
        // simply finds no matching window and returns false (no exception expected here).
        val ga = GoogleAuthenticator()
        val result = ga.checkCode("ABCDEFGHIJKLMNOP", 123456L, 0L)
        assertFalse(result)
    }

    @Test
    fun verifyCode_isDeterministicForKnownVector() {
        // 10 zero bytes, t=0 — deterministic value, repeatable
        val key = ByteArray(10)
        val a = GoogleAuthenticator.verifyCode(key, 0L)
        val b = GoogleAuthenticator.verifyCode(key, 0L)
        assertEquals(a, b)
        assertTrue(a in 0..999999)
    }

    @Test
    fun windowSize_negativeAndZeroIgnored() {
        val ga = GoogleAuthenticator()
        ga.windowSize = 5
        ga.windowSize = -1   // ignored
        assertEquals(5, ga.windowSize)
        ga.windowSize = 0    // ignored
        assertEquals(5, ga.windowSize)
        ga.windowSize = 100  // ignored (>17)
        assertEquals(5, ga.windowSize)
    }
}
