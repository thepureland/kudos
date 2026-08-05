package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.constraint.annotations.Exist
import io.kudos.base.bean.validation.constraint.validator.ExistValidator
import io.kudos.base.bean.validation.kit.ValidationKit
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.constraints.NotBlank
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


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

    /**
     * Collection and Map values go through their own branches of the type dispatch:
     * for a Map only the values (not the keys) are validated.
     */
    @Test
    fun validateCollectionAndMapValues() {
        // List with at least one element satisfying the rule
        assert(ValidationKit.validateValue(TestExistBean::class, "tags", listOf("", null, "x")).isEmpty())

        // List with no element satisfying the rule
        var violations = ValidationKit.validateValue(TestExistBean::class, "tags", listOf("", null))
        assertEquals("at least one tag must be non-blank", violations.first().message)

        // Map with at least one value satisfying the rule (keys are ignored)
        assert(
            ValidationKit.validateValue(
                TestExistBean::class, "attrs", mapOf("a" to "", "b" to "y")
            ).isEmpty()
        )

        // Map with no value satisfying the rule
        violations = ValidationKit.validateValue(TestExistBean::class, "attrs", mapOf("a" to "", "b" to " "))
        assertEquals("at least one attr must be non-blank", violations.first().message)
    }

    /**
     * A non-Hibernate ConstraintValidatorContext (e.g. a mock) cannot supply violation
     * paths, so the validator must fall back to plain Constraints validation.
     */
    @Test
    fun isValidWithNonHibernateContextFallsBack() {
        // Make sure the HV ValidatorFactory (and its initialization context) is built
        ValidationKit.getValidator()

        val validator = ExistValidator()
        validator.initialize(
            Exist(
                Constraints(notBlank = NotBlank(message = "must not be blank")),
                message = "at least one element must satisfy the rule"
            )
        )
        val context = Proxy.newProxyInstance(
            ConstraintValidatorContext::class.java.classLoader,
            arrayOf(ConstraintValidatorContext::class.java)
        ) { _, _, _ -> null } as ConstraintValidatorContext

        assertTrue(validator.isValid(null, context))
        assertTrue(validator.isValid("abc", context))
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
        val name: String?,

        @get:Exist(
            Constraints(notBlank = NotBlank(message = "tag must not be blank")),
            message = "at least one tag must be non-blank"
        )
        val tags: List<String?>? = null,

        @get:Exist(
            Constraints(notBlank = NotBlank(message = "attr must not be blank")),
            message = "at least one attr must be non-blank"
        )
        val attrs: Map<String, String?>? = null

    ) {
        override fun equals(other: Any?): Boolean = true
        override fun hashCode(): Int = 0
    }

}
