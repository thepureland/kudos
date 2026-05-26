package io.kudos.base.bean.validation.constraint.annotations

import jakarta.validation.Constraint
import jakarta.validation.OverridesAttribute
import jakarta.validation.Payload
import jakarta.validation.ReportAsSingleViolation
import jakarta.validation.constraints.Size
import kotlin.reflect.KClass

/**
 * Maximum-size constraint, equivalent to a [Size] with only `max` specified.
 *
 * The validated value's type must be one of the following (or a subtype):
 * CharSequence, Array<*>, Collection<*>, DoubleArray, IntArray, LongArray, CharArray, FloatArray, BooleanArray,
 * ByteArray, ShortArray, Map<*, *>.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@MustBeDocumented
@Constraint(validatedBy = [])
@Size
@ReportAsSingleViolation
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class MaxSize(
    /**
     * Maximum size.
     */
    @get:OverridesAttribute(constraint = Size::class, name = "max")
    val max: Int,
    /**
     * Message to show when validation fails.
     */
    val message: String = "sys.valid-msg.default.MaxSize",
    /**
     * The group classes this validation rule belongs to; groups allow filtering validation rules or ordering
     * validation. The default value must be an empty array.
     */
    val groups: Array<KClass<*>> = [],
    /**
     * The payload of the constraint annotation (typically used to associate some metadata with the constraint; a common use is to express the severity of the validation result with the payload)
     */
    val payload: Array<KClass<out Payload>> = []
)
