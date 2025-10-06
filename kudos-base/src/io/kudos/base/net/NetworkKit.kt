package io.kudos.base.net

import io.kudos.base.lang.SystemKit
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.NetworkInterface
import java.net.Socket
import java.util.regex.Matcher
import java.util.regex.Pattern

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
        val isWindows = System.getProperty("os.name").lowercase().startsWith("windows")
        val delimiter = if (isWindows) "-" else ":"

        val macs = mutableListOf<String>()
        val ifaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()

        for (nif in ifaces.toList()) {
            // 过滤掉回环、虚拟、未启用网卡
            if (!nif.isUp || nif.isLoopback || nif.isVirtual) continue

            val hw = nif.hardwareAddress ?: continue
            if (hw.isEmpty()) continue

            val mac = hw.joinToString(delimiter) { "%02X".format(it) }
            macs.add(mac)
        }
        // 去重并返回
        return macs.distinct()
    }


}