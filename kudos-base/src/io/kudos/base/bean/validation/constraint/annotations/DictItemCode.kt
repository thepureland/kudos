package io.kudos.base.bean.validation.constraint.annotations

import io.kudos.base.bean.validation.constraint.validator.DictItemCodeValidator
import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Dictionary code constraint annotation, a property-level annotation. Used to validate whether a string is the code of one of the dictionary items in the specified dictionary.
 *
 * @author K
 * @since 1.0.0
 */
@Constraint(validatedBy = [DictItemCodeValidator::class]) // The validator is provided by the sys module
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class DictItemCode(

    /**
     * Code of the atomic service this belongs to.
     */
    val atomicServiceCode: String = "",

    /**
     * Dictionary type.
     */
    val dictType: String,

    /**
     * Message to show when validation fails, or its i18n key.
     * Each constraint definition contains a message template used to indicate the validation result, and when declaring a constraint you can override the default message template via the constraint's `message` attribute.
     * If the constraint fails during validation, the MessageInterpolator you have configured will be used as the parser to interpret the message template defined in this constraint,
     * producing the final validation failure message. The parser will try to resolve placeholders (strings wrapped in braces) in the template.
     * The default parser in Hibernate Validator (MessageInterpolator) first looks for a ResourceBundle named `ValidationMessages.properties` on the classpath,
     * then matches placeholders against the resources defined in that file. If the match fails, it falls back to the ResourceBundle bundled with Hibernate Validator at
     * /org/hibernate/validator/ValidationMessages.properties, and so on, recursively matching every placeholder.
     */
    val message: String = "sys.valid-msg.default.DictItemCode",

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
