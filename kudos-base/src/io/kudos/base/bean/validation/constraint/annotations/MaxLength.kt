package io.kudos.base.bean.validation.constraint.annotations

import jakarta.validation.Constraint
import jakarta.validation.OverridesAttribute
import jakarta.validation.Payload
import jakarta.validation.ReportAsSingleViolation
import org.hibernate.validator.constraints.Length
import kotlin.reflect.KClass

/**
 * Maximum length constraint, equivalent to [Length] with only `max` specified.
 *
 * The validated object's type must be CharSequence or a subclass.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@MustBeDocumented
@Constraint(validatedBy = [])
@Length
@ReportAsSingleViolation
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class MaxLength(
    /**
     * Maximum length.
     */
    @get:OverridesAttribute(constraint = Length::class, name = "max")
    val max: Int,
    /**
     * Message displayed when validation fails.
     */
    val message: String = "sys.valid-msg.default.MaxLength",
    /**
     * The group classes this validation rule belongs to; groups allow filtering validation rules or ordering validation sequence. The default value must be an empty array.
     */
    val groups: Array<KClass<*>> = [],
    /**
     * The payload of the constraint annotation (typically used to associate some metadata with the constraint; a common use is to express the severity of the validation result with the payload)
     */
    val payload: Array<KClass<out Payload>> = []
)
