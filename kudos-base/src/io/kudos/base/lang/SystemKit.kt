package io.kudos.base.lang

import io.kudos.base.logger.LogFactory
import org.apache.commons.lang3.SystemUtils
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
     * @author ChatGPT
     * @author K
     * @since 1.0.0
     */
    fun setEnvVars(vars: Map<String, String>) {
        // 第一种方式：尝试修改 java.lang.ProcessEnvironment.theEnvironment / theCaseInsensitiveEnvironment
        try {
            val peClass = Class.forName("java.lang.ProcessEnvironment")
            val envField = peClass.getDeclaredField("theEnvironment").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val env = envField.get(null) as MutableMap<String, String>
            env.putAll(vars)

            val cienvField = peClass.getDeclaredField("theCaseInsensitiveEnvironment").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val cienv = cienvField.get(null) as MutableMap<String, String>
            cienv.putAll(vars)

            return  // 如果这一段没有抛异常，就直接返回
        } catch (_: Throwable) {
            // 任何反射失败都走下面的备用分支
        }

        // 第二种方式：修改 Collections$UnmodifiableMap 底层的 m 字段
        try {
            val env: MutableMap<String, String> = System.getenv() as MutableMap<String, String>
            val classes = Collections::class.java.declaredClasses
            for (cl in classes) {
                if (cl.name == "java.util.Collections\$UnmodifiableMap") {
                    val mField = cl.getDeclaredField("m").apply { isAccessible = true }
                    @Suppress("UNCHECKED_CAST")
                    val internal: MutableMap<String, String> = mField.get(env) as MutableMap<String, String>
                    internal.clear()
                    internal.putAll(vars)
                    return
                }
            }
        } catch (_: Throwable) {
            // 兜底：如果这里也失败，就不做任何事
        }
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
                val buffer = StringBuffer()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    buffer.append(line).append("\n")
                }
                return buffer.toString()
            }
        }
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
     * 获取当前操作系统名称.
     *
     * @return 操作系统名称 例如:windows xp,linux 等.
     * @author K
     * @since 1.0.0
     */
    fun getOSName(): String = System.getProperty("os.name").lowercase()

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
     * 是否为windows操作系统
     *
     * @return true: 为windows操作系统，反之为false
     * @author K
     * @since 1.0.0
     */
    fun isWindowsOS(): Boolean = getOSName().lowercase().contains("windows")

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
    fun isJavaAwtHeadless(): Boolean = SystemUtils.isJavaAwtHeadless()

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // 封装org.apache.commons.lang3.SystemUtils
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

}