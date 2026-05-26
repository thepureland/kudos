package io.kudos.base.lang

import io.kudos.base.enums.impl.OsEnum
import io.kudos.base.logger.LogFactory
import org.apache.commons.lang3.SystemUtils
import java.awt.GraphicsEnvironment
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.util.Collections
import java.util.regex.Pattern

/**
 * System utility.
 *
 * @author K
 * @author AI: ChatGPT
 * @since 1.0.0
 */
object SystemKit {

    /** Logger */
    private val log = LogFactory.getLog(this::class)

    /**
     * Set system environment variables.
     *
     * Tries to modify ProcessEnvironment.theEnvironment first; if that is not allowed
     * (e.g. reflection fails in a modular environment), falls back to modifying Collections\$UnmodifiableMap.
     *
     * @param vars Map(variable name, variable value)
     * @author AI: ChatGPT
     * @author K
     * @since 1.0.0
     */
    fun setEnvVars(vars: Map<String, String>) {
        // First approach: try to modify java.lang.ProcessEnvironment.theEnvironment / theCaseInsensitiveEnvironment
        try {
            val peClass = Class.forName("java.lang.ProcessEnvironment")
            val envField = peClass.getDeclaredField("theEnvironment").apply { isAccessible = true }
            val env = envField.get(null)
            require(updateMutableStringMap(env, vars, clearFirst = false)) {
                "ProcessEnvironment.theEnvironment is not a MutableMap<String, String>"
            }

            val cienvField = peClass.getDeclaredField("theCaseInsensitiveEnvironment").apply { isAccessible = true }
            val cienv = cienvField.get(null)
            require(updateMutableStringMap(cienv, vars, clearFirst = false)) {
                "ProcessEnvironment.theCaseInsensitiveEnvironment is not a MutableMap<String, String>"
            }

            return  // return directly if this branch did not throw
        } catch (_: Throwable) {
            // any reflection failure falls through to the fallback branch below
        }

        // Second approach: modify the underlying `m` field of Collections$UnmodifiableMap
        try {
            val env = System.getenv()
            val classes = Collections::class.java.declaredClasses
            for (cl in classes) {
                if (cl.name == "java.util.Collections\$UnmodifiableMap") {
                    val mField = cl.getDeclaredField("m").apply { isAccessible = true }
                    val internal = mField.get(env)
                    require(updateMutableStringMap(internal, vars, clearFirst = true)) {
                        "Collections\$UnmodifiableMap.m is not a MutableMap<String, String>"
                    }
                    return
                }
            }
        } catch (_: Throwable) {
            // final fallback: if this also fails, do nothing
        }
    }

    /**
     * Reflectively write new entries into a `MutableMap<String, String>`.
     * Used by [setEnvVars]: either clear via [clearFirst] then putAll, or keep existing entries and putAll incrementally.
     * Returns false on any reflection failure, key/value type mismatch, or method signature mismatch, so the caller can fall back.
     *
     * @param target the map reference obtained via reflection, may be null
     * @param vars entries to write
     * @param clearFirst whether to call map.clear() before writing
     * @return true if writing succeeded, false otherwise
     * @author K
     * @since 1.0.0
     */
    private fun updateMutableStringMap(target: Any?, vars: Map<String, String>, clearFirst: Boolean): Boolean {
        val map = target as? MutableMap<*, *> ?: return false
        if (!map.keys.all { it is String } || !map.values.all { it is String }) {
            return false
        }
        return runCatching {
            if (clearFirst) {
                val clearMethod = map.javaClass.methods.firstOrNull {
                    it.name == "clear" && it.parameterCount == 0
                } ?: return false
                clearMethod.invoke(map)
            }
            val putAllMethod = map.javaClass.methods.firstOrNull {
                it.name == "putAll" &&
                    it.parameterCount == 1 &&
                    Map::class.java.isAssignableFrom(it.parameterTypes[0])
            } ?: return false
            putAllMethod.invoke(map, vars)
            true
        }.getOrElse { false }
    }

    /**
     * Execute a single system command.
     *
     * @param command varargs of command components
     * @return Pair(whether the execution succeeded, execution result message)
     * @author K
     * @since 1.0.0
     */
    fun executeCommand(vararg command: String): Pair<Boolean, String?> {
        // ProcessBuilder can be used as an alternative
        val process = try {
            Runtime.getRuntime().exec(command)
        } catch (e: Throwable) {
            log.error(e, "Failed to execute system command [${command.joinToString(" ")}]!")
            return false to e.message
        }
        return try {
            // both streams must be drained to avoid the child process blocking on writes
            val stdout = loadStream(process.inputStream)
            val errorMsg = loadStream(process.errorStream)
            true to errorMsg.ifEmpty { stdout }
        } finally {
            process.destroy()
        }
    }

    /**
     * Read the [Process] output stream into a string using the default UTF-8 charset.
     * The stream is closed automatically once reading completes; a trailing newline is appended to non-empty output to mimic terminal behavior.
     *
     * @param inputStream the process stdout or stderr stream
     * @return the text read; an empty string if the stream is empty
     * @author K
     * @since 1.0.0
     */
    private fun loadStream(inputStream: InputStream): String {
        inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                val text = reader.lineSequence().joinToString("\n")
                return if (text.isEmpty()) "" else "$text\n"
            }
        }
    }

    /**
     * Determine whether the current runtime has a GUI (i.e., can use windows, tray icons, etc.).
     * Notes:
     * 1) Prefer AWT's headless check; if headless, treat it as having no GUI.
     * 2) On Linux, additionally consult X11/Wayland environment variables as a fallback (DISPLAY/WAYLAND_DISPLAY usually indicates a graphical session).
     * 3) Desktop.isDesktopSupported() can serve as a complementary check for "can perform desktop integration (open browser/file manager)".
     */
    fun hasGUI(): Boolean {
        // Explicit system property takes precedence: -Djava.awt.headless=true forces headless mode
        System.getProperty("java.awt.headless")?.lowercase()?.let {
            if (it == "true") return false
        }

        // AWT authoritative check: returns true = headless (usually no graphics stack or the current session is unavailable)
        if (GraphicsEnvironment.isHeadless()) {
            // Try environment-variable fallback for Linux (containers/remote sessions are sometimes misdetected)
            val os = System.getProperty("os.name").lowercase()
            if (os.contains("linux") || os.contains("bsd")) {
                val hasX11 = System.getenv("DISPLAY")?.isNotBlank() == true
                val hasWayland = System.getenv("WAYLAND_DISPLAY")?.isNotBlank() == true
                val sess = System.getenv("XDG_SESSION_TYPE")?.lowercase()
                if ((hasX11 || hasWayland) && sess != "tty") {
                    // Graphical session variables exist but AWT still reports headless -- most likely missing fonts or native window-system bindings.
                    // Err on the side of caution and still treat as "no GUI" to avoid later AWTError throws.
                    return false
                }
            }
            return false
        }

        // Reaching here, GUI capabilities are generally available
        return true
    }

    /**
     * The line separator of the current system.
     *
     * @author K
     * @since 1.0.0
     */
    val LINE_SEPARATOR: String = System.lineSeparator()

    /** Regex used to detect debug mode (`-Xdebug` or `jdwp`) in JVM startup arguments. */
    private val debugPattern = Pattern.compile("-Xdebug|jdwp")

    /**
     * Get the current operating system.
     *
     * @return the OS enum
     * @author AI: ChatGPT
     * @author K
     * @since 1.0.0
     */
    fun currentOs(): OsEnum {
        val osName = (System.getProperty("os.name") ?: "").lowercase()
        val osVersion = (System.getProperty("os.version") ?: "").lowercase()
//        val osArch = (System.getProperty("os.arch") ?: "").lowercase()

        fun hasKeyword(vararg keys: String): Boolean =
            keys.any { osName.contains(it) || osVersion.contains(it) }

        // ---- Mobile/embedded checks first (to avoid being swallowed by the Linux fallback) ----
        // Android: os.name is commonly "Linux", but java.vm.name / java.runtime.name may contain Android/ART/Dalvik
        val vmName = (System.getProperty("java.vm.name") ?: "").lowercase()
        val runtimeName = (System.getProperty("java.runtime.name") ?: "").lowercase()
        val isAndroid = (vmName.contains("dalvik") || vmName.contains("art") ||
                runtimeName.contains("android"))

        if (isAndroid) return OsEnum.ANDROID // if this enum is missing, map to LINUX or OTHER

        // HarmonyOS / OpenHarmony (NEXT, open-source branches, etc.)
        if (hasKeyword("openharmony", "harmonyos", "harmony")) {
            return OsEnum.HARMONY // if this enum is missing, map to LINUX or OTHER
        }

        // ---- Mainstream desktop / server ----
        if (hasKeyword("mac", "darwin", "os x", "mac os")) return OsEnum.MAC

        if (hasKeyword("windows")) return OsEnum.WINDOWS
        // Certain JVMs/environments may report these variants
        if (hasKeyword("mingw", "msys", "cygwin")) return OsEnum.WINDOWS

        // Linux: include keywords for common distributions (os.name is generally Linux, but add fallbacks)
        if (hasKeyword("linux", "gnu/linux", "nux", "ubuntu", "debian", "fedora", "centos", "rhel", "red hat", "alpine")) {
            return OsEnum.LINUX
        }

        // ---- BSD family ----
        if (hasKeyword("freebsd")) return OsEnum.FREEBSD
        if (hasKeyword("openbsd")) return OsEnum.OPENBSD
        if (hasKeyword("netbsd")) return OsEnum.NETBSD
        if (hasKeyword("dragonfly")) return OsEnum.DRAGONFLYBSD

        // ---- Solaris / Illumos ----
        if (hasKeyword("sunos", "solaris")) return OsEnum.SOLARIS
        if (hasKeyword("illumos")) return OsEnum.ILLUMOS

        // ---- IBM / UNIX family ----
        if (hasKeyword("aix")) return OsEnum.AIX
        if (hasKeyword("hp-ux", "hpux")) return OsEnum.HPUX

        // ---- Other Apple platforms (rarely host a JVM directly) ----
        if (hasKeyword("ios")) return OsEnum.IOS
        if (hasKeyword("tvos")) return OsEnum.TVOS
        if (hasKeyword("watchos")) return OsEnum.WATCHOS

        // ---- Fallback: rare / custom platforms ----
        // For container tooling, these are typically treated as OTHER
        return OsEnum.OTHER
    }


    /**
     * Whether debug mode is enabled.
     *
     * @return true if in debug mode, false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isDebug(): Boolean =
        ManagementFactory.getRuntimeMXBean().inputArguments.any { debugPattern.matcher(it).find() }


    /**
     * Get the current system user.
     *
     * @return the current user name
     * @author K
     * @since 1.0.0
     */
    fun getUser(): String = System.getProperty("user.name")

    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    // Wrapper for org.apache.commons.lang3.SystemUtils
    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    /**
     * Get the java home directory as a `File`.
     *
     * @return the directory
     * @throws SecurityException if a security manager exists and its `checkPropertyAccess` method does not allow access to the specified system property
     * @see System.getProperty
     * @author K
     * @since 1.0.0
     */
    fun getJavaHome(): File = SystemUtils.getJavaHome()

    /**
     * Get the IO temp directory as a `File`.
     *
     * @return the directory
     * @throws SecurityException if a security manager exists and its `checkPropertyAccess` method does not allow access to the specified system property
     * @see System.getProperty
     * @author K
     * @since 1.0.0
     */
    fun getJavaIoTmpDir(): File = SystemUtils.getJavaIoTmpDir()

    /**
     * Get the user directory as a `File`.
     *
     * @return the directory
     * @throws SecurityException if a security manager exists and its `checkPropertyAccess` method does not allow access to the specified system property
     * @see System.getProperty
     * @author K
     * @since 1.0.0
     */
    fun getUserDir(): File = SystemUtils.getUserDir()

    /**
     * Get the user home directory as a `File`.
     *
     * @return the directory
     * @throws SecurityException if a security manager exists and its `checkPropertyAccess` method does not allow access to the specified system property
     * @see System.getProperty
     * @author K
     * @since 1.0.0
     */
    fun getUserHome(): File = SystemUtils.getUserHome()

    /**
     * Check whether [.JAVA_AWT_HEADLESS] is `true`.
     *
     * @return `true` if `JAVA_AWT_HEADLESS` is `"true"`, otherwise `false`.
     * @see .JAVA_AWT_HEADLESS
     * @author K
     * @since 1.0.0
     */
    fun isJavaAwtHeadless(): Boolean = GraphicsEnvironment.isHeadless()

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Wrapper for org.apache.commons.lang3.SystemUtils
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

}
