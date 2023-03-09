package io.kudos.base.net

import org.soul.base.net.IpTool


/**
 * IP工具类，支持ipv4和ipv6
 *
 * @author K
 * @since 1.0.0
 */
object IpKit {

    /**
     * 验证指定IP地址是否合法的ipv4
     *
     * @param ip 待验证的ip串
     * @return true: 为合法的ipv4地址
     * @author K
     * @since 1.0.0
     */
    fun isValidIpv4(ip: String): Boolean = IpTool.isValidIpv4(ip)

    /**
     * 将ipv4地址字符串转换为数字表示
     *
     * @param ipv4 ipv4地址
     * @return ipv4的数值表示，非ipv4返回-1
     * @author K
     * @since 1.0.0
     */
    fun ipv4StringToLong(ipv4: String): Long = IpTool.ipv4StringToLong(ipv4)

    /**
     * 将IP地址数字转换成字符串表示
     *
     * @param ipv4Long ipv4长整型值, 小于0或大于4294967295将返回空串
     * @return ipv4地址，参数小于0或大于4294967295将返回空串
     * @author K
     * @since 1.0.0
     */
    fun ipv4LongToString(ipv4Long: Long): String = IpTool.ipv4LongToString(ipv4Long)

    /**
     * 取得定长的ipv4地址(每个段不足三位在前面用0补足)。 例如: 1.2.13.224 => 001.002.013.224
     *
     * @param ipv4 待处理的ipv4，如果ip非法返回空串
     * @return 定长的ipv4地址
     * @author K
     * @since 1.0.0
     */
    fun getFixLengthIpv4(ipv4: String): String = IpTool.getFixLengthIpv4(ipv4)

    /**
     * 将定长的ipv4还原(每个段去掉左边的0). 例如: 001.002.013.224 => 1.2.13.224
     *
     * @param ipv4 待处理的ipv4，如果ip非法返回空串
     * @return 非定长的ipv4
     * @author K
     * @since 1.0.0
     */
    fun getNormalIpv4(ipv4: String): String = IpTool.getNormalIpv4(ipv4)

    /**
     * 检查指定的ipv4地址是否都在同一网段
     *
     * @param maskAddress 子网掩码地址，非法将返回false
     * @param ipv4s ipv4地址可变数组，为空或其中某个ip非法都将返回false
     * @return true: 指定的ipv4地址均在同一网段
     * @author K
     * @since 1.0.0
     */
    fun isSameIpv4Seg(maskAddress: String, vararg ipv4s: String): Boolean = IpTool.isSameIpv4Seg(maskAddress, *ipv4s)

    /**
     * 返回指定的两个ipv4地址(大小不分先后)间的所有ipv4地址,
     * 包括指定的两个ipv4地址，按从小到大的顺序, 两个ip一样将只返回一个
     *
     * 只支持最多65536个的ip地址，超过的话将返回空列表
     *
     * @param beginIp 开始值，包括, 非法ip将返回空列表
     * @param endIp 结束值，包括, 非法ip将返回空列表
     * @return 一个包含指定的两个ipv4地址间的所有ipv4地址的列表, 两个参数任一个非法或超过65536个的ip地址将返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getIpv4sBetween(beginIp: String, endIp: String): List<String> = IpTool.getIpv4sBetween(beginIp, endIp)

    /**
     * 判断是否为本地ipv4地址。如：127.0.0.1、192.168.0.123
     *
     * @param ipv4 待检查的ipv4地址
     * @return true: 为本地ipv4地址
     * @author K
     * @since 1.0.0
     */
    fun isLocalIpv4(ipv4: String): Boolean = IpTool.isLocalIpv4(ipv4)

    /**
     * 返回本机的本地ip地址
     *
     * @return 本机的本地ip地址
     * @author K
     * @since 1.0.0
     */
    fun getLocalIp(): String = IpTool.getLocalIp()

    /**
     * 判断给定的字符串是否为有效的ipv6(包含冒分十六进制表示法、0位压缩表示法、内嵌IPv4地址表示法)
     *
     * @param ipStr ip串
     * @return true: ipv6，false: 非ipv6
     * @author K
     * @since 1.0.0
     */
    fun isValidIpv6(ipStr: String): Boolean = IpTool.isValidIpv6(ipStr)

    /**
     * 将ipv4转为标准全格式的ipv6(每组定长4位的16进制，共8组，每组间用半角冒号分隔)
     *
     * @param ipv4 ipv4地址
     * @return 如果传入参数不是ipv4，将返回原始传入串
     * @author K
     * @since 1.0.0
     */
    fun ipv4ToIpv6(ipv4: String): String = IpTool.ipv4ToIpv6(ipv4)

    /**
     * 标准化ip地址为全格式的ipv6地址(每组定长4位的16进制，共8组，每组间用半角冒号分隔)
     * 对以下情况的参数进行标准化，其他情况将直接返回原始参数：
     * 1. ipv4，如192.168.0.1 => 0000:0000:0000:0000:0000:0000:C0A8:0001
     * 2. 0位压缩表示法的ipv6，如FF01::1101 => FF01:0000:0000:0000:0000:0000:0000:1101、
     * ::1 => 0000:0000:0000:0000:0000:0000:0000:0001、
     * :: => 0000:0000:0000:0000:0000:0000:0000:0000、
     * FF01:0:0:0:0:0:0:1101 => FF01:0000:0000:0000:0000:0000:0000:1101
     * 3. 内嵌IPv4地址表示法的ipv6，如::192.168.0.1  => 0000:0000:0000:0000:0000:0000:C0A8:0001 、
     * ::FFFF:192.168.0.1 => 0000:0000:0000:0000:0000:FFFF:C0A8:0001
     *
     * @param ipStr ip地址，可以是ipv4或ipv6的三种表示法(冒分十六进制表示法、0位压缩表示法、内嵌IPv4地址表示法)
     * @return 标准全格式的ipv6
     * @author K
     * @since 1.0.0
     */
    fun standardizeIpv6(ipStr: String): String? = IpTool.standardizeIpv6(ipStr)

}