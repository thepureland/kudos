package io.kudos.base.query

import io.kudos.base.query.enums.OperatorEnum
import java.io.Serializable

/**
 * Single query-condition container.
 *
 * Wraps a single query condition composed of a property name, an operator, and a value.
 *
 * Core fields:
 * - property: the property (field) name being queried.
 * - operator: logical operator enum (e.g. equals, greater-than, LIKE).
 * - value: the property value (target value of the condition).
 *
 * Additional fields:
 * - alias: alias used to distinguish multiple conditions on the same property name.
 * - encrypt: flag indicating whether the condition value is already encrypted.
 *
 * Use cases:
 * - Dynamic query construction.
 * - Query building in ORM frameworks.
 * - Encapsulating complex query conditions.
 *
 * Notes:
 * - The value may be null (depending on whether the operator accepts null).
 * - The alias distinguishes multiple conditions on the same property.
 * - The encryption flag marks conditions over sensitive data.
 * - toString is for debugging only and cannot be executed as SQL directly.
 *
 * @since 1.0.0
 */
data class Criterion(
    /**
     * Property name being queried.
     */
    val property: String,
    /**
     * Logical operator enum for the query condition.
     */
    val operator: OperatorEnum,
    /**
     * Value corresponding to the property name being queried.
     */
    val value: Any? = null,
    /**
     * Alias, used when multiple conditions share the same property name.
     */
    val alias: String? = null,
    /**
     * Whether the condition value has already been encrypted.
     *
     * Historically this was a `var` field outside the constructor -- it therefore was not included in
     * [equals]/[hashCode] and was not copied by [copy], a subtle trap. Now that it has been moved into
     * the primary constructor, the semantics are aligned: two Criterion instances are only equal when
     * all attributes match; `copy()` also preserves the encrypt state correctly. Use
     * `copy(encrypt = true)` when you need to change the value.
     */
    val encrypt: Boolean = false
) : Serializable {

    /**
     * String code of the operator. A computed property derived from [operator].
     *
     * Historically there was a setter here (resolving via [OperatorEnum.enumOf] and assigning back to
     * `operator`); it has been removed together with the switch of [operator] to `val`. To change the
     * operator, use `criterion.copy(operator = ...)`.
     */
    val operatorCode: String
        get() = operator.code

//    fun getValue(): Any? {
//        return if (value == null || "" == value) {
//            value
//        } else when (operator) {
//            Operator.LIKE, Operator.ILIKE -> "%$value%"
//            Operator.LIKE_S, Operator.ILIKE_S -> value.toString() + "%"
//            Operator.LIKE_E, Operator.ILIKE_E -> "%$value"
//            else -> value
//        }
//        return value
//    }
//
//    fun setValue(fieldValue: Any?) {
//        value = fieldValue
//    }

    /**
     * Outputs the query condition. <br></br>
     * Note: the output only serves to confirm the condition structure and is not an executable SQL expression!
     *
     * @return string representation of the condition
     */
    override fun toString(): String {
        return "$property ${operator.code} ${value ?: ""}".trim()
    }

    companion object {
        private const val serialVersionUID = -8988087738348496878L
    }
}