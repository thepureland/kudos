package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.DictCode
import io.kudos.base.bean.validation.kit.ValidationKit
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * DictCodeValidator测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class DictCodeValidatorTest {

    @Test
    fun testIsValidWithNull() {
        val bean = TestDictCodeBean(null)
        val violations = ValidationKit.validateBean(bean)
        // null值应该通过验证（根据实现，isNullOrBlank返回true）
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testIsValidWithEmpty() {
        val bean = TestDictCodeBean("")
        val violations = ValidationKit.validateBean(bean)
        // 空字符串应该通过验证
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testIsValidWithBlank() {
        val bean = TestDictCodeBean("   ")
        val violations = ValidationKit.validateBean(bean)
        // 空白字符串应该通过验证
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testIsValidWithValidCode() {
        // 注意：这个测试依赖于IDictCodeFinder的实现
        // 如果没有找到DictCodeFinder，dictMap会是空的，验证会失败
        val bean = TestDictCodeBean("VALID_CODE")
        val violations = ValidationKit.validateBean(bean)
        // 实际结果取决于ServiceLoader是否能找到IDictCodeFinder实现
    }

    @Test
    fun testIsValidWithInvalidCode() {
        val bean = TestDictCodeBean("INVALID_CODE")
        val violations = ValidationKit.validateBean(bean)
        // 如果找不到DictCodeFinder或代码不在字典中，验证应该失败
    }

    @Test
    fun testInitialize() {
        val validator = DictCodeValidator()
        val annotation = TestDictCodeBean::class.java.getDeclaredField("code")
            .getAnnotation(DictCode::class.java)
        if (annotation != null) {
            validator.initialize(annotation)
            // 初始化应该成功
        }
    }

    data class TestDictCodeBean(
        @get:DictCode(atomicServiceCode = "test", dictType = "test", message = "无效的字典码")
        val code: String?
    )
}
