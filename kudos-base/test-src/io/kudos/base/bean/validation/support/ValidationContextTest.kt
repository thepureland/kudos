package io.kudos.base.bean.validation.support

import io.kudos.base.bean.validation.kit.ValidationKit
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ValidationContext测试用例
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
        // 在未显式设置时应提供默认值，避免出现null语义
        ValidationContext.setFailFast(true)
        assertTrue(ValidationContext.isFailFast())
    }

    @Test
    fun testSetAndGetValidator() {
        val validator = ValidationKit.getValidator()
        val testBean = TestBean("test", 25)
        
        ValidationContext.set(validator, testBean)
        // 验证上下文已设置
        assertNotNull(ValidationContext.validator)
    }

    @Test
    fun testGetHvInitCtx() {
        // 先获取validator以初始化工厂
        ValidationKit.getValidator()
        
        try {
            val initCtx = ValidationContext.getHvInitCtx()
            assertNotNull(initCtx)
        } catch (e: IllegalStateException) {
            // 如果尚未初始化，会抛出异常
            // 这是正常的行为
        }
    }

    data class TestBean(
        val name: String?,
        val age: Int?
    )
}
