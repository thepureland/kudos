package io.kudos.base.security

import org.soul.base.security.Base36

/**
 * 目的：根据输入的秘钥，对提供的字符串进行加密，和对以此加密规则生成的密文解密
 * 说明：encrypt和decrypt为完全对称的设计，你也可以把decrypt作为加密函数，使用
 *      encrypt函数来还原。
 * 用法：myEncrypt为一个使用例子，实际使用可以根据需要多次调用encrypt、outOrder；
 *      解密时，使用相同的顺序调用decrypt、deOutOrder即可还原。
 * 要求：输入的字符串只能包含数字和字母，key为不超过18位的正整数
 *
 * 修订记录
 * 版本  时间        作者     操作
 * 1.00  2016/04/22  Leisure  创建此类，包括加密，解密函数
 * 1.10  2016/04/22  Leisure  增加字符转换功能，生成的密文不再是有意义的字符组合
 * 1.11  2016/04/22  Leisure  秘钥由int改为Long类型，秘钥长度由最高9位升级为18位
 * 1.12  2016/04/23  Leisure  重构字符串排序方法
 * 1.20  2016/04/23  Leisure  增加MD5校验位
 * 1.21  2016/04/23  Leisure  修正了一个bug，使用自定义编码保证密文的格式与原文兼容
 * 1.22  2016/04/23  Leisure  字符转换功能兼容大小写
 * 1.23  2016/04/25  Leisure  校验位提取到myEncrypt,myDecrypt.原加密解密函数对称
 * 1.24  2016/04/25  Leisure  支持大写字母和数字组合转换后仍为大写字母和数字组合
 * 2.00  2013/04/26  Leisure  修改函数名，避免歧义；修改函数的作用域。撒手不管版
 */
object Base36Kit {

    /**
     * 对源字符串进行加密，并在头部增加一个字符作为源字符串的校验码
     * 要求源字符串只包含大写字母和数字，小写字母将按大写字母处理
     * 使用默认的加密Key
     * @param src
     * @return
     */
    fun encryptIgnoreCase(src: String): String = Base36.encryptIgnoreCase(src)

    /**
     * 对源字符串进行加密，并在头部增加一个字符作为源字符串的校验码
     * 要求源字符串只包含大写字母和数字，小写字母将按大写字母处理
     * @param src
     * @param key
     * @return
     */
    fun encryptIgnoreCase(src: String, key: Long): String = Base36.encryptIgnoreCase(src, key)

    /**
     * 接收含有校验位的加密字符串，对其解密，并验证与校验位是否匹配
     * 使用默认的加密Key
     * @param srcString
     * @return
     */
    fun decryptIgnoreCase(srcString: String): String = Base36.decryptIgnoreCase(srcString)

    /**
     * 接收含有校验位的加密字符串，对其解密，并验证与校验位是否匹配
     * @param src
     * @param key
     * @return
     */
    fun decryptIgnoreCase(src: String, key: Long): String = Base36.decryptIgnoreCase(src, key)

    /**
     * 加密函数
     * @param src
     * @param key
     * @param capitalOnly
     * @return
     */
    fun encrypt(src: String, key: Long, capitalOnly: Boolean): String = Base36.encrypt(src, key, capitalOnly)

    /**
     * 解密函数
     * @param src
     * @param key
     * @param capitalOnly
     * @return
     */
    fun decrypt(src: String, key: Long, capitalOnly: Boolean): String = Base36.decrypt(src, key, capitalOnly)

}