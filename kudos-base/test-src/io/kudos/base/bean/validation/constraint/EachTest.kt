package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.constraint.annotations.Each
import io.kudos.base.bean.validation.constraint.validator.EachValidator
import io.kudos.base.bean.validation.kit.ValidationKit
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.Length
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    /**
     * The type dispatch in EachValidator.isValid must handle every primitive array
     * type as well as Collection and Map (validating map values).
     */
    @Test
    fun typeDispatchCoversPrimitiveArraysCollectionsAndMaps() {
        // Ensure the HV ValidatorFactory (and its initialization context) is built
        ValidationKit.getValidator()

        val validator = EachValidator()
        validator.initialize(Each(Constraints(notNull = NotNull(message = "element must not be null"))))
        val context = Proxy.newProxyInstance(
            ConstraintValidatorContext::class.java.classLoader,
            arrayOf(ConstraintValidatorContext::class.java)
        ) { _, _, _ -> null } as ConstraintValidatorContext

        assertTrue(validator.isValid(booleanArrayOf(true, false), context))
        assertTrue(validator.isValid(byteArrayOf(1, 2), context))
        assertTrue(validator.isValid(charArrayOf('a', 'b'), context))
        assertTrue(validator.isValid(doubleArrayOf(1.0, 2.0), context))
        assertTrue(validator.isValid(floatArrayOf(1f, 2f), context))
        assertTrue(validator.isValid(intArrayOf(1, 2), context))
        assertTrue(validator.isValid(longArrayOf(1L, 2L), context))
        assertTrue(validator.isValid(shortArrayOf(1, 2), context))
        assertTrue(validator.isValid(listOf("a", "b"), context))
        assertTrue(validator.isValid(mapOf("k1" to "v1", "k2" to "v2"), context))
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
