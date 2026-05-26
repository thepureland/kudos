package io.kudos.base.lang

import io.kudos.base.enums.impl.OsEnum
import org.apache.commons.lang3.SystemUtils
import org.junit.jupiter.api.TestInstance
import kotlin.test.*


/**
 * test for SystemKit
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SystemKitTest {

    private lateinit var originalEnv: Map<String, String>

    @BeforeTest
    fun setupOriginalEnvironment() {
        // Back up the current environment variables so the test can restore them afterwards
        originalEnv = System.getenv().toMap()
    }

    @AfterTest
    fun restoreOriginalEnvironment() {
        // Restore environment variables to the pre-test state as best as possible
        try {
            SystemKit.setEnvVars(originalEnv)
        } catch (_: Throwable) {
            // Some JVM implementations may not allow modifying all environment variables; ignore the exception
        }
    }

    @Test
    fun testSetEnvVarsDoesNotThrow_ForNewVariables() {
        // Just verify that calling setEnvVars() does not throw
        SystemKit.setEnvVars(mapOf("KUDOS_TEST_KEY" to "VALUE123"))
    }

    @Test
    fun testSetEnvVarsDoesNotThrow_ForOverrideExisting() {
        // Overriding an existing environment variable (e.g. PATH) should also not throw
        val oldPath = System.getenv("PATH") ?: ""
        SystemKit.setEnvVars(mapOf("PATH" to oldPath + "_EXT"))
    }

    @Test
    fun testExecuteCommandSuccess() {
        // Choose a different echo command depending on the OS
        val result = if (SystemKit.currentOs() == OsEnum.WINDOWS) {
            SystemKit.executeCommand("cmd", "/c", "echo", "HELLO")
        } else {
            SystemKit.executeCommand("echo", "HELLO")
        }

        assertTrue(result.first, "A successful echo command should return success=true")
        val output = result.second
        assertNotNull(output)
        assertTrue(output.contains("HELLO"), "Output should contain HELLO")
    }

    @Test
    fun testExecuteCommandFailure() {
        // Call a command that definitely does not exist
        val (success, message) = SystemKit.executeCommand("no_such_command_foobar")
        assertFalse(success, "A non-existent command should return success=false")
        assertNotNull(message)
        assertTrue(message.isNotBlank(), "Error message should not be blank")
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
        // Cannot force-toggle debug mode at runtime; it is enough that this returns a Boolean
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
        val expected = System.getProperty("java.awt.headless")?.equals("true", ignoreCase = true) == true
        val actual = SystemKit.isJavaAwtHeadless()
        assertEquals(expected, actual)
    }

}
