package io.kudos.base.bean.validation.support

import io.kudos.base.query.enums.OperatorEnum

/**
 * Assertion logic enum.
 *
 * @author K
 * @since 1.0.0
 */
enum class AssertLogicEnum {
    /**
     * Null assertion.
     */
    IS_NULL,

    /**
     * Non-null assertion.
     */
    IS_NOT_NULL,

    /**
     * Empty assertion. For strings checks whether it is the empty string; for arrays, collections and maps checks whether they are empty; for other objects, checks whether toString() yields the empty string. Returns false for null.
     */
    IS_EMPTY,

    /**
     * Non-empty assertion. For strings checks whether it is not the empty string; for arrays, collections and maps checks whether they are not empty; for other objects, checks whether toString() does not yield the empty string. Returns false for null.
     */
    IS_NOT_EMPTY,

    /**
     * Null, empty, or whitespace. For strings checks whether it is the empty string or whitespace; for arrays, collections and maps checks whether they are empty; for other objects, checks whether toString() yields the empty string or whitespace. Returns true for null.
     */
    IS_BLANK,

    /**
     * Not null, not empty, and not whitespace. For strings checks whether it is neither the empty string nor whitespace; for arrays, collections and maps checks whether they are not empty; for other objects, checks whether toString() yields neither the empty string nor whitespace. Returns false for null.
     */
    IS_NOT_BLANK;

    /**
     * Assert the specified value according to the current logic.
     *
     * @param value the value to check
     * @return the assertion result; true means passed, false means failed
     * @author K
     * @since 1.0.0
     */
    fun compare(value: Any?): Boolean {
        when (this) {
            IS_NULL -> return OperatorEnum.IS_NULL.compare(value, null)
            IS_NOT_NULL -> return OperatorEnum.IS_NOT_NULL.compare(value, null)
            IS_EMPTY -> return OperatorEnum.IS_EMPTY.compare(value, null)
            IS_NOT_EMPTY -> return OperatorEnum.IS_NOT_EMPTY.compare(value, null)
            IS_BLANK -> {
                if (value == null) {
                    return true
                }
                if (value is String) {
                    return value.isBlank()
                }
                return OperatorEnum.IS_NULL.compare(value, null) || OperatorEnum.IS_EMPTY.compare(value, null)
            }

            IS_NOT_BLANK -> {
                if (value == null) {
                    return false
                }
                if (value is String) {
                    return value.isNotBlank()
                }
                return OperatorEnum.IS_NOT_NULL.compare(value, null) && OperatorEnum.IS_NOT_EMPTY.compare(value, null)
            }

        }
    }
}
