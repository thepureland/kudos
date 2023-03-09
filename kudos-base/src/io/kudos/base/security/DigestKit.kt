package io.kudos.base.security

import org.soul.base.security.DigestTool
import java.io.InputStream

/**
 * 支持SHA-1/MD5消息摘要的工具类
 * 返回ByteSource，可进一步被编码为Hex, Base64或UrlSafeBase64
 *
 * @author K
 * @since 1.0.0
 */
object DigestKit {

    //region MD5
    /**
     * 对字符串进行MD5加密
     *
     * @param original 源字符串
     * @param salt 盐
     * @return 加密并转成16进制后的字符串
     * @author K
     * @since 1.0.0
     */
    fun getMD5(original: String, salt: String): String = DigestTool.getMD5(original, salt)

    /**
     * 对字符串进行MD5加密
     *
     * @param original 源字符串字节数组
     * @param salt     盐
     * @return 加密并转成16进制后的字符串
     * @author K
     * @since 1.0.0
     */
    fun getMD5(original: ByteArray?, salt: String?): String? = DigestTool.getMD5(original, salt)

    /**
     * 测试字符串用md5加密后是否和给定的md5串相等
     *
     * @param str 未加密的串
     * @param salt 盐
     * @param md5Str 加密后的串
     * @return true:源串加密后与给定的md5串相等，反之为false
     * @author K
     * @since 1.0.0
     */
    fun isMatchMD5(str: String, salt: String, md5Str: String): Boolean = DigestTool.isMatchMD5(str, salt, md5Str)

    /**
     * 对文件进行md5散列.
     *
     * @param input 文件输入流
     * @return 散列后的文件字节数组
     * @author K
     * @since 1.0.0
     */
    fun md5(input: InputStream): ByteArray = DigestTool.md5(input)

    //endregion

    //region SHA1

    /**
     * 对输入字符串字节数组进行sha1散列.
     *
     * @param input 字符串字节数组
     * @param salt 加盐值字节数组
     * @return 进行sha1散列后的字节数组
     * @author K
     * @since 1.0.0
     */
    fun sha1(input: ByteArray, salt: ByteArray? = null): ByteArray = DigestTool.sha1(input, salt)

    /**
     * 对输入字符串字节数组进行sha1散列.
     *
     * @param input 字符串字节数组
     * @param salt 加盐值字节数组
     * @param iterations 迭代次数
     * @return 进行sha1散列后的字节数组
     * @author K
     * @since 1.0.0
     */
    fun sha1(input: ByteArray, salt: ByteArray, iterations: Int): ByteArray = DigestTool.sha1(input, salt, iterations)

    /**
     * 对文件进行sha1散列.
     *
     * @param input 文件输入流
     * @return 散列后的文件字节数组
     * @author K
     * @since 1.0.0
     */
    fun sha1(input: InputStream): ByteArray = DigestTool.sha1(input)

    //endregion

    /**
     * 对字符串进行散列, 支持md5与sha1算法.
     *
     * @param input 字符串字节数组
     * @param algorithm 算法名称，ALGORITHM_SHA1或ALGORITHM_MD5
     * @param salt 加盐值字节数组
     * @param iterations 迭代次数
     * @return 进行散列后的字节数组
     * @author K
     * @since 1.0.0
     */
    fun digest(input: ByteArray, algorithm: String, salt: ByteArray?, iterations: Int): ByteArray =
        DigestTool.digest(input, algorithm, salt, iterations)

    /**
     * 生成随机的Byte[]作为salt.
     *
     * @param numBytes byte数组的大小
     * @return 加盐值字节数组
     * @author K
     * @since 1.0.0
     */
    fun generateSalt(numBytes: Int): ByteArray = DigestTool.generateSalt(numBytes)

}