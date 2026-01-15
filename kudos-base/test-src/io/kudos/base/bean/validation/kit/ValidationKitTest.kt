package io.kudos.base.bean.validation.kit

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import kotlin.test.*

/**
 * ValidationKit测试用例
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
        assertEquals(2, violations.size) // name为null, age小于最小值
    }

    @Test
    fun testValidateBeanFailFast() {
        val bean = TestBean(null, 5)
        val violations = ValidationKit.validateBean(bean, failFast = true)
        // 快速失败模式下，应该只返回第一个违规
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
        // 只验证TestGroup1组的约束
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
        val bean = TestBean("a", 150) // name太短，age太大
        val violations = ValidationKit.validateBean(bean, failFast = false)
        assertFalse(violations.isEmpty())
        assertEquals(2, violations.size)
    }

    data class TestBean(
        @get:NotNull(message = "name不能为空")
        @get:Size(min = 2, max = 10, message = "name长度必须在2到10之间")
        val name: String?,
        
        @get:NotNull(message = "age不能为空")
        @get:Min(value = 18, message = "age必须大于等于18")
        @get:Max(value = 100, message = "age必须小于等于100")
        val age: Int?
    )

    interface TestGroup1
    interface TestGroup2

    data class TestBeanWithGroups(
        @get:NotNull(message = "name不能为空", groups = [TestGroup1::class])
        val name: String?,
        
        @get:NotNull(message = "age不能为空", groups = [TestGroup2::class])
        val age: Int?
    )
}
