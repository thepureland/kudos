package io.kudos.base.net

import io.kudos.base.logger.LogFactory
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

/**
 * IP工具类，支持ipv4和ipv6
 *
 * @author K
 * @since 1.0.0
 */
object IpKit {

    private val LOG = LogFactory.getLog(this)

    // 二进制32位为全1的整数值
    private const val ALL32ONE = 4294967295L

    /**
     * 验证指定IP地址是否合法的ipv4
     *
     * @param ip 待验证的ip串
     * @return true: 为合法的ipv4地址
     * @author K
     * @since 1.0.0
     */
    fun isValidIpv4(ip: String): Boolean {
        if (ip.isBlank()) return false
        val segments = ip.split('.')
        if (segments.size != 4) return false
        return segments.all { part ->
            part.toIntOrNull()?.let { it in 0..255 } == true
        }
    }

    /**
     * 将ipv4地址字符串转换为数字表示
     *
     * @param ipv4 ipv4地址
     * @return ipv4的数值表示，非ipv4返回-1
     * @author K
     * @since 1.0.0
     */
    fun ipv4StringToLong(ipv4: String): Long {
        if (!isValidIpv4(ipv4)) return -1
        return ipv4
            .split('.')
            .map { it.toLong() }
            .fold(0L) { acc, part -> (acc shl 8) or part }
    }

    /**
     * 将IP地址数字转换成字符串表示
     *
     * @param ipv4Long ipv4长整型值, 小于0或大于4294967295将返回空串
     * @return ipv4地址，参数小于0或大于4294967295将返回空串
     * @author K
     * @since 1.0.0
     */
    fun ipv4LongToString(ipv4Long: Long): String {
        var ipLong = ipv4Long
        if (ipLong !in 0..ALL32ONE) {
            return ""
        }
        val mask = 255L
        var result = ipLong and mask
        var temp = result.toString()
        (0..2).forEach { _ ->
            ipLong = ipLong shr 8
            result = ipLong and mask
            temp = "$result.$temp"
        }
        return temp
    }

    /**
     * 取得定长的ipv4地址(每个段不足三位在前面用0补足)。 例如: 1.2.13.224 => 001.002.013.224
     *
     * @param ipv4 待处理的ipv4，如果ip非法返回空串
     * @return 定长的ipv4地址
     * @author K
     * @since 1.0.0
     */
    fun getFixLengthIpv4(ipv4: String): String {
        if (!isValidIpv4(ipv4)) {
            return ""
        }
        val parts = ipv4.split(".")
        return parts.joinToString(".") { it.padStart(3, '0') }
    }

    /**
     * 将定长的ipv4还原(每个段去掉左边的0). 例如: 001.002.013.224 => 1.2.13.224
     *
     * @param ipv4 待处理的ipv4，如果ip非法返回空串
     * @return 非定长的ipv4
     * @author K
     * @since 1.0.0
     */
    fun getNormalIpv4(ipv4: String): String {
        if (!isValidIpv4(ipv4)) {
            return ""
        }
        val parts = ipv4.split(".")
        return parts.joinToString(".") { it.toInt().toString() }
    }

    /**
     * 检查指定的ipv4地址是否都在同一网段
     *
     * @param maskAddress 子网掩码地址，非法将返回false
     * @param ipv4s ipv4地址可变数组，为空或其中某个ip非法都将返回false
     * @return true: 指定的ipv4地址均在同一网段
     * @author K
     * @since 1.0.0
     */
    fun isSameIpv4Seg(maskAddress: String, vararg ipv4s: String): Boolean {
        if (maskAddress.isBlank() || ipv4s.isEmpty()) return false
        val maskIp = ipv4StringToLong(maskAddress)
        if (maskIp == -1L) return false
        val segValues = ipv4s.map { ipv4 ->
            val ipLong = ipv4StringToLong(ipv4)
            if (ipLong == -1L) return false
            maskIp and ipLong
        }
        return segValues.distinct().size == 1
    }

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
    fun getIpv4sBetween(beginIp: String, endIp: String): List<String> {
        var beginIpStr = beginIp
        var endIpStr = endIp
        if (beginIpStr.isEmpty() && endIpStr.isEmpty()) {
            return emptyList()
        }
        if (beginIpStr.isEmpty()) {
            beginIpStr = "0.0.0.0"
        }
        var longBeginIp = ipv4StringToLong(beginIpStr)
        if (longBeginIp == -1L) {
            return emptyList()
        }
        if (endIpStr.isEmpty()) {
            endIpStr = "255.255.255.255"
        }
        var longEndIp = ipv4StringToLong(endIpStr)
        if (longEndIp == -1L) {
            return emptyList()
        }
        if (longBeginIp > longEndIp) {
            val temp = longBeginIp
            longBeginIp = longEndIp
            longEndIp = temp
        }
        // 求解范围之内的IP地址
        val size = (longEndIp - longBeginIp).toInt() + 1
        if (size !in 0..65536) {
            return emptyList()
        } else if (size == 1) {
            return listOf(beginIpStr)
        }
        val ipList = ArrayList<String>(size)
        for (offset in 0 until size) {
            val longIp = longBeginIp + offset.toLong()
            ipList.add(ipv4LongToString(longIp))
        }
        return ipList
    }

    /**
     * 判断是否为本地ipv4地址。如：127.0.0.1、192.168.0.123
     *
     * @param ipv4 待检查的ipv4地址
     * @return true: 为本地ipv4地址
     * @author K
     * @since 1.0.0
     */
    fun isLocalIpv4(ipv4: String): Boolean {
        if ("127.0.0.1" == ipv4) {
            return true
        }
        val l = ipv4StringToLong(ipv4)
        return if (l >= 3232235520L) {
            l <= 3232301055L
        } else l in 167772160L..184549375L
    }

    /**
     * 返回本机的本地ip地址
     *
     * @return 本机的本地ip地址
     * @author K
     * @since 1.0.0
     */
    fun getLocalIp(): String {
        return try {
            InetAddress.getLocalHost().hostAddress
        } catch (e: UnknownHostException) {
            LOG.error(e)
            ""
        }
    }

    /**
     * 判断给定的字符串是否为有效的ipv6(包含冒分十六进制表示法、0位压缩表示法、内嵌IPv4地址表示法)
     *
     * @param ipStr ip串
     * @return true: ipv6，false: 非ipv6
     * @author K
     * @since 1.0.0
     */
    fun isValidIpv6(ipStr: String): Boolean {
        if (ipStr.isBlank()) return false
        if (isValidIpv4(ipStr)) return false
        return try {
            val pure = ipStr.substringBefore('%')
            val a = InetAddress.getByName(pure)
            a is Inet6Address || a.address.size == 16 || // 纯 IPv6
                    a.address.size == 4  // 让嵌 IPv4 的形式也通过，再交给 toFullIpv6 统一
        } catch (_: UnknownHostException) {
            false
        }
    }

    /**
     * 将任意 IPv4/IPv6 统一标准化为“全格式 IPv6”：
     * 8 组、每组 4 位十六进制（大写），组间冒号分隔。
     *
     * @param ip 原始 IP（可为 IPv4、IPv6 缩写、含内嵌 IPv4、可带 zone-id）
     * @return 形如 "0000:0000:0000:0000:0000:FFFF:C0A8:0001" 的全格式 IPv6
     * @throws IllegalArgumentException ip非法时
     * @author AI: ChatGPT
     * @author K
     * @since 1.0.0
     */
    fun toFullIpv6(ip: String): String {
        if (ip.isBlank()) throw IllegalArgumentException("ip is blank")

        // 去掉可能存在的 zone-id（如 fe80::1%eth0）
        val pure = ip.substringBefore('%')

        val addr = try {
            InetAddress.getByName(pure)
        } catch (e: UnknownHostException) {
            throw IllegalArgumentException("invalid IP: $ip", e)
        }

        var bytes = addr.address
        if (bytes.size == 4) {
            // IPv4 -> 统一映射为 IPv6 的 16 字节
            val v6 = ByteArray(16)
            v6[10] = 0xFF.toByte()
            v6[11] = 0xFF.toByte()
            System.arraycopy(bytes, 0, v6, 12, 4)
            bytes = v6
        } else if (bytes.size != 16) {
            throw IllegalArgumentException("unexpected address length")
        }

        // 16 字节 -> 8 组，每组 16 位；统一大写、每组补满 4 位
        val sb = StringBuilder(39)
        for (i in 0 until 16 step 2) {
            val value = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
            if (i > 0) sb.append(':')
            sb.append(String.format(Locale.ROOT, "%04X", value))
        }
        return sb.toString()
    }

}