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
import java.util.*
import java.util.regex.Pattern

/**
 * 系统工具类
 *
 * @author K
 * @author AI: ChatGPT
 * @since 1.0.0
 */
object SystemKit {

    private val log = LogFactory.getLog(this::class)

    /**
     * 设置系统环境变量
     *
     * 尝试先通过修改 ProcessEnvironment.theEnvironment，如果不允许（如模块化环境导致反射失败），
     * 再回退到修改 Collections\$UnmodifiableMap 的方式。
     *
     * @param vars Map(变量名，变量值)
     * @author AI: ChatGPT
     * @author K
     * @since 1.0.0
     */
    fun setEnvVars(vars: Map<String, String>) {
        // 第一种方式：尝试修改 java.lang.ProcessEnvironment.theEnvironment / theCaseInsensitiveEnvironment
        try {
            val peClass = Class.forName("java.lang.ProcessEnvironment")
            val envField = peClass.getDeclaredField("theEnvironment").apply { isAccessible = true }
            val env = envField.get(null)
            require(updateMutableStringMap(env, vars, clearFirst = false)) {
                "ProcessEnvironment.theEnvironment 类型不是 MutableMap<String, String>"
            }

            val cienvField = peClass.getDeclaredField("theCaseInsensitiveEnvironment").apply { isAccessible = true }
            val cienv = cienvField.get(null)
            require(updateMutableStringMap(cienv, vars, clearFirst = false)) {
                "ProcessEnvironment.theCaseInsensitiveEnvironment 类型不是 MutableMap<String, String>"
            }

            return  // 如果这一段没有抛异常，就直接返回
        } catch (_: Throwable) {
            // 任何反射失败都走下面的备用分支
        }

        // 第二种方式：修改 Collections$UnmodifiableMap 底层的 m 字段
        try {
            val env = System.getenv()
            val classes = Collections::class.java.declaredClasses
            for (cl in classes) {
                if (cl.name == "java.util.Collections\$UnmodifiableMap") {
                    val mField = cl.getDeclaredField("m").apply { isAccessible = true }
                    val internal = mField.get(env)
                    require(updateMutableStringMap(internal, vars, clearFirst = true)) {
                        "Collections\$UnmodifiableMap.m 类型不是 MutableMap<String, String>"
                    }
                    return
                }
            }
        } catch (_: Throwable) {
            // 兜底：如果这里也失败，就不做任何事
        }
    }

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
     * 执行单个系统命令
     *
     * @param command 命令组成部分的可变数组
     * @return Pair(是否执行成功，执行结果信息)
     * @author K
     * @since 1.0.0
     */
    fun executeCommand(vararg command: String): Pair<Boolean, String?> {
        var process: Process? = null // 也可用ProcessBuilder构建
        var message: String? = null
        val success = try {
            process = Runtime.getRuntime().exec(command)
            true
        } catch (e: Throwable) {
            message = e.message
            log.error(e, "执行系统命令【${command.joinToString(" ")}】出错！")
            false
        }

//        if (wait) {
//            try {
//                process.waitFor()
//            } catch (e: InterruptedException) {
//                e.printStackTrace()
//            }
//        }


        if (process != null) {
            message = loadStream(process.inputStream)
            val errorMsg = loadStream(process.errorStream)
            if (errorMsg.isNotEmpty()) {
                message = errorMsg
            }
            process.destroy()
        }

        return success to message
    }

    private fun loadStream(inputStream: InputStream): String {
        inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                val text = reader.lineSequence().joinToString("\n")
                return if (text.isEmpty()) "" else "$text\n"
            }
        }
    }

    /**
     * 判断当前运行环境是否具备 GUI（能否使用窗口/托盘等图形能力）
     * 说明：
     * 1) 首选 AWT 的 headless 判断；若为 headless，则基本视为无 GUI。
     * 2) 对 Linux 再结合 X11/Wayland 环境变量做兜底判断（有 DISPLAY/WAYLAND_DISPLAY 通常有图形会话）。
     * 3) Desktop.isDesktopSupported() 可作为“是否能做桌面集成（打开浏览器/文件管理器）”的补充能力判断。
     */
    fun hasGUI(): Boolean {
        // 显式系统属性优先：-Djava.awt.headless=true 会强制“无头”
        System.getProperty("java.awt.headless")?.lowercase()?.let {
            if (it == "true") return false
        }

        // AWT 权威判断：若返回 true = 无头（通常没有图形栈或当前会话不可用）
        if (GraphicsEnvironment.isHeadless()) {
            // 尝试对 Linux 做环境变量兜底（有时容器/远程会误判）
            val os = System.getProperty("os.name").lowercase()
            if (os.contains("linux") || os.contains("bsd")) {
                val hasX11 = System.getenv("DISPLAY")?.isNotBlank() == true
                val hasWayland = System.getenv("WAYLAND_DISPLAY")?.isNotBlank() == true
                val sess = System.getenv("XDG_SESSION_TYPE")?.lowercase()
                if ((hasX11 || hasWayland) && sess != "tty") {
                    // 存在图形会话变量，但 AWT 仍认为 headless —— 多半是缺字体/缺本地窗口系统绑定
                    // 从谨慎角度，仍按“无 GUI”处理，避免后续抛 AWTError
                    return false
                }
            }
            return false
        }

        // 能走到这里，一般已具备 GUI 能力
        return true
    }

    /**
     * 当前系统的回车换行符
     *
     * @author K
     * @since 1.0.0
     */
    val LINE_SEPARATOR: String = System.lineSeparator()

    private val debugPattern = Pattern.compile("-Xdebug|jdwp")

    /**
     * 获取当前操作系统
     *
     * @return 操作系统枚举
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

        // ---- 移动/嵌入式优先判定（避免被 Linux 兜底吞掉） ----
        // Android: 常见 os.name = "Linux"，但 java.vm.name / java.runtime.name 里可能带 Android/ART/Dalvik
        val vmName = (System.getProperty("java.vm.name") ?: "").lowercase()
        val runtimeName = (System.getProperty("java.runtime.name") ?: "").lowercase()
        val isAndroid = (vmName.contains("dalvik") || vmName.contains("art") ||
                runtimeName.contains("android"))

        if (isAndroid) return OsEnum.ANDROID // 如果没有该枚举，可映射到 LINUX 或 OTHER

        // HarmonyOS / OpenHarmony（NEXT/开源分支等）
        if (hasKeyword("openharmony", "harmonyos", "harmony")) {
            return OsEnum.HARMONY // 没有该枚举可映射到 LINUX 或 OTHER
        }

        // ---- 桌面/服务器主流 ----
        if (hasKeyword("mac", "darwin", "os x", "mac os")) return OsEnum.MAC

        if (hasKeyword("windows")) return OsEnum.WINDOWS
        // 某些 JVM/环境可能会出现这些写法
        if (hasKeyword("mingw", "msys", "cygwin")) return OsEnum.WINDOWS

        // Linux：包含很多发行版关键字（一般 os.name 就是 Linux，但加点兜底）
        if (hasKeyword("linux", "gnu/linux", "nux", "ubuntu", "debian", "fedora", "centos", "rhel", "red hat", "alpine")) {
            return OsEnum.LINUX
        }

        // ---- BSD 家族 ----
        if (hasKeyword("freebsd")) return OsEnum.FREEBSD
        if (hasKeyword("openbsd")) return OsEnum.OPENBSD
        if (hasKeyword("netbsd")) return OsEnum.NETBSD
        if (hasKeyword("dragonfly")) return OsEnum.DRAGONFLYBSD

        // ---- Solaris / Illumos ----
        if (hasKeyword("sunos", "solaris")) return OsEnum.SOLARIS
        if (hasKeyword("illumos")) return OsEnum.ILLUMOS

        // ---- IBM / UNIX 系 ----
        if (hasKeyword("aix")) return OsEnum.AIX
        if (hasKeyword("hp-ux", "hpux")) return OsEnum.HPUX

        // ---- Apple 其他平台（理论上 JVM 很少直接跑）----
        if (hasKeyword("ios")) return OsEnum.IOS
        if (hasKeyword("tvos")) return OsEnum.TVOS
        if (hasKeyword("watchos")) return OsEnum.WATCHOS

        // ---- 兜底：一些极少见/自定义 ----
        // 对于容器工具来说，这类通常当作 OTHER 处理即可
        return OsEnum.OTHER
    }


    /**
     * 是否调试模式
     *
     * @return true:调试模式，反之为false
     * @author K
     * @since 1.0.0
     */
    fun isDebug(): Boolean {
        for (arg in ManagementFactory.getRuntimeMXBean().inputArguments) {
            if (debugPattern.matcher(arg).find()) {
                return true
            }
        }
        return false
    }


    /**
     * 得到系统当前用户
     *
     * @return 当前用户名
     * @author K
     * @since 1.0.0
     */
    fun getUser(): String = System.getProperty("user.name")

    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    // 封装org.apache.commons.lang3.SystemUtils
    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    /**
     * 获取java home目录, 并以`File`返回
     *
     * @return 目录
     * @throws SecurityException 如果安全管理器存在并且它的 `checkPropertyAccess` 方法不允许访问特别的系统属性
     * @see System.getProperty
     * @author K
     * @since 1.0.0
     */
    fun getJavaHome(): File = SystemUtils.getJavaHome()

    /**
     * 获取IO临时目录, 并以`File`返回
     *
     * @return 目录
     * @throws SecurityException 如果安全管理器存在并且它的 `checkPropertyAccess` 方法不允许访问特别的系统属性
     * @see System.getProperty
     * @author K
     * @since 1.0.0
     */
    fun getJavaIoTmpDir(): File = SystemUtils.getJavaIoTmpDir()

    /**
     * 获取用户目录, 并以`File`返回
     *
     * @return 目录
     * @throws SecurityException 如果安全管理器存在并且它的 `checkPropertyAccess` 方法不允许访问特别的系统属性
     * @see System.getProperty
     * @author K
     * @since 1.0.0
     */
    fun getUserDir(): File = SystemUtils.getUserDir()

    /**
     * 获取用户home目录, 并以`File`返回
     *
     * @return 目录
     * @throws SecurityException 如果安全管理器存在并且它的 `checkPropertyAccess` 方法不允许访问特别的系统属性
     * @see System.getProperty
     * @author K
     * @since 1.0.0
     */
    fun getUserHome(): File = SystemUtils.getUserHome()

    /**
     * 检测 [.JAVA_AWT_HEADLESS] 値是否为 `true`.
     *
     * @return `true` 如果 `JAVA_AWT_HEADLESS` 为 `"true"`, 否则返回 `false`.
     * @see .JAVA_AWT_HEADLESS
     * @author K
     * @since 1.0.0
     */
    fun isJavaAwtHeadless(): Boolean = GraphicsEnvironment.isHeadless()

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // 封装org.apache.commons.lang3.SystemUtils
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

}