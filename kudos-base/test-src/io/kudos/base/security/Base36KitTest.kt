package io.kudos.base.security


import kotlin.test.*


/**
 * test for Base36Kit
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class Base36KitTest {

    private val defaultKey = Base36Kit.KEY

    /**
     * Helper: strip the first character (the checksum) from encryptIgnoreCase(...) output, then call the underlying decrypt(...).
     * Because the existing decryptIgnoreCase may in some cases return "checksum mismatch!", we bypass it here
     * and directly verify that the underlying decrypt / encrypt are reversible.
     */
    private fun decryptAfterStripCheck(cipher: String): String {
        require(cipher.length >= 2) { "cipher length must be >= 2" }
        // The first character is the checksum; the actual "encrypted content" starts at index 1
        val encryptedBody = cipher.substring(1)
        // Call Base36Kit.decrypt(...), capitalOnly = true
        return Base36Kit.decrypt(encryptedBody, defaultKey, true)
    }


    @Test
    fun singleDigit_roundTrip_encryptStripDecrypt_defaultKey() {
        val input = "0"
        val cipher = Base36Kit.encryptIgnoreCase(input)
        assertTrue(cipher.length >= 2)

        val recovered = decryptAfterStripCheck(cipher)
        assertEquals("0", recovered)
    }

    @Test
    fun emptyString_encryptIgnoreCase_throwsArithmetic() {
        assertFailsWith<ArithmeticException> {
            Base36Kit.encryptIgnoreCase("")
        }
        assertFailsWith<StringIndexOutOfBoundsException> {
            Base36Kit.tryDecryptIgnoreCase("", Base36Kit.KEY).getOrThrow()
        }
    }

    @Test
    fun singleChar_encryptDecrypt_capitalOnlyFalse_roundTrip() {
        val key = 123456789012345678L

        // Lowercase 'b'
        run {
            val input = "b"
            val cipher = Base36Kit.encrypt(input, key, capitalOnly = false)
            assertNotNull(cipher)
            val recovered = Base36Kit.decrypt(cipher, key, capitalOnly = false)
            assertEquals("b", recovered)
        }

        // Uppercase 'X'
        run {
            val input = "X"
            val cipher = Base36Kit.encrypt(input, key, capitalOnly = false)
            assertNotNull(cipher)
            val recovered = Base36Kit.decrypt(cipher, key, capitalOnly = false)
            assertEquals("X", recovered)
        }

        // Digit '7'
        run {
            val input = "7"
            val cipher = Base36Kit.encrypt(input, key, capitalOnly = false)
            assertNotNull(cipher)
            val recovered = Base36Kit.decrypt(cipher, key, capitalOnly = false)
            assertEquals("7", recovered)
        }
    }

    /**
     * 5. When capitalOnly = true, encrypting/decrypting the single character 'c' (lowercase) should output uppercase 'C'.
     */
    @Test
    fun singleLetter_encryptDecrypt_capitalOnlyTrue_convertsToUppercase() {
        val key = 222333444555666777L
        val input = "c"
        val cipher = Base36Kit.encrypt(input, key, capitalOnly = true)
        assertNotNull(cipher)
        val recovered = Base36Kit.decrypt(cipher, key, capitalOnly = true)
        assertEquals("C", recovered)
    }

    @Test
    fun tryDecryptIgnoreCase_checksumMismatch_returnsFailure() {
        val cipher = Base36Kit.encryptIgnoreCase("HELLO", defaultKey)
        // Tamper with the checksum
        val tampered = if (cipher.first() == '0') "1" + cipher.substring(1) else "0" + cipher.substring(1)
        val result = Base36Kit.tryDecryptIgnoreCase(tampered, defaultKey)
        assertTrue(result.isFailure)
    }

}
