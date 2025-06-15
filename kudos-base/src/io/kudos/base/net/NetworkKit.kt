package io.kudos.base.net

import io.kudos.base.lang.SystemKit
import java.io.BufferedReader
import java.io.InputStreamReader
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
        } catch (e: Exception) {
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
        val macs: MutableList<String> = ArrayList()
        val myProc: Process
        var currentLine: String?
        val osName = SystemKit.getOSName()
        var macRegExp: String

        if (osName.startsWith("windows")) {
            macRegExp = "([0-9A-Fa-f]{2}-){5}[0-9A-Fa-f]{2}"  // Update to match Windows MAC format
            myProc = Runtime.getRuntime().exec("ipconfig /all")
        } else if (osName.startsWith("linux")) {
            macRegExp = "([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}"  // Update to match Linux MAC format
            myProc = Runtime.getRuntime().exec("/sbin/ifconfig -a")
        } else {
            throw UnsupportedOperationException("不支持的操作系统")
        }

        val reader = BufferedReader(InputStreamReader(myProc.inputStream))
        val macPattern = Pattern.compile(".*($macRegExp).*")
        var macMatcher: Matcher?

        while (reader.readLine().also { currentLine = it } != null) {
            if (currentLine != null) {
                println("Current line: $currentLine") // Debugging line content
                macMatcher = macPattern.matcher(currentLine)
                if (macMatcher.matches()) {
                    // Capture the matching MAC address
                    val macAddress = macMatcher.group(1)
                    if (!macAddress.isNullOrEmpty()) {
                        macs.add(macAddress)
                        println("MAC address found: $macAddress") // Debugging found MAC address
                    }
                    macMatcher.reset()
                }
            }
        }

        myProc.destroy()
        return macs
    }


}