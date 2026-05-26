package io.kudos.base.bean.validation.constraint.annotations

import jakarta.validation.Constraint
import jakarta.validation.OverridesAttribute
import jakarta.validation.Payload
import jakarta.validation.ReportAsSingleViolation
import jakarta.validation.constraints.Size
import kotlin.reflect.KClass

/**
 * Fixed-length constraint, equivalent to [Size] with `min == max == value`.
 *
 * The validated object's type must be one of the following or a subclass:
 * CharSequence, Array<*>, Collection<*>, DoubleArray, IntArray, LongArray, CharArray, FloatArray, BooleanArray, ByteArray, ShortArray, Map<*, *>.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@MustBeDocumented
@Constraint(validatedBy = [])
@Size
@ReportAsSingleViolation
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class FixedLength(
    /**
     * Must be exactly equal to this length (for CharSequence, the number of character units, consistent with [Size]).
     * The parameter is named `value`, so it can be shortened to `@FixedLength(5)`.
     */
    @get:OverridesAttribute(constraint = Size::class, name = "min")
    @get:OverridesAttribute(constraint = Size::class, name = "max")
    val value: Int,
    /**
     * Message to show when validation fails.
     */
    val message: String = "sys.valid-msg.default.FixedLength",
    /**
     * The group classes this validation rule belongs to; groups allow filtering validation rules or ordering validation sequence. The default value must be an empty array.
     */
    val groups: Array<KClass<*>> = [],
    /**
     * The payload of the constraint annotation (typically used to associate some metadata with the constraint; a common use is to express the severity of the validation result with the payload)
     */
    val payload: Array<KClass<out Payload>> = []
)
