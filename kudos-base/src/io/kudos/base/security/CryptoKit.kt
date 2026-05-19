package io.kudos.base.security

import io.kudos.base.lang.string.EncodeKit
import io.kudos.base.logger.LogFactory
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 加密工具类
 *
 * 支持 HMAC-SHA1 消息签名及 AES 对称加密。
 * 支持 Hex 与 Base64 两种编码方式（本类主要暴露 Hex）。
 *
 * **AES 说明**：新产生的密文使用 **AES-128-GCM**（随机 IV，认证加密）。读路径仍支持旧版 **AES/ECB** 及
 * 历史 **SHA1PRNG 派生密钥** 数据，以便兼容存量数据。
 *
 * @author K
 * @since 1.0.0
 */
object CryptoKit {

    /** 日志器，用于在兼容路径上记录降级解密失败等告警信息 */
    private val logger = LogFactory.getLog(this::class)

    /** AES 算法名 */
    private const val AES_ALGORITHM = "AES"
    /** 与历史 `Cipher.getInstance("AES")` 行为一致，显式写出便于审阅。 */
    private const val AES_ECB_TRANSFORM = "AES/ECB/PKCS5Padding"
    /** AES-GCM 认证加密变换名，无填充 */
    private const val AES_GCM_TRANSFORM = "AES/GCM/NoPadding"
    /** HMAC-SHA1 算法名 */
    private const val HMACSHA1 = "HmacSHA1"
    /** 触发“密文大小写翻转”的密钥前缀（向后兼容老约定） */
    private const val HTB = "_HTB"

    /** [aesEncrypt]/[aesDecrypt] 无参形式使用的密文标记前缀，用于区分历史明文与加密内容 */
    private const val PREFIX = "┼"
    /** 字符串与字节互转使用的字符集 */
    private const val CHARSET = "UTF-8"

    /** HMAC-SHA1 默认密钥长度（RFC2401 建议值） */
    private const val DEFAULT_HMACSHA1_KEYSIZE = 160 // RFC2401

    /** [generateIV] 生成的随机 IV 默认字节数（与 AES 块大小一致） */
    private const val DEFAULT_IVSIZE = 16

    /** 新格式密文前缀：ASCII 「GCM」+ 版本字节 0x01。 */
    private val AES_GCM_MAGIC = byteArrayOf(0x47, 0x43, 0x4D, 0x01)

    /** GCM 模式 IV 长度（NIST 推荐 12 字节） */
    private const val GCM_IV_LENGTH = 12
    /** GCM 认证标签长度（比特），128 位为标准长度 */
    private const val GCM_TAG_LENGTH_BITS = 128

    /** Hex 编码使用的小写字符表 */
    private val DIGITS = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    )

    /** 强随机源，用于 IV 与密钥生成 */
    private val random = SecureRandom()

    //-- HMAC-SHA1 function --//
    /**
     * 使用HMAC-SHA1进行消息签名, 返回字节数组,长度为20字节
     *
     * @param input 原始输入字符数组
     * @param key   HMAC-SHA1密钥
     * @return 签名后的字节数组
     * @author K
     * @since 1.0.0
     */
    fun hmacSha1(input: ByteArray, key: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(key, HMACSHA1)
        val mac = Mac.getInstance(HMACSHA1)
        mac.init(secretKey)
        return mac.doFinal(input)
    }

    /**
     * 校验HMAC-SHA1签名是否正确.
     *
     * @param expected 已存在的签名
     * @param input    原始输入字符串
     * @param key      密钥
     * @return true: HMAC-SHA1签名正确
     * @author K
     * @since 1.0.0
     */
    fun isMacValid(expected: ByteArray, input: ByteArray, key: ByteArray): Boolean {
        val actual = hmacSha1(input, key)
        return expected.contentEquals(actual)
    }

    /**
     * 生成HMAC-SHA1密钥,返回字节数组,长度为160位(20字节).
     * HMAC-SHA1算法对密钥无特殊要求, RFC2401建议最少长度为160位(20字节).
     *
     * @return HMAC-SHA1密钥
     * @author K
     * @since 1.0.0
     */
    fun generateHmacSha1Key(): ByteArray {
        val keyGenerator = KeyGenerator.getInstance(HMACSHA1)
        keyGenerator.init(DEFAULT_HMACSHA1_KEYSIZE)
        val secretKey = keyGenerator.generateKey()
        return secretKey.encoded
    }

    //region AES
    /**
     * 使用 AES-GCM 加密原始字符串，返回其十六进制表示（内含魔数前缀与 IV，**非** ECB 旧格式）。
     *
     * @param input    原始输入字符串.
     * @param password 密钥字符串.
     * @return 加密后的字符串(十六进制表示)
     * @author K
     * @since 1.0.0
     */
    fun aesEncrypt(input: String, password: String): String {
        val plain = input.toByteArray(charset(CHARSET))
        val passwordBytes = password.toByteArray(charset(CHARSET))
        val packed = aesGcmEncrypt(plain, passwordBytes)
        return aesEncryptBytes(packed, password)
    }

    /**
     * 将使用 aes 加密字节数组进行解密（自动识别 GCM 新格式与 ECB/SHA1PRNG 旧格式）.
     *
     * @param content  使用 aes 加密字节数组
     * @param password 加密时使用的密钥
     * @return 原字符串的字节数组
     * @author K
     * @since 1.0.0
     */
    fun aesDecrypt(content: ByteArray, password: String): ByteArray {
        val passwordBytes = password.toByteArray(charset(CHARSET))
        if (content.size >= AES_GCM_MAGIC.size &&
            content.copyOfRange(0, AES_GCM_MAGIC.size).contentEquals(AES_GCM_MAGIC)
        ) {
            return aesGcmDecrypt(content, passwordBytes)
        }
        return runCatching {
            aesEcb(content, passwordBytes, Cipher.DECRYPT_MODE)
        }.getOrElse {
            // Backward compatibility: fallback for historical SHA1PRNG-derived data.
            aesLegacy(content, passwordBytes, Cipher.DECRYPT_MODE)
        }
    }

    /**
     * 使用 AES-128-GCM 对明文加密。
     * 每次随机生成 12 字节 IV，输出格式为 `AES_GCM_MAGIC || IV || ciphertext+tag`。
     *
     * @param plain 待加密明文字节
     * @param password 密码字节，将通过 [buildAesKey] 派生为 128 位密钥
     * @return 带魔数前缀与 IV 的密文字节数组
     * @author K
     * @since 1.0.0
     */
    private fun aesGcmEncrypt(plain: ByteArray, password: ByteArray): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, buildAesKey(password), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ct = cipher.doFinal(plain)
        return AES_GCM_MAGIC + iv + ct
    }

    /**
     * 解密由 [aesGcmEncrypt] 产生的 GCM 密文。
     * 期望输入开头匹配 [AES_GCM_MAGIC]，紧随 12 字节 IV，再是密文与认证标签。
     *
     * @param input 含魔数与 IV 的 GCM 密文
     * @param password 密码字节，须与加密时相同
     * @return 解密后的明文字节数组
     * @throws IllegalArgumentException 当输入长度不足以包含魔数与 IV 时
     * @author K
     * @since 1.0.0
     */
    private fun aesGcmDecrypt(input: ByteArray, password: ByteArray): ByteArray {
        val headerLen = AES_GCM_MAGIC.size + GCM_IV_LENGTH
        require(input.size > headerLen) { "Invalid AES-GCM payload length" }
        val iv = input.copyOfRange(AES_GCM_MAGIC.size, headerLen)
        val ct = input.copyOfRange(headerLen, input.size)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, buildAesKey(password), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ct)
    }

    /**
     * AES/ECB/PKCS5Padding 的加解密（旧格式兼容路径）。
     * 仅用于读取历史数据，不应再用于新数据加密。
     *
     * @param input 待处理数据
     * @param password 密码字节，将通过 [buildAesKey] 派生为 128 位密钥
     * @param mode [Cipher.ENCRYPT_MODE] 或 [Cipher.DECRYPT_MODE]
     * @return 处理后的字节数组
     * @author K
     * @since 1.0.0
     */
    private fun aesEcb(input: ByteArray, password: ByteArray, mode: Int): ByteArray {
        val cipher = Cipher.getInstance(AES_ECB_TRANSFORM)
        cipher.init(mode, buildAesKey(password))
        return cipher.doFinal(input)
    }

    /**
     * 最旧版本派生密钥路径：通过 SHA1PRNG 以 password 作种子生成 128 位密钥。
     * 仅在 [aesEcb] 直接解密失败时降级使用，用于读取最早的存量密文。
     *
     * @param input 待解密数据
     * @param password 密码字节，作为 SHA1PRNG 的种子
     * @param mode [Cipher.ENCRYPT_MODE] 或 [Cipher.DECRYPT_MODE]
     * @return 处理后的字节数组
     * @author K
     * @since 1.0.0
     */
    private fun aesLegacy(input: ByteArray, password: ByteArray, mode: Int): ByteArray {
        val generator = KeyGenerator.getInstance(AES_ALGORITHM)
        val secureRandom = SecureRandom.getInstance("SHA1PRNG")
        secureRandom.setSeed(password)
        generator.init(128, secureRandom)
        val cipher = Cipher.getInstance(AES_ECB_TRANSFORM)
        cipher.init(mode, generator.generateKey())
        return cipher.doFinal(input)
    }

    /**
     * 通过 SHA-256 对密码做一次摘要并截取前 16 字节作为 AES-128 密钥。
     * 既保证密钥长度合法，也避免直接暴露密码字节。
     *
     * @param password 原始密码字节
     * @return 可直接喂给 [Cipher.init] 的 AES 密钥
     * @author K
     * @since 1.0.0
     */
    private fun buildAesKey(password: ByteArray): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256").digest(password)
        val key = digest.copyOf(16) // AES-128
        return SecretKeySpec(key, AES_ALGORITHM)
    }

    /**
     * 将使用 aes 加密并转为十六进制的字符串进行解密。
     *
     * 与 [tryAesDecrypt] 的关系：本方法在解密失败时记录告警并返回空字符串，以保持历史调用约定；
     * 若需区分失败与空明文，请使用 [tryAesDecrypt]。
     *
     * @param contentHex 使用 aes 加密并转为十六进制的字符串
     * @param password   加密时使用的密钥
     * @return 原字符串；解密失败时返回 `""`
     * @author K
     * @since 1.0.0
     */
    fun aesDecrypt(contentHex: String, password: String): String {
        return tryAesDecrypt(contentHex, password).getOrElse {
            logger.warn("AES decrypt failed, returning empty string for compatibility: {0}", it.message)
            ""
        }
    }

    /**
     * 将使用 aes 加密并转为十六进制的字符串进行解密，返回 [Result] 以显式表达失败语义。
     *
     * @param contentHex 使用 aes 加密并转为十六进制的字符串
     * @param password   加密时使用的密钥
     * @return 成功时返回明文字符串，失败时封装异常
     */
    fun tryAesDecrypt(contentHex: String, password: String): Result<String> {
        return runCatching {
            val bytes = aesDecryptBytes(contentHex, password)
            val result = aesDecrypt(bytes, password)
            String(result, charset(CHARSET))
        }
    }

    /**
     * 使用 AES 加密原始字符串，返回其十六进制表示（含 [PREFIX] 前缀，内层为 GCM 密文）.
     *
     * @param input 原始输入字符串.
     * @return 加密后的字符串(十六进制表示)
     * @author K
     * @since 1.0.0
     */
    fun aesEncrypt(input: String): String = PREFIX + aesEncrypt(input, CryptoKey.KEY_DEFAULT)

    /**
     * 将使用 aes 加密并转为十六进制的字符串进行解密，兼容未加密的历史数据
     *
     * @param contentHex 使用 aes 加密并转为十六进制的字符串
     * @return 原字符串
     * @author K
     * @since 1.0.0
     */
    fun aesDecrypt(contentHex: String): String {
        return if (contentHex.startsWith(PREFIX)) { // 有加密过的
            val content = contentHex.replaceFirst(PREFIX.toRegex(), "")
            aesDecrypt(content, CryptoKey.KEY_DEFAULT)
        } else { // 未加密的历史数据
            contentHex
        }
    }

    //endregion

    /**
     * 生成随机向量,默认大小为cipher.getBlockSize(), 16字节.
     *
     * @return 向量的字节数组
     * @author K
     * @since 1.0.0
     */
    fun generateIV(): ByteArray {
        val bytes = ByteArray(DEFAULT_IVSIZE)
        random.nextBytes(bytes)
        return bytes
    }

    /**
     * 将字节数组编码为十六进制表示的字符数组
     *
     * @param data 字节数组
     * @return 十六进制表示的字符数组
     * @author K
     * @since 1.0.0
     */
    fun encodeHex(data: ByteArray): CharArray {
        val l = data.size
        val out = CharArray(l shl 1)
        // two characters form the hex value.
        var i = 0
        var j = 0
        while (i < l) {
            out[j++] = DIGITS[0xF0 and data[i].toInt() ushr 4]
            out[j++] = DIGITS[0x0F and data[i].toInt()]
            i++
        }
        return out
    }

    /**
     * 将十六进制编码的字节数组解码（输入为 ASCII 十六进制字符的字节表示，如 UTF-8 下的 `0-9a-fA-F`）.
     *
     * @param bytes 十六进制编码的字节数组
     * @return 解码后的字节数组
     * @author K
     * @since 1.0.0
     */
    fun decodeHex(bytes: ByteArray): ByteArray {
        val iLen = bytes.size
        require(iLen % 2 == 0) { "Hex string length must be even, got $iLen" }
        val arrOut = ByteArray(iLen / 2)
        var i = 0
        var o = 0
        while (i < iLen) {
            val hi = hexNibble(bytes[i])
            val lo = hexNibble(bytes[i + 1])
            arrOut[o++] = ((hi shl 4) or lo).toByte()
            i += 2
        }
        return arrOut
    }

    /**
     * 把单字节 ASCII 十六进制字符（`0-9` `a-f` `A-F`）转为 0..15 的整数。
     *
     * @param b ASCII 十六进制字符的字节表示
     * @return 该字符对应的十进制值
     * @throws NumberFormatException 当字节不是合法的十六进制字符时
     * @author K
     * @since 1.0.0
     */
    private fun hexNibble(b: Byte): Int {
        val c = b.toInt() and 0xFF
        return when {
            c in 48..57 -> c - 48
            c in 97..102 -> c - 97 + 10
            c in 65..70 -> c - 65 + 10
            else -> throw NumberFormatException("Invalid hex digit: $c")
        }
    }

    /**
     * 判断解密方式：转换大小写
     *
     * @param input
     * @param password
     * @return
     */
    private fun aesDecryptBytes(input: String, password: String): ByteArray {
        return if (password.isNotBlank() && password.startsWith(HTB)) {
            EncodeKit.decodeHex(input.lowercase())
        } else {
            EncodeKit.decodeHex(input)
        }
    }

    /**
     * 判断加密方式：转换大小写
     *
     * @param bytes
     * @param password
     * @return
     */
    private fun aesEncryptBytes(bytes: ByteArray, password: String): String {
        return if (password.isNotBlank() && password.startsWith(HTB)) {
            EncodeKit.encodeHex(bytes).uppercase()
        } else {
            EncodeKit.encodeHex(bytes)
        }
    }

}
