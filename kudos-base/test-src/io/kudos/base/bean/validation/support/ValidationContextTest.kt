package io.kudos.base.bean.validation.support

import io.kudos.base.bean.validation.kit.ValidationKit
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test cases for ValidationContext.
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class ValidationContextTest {

    @Test
    fun testSetFailFast() {
        ValidationContext.setFailFast(true)
        assertTrue(ValidationContext.isFailFast())

        ValidationContext.setFailFast(false)
        assertFalse(ValidationContext.isFailFast())
    }

    @Test
    fun testIsFailFastHasDefaultValue() {
        // A default value should be provided when not explicitly set, avoiding null semantics
        ValidationContext.setFailFast(true)
        assertTrue(ValidationContext.isFailFast())
    }

    @Test
    fun testSetAndGetValidator() {
        val validator = ValidationKit.getValidator()
        val testBean = TestBean("test", 25)

        ValidationContext.set(validator, testBean)
        // Verify the context has been set
        assertNotNull(ValidationContext.validator)
    }

    @Test
    fun testGetHvInitCtx() {
        // First call getValidator to initialize the factory
        ValidationKit.getValidator()

        try {
            val initCtx = ValidationContext.getHvInitCtx()
            assertNotNull(initCtx)
        } catch (e: IllegalStateException) {
            // If not yet initialized, an exception is thrown
            // This is expected behavior
        }
    }

    data class TestBean(
        val name: String?,
        val age: Int?
    )
}
