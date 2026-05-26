package io.kudos.base.net

import java.io.IOException
import java.net.NetworkInterface
import java.net.Socket

/**
 * Network utility.
 *
 * @author K
 * @since 1.0.0
 */
object NetworkKit {

    const val LOCALHOST_IP = "127.0.0.1"
    const val ANYHOST_IP = "0.0.0.0"

    /**
     * Checks whether a port is active.
     *
     * @param ip IP address
     * @param port port number
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
     * Returns the MAC addresses.
     *
     * @return list of MAC addresses for all network interfaces
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
     * Detects the current OS type, matching only by the `os.name` prefix (case insensitive).
     * The default parameter reads the system property; unit tests can pass a string directly to bypass the JVM env.
     *
     * @param osName OS name; defaults to `System.getProperty("os.name")`
     * @return one of the three [Os] enum values; unknown is mapped to OTHER
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
     * Collects MAC addresses of all "up + non-loopback + non-virtual" network interfaces and returns them deduplicated.
     *
     * `delimiter`: Windows uses `-`, Unix-like uses `:`, matching the style of local `ipconfig`/`ifconfig`.
     * Virtual/loopback interfaces are skipped to avoid noise from Docker / VPN.
     *
     * @param delimiter delimiter between MAC address octets
     * @return list of MACs in the form `00:1A:2B:3C:4D:5E`
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