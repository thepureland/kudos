package io.kudos.base.bean.validation.constraint.annotations

import io.kudos.base.bean.validation.constraint.validator.EachValidator
import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Apply the Constraints constraint to every element of an Array, Collection, or Map; every element must pass for validation to ultimately pass.
 * For other types, the Constraints constraint is applied directly to the property value.
 * For Maps, the Constraints constraint applies to every value.
 *
 * @author K
 * @since 1.0.0
 */
@Constraint(validatedBy = [EachValidator::class])
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Each(
    /**
     * Composite constraint applied to each element.
     */
    val value: Constraints,
    /**
     * This attribute is meaningless for this constraint; the specific messages are provided by the sub-constraints. Because the Validation framework specification requires the attribute, it is marked as deprecated as a reminder.
     */
    @get:Deprecated("") val message: String = "Never displayed",
    /**
     * The group classes this validation rule belongs to; groups allow filtering validation rules or ordering validation sequence. The default value must be an empty array.
     * Validation groups let you choose which constraints to apply during validation. In some scenarios (such as a wizard) you can then pick the constraints relevant to each step for that step.
     * Validation groups are passed as varargs to validate, validateProperty and validateValue. If a constraint belongs to multiple groups, the order in which those groups are validated is unpredictable.
     * If a constraint is not assigned to any group, it is grouped into the default group (jakarta.validation.groups.Default).
     *
     * @GroupSequence defines the validation order between groups; usage notes:
     * 1. When applied to a class, it must not contain the jakarta.validation.groups.Default::class group; this is allowed on an interface.
     * 2. When applied to a class, it must include the group of the Class of the Bean to be validated.
     * @GroupSequenceProvider dynamically redefines the default group based on object state; the groups returned by the implementation must contain the group of the Class of the Bean to be validated (because if the `Default` group validates T,
     * the actual instance under validation is passed to this class to determine the default group sequence).
     * Note: when validating with a group sequence, if a group earlier in the sequence fails validation, later groups are no longer validated!
     * Note: constraint validation within the same group is unordered.
     */
    val groups: Array<KClass<*>> = [],
    /**
     * The payload of the constraint annotation (typically used to associate some metadata with the constraint; a common use is to express the severity of the validation result with the payload)
     */
    val payload: Array<KClass<out Payload>> = []
)
