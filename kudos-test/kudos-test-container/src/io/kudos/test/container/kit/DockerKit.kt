import io.kudos.base.enums.impl.OsEnum
import io.kudos.base.lang.SystemKit
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit


/**
 * docker操作工具类
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
object DockerKit {

    /**
     * 在运行 Testcontainers 之前调用：
     * - 如果 Docker 已经可用：直接返回
     * - 如果不可用：尝试启动 Docker，并等待其就绪
     */
    @JvmStatic
    fun ensureDockerRunning(
        waitTimeout: Duration = Duration.ofSeconds(90),
        pollInterval: Duration = Duration.ofMillis(700)
    ) {
        val install = detectDockerInstall()
        if (!install.installed) {
            throw IllegalStateException(buildNotInstalledMessage(install))
        }

        // 已安装：优先用 docker info 判断 daemon 是否可用
        if (isDockerRunning()) return

        // 尝试启动
        startDockerForCurrentOs(install)

        // 等待就绪
        val deadline = System.nanoTime() + waitTimeout.toNanos()
        while (System.nanoTime() < deadline) {
            if (isDockerRunning()) return
            Thread.sleep(pollInterval.toMillis().coerceAtLeast(200))
        }

        throw IllegalStateException(
            "检测到 Docker 已安装，但在 ${waitTimeout.seconds}s 内仍不可用。可能原因：\n" +
                    "- Docker Desktop 正在初始化/卡住\n" +
                    "- 权限弹窗未确认\n" +
                    "- Linux 下 docker 服务未启动或当前用户无权限访问 /var/run/docker.sock\n"
        )
    }

    // ---------- 安装检测 ----------

    private data class InstallInfo(
        val installed: Boolean,
        val cliAvailable: Boolean,
        val desktopAppFound: Boolean,
        val hints: List<String> = emptyList()
    )

    private fun detectDockerInstall(): InstallInfo {
        val os = SystemKit.currentOs()

        val cliAvailable = isDockerCliAvailable()
        val desktopFound = when (os) {
            OsEnum.MAC -> File("/Applications/Docker.app").exists()
            OsEnum.WINDOWS -> windowsDockerDesktopExeCandidates().any { File(it).exists() }
            OsEnum.LINUX -> false // Linux 通常不是 Desktop，主要看 CLI/daemon
            else -> false
        }

        val installed = when (os) {
            OsEnum.MAC -> cliAvailable || desktopFound
            OsEnum.WINDOWS -> cliAvailable || desktopFound
            OsEnum.LINUX -> cliAvailable // Linux 以 docker CLI 是否存在为主
            else -> cliAvailable
        }

        val hints = when (os) {
            OsEnum.MAC -> listOf("macOS：安装 Docker Desktop（Applications 里应出现 Docker.app）。")
            OsEnum.WINDOWS -> listOf(
                "Windows：安装 Docker Desktop（或使用 WSL2 + Docker）。",
                "若公司环境限制安装，可让管理员协助。"
            )
            OsEnum.LINUX -> listOf(
                "Linux：安装 Docker Engine（docker CLI + dockerd）。",
                "安装后通常需要启动服务：systemctl start docker，并把用户加入 docker 组。"
            )
            else -> listOf("请先安装 Docker。")
        }

        return InstallInfo(installed = installed, cliAvailable = cliAvailable, desktopAppFound = desktopFound, hints = hints)
    }

    private fun buildNotInstalledMessage(info: InstallInfo): String {
        return buildString {
            appendLine("未检测到 Docker 已安装，因此无法自动启动。")
            appendLine("检测结果：")
            appendLine("- docker CLI 可用：${info.cliAvailable}")
            appendLine("- Docker Desktop 应用存在：${info.desktopAppFound}")
            appendLine()
            appendLine("处理建议：")
            info.hints.forEach { appendLine("- $it") }
            appendLine()
            appendLine("（提示：如果你是通过 IDE 运行测试，确认 IDE 的 PATH/环境变量与终端一致。）")
        }
    }

    // ---------- 运行检测 ----------

    private fun isDockerRunning(): Boolean {
        val r = runCommand(listOf("docker", "info"), timeoutMillis = 12_000)
        return r.exitCode == 0
    }

    private fun isDockerCliAvailable(): Boolean {
        val r = runCommand(listOf("docker", "--version"), timeoutMillis = 6_000)
        return r.exitCode == 0
    }

    // ---------- 启动逻辑 ----------

    private fun startDockerForCurrentOs(install: InstallInfo) {
        when (SystemKit.currentOs()) {
            OsEnum.MAC -> startDockerOnMac(install)
            OsEnum.WINDOWS -> startDockerOnWindows(install)
            OsEnum.LINUX -> startDockerOnLinux()
            else -> throw IllegalStateException("不支持的操作系统，无法自动启动 Docker。")
        }
    }

    private fun startDockerOnMac(install: InstallInfo) {
        // 如果 Desktop 存在，直接 open
        if (install.desktopAppFound) {
            runCommand(listOf("open", "-g", "-a", "Docker"), timeoutMillis = 10_000)
        } else {
            // 只有 CLI 的情况很少见；仍然尝试 open，失败也没关系
            runCommand(listOf("open", "-g", "-a", "Docker"), timeoutMillis = 10_000)
        }

        // 兜底：少数机器 App 名可能不同
        if (!isDockerRunning()) {
            runCommand(listOf("open", "-g", "-a", "Docker Desktop"), timeoutMillis = 10_000)
        }
    }

    private fun startDockerOnWindows(install: InstallInfo) {
        // 1) 先尝试启动服务（若存在）
        val sc = runCommand(listOf("sc", "start", "com.docker.service"), timeoutMillis = 15_000)
        if (sc.exitCode == 0) return

        // 2) 找 Desktop exe 直接启动
        val exe = windowsDockerDesktopExeCandidates().firstOrNull { File(it).exists() }
        if (exe != null) {
            runCommand(
                listOf(
                    "powershell",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-Command", "Start-Process -FilePath '${exe.replace("'", "''")}'"
                ),
                timeoutMillis = 15_000
            )
        } else {
            // 理论上 install 已判定存在，这里再兜底一次
            throw IllegalStateException("检测到 Docker 已安装，但找不到 Docker Desktop 可执行文件，无法自动启动。")
        }
    }

    private fun startDockerOnLinux() {
        // Linux 主要是启动服务
        val systemctl = runCommand(listOf("systemctl", "start", "docker"), timeoutMillis = 15_000)
        if (systemctl.exitCode == 0) return

        val service = runCommand(listOf("service", "docker", "start"), timeoutMillis = 15_000)
        if (service.exitCode == 0) return

        // 再兜底：尝试启动 dockerd
        runCommand(listOf("sh", "-lc", "nohup dockerd >/tmp/dockerd.log 2>&1 &"), timeoutMillis = 8_000)
    }

    private fun windowsDockerDesktopExeCandidates(): List<String> = listOf(
        """C:\Program Files\Docker\Docker\Docker Desktop.exe""",
        """C:\Program Files (x86)\Docker\Docker\Docker Desktop.exe"""
    )

    private data class CmdResult(val exitCode: Int, val stdout: String, val stderr: String)

    private fun runCommand(cmd: List<String>, timeoutMillis: Long): CmdResult {
        return try {
            val p = ProcessBuilder(cmd).start()

            val stdout = StringBuilder()
            val stderr = StringBuilder()

            val outThread = Thread {
                p.inputStream.bufferedReader().useLines { it.forEach { line -> stdout.appendLine(line) } }
            }
            val errThread = Thread {
                p.errorStream.bufferedReader().useLines { it.forEach { line -> stderr.appendLine(line) } }
            }
            outThread.isDaemon = true
            errThread.isDaemon = true
            outThread.start()
            errThread.start()

            val finished = p.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
            if (!finished) {
                p.destroyForcibly()
                return CmdResult(124, stdout.toString(), "Command timed out: ${cmd.joinToString(" ")}\n$stderr")
            }

            outThread.join(200)
            errThread.join(200)

            CmdResult(p.exitValue(), stdout.toString(), stderr.toString())
        } catch (e: Exception) {
            // 127 类似“命令不存在”
            CmdResult(127, "", e.toString())
        }
    }

}
