package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.Custom
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.bean.validation.support.IBeanValidator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regression tests for [CustomValidator]'s deterministic `validate` method lookup.
 *
 * A class implementing the generic [IBeanValidator]<T> is compiled into TWO single-arg
 * `validate` methods: the real `validate(T)` and a synthetic bridge `validate(Object)`.
 * [Class.getMethods] has no JLS-guaranteed ordering, so the previous
 * `methods.firstOrNull { name == "validate" && parameterCount == 1 }` could non-deterministically
 * pick the bridge instead of the real overload across JVMs / bytecode versions.
 *
 * @author K
 * @since 1.0.0
 */
internal class CustomValidatorMethodSelectionTest {

    internal data class TypedBean(

        @get:Custom(checkClass = TypedValidator::class, message = "must be even length")
        val name: String?
    )

    /** Generic validator: the compiler emits validate(TypedBean) + a synthetic bridge validate(Object). */
    internal class TypedValidator : IBeanValidator<TypedBean> {
        override fun validate(bean: TypedBean): Boolean = (bean.name?.length ?: 0) % 2 == 0
    }

    /**
     * Confirms the compiler really does emit a synthetic bridge `validate(Object)` alongside the
     * real `validate(TypedBean)` (i.e. the non-determinism is genuinely possible), and that the
     * fixed selection logic deterministically prefers the real, non-bridge overload.
     */
    @Test
    fun bridgeMethodIsPresentAndRealOverloadIsSelectedDeterministically() {
        val singleArgValidateMethods = TypedValidator::class.java.methods.filter {
            it.name == "validate" && it.parameterCount == 1
        }
        // There must be at least 2 (real + bridge); otherwise the test class no longer reproduces the hazard.
        assertTrue(
            singleArgValidateMethods.size >= 2,
            "Expected a synthetic bridge in addition to the real validate(TypedBean); got: $singleArgValidateMethods"
        )
        assertTrue(
            singleArgValidateMethods.any { it.isBridge || it.isSynthetic },
            "Expected at least one synthetic/bridge validate method"
        )

        // Replicate CustomValidator's selection logic and assert it is deterministic and picks the real method.
        val bean = TypedBean("even") // length 4
        val nonBridge = singleArgValidateMethods.filter { !it.isBridge && !it.isSynthetic }
        val selected = nonBridge.firstOrNull { it.parameterTypes[0].isInstance(bean) } ?: nonBridge.singleOrNull()
        assertNotNull(selected, "selection must resolve a method")
        assertFalse(selected.isBridge, "selected method must not be the bridge")
        assertFalse(selected.isSynthetic, "selected method must not be synthetic")
        assertTrue(selected.parameterTypes[0] == TypedBean::class.java, "selected param type must be the bean type")
    }

    /**
     * End-to-end check that the full validation flow still routes through the correct overload:
     * an odd-length name fails, an even-length name passes. This is the behavior that would break
     * if the bridge `validate(Object)` were ever invoked instead (it would invoke the real one via
     * dispatch, but selecting a bridge of an unrelated overload set is the hazard guarded against).
     */
    @Test
    fun validationRoutesThroughTheCorrectOverload() {
        assertTrue(ValidationKit.validateBean(TypedBean("even")).isEmpty())
        assertFalse(ValidationKit.validateBean(TypedBean("odd")).isEmpty())
    }

}
