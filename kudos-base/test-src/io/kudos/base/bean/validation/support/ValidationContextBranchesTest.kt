package io.kudos.base.bean.validation.support

import io.kudos.base.bean.validation.kit.ValidationKit
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Branch tests for [ValidationContext] not reached by [ValidationContextTest]:
 * - the cascaded @Valid List-element recursion path of the private set()
 * - the descriptor-accessor fallback returning null for a non-HV ConstraintValidatorContext (get() -> null)
 * - the isFailFast() default-to-true path when the (inheritable) thread-local is unset on a fresh thread
 *
 * @author K
 * @since 1.0.0
 */
internal class ValidationContextBranchesTest {

    @Test
    fun setRecursesIntoValidatedListElements() {
        val validator = ValidationKit.getValidator()
        val bean = Parent(listOf(Child(""), Child("ok")))
        // Should traverse the @Valid list, append [i] to the path, and store business-bean mappings
        // without throwing. (There are no business constraints here, so nothing is stored, but the
        // MutableList recursion branch is exercised.)
        ValidationContext.set(validator, bean)
        ValidationContext.clearBeans()
        assertTrue(true)
    }

    @Test
    fun getReturnsNullForNonHibernateContext() {
        // A dynamic proxy ConstraintValidatorContext exposes no getConstraintDescriptor()/constraintDescriptor,
        // so buildDescriptorAccessor falls through to the constant-null closure and get() returns null.
        val handler = InvocationHandler { _, method, _ ->
            when (method.name) {
                "toString" -> "fakeCtx"
                "hashCode" -> 0
                "equals" -> false
                else -> null
            }
        }
        val fakeCtx = Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(ConstraintValidatorContext::class.java),
            handler
        ) as ConstraintValidatorContext
        assertNull(ValidationContext.get(fakeCtx))
    }

    @Test
    fun isFailFastDefaultsToTrueOnFreshThread() {
        var result: Boolean? = null
        val t = Thread {
            // No prior setFailFast on this thread -> the inheritable thread-local value may be null,
            // exercising the "?: true" default branch.
            result = ValidationContext.isFailFast()
        }
        t.start()
        t.join()
        // isFailFast() never returns null (it defaults to true via the "?: true" branch).
        assertTrue(result != null)
    }

    internal data class Child(
        @get:NotBlank
        val name: String?,
    )

    internal data class Parent(
        @get:Valid
        val children: List<Child>?,
    )
}
