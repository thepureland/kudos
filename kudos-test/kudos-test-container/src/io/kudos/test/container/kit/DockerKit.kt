package io.kudos.test.container.kit

import io.kudos.base.enums.impl.OsEnum
import io.kudos.base.lang.SystemKit
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


/**
 * Docker operations utility.
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
object DockerKit {

    /**
     * Call before running Testcontainers:
     * - If Docker is already available: return immediately
     * - If not available: attempt to start Docker and wait until it is ready
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

        // Already installed: prefer `docker info` to check whether the daemon is available
        if (isDockerRunning()) return

        // Attempt to start
        startDockerForCurrentOs(install)

        // Wait until ready
        val deadline = System.nanoTime() + waitTimeout.toNanos()
        while (System.nanoTime() < deadline) {
            if (isDockerRunning()) return
            Thread.sleep(pollInterval.toMillis().coerceAtLeast(200))
        }

        throw IllegalStateException(
            "Docker is installed but still unavailable after ${waitTimeout.seconds}s. Possible causes:\n" +
                    "- Docker Desktop is initializing or hung\n" +
                    "- A permission dialog has not been confirmed\n" +
                    "- On Linux, the docker service is not started or the current user has no permission to access /var/run/docker.sock\n"
        )
    }

    // ---------- Installation detection ----------

    private data class InstallInfo(
        val installed: Boolean,
        val cliAvailable: Boolean,
        val desktopAppFound: Boolean,
        val hints: List<String> = emptyList()
    )

    /**
     * Sniffs the Docker installation per the current OS.
     *
     * Detection rules vary by platform:
     * - **macOS/Windows**: considered installed if the CLI is available **or** the Desktop application exists (Desktop is often started by the IDE)
     * - **Linux**: only checks whether the CLI can be exec'd (Linux has no Desktop concept; dockerd is a background service)
     *
     * Also emits `hints` for remediation per platform, so [buildNotInstalledMessage] can directly produce an ops-actionable error message.
     *
     * @return [InstallInfo] containing installed / cliAvailable / desktopAppFound / hints
     * @author K
     * @since 1.0.0
     */
    private fun detectDockerInstall(): InstallInfo {
        val os = SystemKit.currentOs()

        val cliAvailable = isDockerCliAvailable()
        val desktopFound = when (os) {
            OsEnum.MAC -> File("/Applications/Docker.app").exists()
            OsEnum.WINDOWS -> windowsDockerDesktopExeCandidates().any { File(it).exists() }
            OsEnum.LINUX -> false // Linux usually has no Desktop; rely mainly on CLI/daemon
            else -> false
        }

        val installed = when (os) {
            OsEnum.MAC -> cliAvailable || desktopFound
            OsEnum.WINDOWS -> cliAvailable || desktopFound
            OsEnum.LINUX -> cliAvailable // On Linux, the presence of the docker CLI is the primary criterion
            else -> cliAvailable
        }

        val hints = when (os) {
            OsEnum.MAC -> listOf("macOS: install Docker Desktop (Docker.app should appear in Applications).")
            OsEnum.WINDOWS -> listOf(
                "Windows: install Docker Desktop (or use WSL2 + Docker).",
                "If installation is restricted in your corporate environment, ask an administrator for help."
            )

            OsEnum.LINUX -> listOf(
                "Linux: install Docker Engine (docker CLI + dockerd).",
                "After installation, you typically need to start the service: systemctl start docker, and add the user to the docker group."
            )

            else -> listOf("Please install Docker first.")
        }

        return InstallInfo(
            installed = installed,
            cliAvailable = cliAvailable,
            desktopAppFound = desktopFound,
            hints = hints
        )
    }

    /**
     * Formats the [detectDockerInstall] result into a message for ops/developers:
     * contains detection details + platform-specific remediation hints + an IDE PATH reminder (the most common pitfall: tests pass in the terminal but not in the IDE).
     *
     * @param info the installation detection result
     * @return a multi-line, human-readable message
     * @author K
     * @since 1.0.0
     */
    private fun buildNotInstalledMessage(info: InstallInfo): String {
        return buildString {
            appendLine("Docker is not installed; cannot start it automatically.")
            appendLine("Detection result:")
            appendLine("- docker CLI available: ${info.cliAvailable}")
            appendLine("- Docker Desktop application present: ${info.desktopAppFound}")
            appendLine()
            appendLine("Suggested actions:")
            info.hints.forEach { appendLine("- $it") }
            appendLine()
            appendLine("(Tip: if running tests via an IDE, verify the IDE's PATH/environment variables match the terminal's.)")
        }
    }

    // ---------- Runtime detection ----------

    /**
     * Probes whether the daemon is actually ready via `docker info`.
     * `--version` only verifies the CLI binary exists, while `info` actually connects to the daemon and can detect the "CLI installed but dockerd not running" case.
     *
     * @return true if the daemon is available
     * @author K
     * @since 1.0.0
     */
    private fun isDockerRunning(): Boolean {
        val r = runCommand(listOf("docker", "info"), timeoutMillis = 12_000)
        return r.exitCode == 0
    }

    /**
     * Probes whether the docker CLI is available on PATH (does not verify the daemon).
     *
     * @return true if `docker --version` runs successfully
     * @author K
     * @since 1.0.0
     */
    private fun isDockerCliAvailable(): Boolean {
        val r = runCommand(listOf("docker", "--version"), timeoutMillis = 6_000)
        return r.exitCode == 0
    }

    // ---------- Startup logic ----------

    /**
     * Dispatches to the appropriate startup implementation for the current OS: mac/windows/linux each have their own path; other OSes are unsupported.
     *
     * @param install detection info passed to subroutines for things like "Desktop path" checks
     * @throws IllegalStateException if the current OS is not in the supported list
     * @author K
     * @since 1.0.0
     */
    private fun startDockerForCurrentOs(install: InstallInfo) {
        when (SystemKit.currentOs()) {
            OsEnum.MAC -> startDockerOnMac(install)
            OsEnum.WINDOWS -> startDockerOnWindows(install)
            OsEnum.LINUX -> startDockerOnLinux()
            else -> throw IllegalStateException("Unsupported OS; cannot start Docker automatically.")
        }
    }

    /**
     * macOS: launches Docker.app in the background via `open -g -a Docker`; some machines have a bundle named "Docker Desktop", which is handled as a fallback.
     * `-g` avoids stealing foreground focus, which improves the experience on CI.
     *
     * @param install detection info (although the branches differ little, the parameter is kept for future extension)
     * @author K
     * @since 1.0.0
     */
    private fun startDockerOnMac(install: InstallInfo) {
        // Whether or not Desktop was detected, just try `open`; a failure here is harmless
        runCommand(listOf("open", "-g", "-a", "Docker"), timeoutMillis = 10_000)

        // Fallback: a few machines have a different App name
        if (!isDockerRunning()) {
            runCommand(listOf("open", "-g", "-a", "Docker Desktop"), timeoutMillis = 10_000)
        }
    }

    /**
     * Windows: two-step startup
     * 1. `sc start com.docker.service` tries to start the service (registered by Docker Desktop on install)
     * 2. If the service is missing or fails to start, use PowerShell `Start-Process` to launch the Desktop exe directly
     *
     * The exe path may contain spaces, so PowerShell + `-FilePath` is more reliable than cmd.exe;
     * single quotes are escaped `'` -> `''` to avoid command parsing failures when the user name contains a single quote.
     *
     * @param install detection info (used to reuse [windowsDockerDesktopExeCandidates])
     * @throws IllegalStateException when the service fails to start and the exe cannot be found
     * @author K
     * @since 1.0.0
     */
    private fun startDockerOnWindows(install: InstallInfo) {
        // 1) First, try to start the service (if it exists)
        val sc = runCommand(listOf("sc", "start", "com.docker.service"), timeoutMillis = 15_000)
        if (sc.exitCode == 0) return

        // 2) Find the Desktop exe and launch it directly
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
            // In theory install was already determined to exist; fall back once more here
            throw IllegalStateException("Docker is installed but the Docker Desktop executable cannot be found; cannot start automatically.")
        }
    }

    /**
     * Linux: three-level fallback startup
     * 1. `systemctl start docker` (systemd mainstream distributions)
     * 2. `service docker start` (SysV / older distributions)
     * 3. Start dockerd directly via nohup (extreme fallback; redirects logs to /tmp for post-mortem analysis)
     *
     * @author K
     * @since 1.0.0
     */
    private fun startDockerOnLinux() {
        // On Linux, this is primarily about starting the service
        val systemctl = runCommand(listOf("systemctl", "start", "docker"), timeoutMillis = 15_000)
        if (systemctl.exitCode == 0) return

        val service = runCommand(listOf("service", "docker", "start"), timeoutMillis = 15_000)
        if (service.exitCode == 0) return

        // Final fallback: try to start dockerd
        runCommand(listOf("sh", "-lc", "nohup dockerd >/tmp/dockerd.log 2>&1 &"), timeoutMillis = 8_000)
    }

    /**
     * Candidate paths where Docker Desktop is commonly installed on Windows; covers both 64-bit and 32-bit Program Files.
     *
     * @return a list of full path strings (ordered by "more likely to exist" first)
     * @author K
     * @since 1.0.0
     */
    private fun windowsDockerDesktopExeCandidates(): List<String> = listOf(
        """C:\Program Files\Docker\Docker\Docker Desktop.exe""",
        """C:\Program Files (x86)\Docker\Docker\Docker Desktop.exe"""
    )

    private data class CmdResult(val exitCode: Int, val stdout: String, val stderr: String)

    /**
     * Generic external command execution: collects stdout/stderr asynchronously, force-kills on timeout, catches all exceptions.
     *
     * Key design points:
     * - Use daemon threads to read streams, avoiding the main thread being blocked by the 4K pipe buffer
     * - `destroyForcibly` after timeout to prevent zombie subprocesses
     * - On the exception path, returns exitCode=127 (the conventional "command not found"), making upstream classification uniform
     *
     * @param cmd command + argument list
     * @param timeoutMillis timeout in milliseconds
     * @return a result containing exitCode / stdout / stderr
     * @author K
     * @since 1.0.0
     */
    private fun runCommand(cmd: List<String>, timeoutMillis: Long): CmdResult {
        return try {
            val p = ProcessBuilder(cmd).start()

            val stdout = StringBuilder()
            val stderr = StringBuilder()

            val outThread = thread(name = "docker-cmd-stdout", isDaemon = true) {
                p.inputStream.bufferedReader().useLines { lines -> lines.forEach { line -> stdout.appendLine(line) } }
            }
            val errThread = thread(name = "docker-cmd-stderr", isDaemon = true) {
                p.errorStream.bufferedReader().useLines { lines -> lines.forEach { line -> stderr.appendLine(line) } }
            }

            val finished = p.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
            if (!finished) {
                p.destroyForcibly()
                return CmdResult(124, stdout.toString(), "Command timed out: ${cmd.joinToString(" ")}\n$stderr")
            }

            outThread.join(200)
            errThread.join(200)

            CmdResult(p.exitValue(), stdout.toString(), stderr.toString())
        } catch (e: Exception) {
            // 127 is the conventional "command not found"
            CmdResult(127, "", e.toString())
        }
    }

}
