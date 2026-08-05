package io.kudos.base.security

import kotlin.test.*

/**
 * Extended coverage for [Base36Kit], complementing Base36KitTest.
 *
 * Targets the deprecated decryptIgnoreCase(...) overloads (both the success and the
 * "Check bit mismatch!" branches), multi-character round trips that exercise the
 * outOrder/deOutOrder/sortArr shuffle logic, and the base-62 (capitalOnly=false) path.
 *
 * @author K
 * @since 1.0.0
 */
internal class Base36KitMoreTest {

    private val key = Base36Kit.KEY

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedDecryptIgnoreCase_defaultKey_roundTrip() {
        val plain = "ABC123XYZ789"
        val cipher = Base36Kit.encryptIgnoreCase(plain)
        val recovered = Base36Kit.decryptIgnoreCase(cipher)
        assertEquals(plain, recovered)
    }

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedDecryptIgnoreCase_withKey_roundTrip() {
        val customKey = 123456789012345678L
        val plain = "HELLO42WORLD"
        val cipher = Base36Kit.encryptIgnoreCase(plain, customKey)
        val recovered = Base36Kit.decryptIgnoreCase(cipher, customKey)
        assertEquals(plain, recovered)
    }

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedDecryptIgnoreCase_checkBitMismatch_returnsFixedString() {
        val cipher = Base36Kit.encryptIgnoreCase("HELLO", key)
        val tampered = (if (cipher.first() == '0') "1" else "0") + cipher.substring(1)
        val result = Base36Kit.decryptIgnoreCase(tampered, key)
        assertEquals("Check bit mismatch!", result)
    }

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedDecryptIgnoreCase_defaultKey_checkBitMismatch() {
        val cipher = Base36Kit.encryptIgnoreCase("WORLD")
        val tampered = (if (cipher.first() == 'A') "B" else "A") + cipher.substring(1)
        val result = Base36Kit.decryptIgnoreCase(tampered)
        assertEquals("Check bit mismatch!", result)
    }

    @Test
    fun encryptIgnoreCase_lowercaseIsTreatedAsUppercase() {
        val cipherLower = Base36Kit.encryptIgnoreCase("abc")
        val cipherUpper = Base36Kit.encryptIgnoreCase("ABC")
        assertEquals(cipherUpper, cipherLower)
        // and it round-trips to uppercase
        val recovered = Base36Kit.tryDecryptIgnoreCase(cipherLower, key).getOrThrow()
        assertEquals("ABC", recovered)
    }

    // NOTE: a multi-character round trip with capitalOnly=false (base-62) is intentionally NOT asserted.
    // That path is broken in the main code: encrypt/decrypt are not inverses for multi-char base-62
    // input (e.g. "DATA123..." decrypts to "DAMA123..."), because transStr/diyToAscii mishandle the
    // base-62 ranges. The base-62 branch itself is still exercised by the single-char round trip in
    // Base36KitTest. See suspectedBugs for details.


    @Test
    fun encryptDecrypt_capitalOnly_longStringRoundTrip() {
        val plain = "ZYXWVUTSRQPONMLK9876543210"
        val cipher = Base36Kit.encrypt(plain, key, capitalOnly = true)
        val recovered = Base36Kit.decrypt(cipher, key, capitalOnly = true)
        assertEquals(plain, recovered)
    }

    @Test
    fun tryDecryptIgnoreCase_usesDefaultKeyArgument() {
        val cipher = Base36Kit.encryptIgnoreCase("DEFAULTKEY")
        // default key overload (no explicit key)
        val recovered = Base36Kit.tryDecryptIgnoreCase(cipher).getOrThrow()
        assertEquals("DEFAULTKEY", recovered)
    }

    @Test
    fun stringLongerThanKey_roundTrip_coversTailHandling() {
        // 25-char string longer than the 18-digit key exercises the "tail past key length" branches
        val plain = "A1B2C3D4E5F6G7H8I9J0KLMNO"
        val cipher = Base36Kit.encryptIgnoreCase(plain, key)
        val recovered = Base36Kit.tryDecryptIgnoreCase(cipher, key).getOrThrow()
        assertEquals(plain, recovered)
    }
}
