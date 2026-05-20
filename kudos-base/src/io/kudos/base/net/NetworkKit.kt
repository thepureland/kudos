package io.kudos.base.net

import java.io.IOException
import java.net.NetworkInterface
import java.net.Socket

/**
 * 网络工具
 *
 * @author K
 * @since 1.0.0
 */
object NetworkKit {

    const val LOCALHOST_IP = "127.0.0.1"
    const val ANYHOST_IP = "0.0.0.0"

    /**
     * 判断端口是否启用
     *
     * @param ip ip地址
     * @param port 端口号
     * @author K
     * @since 1.0.0
     */
    fun isPortActive(ip: String, port: Int): Boolean {
        try {
            Socket(ip, port).close()
            return true
        } catch (_: IOException) {
            return false
        } catch (_: IllegalArgumentException) {
            return false
        }
    }

    /**
     * 获取MAC地址
     *
     * @return 所有网卡的MAC地址列表
     * @author K
     * @since 1.0.0
     */
    fun getMacAddress(): List<String> {
        return when (detectOs()) {
            Os.WINDOWS -> collectMacs("-")
            Os.LINUX, Os.MAC -> collectMacs(":")
            Os.OTHER -> throw UnsupportedOperationException("Unsupported OS: ${System.getProperty("os.name")}")
        }
    }

    /**
     * 判定当前 OS 类型，仅按 `os.name` 前缀匹配（不区分大小写）。
     * 默认参数取系统属性——单测可直接传字符串绕过 JVM 环境。
     *
     * @param osName 操作系统名；默认走 `System.getProperty("os.name")`
     * @return 三档之一的 [Os] 枚举，未知归为 OTHER
     * @author K
     * @since 1.0.0
     */
    private fun detectOs(osName: String = System.getProperty("os.name") ?: ""): Os {
        val n = osName.lowercase()
        return when {
            n.startsWith("windows") -> Os.WINDOWS
            n.startsWith("linux") -> Os.LINUX
            n.startsWith("mac") || n.startsWith("darwin") -> Os.MAC
            else -> Os.OTHER
        }
    }

    /**
     * 收集所有"启用 + 非回环 + 非虚拟"网卡的 MAC 地址，去重后返回。
     *
     * `delimiter` 分隔符：Windows 用 `-`、Unix-like 用 `:`，与本地 `ipconfig`/`ifconfig` 风格一致。
     * 跳过虚拟/回环网卡避免 Docker / VPN 带来的杂噪。
     *
     * @param delimiter MAC 地址段间分隔符
     * @return 形如 `00:1A:2B:3C:4D:5E` 的 MAC 列表
     * @author K
     * @since 1.0.0
     */
    private fun collectMacs(delimiter: String): List<String> {
        val ifaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
        return ifaces.toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .mapNotNull { it.hardwareAddress?.takeIf { hw -> hw.isNotEmpty() } }
            .map { hw -> hw.joinToString(delimiter) { "%02X".format(it) } }
            .distinct()
    }

    private enum class Os { WINDOWS, LINUX, MAC, OTHER }

}