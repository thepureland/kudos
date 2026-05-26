package io.kudos.ability.web.springmvc.support

import io.kudos.base.net.IpKit
import jakarta.servlet.http.HttpServletRequest
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Common extension functions on [HttpServletRequest].
 *
 * Includes client IP resolution (with X-Forwarded-For multi-level reverse proxy handling),
 * heuristic User-Agent -> (browser / OS / terminal) parsing, root URL assembly, etc. All methods are pure functions.
 *
 * **IP resolution security note**: [getRemoteIp] trusts client proxy headers such as `x-forwarded-for`.
 * It must only be used after the application is fronted by a trusted reverse proxy (Nginx / ALB / Cloudflare)
 * that strips/normalises those headers. Using this method on a service directly exposed to the internet
 * lets attackers spoof their own IP by forging the header.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */

/**
 * Obtain the real client IP of the request, supporting multi-level reverse proxies.
 *
 * @return IP address
 * @author K
 * @since 1.0.0
 */
fun HttpServletRequest.getRemoteIp(): String {
    val ip = sequenceOf(
        getHeader("x-forwarded-for"),
        getHeader("Proxy-Client-IP"),
        getHeader("WL-Proxy-Client-IP"),
    )
        .mapNotNull { h -> h?.takeIf { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) } }
        .firstOrNull()
        ?: remoteAddr
    return getIP(ip)
}

/**
 * Obtain the browser information of the request.
 *
 * @return Pair(browser name, version)
 * @author K
 * @since 1.0.0
 */
fun HttpServletRequest.getBrowserInfo(): Pair<String, String> {
    val agent = this.getHeader("User-Agent")
    var name = "unknown"
    var version = "unknown"
    if (agent.isNullOrBlank()) {
//        error("Request did not provide a User-Agent header, ${this.requestURL}")
        return Pair(name, version)
    }
    var regex = """Version/([0-9.]+)"""
    when {
        agent.contains("MSIE") -> {
            name = "MSIE" // Microsoft IE
            regex = """$name\s([0-9.]+)"""
        }
        agent.contains("Firefox") -> {
            name = "Firefox" // Mozilla Firefox
            regex = """$name/([0-9.]+)"""
        }
        agent.contains("Chrome") -> {
            name = "Chrome" // Google
            regex = """$name/([0-9.]+)"""
        }
        agent.contains("Opera") -> name = "Opera"
        agent.contains("Safari") -> name = "Safari"
        agent.contains("app_android") -> name = "Android App"
        agent.contains("app_ios") -> name = "IOS App"
        agent.contains("Trident") -> name = "Trident"
        agent.contains("Edge") -> name = "Edge"
        agent.contains("Maxthon") -> name = "Maxthon" // Maxthon browser
        agent.contains("qqbrowser") -> name = "qqbrowser"
        agent.contains("lbbrowser") -> name = "lbbrowser" // Cheetah browser
        agent.contains("UCBrowser") -> name = "UCBrowser"
        agent.contains("360SE") -> name = "360SE"
    }
    Regex(regex).find(agent)?.groupValues?.getOrNull(1)?.let { version = it }
    return Pair(name, version)
}


/**
 * Obtain the operating system information of the request.
 *
 * @return Pair(OS name, version)
 * @author K
 * @since 1.0.0
 */
fun HttpServletRequest.getOsInfo(): Pair<String, String> {
    val agent = this.getHeader("User-Agent")
    var name = "unknown"
    var version = "unknown"
    if (agent.isNullOrBlank()) {
//        error("Request did not provide a User-Agent header, ${this.requestURL}")
        return Pair(name, version)
    }
    when {
        agent.contains("Windows") -> {
            name = "Windows" // e.g. Windows 7 = Windows NT 6.1
            Regex("""$name\s([a-zA-Z0-9]+\s[0-9.]+)""").find(agent)?.groupValues?.getOrNull(1)?.let { version = it }
        }
        agent.contains("FreeBSD") -> name = "FreeBSD"
        agent.contains("Macintosh") -> name = "Mac"
        agent.contains("SunOS") -> name = "Solaris"
        agent.contains("app_android") -> name = "app_android"
        agent.contains("app_ios") -> name = "app_ios"
        agent.contains("Android") -> name = "Android"
        agent.contains("x11") || agent.contains("unix") -> name = "Unix"
        agent.contains("iPhone") || agent.contains("iPad") -> name = "ios"
        agent.contains("Linux") -> {
            name = "Linux"
            if (agent.contains("Ubuntu")) {
                version = "Ubuntu"
            }
        }
    }
    return Pair(name, version)
}

/**
 * Return the terminal type of the request.
 *
 * @return terminal type
 * @author K
 * @since 1.0.0
 */
fun HttpServletRequest.getClientTerminal(): String {
    val agent = this.getHeader("User-Agent")
    var name = "unknown"
    if (agent.isNullOrBlank()) {
//        LOG.warn("Request log {0}, request did not provide a User-Agent header", request.requestURL)
        return name
    }
    name = if (agent.contains("app_android") || agent.contains("app_ios")) {
        "App"
    } else if (agent.contains("Android") || agent.contains("iPhone") || agent.contains("iPad") ||
        agent.contains("Windows Phone") || agent.contains("BlackBerry") || agent.contains("SymbianOS")
    ) {
        "Mobile"
    } else {
        "PC"
    }
    return name
}

/**
 * Obtain the site's root path, i.e. scheme + host + port + context.
 *
 * @return site root path
 * @author K
 * @since 1.0.0
 */
fun HttpServletRequest.getRootPath(): String {
    val requestURL = this.requestURL
    val requestURI = this.requestURI
    val root = requestURL.substring(0, requestURL.length - requestURI.length)
    return root + this.contextPath
}

/**
 * Obtain the site's root path, i.e. scheme + host + port.
 *
 * @return site root path
 * @author K
 * @since 1.0.0
 */
fun HttpServletRequest.getDomainPath(): String {
    val requestURL = this.requestURL
    val requestURI = this.requestURI
    return requestURL.substring(0, requestURL.length - requestURI.length)
}

/**
 * In multi-level reverse proxy scenarios, pick the first **non-private** address from a comma-separated list such as `x-forwarded-for`;
 * translate the IPv6 loopback `0:0:0:0:0:0:0:1` to the local IPv4 hostAddress.
 *
 * Private subnet checks: see [isLocalA] / [isLocalB] / [isLocalC] / [isLocal0].
 *
 * @param ip a string possibly containing multiple IPs (comma separated)
 * @return the first public IPv4; if all addresses are private, returns the last non-empty segment; IPv6 loopback is converted to the local hostAddress
 * @throws IllegalStateException when IPv6 loopback translation fails
 * @author K
 * @since 1.0.0
 */
private fun getIP(ip: String): String {
    var ipAddress = ip
    for (part in ip.split(',')) {
        ipAddress = part.trim { it <= ' ' }
        val ipLong = IpKit.ipv4StringToLong(ipAddress)
        if (!isLocalA(ipLong) && !isLocalB(ipLong) && !isLocalC(ipLong) && !isLocal0(ipLong)) {
            break
        }
    }
    // IPv6 loopback: translate to the IPv4 host address for easier auditing / log display.
    if ("0:0:0:0:0:0:0:1" == ipAddress) {
        ipAddress = try {
            InetAddress.getLocalHost().hostAddress
        } catch (e: UnknownHostException) {
            throw IllegalStateException("Unable to resolve local IP (IPv6 loopback translation failed)", e)
        }
    }
    return ipAddress
}

// Pre-convert private subnet boundary values to Long and cache them, avoiding a 4-segment string -> long computation per request.
/** Class A private subnet start: 10.0.0.0 */
private val LOCAL_A_START = IpKit.ipv4StringToLong("10.0.0.0")
/** Class A private subnet end: 10.255.255.255 */
private val LOCAL_A_END = IpKit.ipv4StringToLong("10.255.255.255")
/** Class B private subnet start: 172.16.0.0 */
private val LOCAL_B_START = IpKit.ipv4StringToLong("172.16.0.0")
/** Class B private subnet end: 172.31.255.255 */
private val LOCAL_B_END = IpKit.ipv4StringToLong("172.31.255.255")
/** Class C private subnet start: 192.168.0.0 */
private val LOCAL_C_START = IpKit.ipv4StringToLong("192.168.0.0")
/** Class C private subnet end: 192.168.255.255 */
private val LOCAL_C_END = IpKit.ipv4StringToLong("192.168.255.255")
/** Local loopback address: 127.0.0.1 */
private val LOCAL_LOOPBACK = IpKit.ipv4StringToLong("127.0.0.1")
/** Placeholder address: 0.0.0.0 */
private val LOCAL_ZERO = IpKit.ipv4StringToLong("0.0.0.0")

/**
 * Determine whether the IP is in the 192.168.0.0/16 Class C private subnet.
 * @param ip the Long representation of the IP to check
 * @return true if it belongs to a Class C private subnet
 * @author K
 * @since 1.0.0
 */
private fun isLocalC(ip: Long): Boolean = ip in LOCAL_C_START..LOCAL_C_END

/**
 * Determine whether the IP is in the 172.16.0.0/12 Class B private subnet.
 * @param ip the Long representation of the IP to check
 * @return true if it belongs to a Class B private subnet
 * @author K
 * @since 1.0.0
 */
private fun isLocalB(ip: Long): Boolean = ip in LOCAL_B_START..LOCAL_B_END

/**
 * Determine whether the IP is in the 10.0.0.0/8 Class A private subnet.
 * @param ip the Long representation of the IP to check
 * @return true if it belongs to a Class A private subnet
 * @author K
 * @since 1.0.0
 */
private fun isLocalA(ip: Long): Boolean = ip in LOCAL_A_START..LOCAL_A_END

/**
 * Determine whether the IP is a local loopback / placeholder address (127.0.0.1 or 0.0.0.0).
 * @param ip the Long representation of the IP to check
 * @return true if it is a local or placeholder address
 * @author K
 * @since 1.0.0
 */
private fun isLocal0(ip: Long): Boolean = ip == LOCAL_LOOPBACK || ip == LOCAL_ZERO