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
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class NetworkKitTest {

    /**
     * Test: when no service is listening on a port, isPortActive should return false.
     */
    @Test
    fun testIsPortActive_PortClosed_ReturnsFalse() {
        // Pick a port that is unlikely to be open (e.g. 54321)
        val port = 54321
        assertFalse(
            NetworkKit.isPortActive(NetworkKit.LOCALHOST_IP, port),
            "when port $port is not being listened on, isPortActive should return false"
        )
    }

    /**
     * Test: when a port is being listened on by a ServerSocket, isPortActive should return true; once the socket
     * is closed it should return false.
     */
    @Test
    fun testIsPortActive_WithServerSocket_ReturnsTrueThenFalse() {
        // Create a ServerSocket on port 0 so the system assigns an available port
        val serverSocket = ServerSocket(0)
        val port = serverSocket.localPort

        try {
            // serverSocket is now listening on port
            assertTrue(
                NetworkKit.isPortActive(NetworkKit.LOCALHOST_IP, port),
                "while ServerSocket is listening, isPortActive($port) should return true"
            )
        } finally {
            serverSocket.close()
        }

        // After closing, verify isPortActive returns false
        assertFalse(
            NetworkKit.isPortActive(NetworkKit.LOCALHOST_IP, port),
            "after ServerSocket is closed, isPortActive($port) should return false"
        )
    }

    /**
     * Test: on Linux or Windows, getMacAddress should return at least one MAC address in a valid format; otherwise
     * it should throw UnsupportedOperationException.
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
                "on $osName, getMacAddress should not return an empty list"
            )

            // Verify each returned value matches the "XX:XX:XX:XX:XX:XX" or "XX-XX-XX-XX-XX-XX" format
            val regex = "^([0-9A-Fa-f]{2}([-:])){5}([0-9A-Fa-f]{2})$"
            val pattern = Pattern.compile(regex)

            for (mac in macList) {
                assertTrue(
                    pattern.matcher(mac).matches(),
                    "returned MAC address has an invalid format: $mac"
                )
            }
        } else {
            // If the runtime is not Linux/Windows, just skip (the next test verifies the throw on unsupported systems)
            println("current OS ($osName) is not Linux/Windows; skipping this test")
        }
    }

    /**
     * Test: on non-Linux/Windows systems (e.g. macOS), getMacAddress should throw UnsupportedOperationException.
     */
    @Test
    fun testGetMacAddress_UnsupportedOs_Throws() {
        val originalOs = System.getProperty("os.name")
        try {
            // Force os.name to "MySpecialOS" so it does not start with either "linux" or "windows"
            System.setProperty("os.name", "MySpecialOS")
            assertFailsWith<UnsupportedOperationException> {
                NetworkKit.getMacAddress()
            }
        } finally {
            // Restore the original os.name
            System.setProperty("os.name", originalOs)
        }
    }

}
