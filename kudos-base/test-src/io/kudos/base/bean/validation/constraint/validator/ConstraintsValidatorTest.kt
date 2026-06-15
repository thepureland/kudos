package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.AtLeast
import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.constraint.annotations.MaxSize
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.support.logic.AndOrEnum
import jakarta.validation.ValidationException
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Null
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.Length
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [ConstraintsValidator].
 *
 * Coverage:
 * - AndOrEnum.OR branch: pass via the first / a later sub-constraint, and the all-fail case
 *   (the reported message must be the Constraints message).
 * - NotBlank sub-constraint with a null value (the null short-circuit must NOT skip NotBlank).
 * - AtLeast as a sub-constraint of Constraints (bean is fetched from ValidationContext).
 * - Unsupported sub-constraint (no entry in ValidatorFactory.BUILDERS) raises an error.
 * - getAnnotations: priority promotion of NotNull-like constraints, explicit order sorting and
 *   the error path when order references a sub-constraint that is not enabled, plus the error
 *   path when a (proxied) Constraints exposes a non-annotation sub-constraint property.
 * - getAnnotationMessage: normal path and the non-String message error path.
 * - buildConstraintDescriptorProxy: all explicitly handled proxy methods (including the
 *   getAnnotationType mapping of the underlying InvocationHandler), the groups/payload
 *   elvis fallbacks for annotations without those attributes, and defaultReturn for every
 *   primitive / collection / object return type.
 *
 * @author K
 * @since 1.0.0
 */
internal class ConstraintsValidatorTest {

    // ---------------------------------------------------------------------
    // AndOrEnum.OR
    // ---------------------------------------------------------------------

    @Test
    fun orPassesWhenFirstConstraintPasses() {
        // null satisfies the beNull sub-constraint
        assertTrue(ValidationKit.validateValue(TestOrBean::class, "name", null).isEmpty())
    }

    @Test
    fun orPassesWhenLaterConstraintPasses() {
        // "abc" violates beNull but satisfies the lowercase pattern
        assertTrue(ValidationKit.validateValue(TestOrBean::class, "name", "abc").isEmpty())
    }

    @Test
    fun orFailsWhenAllConstraintsFail() {
        val violations = ValidationKit.validateValue(TestOrBean::class, "name", "ABC")
        assertEquals(1, violations.size)
        assertEquals("name validation failed", violations.first().message)
    }

    // ---------------------------------------------------------------------
    // AND branch corner cases
    // ---------------------------------------------------------------------

    @Test
    fun notBlankSubConstraintFailsOnNullValue() {
        val violations = ValidationKit.validateValue(TestNotBlankBean::class, "name", null)
        assertEquals("name must not be blank", violations.first().message)
    }

    @Test
    fun notBlankSubConstraintPassesOnNonBlankValue() {
        assertTrue(ValidationKit.validateValue(TestNotBlankBean::class, "name", "abc").isEmpty())
    }

    @Test
    fun atLeastAsSubConstraintValidatesAgainstBean() {
        // one of p1/p2 is non-null -> passes
        assertTrue(ValidationKit.validateBean(TestAtLeastInConstraintsBean("a", null)).isEmpty())

        // none of p1/p2 is non-null -> fails with the AtLeast message
        val violations = ValidationKit.validateBean(TestAtLeastInConstraintsBean(null, null))
        assertEquals("at least one of p1, p2 must be non-null", violations.first().message)
    }

    @Test
    fun unsupportedSubConstraintThrows() {
        // MaxSize is not registered in ValidatorFactory.BUILDERS, so using it as a
        // sub-constraint of Constraints must raise an error for a non-null value.
        val e = assertFailsWith<ValidationException> {
            ValidationKit.validateValue(TestUnsupportedSubConstraintBean::class, "name", "abcd")
        }
        assertTrue(generateSequence<Throwable>(e) { it.cause }.any {
            it is IllegalStateException && (it.message ?: "").contains("MaxSize")
        })
    }

    // ---------------------------------------------------------------------
    // getAnnotations
    // ---------------------------------------------------------------------

    @Test
    fun getAnnotationsPromotesPriorityConstraintAndAppliesOrder() {
        val constraints = Constraints(
            order = arrayOf(Pattern::class, Length::class),
            pattern = Pattern(regexp = "[a-z]+", message = "p"),
            length = Length(min = 1, max = 2, message = "l"),
            notNull = NotNull(message = "n")
        )
        val annotations = ConstraintsValidator.getAnnotations(constraints)
        // NotNull is force-promoted to the front, then the user-specified order applies
        assertEquals(
            listOf(NotNull::class, Pattern::class, Length::class),
            annotations.map { it.annotationClass }
        )
    }

    @Test
    fun getAnnotationsWithoutOrderKeepsDeclaredConstraints() {
        val constraints = Constraints(
            pattern = Pattern(regexp = "[a-z]+", message = "p"),
            length = Length(min = 1, max = 2, message = "l")
        )
        val annotations = ConstraintsValidator.getAnnotations(constraints)
        assertEquals(
            setOf(Pattern::class, Length::class),
            annotations.map { it.annotationClass }.toSet()
        )
    }

    @Test
    fun getAnnotationsFailsWhenOrderReferencesUndefinedConstraint() {
        val constraints = Constraints(
            order = arrayOf(NotNull::class, Pattern::class),
            notNull = NotNull(message = "n")
            // pattern is NOT enabled but referenced in order
        )
        val e = assertFailsWith<IllegalStateException> {
            ConstraintsValidator.getAnnotations(constraints)
        }
        assertTrue((e.message ?: "").contains("Pattern"))
    }

    @Test
    fun getAnnotationsFailsWhenSubConstraintPropertyIsNotAnnotation() {
        // A real Constraints annotation can never expose a non-annotation sub-constraint
        // property, so this defensive branch is exercised with a JDK proxy implementing
        // both Constraints and a fake annotation type whose property returns a String.
        val handler = InvocationHandler { _, method, _ ->
            when (method.name) {
                "annotationType" -> NotAnnotationProperty::class.java
                "foo" -> "definitely-not-an-annotation"
                "toString" -> "fake-constraints-proxy"
                "hashCode" -> 0
                "equals" -> false
                else -> null
            }
        }
        val fake = Proxy.newProxyInstance(
            Constraints::class.java.classLoader,
            arrayOf(Constraints::class.java, NotAnnotationProperty::class.java),
            handler
        ) as Constraints

        val e = assertFailsWith<IllegalStateException> { ConstraintsValidator.getAnnotations(fake) }
        assertTrue((e.message ?: "").contains("foo"))
    }

    // ---------------------------------------------------------------------
    // getAnnotationMessage
    // ---------------------------------------------------------------------

    @Test
    fun getAnnotationMessageReadsMessageAttribute() {
        assertEquals("msg", ConstraintsValidator.getAnnotationMessage(NotNull(message = "msg")))
    }

    @Test
    fun getAnnotationMessageFailsForNonStringMessage() {
        val e = assertFailsWith<IllegalStateException> {
            ConstraintsValidator.getAnnotationMessage(IntMessage(message = 5))
        }
        assertTrue((e.message ?: "").contains("String"))
    }

    // ---------------------------------------------------------------------
    // buildConstraintDescriptorProxy / defaultReturn
    // ---------------------------------------------------------------------

    @Test
    fun buildConstraintDescriptorProxyExposesAnnotationMetadata() {
        val annotation = NotNull(message = "m")
        val descriptor = ConstraintsValidator().buildConstraintDescriptorProxy(annotation)

        assertEquals(annotation as Annotation, descriptor.annotation)
        assertEquals("m", descriptor.messageTemplate)
        assertTrue(descriptor.groups.isEmpty())
        assertTrue(descriptor.payload.isEmpty())
        assertTrue(descriptor.attributes.containsKey("message"))
        assertTrue(descriptor.composingConstraints.isEmpty())
        assertFalse(descriptor.isReportAsSingleViolation)
        assertTrue(descriptor.constraintValidatorClasses.isEmpty())
        assertNull(descriptor.validationAppliesTo)
        // unhandled method -> defaultReturn(returnType); the erased Object return type of
        // unwrap is assignable from Set, so the Set fallback kicks in
        assertEquals(emptySet<Any>(), descriptor.unwrap(Any::class.java))
    }

    @Test
    fun buildConstraintDescriptorProxyToleratesAnnotationWithoutStandardAttributes() {
        // no message / groups / payload attributes at all
        val descriptor = ConstraintsValidator().buildConstraintDescriptorProxy(NoMeta(foo = "f"))
        assertEquals("", descriptor.messageTemplate)
        assertTrue(descriptor.groups.isEmpty())
        assertTrue(descriptor.payload.isEmpty())
        assertEquals("f", descriptor.attributes["foo"])
    }

    @Test
    fun constraintDescriptorProxyHandlerMapsGetAnnotationType() {
        // getAnnotationType is not part of the jakarta ConstraintDescriptor interface, so it
        // can never be dispatched through the proxy itself; invoke the InvocationHandler
        // directly with a same-named method to verify the mapping.
        val annotation = NotNull(message = "m")
        val descriptor = ConstraintsValidator().buildConstraintDescriptorProxy(annotation)
        val handler = Proxy.getInvocationHandler(descriptor)
        val method = HasGetAnnotationType::class.java.getMethod("getAnnotationType")
        val result = handler.invoke(descriptor, method, null)
        assertEquals(NotNull::class.java, result)
    }

    @Test
    fun defaultReturnCoversAllReturnTypes() {
        val validator = ConstraintsValidator()
        assertEquals(false, validator.defaultReturn(java.lang.Boolean.TYPE))
        assertEquals(0, validator.defaultReturn(Integer.TYPE))
        assertEquals(0L, validator.defaultReturn(java.lang.Long.TYPE))
        assertEquals(0.toShort(), validator.defaultReturn(java.lang.Short.TYPE))
        assertEquals(0.toByte(), validator.defaultReturn(java.lang.Byte.TYPE))
        assertEquals(0.toChar(), validator.defaultReturn(Character.TYPE))
        assertEquals(0f, validator.defaultReturn(java.lang.Float.TYPE))
        assertEquals(0.0, validator.defaultReturn(java.lang.Double.TYPE))
        assertEquals(emptySet<Any>(), validator.defaultReturn(Set::class.java))
        assertEquals(emptyList<Any>(), validator.defaultReturn(List::class.java))
        assertNull(validator.defaultReturn(String::class.java))
    }

    // ---------------------------------------------------------------------
    // fixtures
    // ---------------------------------------------------------------------

    /** Test-only annotation whose message attribute is deliberately not a String. */
    internal annotation class IntMessage(val message: Int)

    /** Test-only annotation type whose only property is deliberately not an annotation. */
    internal annotation class NotAnnotationProperty(val foo: String)

    /** Test-only interface providing a Method named getAnnotationType for handler dispatch. */
    internal interface HasGetAnnotationType {
        @Suppress("unused")
        fun getAnnotationType(): Class<*>
    }

    /** Test-only annotation without message/groups/payload attributes. */
    internal annotation class NoMeta(val foo: String)

    internal data class TestOrBean(

        @get:Constraints(
            andOr = AndOrEnum.OR,
            beNull = Null(),
            pattern = Pattern(regexp = "[a-z]+", message = "name must consist of lowercase letters"),
            message = "name validation failed"
        )
        val name: String? = null

    )

    internal data class TestNotBlankBean(

        @get:Constraints(
            notBlank = NotBlank(message = "name must not be blank")
        )
        val name: String? = null

    )

    internal data class TestAtLeastInConstraintsBean(

        val p1: String?,

        val p2: String?,

        @get:Constraints(
            atLeast = AtLeast(properties = ["p1", "p2"], count = 1, message = "at least one of p1, p2 must be non-null")
        )
        val trigger: String? = "x"

    )

    internal data class TestUnsupportedSubConstraintBean(

        @get:Constraints(
            maxSize = MaxSize(3, message = "name length must not exceed 3")
        )
        val name: String? = null

    )

}
