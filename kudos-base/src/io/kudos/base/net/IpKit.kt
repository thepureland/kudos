package io.kudos.base.net

import io.kudos.base.logger.LogFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Locale

/**
 * IP工具类，支持ipv4和ipv6
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object IpKit {

    /** 日志器 */
    private val LOG = LogFactory.getLog(this::class)

    /** 32 位无符号最大值（即 2³²-1），用于 IPv4 长整型范围校验 */
    private const val ALL32ONE = 4294967295L

    /** 128 位无符号最大值（含），与 `NUMERIC(39,0)` 存 IPv6 整值时一致。 */
    private val UINT128_MAX: BigInteger = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE)

    /**
     * 与库表 `NUMERIC(39,0)` 存 IP 整值时的解析策略（查询条件、表单入库等共用）。
     */
    enum class IpStorageNumericMode {
        /**
         * 纯十进制串优先；否则合法 IPv4 点分；否则合法 IPv6；再否则按十进制串兜底。
         */
        AUTO,

        /**
         * 按 IPv4：纯十进制串，或经 [getNormalIpv4]/[getFixLengthIpv4] 与 [ipv4StringToLong] 转无符号 32 位；无法再解析时按十进制串兜底。
         */
        IPV4,

        /**
         * 按 IPv6：经 [toFullIpv6] 与 [fullIpv6ColonGroupsTextToBigInteger]；[toFullIpv6] 失败时按十进制串兜底。
         */
        IPV6,
    }

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
        if (ipv4Long !in 0..ALL32ONE) return ""
        // 高字节在前：byte 0 = 第一个 octet，byte 3 = 最末 octet
        return (3 downTo 0).joinToString(".") { i -> ((ipv4Long shr (i * 8)) and 0xFFL).toString() }
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
        if (!isValidIpv4(ipv4)) return ""
        return ipv4.split(".").joinToString(".") { it.padStart(3, '0') }
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
        if (!isValidIpv4(ipv4)) return ""
        return ipv4.split(".").joinToString(".") { it.toInt().toString() }
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
        if (beginIp.isEmpty() && endIp.isEmpty()) return emptyList()
        val rawBegin = ipv4StringToLong(beginIp.ifEmpty { "0.0.0.0" })
        if (rawBegin == -1L) return emptyList()
        val rawEnd = ipv4StringToLong(endIp.ifEmpty { "255.255.255.255" })
        if (rawEnd == -1L) return emptyList()
        // 不管谁大谁小，对外约定按"从小到大"返回
        val (lo, hi) = if (rawBegin <= rawEnd) rawBegin to rawEnd else rawEnd to rawBegin
        val size = (hi - lo).toInt() + 1
        if (size !in 0..65536) return emptyList()
        return (0 until size).map { offset -> ipv4LongToString(lo + offset) }
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
        if (ipv4 == "127.0.0.1") return true
        val l = ipv4StringToLong(ipv4)
        // 192.168.0.0–192.168.255.255 或 10.0.0.0–10.255.255.255
        return l in 3232235520L..3232301055L || l in 167772160L..184549375L
    }

    /**
     * 返回本机的本地ip地址
     *
     * @return 本机的本地ip地址
     * @author K
     * @since 1.0.0
     */
    fun getLocalIp(): String = runCatching { InetAddress.getLocalHost().hostAddress }
        .getOrElse {
            if (it is UnknownHostException) LOG.error(it) else throw it
            ""
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
        require(ip.isNotBlank()) { "ip is blank" }
        // 去掉可能存在的 zone-id（如 fe80::1%eth0）
        val pure = ip.substringBefore('%')
        val addr = runCatching { InetAddress.getByName(pure) }
            .getOrElse { throw IllegalArgumentException("invalid IP: $ip", it) }
        val bytes = when (addr.address.size) {
            // IPv4 -> 统一映射为 IPv6 的 16 字节
            4 -> ByteArray(16).also {
                it[10] = 0xFF.toByte()
                it[11] = 0xFF.toByte()
                System.arraycopy(addr.address, 0, it, 12, 4)
            }
            16 -> addr.address
            else -> throw IllegalArgumentException("unexpected address length")
        }
        // 16 字节 -> 8 组，每组 16 位；统一大写、每组补满 4 位
        return (0 until 16 step 2).joinToString(":") { i ->
            val value = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
            String.format(Locale.ROOT, "%04X", value)
        }
    }

    /**
     * 将 [toFullIpv6] 输出的 8 段全大写十六进制文本转为 128 位**无符号**整数（大端组序），与 `NUMERIC(39,0)` 存值一致。
     *
     * @param full 形如 `0000:0000:0000:0000:0000:FFFF:C0A8:0001`
     * @throws IllegalArgumentException 段数不是 8、段值越界或整体超过 2¹²⁸−1
     */
    fun fullIpv6ColonGroupsTextToBigInteger(full: String): BigInteger {
        val parts = full.split(':')
        require(parts.size == 8) { "IPv6 须为全格式 8 段" }
        val acc = parts.fold(BigInteger.ZERO) { acc, p ->
            val n = p.toInt(16)
            require(n in 0..0xffff)
            acc.shiftLeft(16).add(BigInteger.valueOf(n.toLong()))
        }
        require(acc.signum() >= 0 && acc <= UINT128_MAX) { "IPv6 数值越界" }
        return acc
    }

    /** [fullIpv6ColonGroupsTextToBigInteger] 的 [BigDecimal] 形式，便于与 Ktorm/JDBC `NUMERIC` 列绑定。 */
    fun fullIpv6ColonGroupsTextToUnsignedDecimal(full: String): BigDecimal =
        BigDecimal(fullIpv6ColonGroupsTextToBigInteger(full))

    /**
     * 将库内 `NUMERIC(39,0)` / [fullIpv6ColonGroupsTextToUnsignedDecimal] 表示的无符号 128 位 IPv6 整值转为全格式 IPv6 文本（与 [toFullIpv6] 输出风格一致：8 段、每段 4 位大写十六进制）。
     *
     * @param value 无符号整数；非整数、为负或大于 2¹²⁸−1 时无法表示为 IPv6 存储值
     * @return 形如 `0000:0000:0000:0000:0000:FFFF:C0A8:0001` 的字符串；[value] 为 null 或非法时返回空串
     */
    fun ipv6BigDecimalToFullString(value: BigDecimal?): String {
        if (value == null) return ""
        val bi = runCatching { value.toBigIntegerExact() }.getOrElse { return "" }
        if (bi.signum() < 0 || bi > UINT128_MAX) return ""
        return (0 until 8).joinToString(":") { i ->
            val shift = 112 - 16 * i
            val group = bi.shiftRight(shift).and(BigInteger.valueOf(0xffffL)).toInt()
            String.format(Locale.ROOT, "%04X", group)
        }
    }

    /**
     * 将用户输入的 IP 文本转为与库内 `ip_start`/`ip_end`（`NUMERIC(39,0)`）一致的**无符号** [BigDecimal]。
     *
     * @param ip 点分 IPv4、缩写/全格式 IPv6、或纯十进制整数字符串
     * @param mode 见 [IpStorageNumericMode]
     * @return 无法解析且无法按十进制兜底时返回 null；[ip] 仅空白时返回 null
     */
    fun ipTextToUnsignedStorageDecimal(
        ip: String,
        mode: IpStorageNumericMode = IpStorageNumericMode.AUTO,
    ): BigDecimal? {
        val s = ip.trim().ifEmpty { return null }
        return when (mode) {
            IpStorageNumericMode.IPV6 -> parseIpv6PreferToDecimal(s)
            IpStorageNumericMode.IPV4 -> parseIpv4PreferToDecimal(s)
            IpStorageNumericMode.AUTO -> parseAutoToDecimal(s)
        }
    }

    /**
     * 以 IPv6 优先的策略将文本解析为存储用 `BigDecimal`：
     * 先尝试 [toFullIpv6] + [fullIpv6ColonGroupsTextToUnsignedDecimal]；失败时按十进制串兜底。
     *
     * @param s 已 trim 的输入文本
     * @return 解析结果，无法解析时返回 null
     * @author K
     * @since 1.0.0
     */
    private fun parseIpv6PreferToDecimal(s: String): BigDecimal? =
        runCatching { fullIpv6ColonGroupsTextToUnsignedDecimal(toFullIpv6(s)) }
            .getOrElse { runCatching { BigDecimal(s) }.getOrNull() }

    /**
     * 以 IPv4 优先的策略将文本解析为存储用 `BigDecimal`：
     * 纯数字串直接当十进制处理；否则按点分/定长 IPv4 经 [ipv4DottedOrFixedToUnsignedDecimal] 转换。
     *
     * @param s 已 trim 的输入文本
     * @return 解析结果，无法解析时返回 null
     * @author K
     * @since 1.0.0
     */
    private fun parseIpv4PreferToDecimal(s: String): BigDecimal? =
        if (s.all(Char::isDigit)) runCatching { BigDecimal(s) }.getOrNull()
        else ipv4DottedOrFixedToUnsignedDecimal(s)

    /**
     * 把点分（含定长零补齐）形式 IPv4 转为存储用 `BigDecimal`：
     * 先归一为非定长 IPv4 文本；归一失败时按十进制串兜底；归一成功后将无符号 32 位整数包成 `BigDecimal` 返回。
     *
     * @param s 输入文本
     * @return 转换结果，[ipv4StringToLong] 返回 -1 时返回 null
     * @author K
     * @since 1.0.0
     */
    private fun ipv4DottedOrFixedToUnsignedDecimal(s: String): BigDecimal? {
        var normal = getNormalIpv4(s)
        if (normal.isEmpty()) {
            val fixed = getFixLengthIpv4(s)
            normal = if (fixed.isEmpty()) "" else getNormalIpv4(fixed)
        }
        if (normal.isEmpty()) return runCatching { BigDecimal(s) }.getOrNull()
        val l = ipv4StringToLong(normal)
        return if (l < 0) null else BigDecimal(BigInteger.valueOf(l and 0xFFFFFFFFL))
    }

    /**
     * 自动判定输入类型并解析为存储用 `BigDecimal`：
     * 优先级 — 纯十进制串 > 合法 IPv4 > 合法 IPv6 > 兜底按十进制串。
     *
     * @param s 已 trim 的输入文本
     * @return 解析结果，全部尝试失败时返回 null
     * @author K
     * @since 1.0.0
     */
    private fun parseAutoToDecimal(s: String): BigDecimal? = when {
        s.all(Char::isDigit) -> runCatching { BigDecimal(s) }.getOrNull()
        isValidIpv4(s) -> {
            val l = ipv4StringToLong(getNormalIpv4(s))
            if (l < 0) runCatching { BigDecimal(s) }.getOrNull()
            else BigDecimal(BigInteger.valueOf(l and 0xFFFFFFFFL))
        }
        isValidIpv6(s) -> runCatching { fullIpv6ColonGroupsTextToUnsignedDecimal(toFullIpv6(s)) }.getOrNull()
        else -> runCatching { BigDecimal(s) }.getOrNull()
    }

}