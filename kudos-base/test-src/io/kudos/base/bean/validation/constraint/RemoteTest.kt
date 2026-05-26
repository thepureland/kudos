package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.Remote
import io.kudos.base.bean.validation.constraint.validator.RemoteValidator
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.bean.validation.support.IBeanValidator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test cases for Remote.
 *
 * @author AI: cursor
 * @author K
 * @author K
 * @since 1.0.0
 */
internal class RemoteTest {

    @Test
    fun testIsValidWithNull() {
        val bean = TestRemoteBean(null)
        val violations = ValidationKit.validateBean(bean)
        // Null values should pass validation (CustomValidator handles null)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testIsValidWithValidValue() {
        val bean = TestRemoteBean("valid")
        val violations = ValidationKit.validateBean(bean)
        // Per TestRemoteValidator's implementation, this should pass
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testIsValidWithInvalidValue() {
        val bean = TestRemoteBean("invalid")
        val violations = ValidationKit.validateBean(bean)
        // Per TestRemoteValidator's implementation, this should fail
        assertFalse(violations.isEmpty())
    }

    @Test
    fun testInitialize() {
        val validator = RemoteValidator()
        val annotation = TestRemoteBean::class.java.getDeclaredField("value")
            .getAnnotation(Remote::class.java)
        if (annotation != null) {
            validator.initialize(annotation)
            // Initialization should succeed
        }
    }

    data class TestRemoteBean(
        @get:Remote(checkClass = TestRemoteValidator::class, message = "remote validation failed", requestUrl = "")
        val value: String?
    )

    class TestRemoteValidator : IBeanValidator<TestRemoteBean> {
        override fun validate(bean: TestRemoteBean): Boolean {
            return bean.value == "valid"
        }
    }
}
