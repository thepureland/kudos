package io.kudos.base.net

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
        } catch (_: Exception) {
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

    private fun detectOs(osName: String = System.getProperty("os.name") ?: ""): Os {
        val n = osName.lowercase()
        return when {
            n.startsWith("windows") -> Os.WINDOWS
            n.startsWith("linux") -> Os.LINUX
            n.startsWith("mac") || n.startsWith("darwin") -> Os.MAC
            else -> Os.OTHER
        }
    }

    private fun collectMacs(delimiter: String): List<String> {
        val ifaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
        val macs = mutableListOf<String>()
        for (nif in ifaces.toList()) {
            if (!nif.isUp || nif.isLoopback || nif.isVirtual) continue
            val hw = nif.hardwareAddress ?: continue
            if (hw.isEmpty()) continue
            macs += hw.joinToString(delimiter) { "%02X".format(it) }
        }
        return macs.distinct()
    }

    private enum class Os { WINDOWS, LINUX, MAC, OTHER }

}