package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.Compare
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.bean.validation.support.Depends
import io.kudos.base.support.logic.LogicOperatorEnum
import jakarta.validation.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Test cases for the Compare constraint validator.
 *
 * @author K
 * @since 1.0.0
 */
internal class CompareTest {

    /**
     * Tests behavior when Depends is present.
     */
    @Test
    fun validateDepends() {
        // Depends does not pass, so Compare does not need to be evaluated
        val bean1 = CompareTestBean(null, "123456", "123")
        assert(ValidationKit.validateBean(bean1).isEmpty())
    }

    /**
     * Tests behavior when Depends is present and the comparison is single-valued.
     */
    @Test
    fun validateDependsSingleValue() {
        // Depends passes and the passwords are equal
        val bean1 = CompareTestBean(true, "123456", "123456")
        assert(ValidationKit.validateBean(bean1).isEmpty())

        // Depends passes but the passwords differ
        val bean2 = CompareTestBean(true, "123456", "123")
        val violations = ValidationKit.validateBean(bean2)
        assertEquals("the two passwords differ", violations.first().message)
    }

    /**
     * Tests multi-value comparison.
     */
    @Test
    fun validateMultiValues() {
        // Both password groups are equal
        val bean1 = CompareValuesTestBean(arrayOf("1", "2"), arrayOf("1", "2"))
        assert(ValidationKit.validateBean(bean1).isEmpty())

        // The two password groups have at least one equal element but are not fully equal
        val bean2 = CompareValuesTestBean(arrayOf("1", "2"), arrayOf("1", "3"))
        val violations = ValidationKit.validateBean(bean2)
        assertEquals("the two password groups differ", violations.first().message)

        // Array lengths differ (user-data issue, treated as a validation failure)
        val bean3 = CompareValuesTestBean(arrayOf("1", "2"), arrayOf("1"))
        val bean3Violations = ValidationKit.validateBean(bean3)
        assertEquals("the two password groups differ", bean3Violations.first().message)

        // Array elements are not a Compare-compatible type
        val bean4 = CompareValuesTestBean1(arrayOf("1", "2"), arrayOf(arrayOf("1")))
        assertFailsWith<ValidationException> { ValidationKit.validateBean(bean4) }

        // Not an array type
        val bean5 = CompareValuesTestBean2(intArrayOf(1, 2), intArrayOf(1, 2))
        assertFailsWith<ValidationException> { ValidationKit.validateBean(bean5) }
    }

    /**
     * Tests validation with multiple Compare constraints.
     */
    @Test
    fun validateMultiCompares() {
        // Both Compare constraints pass
        val bean1 = ComparesTestBean("x", "xx", "xxx")
        assert(ValidationKit.validateBean(bean1).isEmpty())

        // One of the Compare constraints fails
        val bean2 = ComparesTestBean("x", "xxx", "xx")
        val violations = ValidationKit.validateBean(bean2)
        assertEquals("medium must be less than large", violations.first().message)
    }


    internal data class CompareTestBean(
        val validate: Boolean?,

        val password: String?,

        @get:Compare(
            depends = Depends(
                properties = ["validate"],
                values = ["true"]
            ),
            anotherProperty = "password",
            logic = LogicOperatorEnum.EQ,
            message = "the two passwords differ"
        )
        val confirmPassword: String?
    )


    internal data class ComparesTestBean(
        val small: String?,

        @get:Compare.List(
            Compare(
                anotherProperty = "small",
                logic = LogicOperatorEnum.GT,
                message = "medium must be greater than small"
            ),
            Compare(
                anotherProperty = "large",
                logic = LogicOperatorEnum.LT,
                message = "medium must be less than large"
            )
        )
        val medium: String?,

        val large: String?
    )


    internal data class CompareValuesTestBean(
        val passwords: Array<String>? = null,

        @get:Compare(
            anotherProperty = "passwords",
            logic = LogicOperatorEnum.EQ,
            message = "the two password groups differ"
        )
        val confirmPasswords: Array<String>? = null
    ) {
        override fun equals(other: Any?): Boolean = true
        override fun hashCode(): Int = 0
    }


    internal data class CompareValuesTestBean1(
        val passwords: Array<String>? = null,

        @get:Compare(
            anotherProperty = "passwords",
            logic = LogicOperatorEnum.EQ,
            message = "the two password groups differ"
        )
        val confirmPasswords: Array<Array<String>>? = null
    ) {
        override fun equals(other: Any?): Boolean = true
        override fun hashCode(): Int = 0
    }


    internal data class CompareValuesTestBean2(
        val passwords: IntArray? = null,

        @get:Compare(
            anotherProperty = "passwords",
            logic = LogicOperatorEnum.EQ,
            message = "the two password groups differ"
        )
        val confirmPasswords: IntArray? = null
    ) {
        override fun equals(other: Any?): Boolean = true
        override fun hashCode(): Int = 0
    }

}
