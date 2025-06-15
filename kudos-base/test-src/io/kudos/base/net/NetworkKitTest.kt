package io.kudos.base.net

import java.net.ServerSocket
import java.util.regex.Pattern
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * test for NetworkKit
 *
 * @author ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class NetworkKitTest {

    /**
     * 测试：当某端口没有服务监听时，isPortActive 应返回 false
     */
    @Test
    fun testIsPortActive_PortClosed_ReturnsFalse() {
        // 先找一个很可能未打开的端口（例如 54321）
        val port = 54321
        assertFalse(
            NetworkKit.isPortActive(NetworkKit.LOCALHOST_IP, port),
            "端口 $port 未被监听时，isPortActive 应返回 false"
        )
    }

    /**
     * 测试：当某端口由 ServerSocket 监听时，isPortActive 应返回 true，然后关闭后应返回 false
     */
    @Test
    fun testIsPortActive_WithServerSocket_ReturnsTrueThenFalse() {
        // 在 0 端口创建 ServerSocket，让系统分配一个可用端口
        val serverSocket = ServerSocket(0)
        val port = serverSocket.localPort

        try {
            // 此时 serverSocket 正在监听 port
            assertTrue(
                NetworkKit.isPortActive(NetworkKit.LOCALHOST_IP, port),
                "ServerSocket 监听时, isPortActive($port) 应返回 true"
            )
        } finally {
            serverSocket.close()
        }

        // 关闭之后，再次验证应返回 false
        assertFalse(
            NetworkKit.isPortActive(NetworkKit.LOCALHOST_IP, port),
            "ServerSocket 关闭后, isPortActive($port) 应返回 false"
        )
    }

    /**
     * 测试：在 Linux 或 Windows 环境下，getMacAddress 应至少返回一个合法格式的 MAC 地址；否则抛出 UnsupportedOperationException
     */
    @Test
    fun testGetMacAddress_SupportedOs_ReturnsValidMacs() {
        val osName = System.getProperty("os.name").lowercase()
        val isLinux = osName.startsWith("linux")
        val isWindows = osName.startsWith("windows")

        if (isLinux || isWindows) {
            val macList = NetworkKit.getMacAddress()
            assertTrue(
                macList.isNotEmpty(),
                "在 $osName 系统下，getMacAddress 不应返回空列表"
            )

            // 验证每个返回值都符合 “XX:XX:XX:XX:XX:XX” 或 “XX-XX-XX-XX-XX-XX” 格式
            val regex = "^([0-9A-Fa-f]{2}([-:])){5}([0-9A-Fa-f]{2})$"
            val pattern = Pattern.compile(regex)

            for (mac in macList) {
                assertTrue(
                    pattern.matcher(mac).matches(),
                    "返回的 MAC 地址格式不正确: $mac"
                )
            }
        } else {
            // 如果运行环境并非 Linux/Windows，此处只跳过（交由下一个测试验证非支持系统抛出）
            println("当前系统 ($osName) 不是 Linux/Windows，跳过此测试")
        }
    }

    /**
     * 测试：在非 Linux/Windows 系统（例如 macOS）上，getMacAddress 应抛出 UnsupportedOperationException
     */
    @Test
    fun testGetMacAddress_UnsupportedOs_Throws() {
        val originalOs = System.getProperty("os.name")
        try {
            // 强制将 os.name 设置为 “MySpecialOS”，保证既不以 “linux” 开头也不以 “windows” 开头
            System.setProperty("os.name", "MySpecialOS")
            assertFailsWith<UnsupportedOperationException> {
                NetworkKit.getMacAddress()
            }
        } finally {
            // 恢复原始 os.name
            System.setProperty("os.name", originalOs)
        }
    }

}