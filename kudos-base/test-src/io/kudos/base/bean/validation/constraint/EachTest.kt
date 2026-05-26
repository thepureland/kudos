package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.constraint.annotations.Each
import io.kudos.base.bean.validation.kit.ValidationKit
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test cases for Each.
 *
 * @author K
 * @since 1.0.0
 */
internal class EachTest {

    @Test
    fun validate() {
        // Array type with some elements failing one rule
        var violations = ValidationKit.validateValue(TestEachBean::class, "contactWays", arrayOf("", null, "123"))
        assertEquals("contact ways must not be blank", violations.first().message)

        // Array type with some elements failing another rule
        violations = ValidationKit.validateValue(TestEachBean::class, "contactWays", arrayOf("123"))
        assertEquals("contact way length must be between 8 and 32", violations.first().message)

        // Array type with null value; validation passes directly
        assert(ValidationKit.validateValue(TestEachBean::class, "contactWays", null).isEmpty())

        // Array type with every element satisfying all rules
        assert(ValidationKit.validateValue(TestEachBean::class, "contactWays", arrayOf("12345678", "abcdefghi")).isEmpty())

        // String type: equivalent to using Constraints directly
        assert(ValidationKit.validateValue(TestEachBean::class, "name", " ").isNotEmpty())
    }

    internal data class TestEachBean(

        @get:Each(
            Constraints(
                notBlank = NotBlank(message = "contact ways must not be blank"),
                length = Length(min = 8, max = 32, message = "contact way length must be between 8 and 32")
            )
        )
        val contactWays: Array<String>?,

        @get:Each(
            Constraints(notBlank = NotBlank(message = "name must not be blank"))
        )
        val name: String?


    ) {
        override fun equals(other: Any?): Boolean = true
        override fun hashCode(): Int = 0
    }

}
