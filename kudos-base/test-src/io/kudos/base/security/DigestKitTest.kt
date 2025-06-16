package io.kudos.base.security

import io.kudos.base.lang.string.EncodeKit
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import kotlin.test.*
import kotlin.test.DefaultAsserter.assertNotNull


/**
 * test for DigestKit
 *
 * @author ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class DigestKitTest {

    @Test
    fun sha1() {
        val input = "user"
        val salt = DigestKit.generateSalt(8) // 随机盐

        assertEquals(
            "12dea96fec20593566ab75692c9949596833adc9", EncodeKit.encodeHex(DigestKit.sha1(input.toByteArray()))
        )

        println(EncodeKit.encodeHex(DigestKit.sha1(input.toByteArray(), salt)))
        println(EncodeKit.encodeHex(DigestKit.sha1(input.toByteArray(), salt, 1024)))
    }

    companion object {
        private const val EXPECTED_SHA1_HELLO = "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d"
        private const val EXPECTED_MD5_SALT_HELLO = "06decc8b095724f80103712c235586be"
        private const val EXPECTED_MD5_HELLO      = "5d41402abc4b2a76b9719d911017c592"
    }

    @Test
    fun testGetMD5_withSalt() {
        // 使用 String 版本带盐测试
        val md5Hex = DigestKit.getMD5("hello", "salt")
        assertEquals(EXPECTED_MD5_SALT_HELLO, md5Hex, "MD5(原文 + 盐) 应当与预期一致")
    }

    @Test
    fun testGetMD5_byteArray_nullOrEmpty() {
        // original 为 null，应该返回 null
        val resultNull = DigestKit.getMD5(null, "anySalt")
        assertNull(resultNull, "传入 original 为 null，应返回 null")

        // original 为空字节数组，也应该返回 null
        val emptyBytes = ByteArray(0)
        val resultEmpty = DigestKit.getMD5(emptyBytes, "salt")
        assertNull(resultEmpty, "传入 original 为空字节数组，应返回 null")
    }

    @Test
    fun testGetMD5_byteArray_withoutSalt() {
        // original 不为 null，但 salt 传入 null 时，相当于无盐
        val originalBytes = "hello".toByteArray(Charsets.UTF_8)
        val md5HexNoSalt = DigestKit.getMD5(originalBytes, null)
        assertNotNull(md5HexNoSalt, "当 original 不为空且 salt 为 null 时，不应返回 null")
        assertEquals(EXPECTED_MD5_HELLO, md5HexNoSalt, "MD5(原文) 应当与无盐预期值一致")
    }

    @Test
    fun testIsMatchMD5_withCorrectSalt() {
        // 正常匹配：使用相同的原文和盐计算出的 MD5 与给定 md5Str 相等
        val md5Hex = DigestKit.getMD5("hello", "salt")
        assertTrue(DigestKit.isMatchMD5("hello", "salt", md5Hex), "正确的原文 + 盐，应匹配上 md5Str")
    }

    @Test
    fun testIsMatchMD5_withoutSaltHistoricalData() {
        // 当指定盐加密不匹配，但无盐加密匹配时，也应返回 true（兼容历史无盐数据）
        val md5NoSalt = DigestKit.getMD5("hello", "") // 本质上等同于 MD5("hello")
        // 故意传入错误盐 "wrong"，使得第一步匹配失败，但第二步使用空盐匹配成功
        assertTrue(DigestKit.isMatchMD5("hello", "wrong", md5NoSalt),
            "first use salt wrong 计算失败，second 用空盐计算成功，整体应返回 true")
    }

    @Test
    fun testMd5_inputStream() {
        // 使用 ByteArrayInputStream 模拟文件输入流
        val data = "abcdefg".toByteArray(Charsets.UTF_8)
        val expectedMd5Bytes = MessageDigest.getInstance(DigestKit.MD5).digest(data)
        val resultBytes = DigestKit.md5(ByteArrayInputStream(data))
        assertTrue(expectedMd5Bytes.contentEquals(resultBytes), "输入流 MD5 应与预期一致")
    }

    @Test
    fun testSha1_byteArray_noSalt() {
        // 纯 SHA-1("hello")
        val result = DigestKit.sha1("hello".toByteArray(Charsets.UTF_8))
        // 将 result 转为十六进制之后比较
        val hex = EncodeKit.encodeHex(result)
        assertEquals(EXPECTED_SHA1_HELLO, hex, "SHA-1(hello) 应与预期一致")
    }

    @Test
    fun testSha1_byteArray_withSaltAndIterations() {
        // 验证带盐和多次迭代的 sha1
        val input = "hello".toByteArray(Charsets.UTF_8)
        val salt = "salt".toByteArray(Charsets.UTF_8)
        val iterations = 3

        // 手动计算：第一次 result1 = SHA1(salt || input)
        val md = MessageDigest.getInstance(DigestKit.SHA1)
        md.update(salt)
        val first = md.digest(input)
        // 第二次 result2 = SHA1(result1)
        md.reset()
        val second = md.digest(first)
        // 第三次 result3 = SHA1(result2)
        md.reset()
        val third = md.digest(second)

        val expectedHex = EncodeKit.encodeHex(third)
        val actual = DigestKit.sha1(input, salt, iterations)
        val actualHex = EncodeKit.encodeHex(actual)

        assertEquals(expectedHex, actualHex, "带盐并迭代 $iterations 次的 SHA-1 应与手动计算一致")
    }

    @Test
    fun testSha1_inputStream() {
        // 使用 ByteArrayInputStream 测试文件 SHA-1
        val data = "kotlinTest".toByteArray(Charsets.UTF_8)
        // 预先计算 SHA-1("kotlinTest")
        val expectedBytes = MessageDigest.getInstance(DigestKit.SHA1).digest(data)
        val resultBytes = DigestKit.sha1(ByteArrayInputStream(data))
        assertTrue(expectedBytes.contentEquals(resultBytes), "输入流 SHA-1 应与预期一致")
    }

    @Test
    fun testGenerateSalt_validLength() {
        // 测试生成不同长度的 salt，并且两次调用结果应不同
        val salt1 = DigestKit.generateSalt(16)
        val salt2 = DigestKit.generateSalt(16)
        assertEquals(16, salt1.size, "生成的 salt 长度应为 16")
        assertEquals(16, salt2.size, "生成的 salt 长度应为 16")
        assertFalse(salt1.contentEquals(salt2), "多次调用 generateSalt(16) 应得到不一样的随机结果")
    }

    @Test
    fun testGenerateSalt_invalidLength_throws() {
        // numBytes <= 0 时，应抛出 IllegalArgumentException（由 Validate.isTrue 触发）
        assertFailsWith<IllegalArgumentException> {
            DigestKit.generateSalt(0)
        }
        assertFailsWith<IllegalArgumentException> {
            DigestKit.generateSalt(-5)
        }
    }

    @Test
    fun testDigest_privateMethod_behavior_equivalence() {
        // 测试 digest(ByteArray, algorithm, null, 1) 等价于手动 MD5/SHA1
        val data = "digestTest".toByteArray(Charsets.UTF_8)

        // 测试 MD5
        val expectedMd5 = MessageDigest.getInstance(DigestKit.MD5).digest(data)
        val actualMd5 = DigestKit.digest(data, DigestKit.MD5, null, 1)
        assertTrue(expectedMd5.contentEquals(actualMd5), "DigestKit.digest MD5 应与 MessageDigest 计算一致")

        // 测试 SHA-1
        val expectedSha1 = MessageDigest.getInstance(DigestKit.SHA1).digest(data)
        val actualSha1 = DigestKit.digest(data, DigestKit.SHA1, null, 1)
        assertTrue(expectedSha1.contentEquals(actualSha1), "DigestKit.digest SHA-1 应与 MessageDigest 计算一致")
    }

}