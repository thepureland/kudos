package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.constraint.annotations.Exist
import io.kudos.base.bean.validation.kit.ValidationKit
import jakarta.validation.constraints.NotBlank
import kotlin.test.Test
import kotlin.test.assertEquals


/**
 * Test cases for Exist.
 *
 * @author K
 * @since 1.0.0
 */
internal class ExistTest {

    @Test
    fun validate() {
        // Array type with some elements satisfying the rule
        assert(ValidationKit.validateValue(TestExistBean::class, "contactWays", arrayOf("", null, "123")).isEmpty())

        // Array type with no element satisfying the rule
        for (i in 1..10) {
            val violations = ValidationKit.validateValue(TestExistBean::class, "contactWays", arrayOf("", null))
            assertEquals("at least one contact way must be provided", violations.first().message)
        }

        // Array type with null value: validation passes directly
        assert(ValidationKit.validateValue(TestExistBean::class, "contactWays", null).isEmpty())

        // String type: equivalent to using Constraints directly
        assert(ValidationKit.validateValue(TestExistBean::class, "name", " ").isNotEmpty())
    }

    internal data class TestExistBean(

        @get:Exist(
            Constraints(
                notBlank = NotBlank(message = "contact way must not be blank"),
            ),
            message = "at least one contact way must be provided"
        )
        val contactWays: Array<String>?,

        @get:Exist(
            Constraints(notBlank = NotBlank(message = "name must not be blank"))
        )
        val name: String?

    ) {
        override fun equals(other: Any?): Boolean = true
        override fun hashCode(): Int = 0
    }

}
