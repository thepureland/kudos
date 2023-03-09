package io.kudos.base.lang.string

import org.soul.base.lang.string.EncodeTool

/**
 * 封装各种格式的编码解码工具类
 *
 * <p>
 * 1.Commons-Codec的 hex/base64 编码
 * 2.自制的base62 编码
 * 3.JDK提供的URLEncoder
 * </p>
 *
 * @author K
 * @since 1.0.0
 */
object EncodeKit {

    /**
     * Hex编码
     *
     * @param input 待Hex编码的字节数组
     * @return 编码后的字符串
     * @author K
     * @since 1.0.0
     */
    fun encodeHex(input: ByteArray): String = EncodeTool.encodeHex(input)

    /**
     * Hex解码
     *
     * @param input 待Hex解码的字符串
     * @return 解码后的字节数组
     * @author K
     * @since 1.0.0
     */
    fun decodeHex(input: String): ByteArray = EncodeTool.decodeHex(input)

    /**
     * Base64编码
     *
     * @param input 待Base64编码的字节数组
     * @return 编码后的字符串
     * @author K
     * @since 1.0.0
     */
    fun encodeBase64(input: ByteArray): String = EncodeTool.encodeBase64(input)

    /**
     * Base64编码, URL安全(将Base64中的URL非法字符'+'和'/'转为'-'和'_', 见RFC3548).
     *
     * @param input 待Base64编码的字节数组
     * @return 编码后的字符串
     * @author K
     * @since 1.0.0
     */
    fun encodeUrlSafeBase64(input: ByteArray): String = EncodeTool.encodeUrlSafeBase64(input)

    /**
     * Base64解码
     *
     * @param input 待Hex解码的字符串
     * @return 解码后的字节数组
     * @author K
     * @since 1.0.0
     */
    fun decodeBase64(input: String): ByteArray = EncodeTool.decodeBase64(input)

    /**
     * Base62编码
     *
     * @param input 待Base62编码的字节数组
     * @return 编码后的字符串
     * @author K
     * @since 1.0.0
     */
    fun encodeBase62(input: ByteArray): String = EncodeTool.encodeBase62(input)

    /**
     * URL 编码, Encode默认为UTF-8.
     *
     * @param part 待编码的部分
     * @return 编码完的字符串
     * @author K
     * @since 1.0.0
     */
    fun urlEncode(part: String): String = EncodeTool.urlEncode(part)

    /**
     * URL 解码, Encode默认为UTF-8.
     *
     * @param part 待解码的部分
     * @return 解码后的字符串
     * @author K
     * @since 1.0.0
     */
    fun urlDecode(part: String): String = EncodeTool.urlDecode(part)

}