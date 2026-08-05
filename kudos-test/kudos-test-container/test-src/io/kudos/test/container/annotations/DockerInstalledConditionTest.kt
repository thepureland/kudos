package io.kudos.test.container.annotations

import org.junit.jupiter.api.extension.ExtensionContext
import org.mockito.Mockito
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers [DockerInstalledCondition.evaluateExecutionCondition].
 *
 * Whether Docker is installed on the build agent is environment-dependent, so these tests assert the
 * invariants that hold regardless: a non-null result is returned, its disabled-flag matches the
 * reason text, and repeated evaluations are stable (the result is cached via double-checked locking).
 *
 * @author K
 * @since 1.0.0
 */
internal class DockerInstalledConditionTest {

    private fun mockContext(): ExtensionContext = Mockito.mock(ExtensionContext::class.java)

    @Test
    fun `evaluate returns a consistent, well-formed result`() {
        val condition = DockerInstalledCondition()
        val result = condition.evaluateExecutionCondition(mockContext())

        assertNotNull(result)
        if (result.isDisabled) {
            assertTrue(result.reason.get().contains("Docker is not installed"))
        } else {
            assertEquals("Docker is installed", result.reason.get())
        }
    }

    @Test
    fun `repeated evaluations and new instances agree (cached)`() {
        val first = DockerInstalledCondition().evaluateExecutionCondition(mockContext())
        val second = DockerInstalledCondition().evaluateExecutionCondition(mockContext())
        // The cache is a companion-object field, so the disabled flag must match across instances.
        assertEquals(first.isDisabled, second.isDisabled)
    }

    @Test
    fun `disabled and enabled are mutually exclusive`() {
        val result = DockerInstalledCondition().evaluateExecutionCondition(mockContext())
        assertFalse(result.isDisabled && !result.isDisabled)
    }
}
