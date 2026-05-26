package io.kudos.base.bean.validation.support

import io.kudos.base.support.logic.AndOrEnum
import io.kudos.base.support.logic.LogicOperatorEnum


/**
 * Dependency constraint annotation. Not a top-level constraint annotation; it is used only as an attribute of other top-level annotations and represents the precondition of the owning top-level annotation (the top-level annotation is applied only when the Depends condition expression holds). See @Compare as a reference.
 * As a non-top-level constraint annotation, the validation framework cannot automatically invoke its Validator. The corresponding validation method on this annotation's validator must be manually invoked from the validator of the owning top-level annotation.
 * To implement "apply the constraint on the current property only when other properties satisfy certain conditions", use @GroupSequenceProvider to dynamically redefine the default group.
 *
 * @author K
 * @since 1.0.0
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
// @Target(AnnotationTarget.EXPRESSION)
annotation class Depends(
    /**
     * Names of the dependent properties.
     */
    val properties: Array<String>,
    /**
     * Logical operator enums of the expression.
     */
    val logics: Array<LogicOperatorEnum> = [LogicOperatorEnum.EQ],
    /**
     * Values of the expression. Must be convertible to the type of the dependent property. Cannot be declared as Array<Any> because all annotation values must be constants.
     * Array elements support the string array form, for example: "[1,2,3]".
     * When evaluating the expression, the left value is first reduced to its toString() form and then compared against the right value using the operator.
     */
    val values: Array<String> = [],
    /**
     * Logical relationship across multiple expressions; default is AND.
     */
    val andOrEnum: AndOrEnum = AndOrEnum.AND
)
