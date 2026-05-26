package io.kudos.base.net

import io.kudos.base.logger.LogFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Locale

/**
 * IP utility supporting both IPv4 and IPv6.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object IpKit {

    /** Logger. */
    private val LOG = LogFactory.getLog(this::class)

    /** 32-bit unsigned max value (2^32 - 1), used for IPv4 long-range validation. */
    private const val ALL32ONE = 4294967295L

    /** 128-bit unsigned max value (inclusive), matching the IPv6 integer stored as `NUMERIC(39,0)`. */
    private val UINT128_MAX: BigInteger = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE)

    /**
     * Parsing strategy used when interpreting IP integers stored as `NUMERIC(39,0)` (shared by query conditions, form persistence, etc.).
     */
    enum class IpStorageNumericMode {
        /**
         * Pure decimal string first; otherwise a valid dotted IPv4; otherwise a valid IPv6; otherwise fall back to a decimal string.
         */
        AUTO,

        /**
         * Treat as IPv4: pure decimal string, or normalized via [getNormalIpv4]/[getFixLengthIpv4] and [ipv4StringToLong] to an unsigned 32-bit value; fall back to a decimal string when parsing is no longer possible.
         */
        IPV4,

        /**
         * Treat as IPv6: via [toFullIpv6] and [fullIpv6ColonGroupsTextToBigInteger]; fall back to a decimal string when [toFullIpv6] fails.
         */
        IPV6,
    }

    /**
     * Validates whether the given IP address is a valid IPv4 address.
     *
     * @param ip the IP string to validate
     * @return true if it is a valid IPv4 address
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
     * Converts an IPv4 address string to its numeric representation.
     *
     * @param ipv4 the IPv4 address
     * @return the numeric representation of the IPv4 address; -1 if not an IPv4 address
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
     * Converts a numeric IP address back to a string representation.
     *
     * @param ipv4Long the IPv4 long value; values less than 0 or greater than 4294967295 return an empty string
     * @return the IPv4 address; an empty string is returned when the argument is less than 0 or greater than 4294967295
     * @author K
     * @since 1.0.0
     */
    fun ipv4LongToString(ipv4Long: Long): String {
        if (ipv4Long !in 0..ALL32ONE) return ""
        // High byte first: byte 0 = first octet, byte 3 = last octet
        return (3 downTo 0).joinToString(".") { i -> ((ipv4Long shr (i * 8)) and 0xFFL).toString() }
    }

    /**
     * Returns the fixed-length IPv4 address (each segment is left-padded with 0 to 3 digits). For example: 1.2.13.224 => 001.002.013.224
     *
     * @param ipv4 the IPv4 to process; returns an empty string if the IP is invalid
     * @return the fixed-length IPv4 address
     * @author K
     * @since 1.0.0
     */
    fun getFixLengthIpv4(ipv4: String): String {
        if (!isValidIpv4(ipv4)) return ""
        return ipv4.split(".").joinToString(".") { it.padStart(3, '0') }
    }

    /**
     * Restores a fixed-length IPv4 (removes leading zeros from each segment). For example: 001.002.013.224 => 1.2.13.224
     *
     * @param ipv4 the IPv4 to process; returns an empty string if the IP is invalid
     * @return the non-fixed-length IPv4
     * @author K
     * @since 1.0.0
     */
    fun getNormalIpv4(ipv4: String): String {
        if (!isValidIpv4(ipv4)) return ""
        return ipv4.split(".").joinToString(".") { it.toInt().toString() }
    }

    /**
     * Checks whether the given IPv4 addresses all belong to the same subnet.
     *
     * @param maskAddress the subnet mask address; returns false if invalid
     * @param ipv4s a varargs array of IPv4 addresses; returns false if empty or if any IP is invalid
     * @return true if the given IPv4 addresses all belong to the same subnet
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
     * Returns all IPv4 addresses between the two given IPv4 addresses (order does not matter),
     * including both endpoints, in ascending order; returns only one IP if the two are identical.
     *
     * At most 65536 IP addresses are supported; an empty list is returned if exceeded.
     *
     * @param beginIp the begin value, inclusive; an invalid IP returns an empty list
     * @param endIp the end value, inclusive; an invalid IP returns an empty list
     * @return a list containing all IPv4 addresses between the two given IPv4 addresses; an empty list is returned if either argument is invalid or the range exceeds 65536
     * @author K
     * @since 1.0.0
     */
    fun getIpv4sBetween(beginIp: String, endIp: String): List<String> {
        if (beginIp.isEmpty() && endIp.isEmpty()) return emptyList()
        val rawBegin = ipv4StringToLong(beginIp.ifEmpty { "0.0.0.0" })
        if (rawBegin == -1L) return emptyList()
        val rawEnd = ipv4StringToLong(endIp.ifEmpty { "255.255.255.255" })
        if (rawEnd == -1L) return emptyList()
        // Regardless of which is larger, return values in ascending order by contract
        val (lo, hi) = if (rawBegin <= rawEnd) rawBegin to rawEnd else rawEnd to rawBegin
        val size = (hi - lo).toInt() + 1
        if (size !in 0..65536) return emptyList()
        return (0 until size).map { offset -> ipv4LongToString(lo + offset) }
    }

    /**
     * Determines whether the address is a local IPv4 address. For example: 127.0.0.1, 192.168.0.123.
     *
     * @param ipv4 the IPv4 address to check
     * @return true if it is a local IPv4 address
     * @author K
     * @since 1.0.0
     */
    fun isLocalIpv4(ipv4: String): Boolean {
        if (ipv4 == "127.0.0.1") return true
        val l = ipv4StringToLong(ipv4)
        // 192.168.0.0–192.168.255.255 or 10.0.0.0–10.255.255.255
        return l in 3232235520L..3232301055L || l in 167772160L..184549375L
    }

    /**
     * Returns the local IP address of this machine.
     *
     * @return the local IP address of this machine
     * @author K
     * @since 1.0.0
     */
    fun getLocalIp(): String = runCatching { InetAddress.getLocalHost().hostAddress }
        .getOrElse {
            if (it is UnknownHostException) LOG.error(it) else throw it
            ""
        }

    /**
     * Determines whether the given string is a valid IPv6 address (including colon-hex notation, zero-compression notation, and embedded-IPv4 notation).
     *
     * @param ipStr the IP string
     * @return true if it is IPv6; false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isValidIpv6(ipStr: String): Boolean {
        if (ipStr.isBlank()) return false
        if (isValidIpv4(ipStr)) return false
        return try {
            val pure = ipStr.substringBefore('%')
            val a = InetAddress.getByName(pure)
            a is Inet6Address || a.address.size == 16 || // pure IPv6
                    a.address.size == 4  // also allow embedded-IPv4 forms, normalized by toFullIpv6
        } catch (_: UnknownHostException) {
            false
        }
    }

    /**
     * Normalizes any IPv4/IPv6 address to a "full-format IPv6":
     * 8 groups, each 4 hexadecimal digits (uppercase), separated by colons.
     *
     * @param ip the raw IP (may be IPv4, abbreviated IPv6, IPv6 with embedded IPv4, or include a zone-id)
     * @return a full-format IPv6 such as "0000:0000:0000:0000:0000:FFFF:C0A8:0001"
     * @throws IllegalArgumentException if the ip is invalid
     * @author AI: ChatGPT
     * @author K
     * @since 1.0.0
     */
    fun toFullIpv6(ip: String): String {
        require(ip.isNotBlank()) { "ip is blank" }
        // Strip any zone-id present (e.g. fe80::1%eth0)
        val pure = ip.substringBefore('%')
        val addr = runCatching { InetAddress.getByName(pure) }
            .getOrElse { throw IllegalArgumentException("invalid IP: $ip", it) }
        val bytes = when (addr.address.size) {
            // IPv4 -> map to the 16 bytes of IPv6
            4 -> ByteArray(16).also {
                it[10] = 0xFF.toByte()
                it[11] = 0xFF.toByte()
                System.arraycopy(addr.address, 0, it, 12, 4)
            }
            16 -> addr.address
            else -> throw IllegalArgumentException("unexpected address length")
        }
        // 16 bytes -> 8 groups, 16 bits each; uppercase, zero-pad each group to 4 digits
        return (0 until 16 step 2).joinToString(":") { i ->
            val value = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
            String.format(Locale.ROOT, "%04X", value)
        }
    }

    /**
     * Converts the 8-segment, all-uppercase hex text produced by [toFullIpv6] to a 128-bit **unsigned** integer (big-endian group order), matching the `NUMERIC(39,0)` storage value.
     *
     * @param full text such as `0000:0000:0000:0000:0000:FFFF:C0A8:0001`
     * @throws IllegalArgumentException if the segment count is not 8, a segment value is out of range, or the overall value exceeds 2^128 - 1
     */
    fun fullIpv6ColonGroupsTextToBigInteger(full: String): BigInteger {
        val parts = full.split(':')
        require(parts.size == 8) { "IPv6 must be full-format 8 segments" }
        val acc = parts.fold(BigInteger.ZERO) { acc, p ->
            val n = p.toInt(16)
            require(n in 0..0xffff)
            acc.shiftLeft(16).add(BigInteger.valueOf(n.toLong()))
        }
        require(acc.signum() >= 0 && acc <= UINT128_MAX) { "IPv6 numeric value out of range" }
        return acc
    }

    /** [BigDecimal] form of [fullIpv6ColonGroupsTextToBigInteger], convenient for binding to Ktorm/JDBC `NUMERIC` columns. */
    fun fullIpv6ColonGroupsTextToUnsignedDecimal(full: String): BigDecimal =
        BigDecimal(fullIpv6ColonGroupsTextToBigInteger(full))

    /**
     * Converts an unsigned 128-bit IPv6 integer value stored as `NUMERIC(39,0)` / [fullIpv6ColonGroupsTextToUnsignedDecimal] back to a full-format IPv6 text (in the same style as [toFullIpv6]: 8 segments, each 4 uppercase hex digits).
     *
     * @param value an unsigned integer; non-integer, negative, or greater than 2^128 - 1 values cannot be represented as an IPv6 storage value
     * @return a string such as `0000:0000:0000:0000:0000:FFFF:C0A8:0001`; returns an empty string when [value] is null or invalid
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
     * Converts user-supplied IP text to an **unsigned** [BigDecimal] consistent with the `ip_start`/`ip_end` (`NUMERIC(39,0)`) values stored in the database.
     *
     * @param ip a dotted IPv4, abbreviated/full-format IPv6, or pure decimal integer string
     * @param mode see [IpStorageNumericMode]
     * @return null when parsing fails and no decimal fallback is possible; null when [ip] is blank only
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
     * Parses text into a storage `BigDecimal` with an IPv6-first strategy:
     * try [toFullIpv6] + [fullIpv6ColonGroupsTextToUnsignedDecimal] first; on failure, fall back to a decimal string.
     *
     * @param s trimmed input text
     * @return the parsed result, or null if parsing fails
     * @author K
     * @since 1.0.0
     */
    private fun parseIpv6PreferToDecimal(s: String): BigDecimal? =
        runCatching { fullIpv6ColonGroupsTextToUnsignedDecimal(toFullIpv6(s)) }
            .getOrElse { runCatching { BigDecimal(s) }.getOrNull() }

    /**
     * Parses text into a storage `BigDecimal` with an IPv4-first strategy:
     * a pure-numeric string is treated as decimal directly; otherwise it is converted via dotted/fixed-length IPv4 through [ipv4DottedOrFixedToUnsignedDecimal].
     *
     * @param s trimmed input text
     * @return the parsed result, or null if parsing fails
     * @author K
     * @since 1.0.0
     */
    private fun parseIpv4PreferToDecimal(s: String): BigDecimal? =
        if (s.all(Char::isDigit)) runCatching { BigDecimal(s) }.getOrNull()
        else ipv4DottedOrFixedToUnsignedDecimal(s)

    /**
     * Converts a dotted IPv4 (including fixed-length zero-padded form) to a storage `BigDecimal`:
     * first normalizes to a non-fixed-length IPv4 text; if normalization fails, falls back to a decimal string;
     * after successful normalization, wraps the unsigned 32-bit integer into a `BigDecimal`.
     *
     * @param s the input text
     * @return the converted result, or null when [ipv4StringToLong] returns -1
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
     * Automatically determines the input type and parses it into a storage `BigDecimal`:
     * priority order — pure decimal string > valid IPv4 > valid IPv6 > fall back to decimal string.
     *
     * @param s trimmed input text
     * @return the parsed result, or null if every attempt fails
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
