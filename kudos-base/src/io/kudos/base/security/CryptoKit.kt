package io.kudos.base.security

import org.soul.base.security.CryptoTool

/**
 * 加密工具类
 *
 * 支持HMAC-SHA1消息签名 及 DES/AES对称加密.
 * 支持Hex与Base64两种编码方式.
 *
 * @author K
 * @since 1.0.0
 */
object CryptoKit {

    //-- HMAC-SHA1 funciton --//
    /**
     * 使用HMAC-SHA1进行消息签名, 返回字节数组,长度为20字节
     *
     * @param input 原始输入字符数组
     * @param key   HMAC-SHA1密钥
     * @return 签名后的字节数组
     * @author K
     * @since 1.0.0
     */
    fun hmacSha1(input: ByteArray, key: ByteArray): ByteArray = CryptoTool.hmacSha1(input, key)

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
    fun isMacValid(expected: ByteArray, input: ByteArray, key: ByteArray): Boolean =
        CryptoTool.isMacValid(expected, input, key)

    /**
     * 生成HMAC-SHA1密钥,返回字节数组,长度为160位(20字节).
     * HMAC-SHA1算法对密钥无特殊要求, RFC2401建议最少长度为160位(20字节).
     *
     * @return HMAC-SHA1密钥
     * @author K
     * @since 1.0.0
     */
    fun generateHmacSha1Key(): ByteArray = CryptoTool.generateHmacSha1Key()

    //region AES
    /**
     * 使用AES加密原始字符串，返回其十六进制表示
     *
     * @param input    原始输入字符串.
     * @param password 密钥字符串.
     * @return 加密后的字符串(十六进制表示)
     * @author K
     * @since 1.0.0
     */
    fun aesEncrypt(input: String, password: String): String = CryptoTool.aesEncrypt(input, password)

    /**
     * 将使用aes加密字节数组进行解密
     *
     * @param content  使用aes加密字节数组
     * @param password 加密时使用的密钥
     * @return 原字符串的字节数组
     * @author K
     * @since 1.0.0
     */
    fun aesDecrypt(content: ByteArray, password: String): ByteArray = CryptoTool.aesDecrypt(content, password)

    /**
     * 将使用aes加密并转为十六进制的字符串进行解密
     *
     * @param contentHex 使用aes加密并转为十六进制的字符串
     * @param password   加密时使用的密钥
     * @return 原字符串
     * @author K
     * @since 1.0.0
     */
    fun aesDecrypt(contentHex: String, password: String): String  = CryptoTool.aesDecrypt(contentHex, password)

    /**
     * 使用AES加密原始字符串，返回其十六进制表示
     *
     * @param input 原始输入字符串.
     * @return 加密后的字符串(十六进制表示)
     * @author K
     * @since 1.0.0
     */
    fun aesEncrypt(input: String): String = CryptoTool.aesEncrypt(input)

    /**
     * 将使用aes加密并转为十六进制的字符串进行解密，兼容未加密的历史数据
     *
     * @param contentHex 使用aes加密并转为十六进制的字符串
     * @return 原字符串
     * @author K
     * @since 1.0.0
     */
    fun aesDecrypt(contentHex: String): String = CryptoTool.aesDecrypt(contentHex)

    //endregion

    /**
     * 生成随机向量,默认大小为cipher.getBlockSize(), 16字节.
     *
     * @return 向量的字节数组
     * @author K
     * @since 1.0.0
     */
    fun generateIV(): ByteArray = CryptoTool.generateIV()

    /**
     * 将字节数组编码为十六进制表示的字符数组
     *
     * @param data 字节数组
     * @return 十六进制表示的字符数组
     * @author K
     * @since 1.0.0
     */
    fun encodeHex(data: ByteArray): CharArray = CryptoTool.encodeHex(data)

    /**
     * 将十六进制编码的字节数组解码
     *
     * @param bytes 十六进制编码的字节数组
     * @return 解码后的字节数组
     * @author K
     * @since 1.0.0
     */
    fun decodeHex(bytes: ByteArray): ByteArray = CryptoTool.decodeHex(bytes)

}