package io.kudos.ability.web.springmvc.support

import io.kudos.base.net.IpKit
import jakarta.servlet.http.HttpServletRequest
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * [HttpServletRequest] 上的常用扩展函数集合。
 *
 * 包含客户端 IP 解析（含 X-Forwarded-For 多级反向代理处理）、User-Agent → (浏览器 / OS / 终端)
 * 启发式解析、根 URL 拼装等。所有方法均为纯函数。
 *
 * **IP 解析安全提示**：[getRemoteIp] 信任客户端的 `x-forwarded-for` 等代理头——只能在
 * 应用已被可信反向代理（Nginx / ALB / Cloudflare）兜底过滤后使用。直接对外暴露的服务
 * 用本方法会被攻击者通过伪造头任意"修改"自身 IP。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */

/**
 * 获取请求的真实ip地址，支持多级反向代理
 *
 * @return ip地址
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
 * 获取请求的浏览器信息
 *
 * @return Pair(浏览器名称，版本)
 * @author K
 * @since 1.0.0
 */
fun HttpServletRequest.getBrowserInfo(): Pair<String, String> {
    val agent = this.getHeader("User-Agent")
    var name = "unknown"
    var version = "unknown"
    if (agent.isNullOrBlank()) {
//        error("用户浏览器头未提供User-Agent信息，${this.requestURL}")
        return Pair(name, version)
    }
    var regex = """Version/([0-9.]+)"""
    when {
        agent.contains("MSIE") -> {
            name = "MSIE" // 微软IE
            regex = """$name\s([0-9.]+)"""
        }
        agent.contains("Firefox") -> {
            name = "Firefox" // 火狐
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
        agent.contains("Maxthon") -> name = "Maxthon" // 遨游浏览器
        agent.contains("qqbrowser") -> name = "qqbrowser"
        agent.contains("lbbrowser") -> name = "lbbrowser" // 猎豹浏览器
        agent.contains("UCBrowser") -> name = "UCBrowser"
        agent.contains("360SE") -> name = "360SE"
    }
    Regex(regex).find(agent)?.groupValues?.getOrNull(1)?.let { version = it }
    return Pair(name, version)
}


/**
 * 获取请求的操作系统信息
 *
 * @return Pair(操作系统名称，版本)
 * @author K
 * @since 1.0.0
 */
fun HttpServletRequest.getOsInfo(): Pair<String, String> {
    val agent = this.getHeader("User-Agent")
    var name = "unknown"
    var version = "unknown"
    if (agent.isNullOrBlank()) {
//        error("用户浏览器头未提供User-Agent信息，${this.requestURL}")
        return Pair(name, version)
    }
    when {
        agent.contains("Windows") -> {
            name = "Windows" //如：win7 = Windows NT 6.1
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
 * 返回请求终端类型
 *
 * @return 终端类型
 * @author K
 * @since 1.0.0
 */
fun HttpServletRequest.getClientTerminal(): String {
    val agent = this.getHeader("User-Agent")
    var name = "unknown"
    if (agent.isNullOrBlank()) {
//        LOG.warn("请求日志{0},用户浏览器头未提供User-Agent信息", request.requestURL)
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
 * 获取站点的根路径，即协议+主机+端口+上下文
 *
 * @return 站点的根路径
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
 * 获取站点的根路径，即协议+主机+端口
 *
 * @return 站点的根路径
 * @author K
 * @since 1.0.0
 */
fun HttpServletRequest.getDomainPath(): String {
    val requestURL = this.requestURL
    val requestURI = this.requestURI
    return requestURL.substring(0, requestURL.length - requestURI.length)
}

/**
 * 多级反向代理场景下，从 `x-forwarded-for` 之类的逗号分隔列表中挑出第一个**非私网**地址；
 * IPv6 loopback `0:0:0:0:0:0:0:1` 翻译为本机 IPv4 hostAddress。
 *
 * 私网段判定见 [isLocalA] / [isLocalB] / [isLocalC] / [isLocal0]。
 *
 * @param ip 可能含多个 IP（逗号分隔）的字符串
 * @return 第一个公网 IPv4；全为私网时返回最后一个非空段；IPv6 loopback 转为本机 hostAddress
 * @throws IllegalStateException IPv6 loopback 翻译失败时
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
    // IPv6 本机回环：翻成 IPv4 主机地址，便于审计 / 日志展示
    if ("0:0:0:0:0:0:0:1" == ipAddress) {
        ipAddress = try {
            InetAddress.getLocalHost().hostAddress
        } catch (e: UnknownHostException) {
            throw IllegalStateException("无法解析本机 IP（IPv6 loopback 翻译失败）", e)
        }
    }
    return ipAddress
}

// 私网段边界值预先转 Long 缓存，避免每个请求做 4 段字符串→long 计算
/** A 类私网段起始：10.0.0.0 */
private val LOCAL_A_START = IpKit.ipv4StringToLong("10.0.0.0")
/** A 类私网段结束：10.255.255.255 */
private val LOCAL_A_END = IpKit.ipv4StringToLong("10.255.255.255")
/** B 类私网段起始：172.16.0.0 */
private val LOCAL_B_START = IpKit.ipv4StringToLong("172.16.0.0")
/** B 类私网段结束：172.31.255.255 */
private val LOCAL_B_END = IpKit.ipv4StringToLong("172.31.255.255")
/** C 类私网段起始：192.168.0.0 */
private val LOCAL_C_START = IpKit.ipv4StringToLong("192.168.0.0")
/** C 类私网段结束：192.168.255.255 */
private val LOCAL_C_END = IpKit.ipv4StringToLong("192.168.255.255")
/** 本机回环地址：127.0.0.1 */
private val LOCAL_LOOPBACK = IpKit.ipv4StringToLong("127.0.0.1")
/** 占位地址：0.0.0.0 */
private val LOCAL_ZERO = IpKit.ipv4StringToLong("0.0.0.0")

/**
 * 192.168.0.0/16 C 类私网段判定。
 * @param ip 待判定 IP 的 Long 表示
 * @return true 表示属于 C 类私网
 * @author K
 * @since 1.0.0
 */
private fun isLocalC(ip: Long): Boolean = ip in LOCAL_C_START..LOCAL_C_END

/**
 * 172.16.0.0/12 B 类私网段判定。
 * @param ip 待判定 IP 的 Long 表示
 * @return true 表示属于 B 类私网
 * @author K
 * @since 1.0.0
 */
private fun isLocalB(ip: Long): Boolean = ip in LOCAL_B_START..LOCAL_B_END

/**
 * 10.0.0.0/8 A 类私网段判定。
 * @param ip 待判定 IP 的 Long 表示
 * @return true 表示属于 A 类私网
 * @author K
 * @since 1.0.0
 */
private fun isLocalA(ip: Long): Boolean = ip in LOCAL_A_START..LOCAL_A_END

/**
 * 本机回环 / 占位地址判定（127.0.0.1 或 0.0.0.0）。
 * @param ip 待判定 IP 的 Long 表示
 * @return true 表示属于本机或占位地址
 * @author K
 * @since 1.0.0
 */
private fun isLocal0(ip: Long): Boolean = ip == LOCAL_LOOPBACK || ip == LOCAL_ZERO