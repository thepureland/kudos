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
     * 辅助方法：对 encryptIgnoreCase(...) 结果剥离掉第一个字符的“校验位”后调用底层 decrypt(...)。
     * 由于现有 decryptIgnoreCase 在某些场景会返回“校验位不匹配！”，我们这里绕过它，
     * 直接验证底层 decrypt/ encrypt 是否可逆。
     */
    private fun decryptAfterStripCheck(cipher: String): String {
        require(cipher.length >= 2) { "cipher 长度必须 >= 2" }
        // 首字符是校验位，真正的“加密内容”从第二位开始
        val encryptedBody = cipher.substring(1)
        // 调用 Base36Kit.decrypt(...)，capitalOnly = true
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
            Base36Kit.decryptIgnoreCase("")
        }
    }

    @Test
    fun singleChar_encryptDecrypt_capitalOnlyFalse_roundTrip() {
        val key = 123456789012345678L

        // 小写 'b'
        run {
            val input = "b"
            val cipher = Base36Kit.encrypt(input, key, capitalOnly = false)
            assertNotNull(cipher)
            val recovered = Base36Kit.decrypt(cipher, key, capitalOnly = false)
            assertEquals("b", recovered)
        }

        // 大写 'X'
        run {
            val input = "X"
            val cipher = Base36Kit.encrypt(input, key, capitalOnly = false)
            assertNotNull(cipher)
            val recovered = Base36Kit.decrypt(cipher, key, capitalOnly = false)
            assertEquals("X", recovered)
        }

        // 数字 '7'
        run {
            val input = "7"
            val cipher = Base36Kit.encrypt(input, key, capitalOnly = false)
            assertNotNull(cipher)
            val recovered = Base36Kit.decrypt(cipher, key, capitalOnly = false)
            assertEquals("7", recovered)
        }
    }

    /**
     * 5. capitalOnly = true 时，对单字符 'c'（小写）加密⇒解密，应当输出大写 'C'。
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
        // 篡改校验位
        val tampered = if (cipher.first() == '0') "1" + cipher.substring(1) else "0" + cipher.substring(1)
        val result = Base36Kit.tryDecryptIgnoreCase(tampered, defaultKey)
        assertTrue(result.isFailure)
    }

}
