package io.kudos.base.bean.validation.kit

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import kotlin.test.*

/**
 * Test cases for ValidationKit.
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class ValidationKitTest {

    @Test
    fun testValidateBeanSuccess() {
        val bean = TestBean("test", 25)
        val violations = ValidationKit.validateBean(bean)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testValidateBeanWithViolations() {
        val bean = TestBean(null, 5)
        val violations = ValidationKit.validateBean(bean, failFast = false)
        assertFalse(violations.isEmpty())
        assertEquals(2, violations.size) // name is null, age is below the minimum
    }

    @Test
    fun testValidateBeanFailFast() {
        val bean = TestBean(null, 5)
        val violations = ValidationKit.validateBean(bean, failFast = true)
        // In fail-fast mode, only the first violation should be returned
        assertFalse(violations.isEmpty())
    }

    @Test
    fun testValidateProperty() {
        val bean = TestBean("test", 25)
        val violations = ValidationKit.validateProperty(bean, "name")
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testValidatePropertyWithViolation() {
        val bean = TestBean("", 25)
        val violations = ValidationKit.validateProperty(bean, "name", failFast = false)
        assertFalse(violations.isEmpty())
    }

    @Test
    fun testValidateValue() {
        val violations = ValidationKit.validateValue(TestBean::class, "age", 25)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testValidateValueWithViolation() {
        val violations = ValidationKit.validateValue(TestBean::class, "age", 5)
        assertFalse(violations.isEmpty())
    }

    @Test
    fun testGetValidator() {
        val validator = ValidationKit.getValidator()
        assertNotNull(validator)
    }

    @Test
    fun testGetValidatorFailFast() {
        val validator1 = ValidationKit.getValidator(failFast = true)
        val validator2 = ValidationKit.getValidator(failFast = false)
        assertNotNull(validator1)
        assertNotNull(validator2)
    }

    @Test
    fun testValidateBeanWithGroups() {
        val bean = TestBeanWithGroups("test", 25)
        val violations = ValidationKit.validateBean(bean, TestGroup1::class)
        // Only validate constraints in the TestGroup1 group
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testValidatePropertyWithGroups() {
        val bean = TestBeanWithGroups("test", 25)
        val violations = ValidationKit.validateProperty(bean, "name", TestGroup1::class)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testValidateValueWithGroups() {
        val violations = ValidationKit.validateValue(TestBeanWithGroups::class, "name", "test", TestGroup1::class)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testComplexValidation() {
        val bean = TestBean("a", 150) // name too short, age too large
        val violations = ValidationKit.validateBean(bean, failFast = false)
        assertFalse(violations.isEmpty())
        assertEquals(2, violations.size)
    }

    data class TestBean(
        @get:NotNull(message = "name must not be null")
        @get:Size(min = 2, max = 10, message = "name length must be between 2 and 10")
        val name: String?,

        @get:NotNull(message = "age must not be null")
        @get:Min(value = 18, message = "age must be >= 18")
        @get:Max(value = 100, message = "age must be <= 100")
        val age: Int?
    )

    interface TestGroup1
    interface TestGroup2

    data class TestBeanWithGroups(
        @get:NotNull(message = "name must not be null", groups = [TestGroup1::class])
        val name: String?,

        @get:NotNull(message = "age must not be null", groups = [TestGroup2::class])
        val age: Int?
    )

}
