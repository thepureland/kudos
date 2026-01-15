package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.Remote
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.bean.validation.support.IBeanValidator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * RemoteValidator测试用例
 *
 * @author AI: cursor
 * @author K
 * @author K
 * @since 1.0.0
 */
internal class RemoteValidatorTest {

    @Test
    fun testIsValidWithNull() {
        val bean = TestRemoteBean(null)
        val violations = ValidationKit.validateBean(bean)
        // null值应该通过验证（CustomValidator会处理null）
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testIsValidWithValidValue() {
        val bean = TestRemoteBean("valid")
        val violations = ValidationKit.validateBean(bean)
        // 根据TestRemoteValidator的实现，应该通过验证
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testIsValidWithInvalidValue() {
        val bean = TestRemoteBean("invalid")
        val violations = ValidationKit.validateBean(bean)
        // 根据TestRemoteValidator的实现，应该失败
        assertFalse(violations.isEmpty())
    }

    @Test
    fun testInitialize() {
        val validator = RemoteValidator()
        val annotation = TestRemoteBean::class.java.getDeclaredField("value")
            .getAnnotation(Remote::class.java)
        if (annotation != null) {
            validator.initialize(annotation)
            // 初始化应该成功
        }
    }

    data class TestRemoteBean(
        @get:Remote(checkClass = TestRemoteValidator::class, message = "远程验证失败", requestUrl = "")
        val value: String?
    )

    class TestRemoteValidator : IBeanValidator<TestRemoteBean> {
        override fun validate(bean: TestRemoteBean): Boolean {
            return bean.value == "valid"
        }
    }
}
