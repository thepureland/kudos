package io.kudos.base.bean.validation.constraint.annotations

import io.kudos.base.bean.validation.constraint.validator.MatchesValidator
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Only supports the categorized regexes built into the framework via [RegExpEnum] and [io.kudos.base.bean.validation.support.RegExps];
 * the behavior is consistent with [jakarta.validation.constraints.Pattern] (null is treated as valid, intended to be combined with [NotBlank] and similar).
 * For business-specific custom rules, use [@Pattern][jakarta.validation.constraints.Pattern] and reference the constants in [io.kudos.base.bean.validation.support.RegExps].
 * The terminal constraint is converted into a `Pattern` rule description via [io.kudos.base.bean.validation.terminal.convert.converter.impl.MatchesConstraintConvertor].
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Constraint(validatedBy = [MatchesValidator::class])
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Matches(
    /**
     * Built-in regex category (one-to-one with [RegExps][io.kudos.base.bean.validation.support.RegExps]).
     */
    val value: RegExpEnum,
    /**
     * Message to show when validation fails or its i18n key; when empty, [RegExpEnum.defaultMessageKey] is used.
     */
    val message: String = "",
    /**
     * The group classes this validation rule belongs to.
     */
    val groups: Array<KClass<*>> = [],
    /**
     * The payload of the constraint annotation.
     */
    val payload: Array<KClass<out Payload>> = [],
)
