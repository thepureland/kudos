package io.kudos.test.container.kit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the Docker-daemon-independent surface of [DockerKit]:
 * - [DockerKit.runCommand] success, non-zero exit, timeout (124), and command-not-found (127)
 * - [DockerKit.detectDockerInstall] returning a coherent [DockerKit.InstallInfo]
 * - [DockerKit.buildNotInstalledMessage] formatting all detection fields + hints
 * - [DockerKit.windowsDockerDesktopExeCandidates] list
 * - CLI/daemon probes returning a boolean without throwing
 *
 * The actual `docker` probes depend on the agent's environment, so only the invariants that hold
 * regardless are asserted.
 *
 * @author K
 * @since 1.0.0
 */
internal class DockerKitTest {

    @Test
    fun `runCommand returns 127 for a non-existent executable`() {
        val result = DockerKit.runCommand(listOf("kudos-no-such-binary-xyz"), timeoutMillis = 2_000)
        assertEquals(127, result.exitCode)
        assertTrue(result.stderr.isNotEmpty())
    }

    @Test
    fun `runCommand captures stdout of a real command`() {
        val os = System.getProperty("os.name").lowercase()
        val cmd = if (os.contains("win")) {
            listOf("cmd", "/c", "echo", "kudos-hello")
        } else {
            listOf("sh", "-c", "echo kudos-hello")
        }
        val result = DockerKit.runCommand(cmd, timeoutMillis = 10_000)
        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("kudos-hello"))
    }

    @Test
    fun `runCommand returns non-zero exit for failing command`() {
        val os = System.getProperty("os.name").lowercase()
        val cmd = if (os.contains("win")) {
            listOf("cmd", "/c", "exit 3")
        } else {
            listOf("sh", "-c", "exit 3")
        }
        val result = DockerKit.runCommand(cmd, timeoutMillis = 10_000)
        assertEquals(3, result.exitCode)
    }

    @Test
    fun `runCommand times out and returns 124`() {
        // 'ping -n 5 127.0.0.1' on Windows takes ~4s; a 300ms budget forces destroyForcibly + 124.
        val os = System.getProperty("os.name").lowercase()
        val cmd = if (os.contains("win")) {
            listOf("ping", "-n", "5", "127.0.0.1")
        } else {
            listOf("sleep", "5")
        }
        val result = DockerKit.runCommand(cmd, timeoutMillis = 300)
        assertEquals(124, result.exitCode)
        assertTrue(result.stderr.contains("timed out"))
    }

    @Test
    fun `detectDockerInstall yields coherent install info`() {
        val info = DockerKit.detectDockerInstall()
        // installed implies at least one positive signal on this OS branch
        if (info.installed) {
            assertTrue(info.cliAvailable || info.desktopAppFound)
        }
        assertTrue(info.hints.isNotEmpty())
    }

    @Test
    fun `buildNotInstalledMessage includes detection fields and hints`() {
        val info = DockerKit.InstallInfo(
            installed = false,
            cliAvailable = false,
            desktopAppFound = true,
            hints = listOf("hint-one", "hint-two")
        )
        val msg = DockerKit.buildNotInstalledMessage(info)
        assertTrue(msg.contains("Docker is not installed"))
        assertTrue(msg.contains("docker CLI available: false"))
        assertTrue(msg.contains("Docker Desktop application present: true"))
        assertTrue(msg.contains("hint-one"))
        assertTrue(msg.contains("hint-two"))
        assertTrue(msg.contains("IDE"))
    }

    @Test
    fun `windows desktop exe candidates cover both program files`() {
        val candidates = DockerKit.windowsDockerDesktopExeCandidates()
        assertEquals(2, candidates.size)
        assertTrue(candidates.any { it.contains("Program Files\\Docker") })
        assertTrue(candidates.any { it.contains("Program Files (x86)") })
    }

    @Test
    fun `cli and daemon probes return without throwing`() {
        // Either true or false is acceptable; just must not blow up.
        val cli = DockerKit.isDockerCliAvailable()
        val running = DockerKit.isDockerRunning()
        assertTrue(cli || !cli)
        assertTrue(running || !running)
    }

    @Test
    fun `InstallInfo and CmdResult data classes expose their fields`() {
        val info = DockerKit.InstallInfo(true, true, false)
        assertEquals(true, info.installed)
        assertEquals(true, info.cliAvailable)
        assertEquals(false, info.desktopAppFound)
        assertTrue(info.hints.isEmpty())
        assertTrue(info.toString().contains("InstallInfo"))

        val r = DockerKit.CmdResult(0, "out", "err")
        assertEquals(0, r.exitCode)
        assertEquals("out", r.stdout)
        assertEquals("err", r.stderr)
        assertTrue(r.copy(exitCode = 1).exitCode == 1)
    }
}
