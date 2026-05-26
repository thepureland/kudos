package io.kudos.base.support.logic

import io.kudos.base.enums.ienums.IDictEnum
import io.kudos.base.lang.EnumKit
import io.kudos.base.query.enums.OperatorEnum
import java.util.Locale

/**
 * Logical operator enumeration.
 *
 * Defines various logical comparison operators used for property value comparisons.
 * Similar in functionality to OperatorEnum, but mainly used in validation scenarios such as DependsValidator.
 *
 * Supported operator categories:
 * 1. Equality comparison: EQ (equal), IEQ (case-insensitive equal), NE/LG (not equal)
 * 2. Magnitude comparison: GE (greater than or equal), LE (less than or equal), GT (greater than), LT (less than)
 * 3. String matching: LIKE (anywhere), LIKE_S (starts with), LIKE_E (ends with)
 * 4. Case-insensitive matching: ILIKE, ILIKE_S, ILIKE_E
 * 5. Set operations: IN (contained), NOT_IN (not contained)
 * 6. Null checks: IS_NULL (is null), IS_NOT_NULL (is not null)
 * 7. Empty string checks: IS_EMPTY (is empty string), IS_NOT_EMPTY (is not empty string)
 *
 * Property descriptions:
 * - code: operator code, used for serialization and transmission
 * - displayText: display description of the operator
 * - acceptNull: whether null values are accepted; true means null is also a valid comparison value
 * - stringOnly: whether only string types are accepted; true means it can only be used for string comparisons
 *
 * Implementation:
 * - The compare method internally delegates to the corresponding method of OperatorEnum to perform the actual comparison
 * - Stays consistent with OperatorEnum for easy unified maintenance
 *
 * Use cases:
 * - Dependency condition validation in DependsValidator
 * - Building dynamic query conditions
 * - Logical comparisons of property values
 *
 * Notes:
 * - The v2 parameter is meaningless for some operators (such as IS_NULL and IS_EMPTY)
 * - Operators with stringOnly set to true can only be used with string types
 * - Operators with acceptNull set to true can accept null values as valid comparison values
 *
 * @since 1.0.0
 */
enum class LogicOperatorEnum(
    override val code: String,
    override val displayText: String,
    // Whether the value can be null
    val acceptNull: Boolean = false,
    // The operand only accepts string types
    val stringOnly: Boolean = false
) : IDictEnum {

    /** Equal */
    EQ("=", "Equal"),

    /** Case-insensitive equal */
    IEQ("I=", "Case-insensitive equal", false, true),

    /** Not equal */
    NE("!=", "Not equal", false, false),

    /** Less than or greater than (not equal) */
    LG("<>", "Less than or greater than (not equal)"),

    /** Greater than or equal */
    GE(">=", "Greater than or equal"),

    /** Less than or equal */
    LE("<=", "Less than or equal"),

    /** Greater than */
    GT(">", "Greater than"),

    /** Less than */
    LT("<", "Less than"),

    /** Match anywhere in the string */
    LIKE("LIKE", "Match anywhere", false, true),

    /** Match at the start of the string */
    LIKE_S("LIKE_S", "Match start", false, true),

    /** Match at the end of the string */
    LIKE_E("LIKE_E", "Match end", false, true),

    /** Case-insensitive match anywhere in the string */
    ILIKE("ILIKE", "Case-insensitive match anywhere", false, true),

    /** Case-insensitive match at the start of the string */
    ILIKE_S("ILIKE_S", "Case-insensitive match start", false, true),

    /** Case-insensitive match at the end of the string */
    ILIKE_E("ILIKE_E", "Case-insensitive match end", false, true),

    /** IN query */
    IN("IN", "IN query"),

    /** NOT IN query */
    NOT_IN("NOT IN", "NOT IN query"),

    /** Whether the value is null */
    IS_NULL("IS NULL", "Is null", true, false),

    /** Whether the value is not null */
    IS_NOT_NULL("IS NOT NULL", "Is not null", true, false),

    /** Whether the value is an empty string */
    IS_EMPTY("=''", "Equal to empty string", true, true),

    /** Whether the value is not an empty string */
    IS_NOT_EMPTY("!=''", "Not equal to empty string", true, true);


    /**
     * Performs an assertion based on the current operator.
     *
     * @param v1 Left value
     * @param v2 Right value (meaningless for IS_NULL, IS_NOT_NULL, IS_EMPTY, IS_NOT_EMPTY)
     * @return Whether the logical relationship is satisfied
     * @author K
     * @since 1.0.0
     */
    fun compare(v1: Any?, v2: Any? = null): Boolean {
        return when (this) {
            EQ -> OperatorEnum.EQ.compare(v1, v2)
            IEQ -> OperatorEnum.IEQ.compare(v1, v2)
            NE, LG -> OperatorEnum.NE.compare(v1, v2)
            GE -> OperatorEnum.GE.compare(v1, v2)
            LE -> OperatorEnum.LE.compare(v1, v2)
            GT -> OperatorEnum.GT.compare(v1, v2)
            LT ->OperatorEnum.LT.compare(v1, v2)
            LIKE -> OperatorEnum.LIKE.compare(v1, v2)
            LIKE_S -> OperatorEnum.LIKE_S.compare(v1, v2)
            LIKE_E -> OperatorEnum.LIKE_E.compare(v1, v2)
            ILIKE -> OperatorEnum.ILIKE.compare(v1, v2)
            ILIKE_S -> OperatorEnum.ILIKE_S.compare(v1, v2)
            ILIKE_E -> OperatorEnum.ILIKE_E.compare(v1, v2)
            IN -> OperatorEnum.IN.compare(v1, v2)
            NOT_IN -> OperatorEnum.NOT_IN.compare(v1, v2)
            IS_NULL -> OperatorEnum.IS_NULL.compare(v1, v2)
            IS_NOT_NULL -> OperatorEnum.IS_NOT_NULL.compare(v1, v2)
            IS_NOT_EMPTY -> OperatorEnum.IS_NOT_EMPTY.compare(v1, v2)
            IS_EMPTY -> OperatorEnum.IS_EMPTY.compare(v1, v2)
        }
    }

    companion object Companion {
        fun enumOf(code: String): LogicOperatorEnum? {
            var codeStr = code
            if (codeStr.isNotBlank()) {
                codeStr = codeStr.uppercase(Locale.getDefault())
            }
            return EnumKit.enumOf(LogicOperatorEnum::class, codeStr)
        }
    }

}