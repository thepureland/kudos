package io.kudos.context.retry

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test cases for RetryConfig.
 *
 * Covers path resolution priority, whitespace handling, and cross-platform default values.
 *
 * Note: [RetryConfig.baseFailedDataPath] is `by lazy` and is resolved only once for the JVM lifetime.
 * Tests can only verify the internal [RetryConfig.resolveBasePath] method (which re-evaluates the
 * priority on every call).
 *
 * @author K
 * @since 1.0.0
 */
internal class RetryConfigTest {

    @AfterTest
    fun cleanup() {
        System.clearProperty(RetryConfig.SYS_PROP_BASE_PATH)
    }

    @Test
    fun resolveBasePathPicksSystemPropertyWhenSet() {
        System.setProperty(RetryConfig.SYS_PROP_BASE_PATH, "/custom/path")
        assertEquals("/custom/path", RetryConfig.resolveBasePath())
    }

    @Test
    fun resolveBasePathFallsBackToTmpDirWhenNothingSet() {
        System.clearProperty(RetryConfig.SYS_PROP_BASE_PATH)
        // Env vars are usually unset in the test process, so should fall back to tmpdir
        val resolved = RetryConfig.resolveBasePath()
        val tmp = System.getProperty("java.io.tmpdir").trimEnd('/', '\\')
        assertEquals(tmp + File.separator + "kudos-failed-data", resolved)
    }

    @Test
    fun resolveBasePathIgnoresBlankSystemProperty() {
        // A blank string must not be treated as a valid configuration
        System.setProperty(RetryConfig.SYS_PROP_BASE_PATH, "   ")
        val resolved = RetryConfig.resolveBasePath()
        assertTrue(resolved.endsWith("kudos-failed-data"), "Blank should be ignored and fall back to the default: $resolved")
    }

    @Test
    fun pathForUsesProvidedServiceCode() {
        System.setProperty(RetryConfig.SYS_PROP_BASE_PATH, "/base")
        // baseFailedDataPath is lazy and has been frozen on first access, so pathFor here uses the value resolved on first access.
        // The test only checks pathFor's concatenation logic — use resolveBasePath rather than baseFailedDataPath to build the expected value.
        val expected = RetryConfig.baseFailedDataPath + File.separator + "sys"
        assertEquals(expected, RetryConfig.pathFor("sys"))
    }

    @Test
    fun pathForFallsBackToDefaultForNullOrBlankServiceCode() {
        val withNull = RetryConfig.pathFor(null)
        val withBlank = RetryConfig.pathFor("   ")
        val withEmpty = RetryConfig.pathFor("")

        assertTrue(withNull.endsWith(File.separator + "default"), "null serviceCode should use 'default'")
        assertTrue(withBlank.endsWith(File.separator + "default"), "Blank serviceCode should use 'default'")
        assertTrue(withEmpty.endsWith(File.separator + "default"), "Empty serviceCode should use 'default'")
    }

    @Test
    fun pathForKeepsValidServiceCodeAsIs() {
        val result = RetryConfig.pathFor("payment-svc")
        assertTrue(result.endsWith(File.separator + "payment-svc"))
        assertNotNull(result)
    }
}
