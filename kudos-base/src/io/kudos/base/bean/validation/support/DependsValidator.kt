package io.kudos.base.bean.validation.support

import io.kudos.base.bean.BeanKit
import io.kudos.base.support.logic.AndOrEnum
import io.kudos.base.support.logic.LogicOperatorEnum

/**
 * Depends constraint validator.
 *
 * Validates dependency relationships between bean properties and supports complex logical conditions.
 * Invoked by other primary constraint validators to provide conditional validation.
 *
 * Core features:
 * 1. Dependency validation: verifies whether multiple property values satisfy specified logical conditions
 * 2. Logical composition: supports both AND and OR logical relationships
 * 3. Operator support: supports various logical operators (equals, greater than, LIKE, etc.)
 * 4. Array value handling: supports array-formatted string values (e.g., "['a','b','c']")
 *
 * Validation flow:
 * 1. Extract property names, values, operators and the logical relationship from the Depends annotation
 * 2. Retrieve the bean's property values via reflection
 * 3. Call the validate method to perform the logical judgment
 * 4. Return the validation result (true means the condition holds, false otherwise)
 *
 * Logical relationships:
 * - AND: all conditions must be satisfied; returns false immediately upon any failure (short-circuit evaluation)
 * - OR: any one condition satisfying is enough; returns true immediately upon any success (short-circuit evaluation)
 *
 * Array value format:
 * - Supports string array format: "['value1','value2','value3']"
 * - Brackets and quotes are stripped automatically and converted to a plain string
 * - Used to support scenarios requiring multiple values, such as the IN operator
 *
 * Use cases:
 * - Conditional validation via the @Depends annotation
 * - Field dependency validation for dynamic forms
 * - Validation of complex business rules
 *
 * Notes:
 * - The three arrays (left values, right values, operators) must be exactly the same size
 * - Left values are converted to String before comparison
 * - Uses short-circuit evaluation to optimize performance
 *
 * @since 1.0.0
 */
object DependsValidator {

    /**
     * Returns whether the expression defined by depends holds; true if it holds, false otherwise.
     *
     * @param depends the dependency annotation
     * @param bean the bean to validate
     * @return whether validation passes
     * @author K
     * @since 1.0.0
     */
    fun validate(depends: Depends, bean: Any): Boolean {
        val leftValues = depends.properties.map { BeanKit.getProperty(bean, it) }.toTypedArray()
        return validate(leftValues, depends.values, depends.logics, depends.andOrEnum)
    }

    /**
     * Check whether the property logic holds.
     *
     * Validates whether multiple property values satisfy the specified logical conditions; supports both AND and OR.
     *
     * Workflow:
     * 1. Array size check: the three arrays (left values, right values, operators) must be the same size
     * 2. Array value handling: if the right value is array-formatted (e.g., "['a','b','c']"), parse it into a plain string
     * 3. Pairwise comparison: at each index, compare the left value and right value using the corresponding operator
     * 4. Logical relationship judgment:
     *    - AND: if any comparison fails, return false immediately
     *    - OR: if any comparison succeeds, return true immediately; return false if all fail
     *
     * Array value format:
     * - Supports string array format: "['value1','value2','value3']"
     * - Brackets and quotes are stripped automatically and converted to a plain string
     * - Supports single quotes and comma separation
     *
     * Logical relationships:
     * - AND: all conditions must be satisfied; returns false immediately upon any failure (short-circuit evaluation)
     * - OR: any one condition satisfying is enough; returns true immediately upon any success (short-circuit evaluation)
     *
     * Notes:
     * - The three arrays must be exactly the same size; otherwise an exception is thrown
     * - Left values are converted to String before comparison
     * - Uses short-circuit evaluation: AND returns immediately on failure, OR returns immediately on success
     *
     * @param leftValues array of left values, the property values to be compared
     * @param rightValues array of right values, the target values for comparison (supports array-formatted strings)
     * @param operators array of operators, the comparison operator at each position
     * @param andOrEnum the logical relationship across groups of values; AND means all conditions must hold, OR means any condition holding suffices
     * @return true if the logic holds, false otherwise
     */
    fun validate(
        leftValues: Array<Any?>, rightValues: Array<String>, operators: Array<LogicOperatorEnum>, andOrEnum: AndOrEnum = AndOrEnum.AND
    ): Boolean {
        if (leftValues.size != rightValues.size || rightValues.size != operators.size) {
            error("The left-value array, right-value array, and operator array must be the same size!")
        }

        var result = true
        leftValues.forEachIndexed { index, leftValue ->
            var rightValue = rightValues[index]

            // Array handling
            if (rightValue.startsWith("[") && rightValue.endsWith("]")) {
                rightValue = rightValue.replaceFirst("\\['".toRegex(), "")
                    .replaceFirst("']".toRegex(), "")
                    .replace("',\\s*'".toRegex(), ",")
            }

            val compare: Boolean = operators[index].compare(leftValue?.toString(), rightValue)
            if (andOrEnum === AndOrEnum.AND) {
                if (!compare) {
                    return false
                }
            } else {
                if (compare) {
                    return true
                } else {
                    result = false
                }
            }
        }
        return result
    }

}
