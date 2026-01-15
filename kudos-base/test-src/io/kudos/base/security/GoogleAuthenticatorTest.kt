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

        // 默认 window_size = 3
        assertEquals(3, ga.windowSize, "初始 window_size 应为 3")

        // 设为合法值 1 - 17 之间
        ga.windowSize = 1
        assertEquals(1, ga.windowSize, "setWindowSize(1) 后，window_size 应改为 1")

        ga.windowSize = 17
        assertEquals(17, ga.windowSize, "setWindowSize(17) 后，window_size 应改为 17")

        // 设为非法值（<1、>17），不应该改变原有 window_size
        ga.windowSize = 0
        assertEquals(17, ga.windowSize, "setWindowSize(0) 无效，window_size 应保持为 17")

        ga.windowSize = 18
        assertEquals(17, ga.windowSize, "setWindowSize(18) 无效，window_size 应保持为 17")
    }

    @Test
    fun testGenerateSecretKey_notNull_and_decodable() {
        // GoogleAuthenticator.generateSecretKey 内部使用了固定的 SEED，对应的 SecureRandom 输出是确定的
        val secret = GoogleAuthenticator.generateSecretKey()
        assertNotNull(secret, "generateSecretKey 不应返回 null")

        // 用 Base32 解码后，byte 数组长度应该等于 SECRET_SIZE
        val codec = Base32()
        val decoded = codec.decode(secret)
        assertEquals(
            GoogleAuthenticator.SECRET_SIZE, decoded.size,
            "Base32.decode(generateSecretKey()) 后的长度应是 SECRET_SIZE = ${GoogleAuthenticator.SECRET_SIZE}"
        )
    }

    @Test
    fun testGetQRBarcodeURL_format() {
        val user = "testUser"
        val host = "example.com"
        val secret = "JBSWY3DPEHPK3PXP"  // 一个合法的 Base32 字符串
        val url = GoogleAuthenticator.getQRBarcodeURL(user, host, secret)

        // 格式化后应该为：
        // https://www.google.com/chart?chs=200x200&chld=M%7C0&cht=qr&chl=otpauth://totp/testUser@example.com%3Fsecret%3DJBSWY3DPEHPK3PXP
        val expected =
            "https://www.google.com/chart?chs=200x200&chld=M%7C0&cht=qr&chl=otpauth://totp/" +
                    "${user}@${host}%3Fsecret%3D${secret}"
        assertEquals(expected, url, "getQRBarcodeURL 返回的 URL 应当与预期格式一致")
    }

    @Test
    fun testCheckCode_withCorrectAndIncorrectValues() {
        // 1）先用 generateSecretKey 生成一个“确定性”的 secret
        val secret = GoogleAuthenticator.generateSecretKey()!!
        val codec = Base32()
        val decodedKey = codec.decode(secret)

        // 2）选一个固定时刻 timeMsec，比如 0（代表 Unix epoch），其对应的 TOTP 时间窗口 t = 0 / 1000 / 30 = 0
        val timeMsec = 0L
        val t = 0 / 30L

        // 3）通过反射调用私有的 verify_code(decodedKey, t) 来生成“正确”的 six-digit code
        val correctCodeObj = GoogleAuthenticator.verifyCode(decodedKey, t)
        // correctCodeObj 是一个 0..999999 之间的整数

        // 4）新建一个实例，默认 window_size = 3，可以在 [-3..3] 范围内校验
        val ga = GoogleAuthenticator()

        // 5.1）用正确的 code 去检查：应返回 true
        val resultTrue = ga.checkCode(secret, correctCodeObj.toLong(), timeMsec)
        assertTrue(resultTrue, "使用正确 code，在默认窗口大小下，check_code 应返回 true")

        // 5.2）用错误的 code 去检查：应返回 false
        val wrongCode = (correctCodeObj + 1) % 1000000  // 制造一个不同的验证码
        val resultFalse = ga.checkCode(secret, wrongCode.toLong(), timeMsec)
        assertFalse(resultFalse, "使用错误 code，应返回 false")

        // 5.3）在超出 window 范围的偏移时间内也应返回 false
        val outOfWindowT = ga.windowSize + 1L
        val outOfWindowCode = GoogleAuthenticator.verifyCode(decodedKey, outOfWindowT)
        val resultOutOfWindow = ga.checkCode(secret, outOfWindowCode.toLong(), timeMsec)
        assertFalse(resultOutOfWindow, "超出 window_size 范围的 code，应返回 false")
    }


    @Test
    fun testVerifyCode_directInvocation_forConsistency() {
        // 验证私有方法 verify_code 在不同时间窗口下行为合理。
        // 使用一个固定 key, 例如：全 0 的 10 个字节
        val key = ByteArray(10) { 0x00 }
        // 在 t=0 时应该有一个确定值
        val codeAt0 = GoogleAuthenticator.verifyCode(key, 0L)
        // 再次调用（同样 key、同样 t），值应该相同
        val codeAt02 = GoogleAuthenticator.verifyCode(key, 0L)
        assertEquals(codeAt0, codeAt02, "同一 key、同一 t，多次调用 verify_code 应返回相同值")
        // 对不同 t 值，code 应该不同（极小概率碰撞不测试）
        val codeAt1 = GoogleAuthenticator.verifyCode(key, 1L)
        assertNotEquals(codeAt0, codeAt1, "同一 key，但 t 不同，codeAt0 和 codeAt1 应不同")
    }

}