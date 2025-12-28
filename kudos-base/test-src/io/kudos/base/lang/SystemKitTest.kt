package io.kudos.base.lang

import io.kudos.base.enums.impl.OsEnum
import org.apache.commons.lang3.SystemUtils
import org.junit.jupiter.api.TestInstance
import kotlin.test.*


/**
 * test for SystemKit
 *
 * @author ChatGPT
 * @author K
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SystemKitTest {

    private lateinit var originalEnv: Map<String, String>

    @BeforeTest
    fun setupOriginalEnvironment() {
        // 备份当前的环境变量，以便测试结束后能够恢复
        originalEnv = System.getenv().toMap()
    }

    @AfterTest
    fun restoreOriginalEnvironment() {
        // 尽量将环境变量恢复到测试前的状态
        try {
            SystemKit.setEnvVars(originalEnv)
        } catch (_: Throwable) {
            // 某些 JVM 实现可能不允许修改所有环境变量，这里忽略异常
        }
    }

    @Test
    fun testSetEnvVarsDoesNotThrow_ForNewVariables() {
        // 只是验证调用 setEnvVars() 时不会抛异常
        SystemKit.setEnvVars(mapOf("KUDOS_TEST_KEY" to "VALUE123"))
    }

    @Test
    fun testSetEnvVarsDoesNotThrow_ForOverrideExisting() {
        // 覆盖已存在的环境变量（如 PATH），也不应抛异常
        val oldPath = System.getenv("PATH") ?: ""
        SystemKit.setEnvVars(mapOf("PATH" to oldPath + "_EXT"))
    }

    @Test
    fun testExecuteCommandSuccess() {
        // 根据操作系统选择不同的 echo 命令
        val result = if (SystemKit.currentOs() == OsEnum.WINDOWS) {
            SystemKit.executeCommand("cmd", "/c", "echo", "HELLO")
        } else {
            SystemKit.executeCommand("echo", "HELLO")
        }

        assertTrue(result.first, "正常执行 echo 命令应返回 success=true")
        val output = result.second
        assertNotNull(output)
        assertTrue(output.contains("HELLO"), "输出应包含 HELLO")
    }

    @Test
    fun testExecuteCommandFailure() {
        // 调用一个肯定不存在的命令
        val (success, message) = SystemKit.executeCommand("no_such_command_foobar")
        assertFalse(success, "不存在的命令应返回 success=false")
        assertNotNull(message)
        assertTrue(message.isNotBlank(), "错误信息不应为空")
    }

    @Test
    fun testLineSeparatorMatchesSystemProperty() {
        assertEquals(System.lineSeparator(), SystemKit.LINE_SEPARATOR)
    }

    @Test
    fun testCurrentOs() {
        assert(SystemKit.currentOs() != OsEnum.OTHER)
    }

    @Test
    fun testGetUserReturnsUserName() {
        val actualUser = SystemKit.getUser()
        val expectedUser = System.getProperty("user.name")
        assertEquals(expectedUser, actualUser)
    }

    @Test
    fun testIsDebugReturnsBoolean() {
        // 无法在运行时强制切换 debug 模式，只要返回 Boolean 即可
        SystemKit.isDebug()
    }

    @Test
    fun testGetJavaHomeMatchesSystemUtils() {
        val expected = SystemUtils.getJavaHome().absolutePath
        val actual = SystemKit.getJavaHome().absolutePath
        assertEquals(expected, actual)
    }

    @Test
    fun testGetJavaIoTmpDirMatchesSystemUtils() {
        val expected = SystemUtils.getJavaIoTmpDir().absolutePath
        val actual = SystemKit.getJavaIoTmpDir().absolutePath
        assertEquals(expected, actual)
    }

    @Test
    fun testGetUserDirMatchesSystemUtils() {
        val expected = SystemUtils.getUserDir().absolutePath
        val actual = SystemKit.getUserDir().absolutePath
        assertEquals(expected, actual)
    }

    @Test
    fun testGetUserHomeMatchesSystemUtils() {
        val expected = SystemUtils.getUserHome().absolutePath
        val actual = SystemKit.getUserHome().absolutePath
        assertEquals(expected, actual)
    }

    @Test
    fun testIsJavaAwtHeadlessMatchesSystemUtils() {
        val expected = SystemUtils.isJavaAwtHeadless()
        val actual = SystemKit.isJavaAwtHeadless()
        assertEquals(expected, actual)
    }

}
