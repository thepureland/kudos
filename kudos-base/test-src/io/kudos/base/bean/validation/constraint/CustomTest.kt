package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.Custom
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.bean.validation.support.IBeanValidator
import jakarta.validation.ValidationException
import org.junit.jupiter.api.Assertions.assertFalse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Test cases for Custom.
 *
 * @author K
 * @since 1.0.0
 */
internal class CustomTest {

    @Test
    fun validate() {
        val bean1 = TestRemoteBean("user1", false, null)
        assert(ValidationKit.validateBean(bean1).isEmpty())

        val bean2 = TestRemoteBean("user2", true, null)
        assertFalse(ValidationKit.validateBean(bean2).isEmpty())

        val bean3 = TestRemoteBean(null, true, "address")
        assertEquals(1, ValidationKit.validateBean(bean3, failFast = false).size)
    }

    /**
     * The Custom constraint requires the full bean: validateValue (which has no bean
     * in the ValidationContext) must fail loudly instead of silently passing.
     */
    @Test
    fun validateValueWithoutBeanThrows() {
        assertFailsWith<ValidationException> {
            ValidationKit.validateValue(TestRemoteBean::class, "username", "user1")
        }
    }

    // NOTE: the error branches in CustomValidator.validate for "missing validate(bean)
    // method" and "validate return value is not Boolean" are unreachable: checkClass is
    // bounded by KClass<out IBeanValidator<*>> and the Kotlin compiler emits a CHECKCAST
    // to IBeanValidator right after newInstance(), so any object reaching the reflection
    // lookup is guaranteed to expose validate(bean): Boolean.

    internal data class TestRemoteBean(

        @get:Custom(checkClass = ExistValidator::class, message = "username already exists")
        val username: String?,

        val mockExist: Boolean = false, // simulates whether the username exists

        @get:Custom.List(
            Custom(checkClass = Rule1Validator::class, message = "does not satisfy rule 1"),
            Custom(checkClass = Rule2Validator::class, message = "does not satisfy rule 2")
        )
        val address: String?
    )

    internal class ExistValidator : IBeanValidator<TestRemoteBean> {

        override fun validate(bean: TestRemoteBean): Boolean {
            return !bean.mockExist
        }

    }

    internal class Rule1Validator : IBeanValidator<TestRemoteBean> {

        override fun validate(bean: TestRemoteBean): Boolean {
            return true
        }

    }

    internal class Rule2Validator : IBeanValidator<TestRemoteBean> {

        override fun validate(bean: TestRemoteBean): Boolean {
            return false
        }

    }

}
