package io.kudos.base.query.enums

import io.kudos.base.enums.ienums.IDictEnum
import io.kudos.base.lang.EnumKit
import io.kudos.base.lang.collections.containsAll
import java.lang.reflect.Method
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap


/**
 * Enum of logical query operators.
 *
 * @author K
 * @since 1.0.0
 */
enum class OperatorEnum(
    override val code: String,
    override val displayText: String,
    val acceptNull: Boolean = false, // Whether the value may be null.
    val stringOnly: Boolean = false  // Whether the operand only accepts String type.
) : IDictEnum {

    /**
     * Equal to.
     */
    EQ("=", "Equal to"),

    /**
     * Equal to (case-insensitive).
     */
    IEQ("I=", "Equal to (case-insensitive)", false, true),

    /**
     * Not equal to.
     */
    NE("!=", "Not equal to", false, false),

    /**
     * Less-than greater-than (not equal to).
     */
    LG("<>", "Less-than greater-than (not equal to)"),

    /**
     * Greater than or equal to.
     */
    GE(">=", "Greater than or equal to"),

    /**
     * Less than or equal to.
     */
    LE("<=", "Less than or equal to"),

    /**
     * Greater than.
     */
    GT(">", "Greater than"),

    /**
     * Less than.
     */
    LT("<", "Less than"),

    /**
     * Equal to another property.
     */
    EQ_P("P=", "Equal to property", false, false),

    /**
     * Not equal to another property.
     */
    NE_P("P!=", "Not equal to property", false, false),

    /**
     * Greater than or equal to another property.
     */
    GE_P("P>=", "Greater than or equal to property", false, false),

    /**
     * Less than or equal to another property.
     */
    LE_P("P<=", "Less than or equal to property", false, false),

    /**
     * Greater than another property.
     */
    GT_P("P>", "Greater than property", false, false),

    /**
     * Less than another property.
     */
    LT_P("P<", "Less than property", false, false),

    /**
     * Matches anywhere in the string.
     */
    LIKE("LIKE", "Match anywhere", false, true),

    /**
     * Matches at the start of the string.
     */
    LIKE_S("LIKE_S", "Match start", false, true),

    /**
     * Matches at the end of the string.
     */
    LIKE_E("LIKE_E", "Match end", false, true),

    /**
     * Matches anywhere in the string (case-insensitive).
     */
    ILIKE("ILIKE", "Match anywhere (case-insensitive)", false, true),

    /**
     * Matches at the start of the string (case-insensitive).
     */
    ILIKE_S("ILIKE_S", "Match start (case-insensitive)", false, true),

    /**
     * Matches at the end of the string (case-insensitive).
     */
    ILIKE_E("ILIKE_E", "Match end (case-insensitive)", false, true),

    /**
     * IN query.
     */
    IN("IN", "IN query"),

    /**
     * NOT IN query.
     */
    NOT_IN("NOT IN", "NOT IN query"),

    /**
     * Is null.
     */
    IS_NULL("IS NULL", "Is null", true, false),

    /**
     * Is not null.
     */
    IS_NOT_NULL("IS NOT NULL", "Is not null", true, false),

    /**
     * Is empty.
     * Strings: empty string; arrays/collections/maps: empty; other objects: toString() returns empty.
     */
    IS_EMPTY("=''", "Equals empty string", true, true),

    /**
     * Is non-empty string.
     */
    IS_NOT_EMPTY("!=''", "Not equal to empty string", true, true),

    /**
     * Between two values.
     */
    BETWEEN("BETWEEN", "Between two values", false, false),

    /**
     * Not between two values.
     */
    NOT_BETWEEN("NOT BETWEEN", "Not between two values", false, false);

    /**
     * Compares two values using the current operator.
     *
     * Performs the appropriate comparison logic for the operator, supporting multiple data types and comparison modes.
     *
     * Supported operators:
     * 1. Equality:
     *    - EQ: strict equality (==).
     *    - IEQ: case-insensitive equality (strings only).
     *    - NE/LG: not equal (!=).
     * 2. Ordering:
     *    - GE: greater than or equal (>=).
     *    - LE: less than or equal (<=).
     *    - GT: greater than (>).
     *    - LT: less than (<).
     * 3. String matching:
     *    - LIKE: contains.
     *    - LIKE_S: startsWith.
     *    - LIKE_E: endsWith.
     *    - ILIKE: case-insensitive contains.
     *    - ILIKE_S: case-insensitive startsWith.
     *    - ILIKE_E: case-insensitive endsWith.
     * 4. Collection operations:
     *    - IN: contained in the collection.
     *    - NOT_IN: not contained in the collection.
     * 5. Null checks:
     *    - IS_NULL: is null.
     *    - IS_NOT_NULL: is not null.
     *    - IS_EMPTY: empty (strings, collections, arrays, maps).
     *    - IS_NOT_EMPTY: not empty.
     * 6. Range checks:
     *    - BETWEEN: within range.
     *    - NOT_BETWEEN: outside range.
     *
     * Type handling:
     * - Comparison operators require the value to implement Comparable.
     * - String-matching operators support only String type.
     * - Null-check operators support multiple types (String, Array, Collection, Map).
     * - IN supports multiple type conversions.
     *
     * Null handling:
     * - EQ/IEQ: null == null returns true.
     * - NE: null != null returns false.
     * - Ordering operators: null values typically return false.
     * - IS_EMPTY: null returns false (IS_NOT_EMPTY returns true).
     *
     * Notes:
     * - Type mismatches usually return false.
     * - Type casts (as) may throw ClassCastException.
     * - String matching trims leading/trailing whitespace.
     * - ILIKE-family operators compare in lower case.
     *
     * @param v1 left-hand value to compare
     * @param v2 right-hand target value (meaningless for IS_NULL etc.)
     * @return true if the logical relation holds, false otherwise
     */
    fun compare(v1: Any?, v2: Any?): Boolean {
        return when (this) {
            EQ -> {
                if (v1 == null && v2 == null) {
                    return true
                }
                if (v1 == null || v2 == null) {
                    false
                } else v1 == v2
            }
            IEQ -> {
                if (v1 == null && v2 == null) {
                    return true
                }
                if (v1 == null || v2 == null) {
                    return false
                }
                if (v1 is String && v2 is String) {
                    v1.equals(v2, ignoreCase = true)
                } else v1 == v2
            }
            NE, LG -> {
                if (v1 == null && v2 == null) {
                    return false
                }
                if (v1 == null || v2 == null) {
                    return true
                }
                if (v1 is String && v2 is String) {
                    v1 != v2
                } else v1 != v2
            }
            GE -> {
                if (v1 == null && v2 == null) {
                    return true
                }
                compareComparableValues(v1, v2)?.let { it >= 0 } ?: false
            }
            LE -> {
                if (v1 == null && v2 == null) {
                    return true
                }
                compareComparableValues(v1, v2)?.let { it <= 0 } ?: false
            }
            GT -> {
                compareComparableValues(v1, v2)?.let { it > 0 } ?: false
            }
            LT -> {
                compareComparableValues(v1, v2)?.let { it < 0 } ?: false
            }
            LIKE -> {
                if (v1 is String && v2 is String) {
                    v1.contains(v2)
                } else false
            }
            LIKE_S -> {
                if (v1 is String && v2 is String) {
                    v1.trim { it <= ' ' }.startsWith(v2)
                } else false
            }
            LIKE_E -> {
                if (v1 is String && v2 is String) {
                    v1.trim { it <= ' ' }.endsWith(v2)
                } else false
            }
            ILIKE -> {
                if (v1 is String && v2 is String) {
                    v1.lowercase().contains(v2.lowercase())
                } else false
            }
            ILIKE_S -> {
                if (v1 is String && v2 is String) {
                    v1.trim { it <= ' ' }.lowercase().startsWith(v2.lowercase())
                } else false
            }
            ILIKE_E -> {
                if (v1 is String && v2 is String) {
                    v1.trim { it <= ' ' }.lowercase().endsWith(v2.lowercase())
                } else false
            }
            IN -> inOperation(v1, v2)
            NOT_IN -> !inOperation(v1, v2)
            IS_NULL -> v1 == null
            IS_NOT_NULL -> v1 != null
            IS_NOT_EMPTY -> {
                if (v1 == null) {
                    return true
                }
                if (v1 is String) {
                    return v1.isNotEmpty()
                }
                if (v1 is Array<*>) {
                    return v1.isNotEmpty()
                }
                if (v1 is Collection<*>) {
                    return !v1.isEmpty()
                }
                if (v1 is Map<*, *>) {
                    v1.isNotEmpty()
                } else v1.toString().isNotEmpty()
            }
            IS_EMPTY -> {
                if (v1 == null) {
                    return false
                }
                if (v1 is String) {
                    return v1.isEmpty()
                }
                if (v1 is Array<*>) {
                    return v1.isEmpty()
                }
                if (v1 is Collection<*>) {
                    return v1.isEmpty()
                }
                if (v1 is Map<*, *>) {
                    v1.isEmpty()
                } else v1.toString().isEmpty()
            }
            BETWEEN -> {
                if (v2 !is ClosedFloatingPointRange<*>) {
                    false
                } else {
                    val geStart = compareComparableValues(v1, v2.start)?.let { it >= 0 } ?: false
                    val leEnd = compareComparableValues(v1, v2.endInclusive)?.let { it <= 0 } ?: false
                    geStart && leEnd
                }
            }
            NOT_BETWEEN -> {
                if (v2 !is ClosedFloatingPointRange<*>) {
                    true
                } else {
                    val ltStart = compareComparableValues(v1, v2.start)?.let { it < 0 } ?: false
                    val gtEnd = compareComparableValues(v1, v2.endInclusive)?.let { it > 0 } ?: false
                    ltStart || gtEnd
                }
            }
            else -> false
        }
    }

    /**
     * General-purpose comparison: common primitive types (Number, String, Char, Boolean) take dedicated
     * paths; for other types, a `compareTo` method on the left-hand type that accepts the right-hand
     * type is located via reflection.
     *
     * @param left left value; returns null immediately when null
     * @param right right value; returns null immediately when null
     * @return standard comparison result (negative / zero / positive); null when not comparable
     * @author K
     * @since 1.0.0
     */
    private fun compareComparableValues(left: Any?, right: Any?): Int? {
        if (left == null || right == null) {
            return null
        }
        if (left is Number && right is Number) {
            return compareNumbers(left, right)
        }
        if (left is String && right is String) {
            return left.compareTo(right)
        }
        if (left is Char && right is Char) {
            return left.compareTo(right)
        }
        if (left is Boolean && right is Boolean) {
            return left.compareTo(right)
        }
        val compareToMethod = getCompareToMethod(left.javaClass, right.javaClass) ?: return null
        return runCatching { compareToMethod.invoke(left, right) as? Int }.getOrNull()
    }

    /**
     * Reflectively locates a `compareTo` method on `leftClass` that accepts `rightClass` as a parameter.
     * The result is cached by (leftClass, rightClass) so each comparison does not re-scan the method table.
     *
     * @param leftClass left-value type
     * @param rightClass right-value type
     * @return the located method, or null when none is found
     * @author K
     * @since 1.0.0
     */
    private fun getCompareToMethod(leftClass: Class<*>, rightClass: Class<*>): Method? {
        val cacheKey = MethodCacheKey(leftClass, rightClass)
        compareToMethodCache[cacheKey]?.let { return it }
        val method = leftClass.methods.firstOrNull { candidate ->
            candidate.name == "compareTo" &&
                candidate.parameterCount == 1 &&
                candidate.parameterTypes.first().isAssignableFrom(rightClass)
        } ?: return null
        compareToMethodCache[cacheKey] = method
        return method
    }

    /**
     * Numeric comparison: same-type operands use native compareTo; cross-type operands are promoted to
     * [BigDecimal] to avoid precision loss.
     *
     * @param left left operand
     * @param right right operand
     * @return standard comparison result; null when [BigDecimal] parsing fails
     * @author K
     * @since 1.0.0
     */
    private fun compareNumbers(left: Number, right: Number): Int? {
        return when {
            left is Int && right is Int -> left.compareTo(right)
            left is Long && right is Long -> left.compareTo(right)
            left is Double && right is Double -> left.compareTo(right)
            left is Float && right is Float -> left.compareTo(right)
            left is Short && right is Short -> left.compareTo(right)
            left is Byte && right is Byte -> left.compareTo(right)
            else -> runCatching {
                BigDecimal(left.toString()).compareTo(BigDecimal(right.toString()))
            }.getOrNull()
        }
    }

    /**
     * Comparison logic for the IN operator.
     *
     * Determines whether the left value is contained in the right value, with type conversion across multiple data types.
     *
     * Workflow:
     * 1. String handling: when both values are String, split the right value on commas before testing.
     * 2. Array conversion: convert Array to List so the rest of the logic is uniform.
     * 3. Collection check:
     *    - If the right value is a Collection:
     *      * If the left value is also a Collection: test whether the right collection contains every element of the left (containsAll).
     *      * Otherwise: test whether the right collection contains the left value (contains).
     * 4. Map check: when both values are Maps, test whether the right Map contains every entry of the left Map.
     * 5. Otherwise: return false.
     *
     * String splitting:
     * - When both values are String, the right value is split on commas.
     * - For example, "a,b,c" becomes ["a", "b", "c"].
     * - The left value is then tested for membership in the resulting array.
     *
     * Collection containment:
     * - Single-value: uses contains.
     * - Collection-vs-collection: uses containsAll (subset test).
     * - Works with any Collection type.
     *
     * Map containment:
     * - Uses containsAll to test entries.
     * - Requires the right Map to contain every entry of the left Map.
     *
     * Type conversion:
     * - Array is converted to List for uniform handling.
     * - The converted values are used in the subsequent checks.
     *
     * Notes:
     * - String splitting uses comma as the delimiter.
     * - Collection checks rely on contains/containsAll.
     * - Type mismatches return false.
     * - Map containsAll compares entries, not keys.
     *
     * @param v1 left value to test
     * @param v2 right value: a collection or map
     * @return true when v1 is contained in v2, otherwise false
     */
    private fun inOperation(v1: Any?, v2: Any?): Boolean {
        var value1 = v1
        var value2 = v2
        if (value1 is String && value2 is String) {
            val elems = value2.split(",").toTypedArray()
            return value1 in elems
        }
        if (value1 is Array<*>) {
            value1 = listOf(*value1)
        }
        if (value2 is Array<*>) {
            value2 = listOf(*value2)
        }
        if (value2 is Collection<*>) {
            return if (value1 is Collection<*>) {
                value2.containsAll(value1)
            } else {
                value2.contains(value1)
            }
        }
        return if (value1 is Map<*, *> && value2 is Map<*, *>) {
            value2.containsAll(value1)
        } else false
    }

    companion object Companion {
        /** Composite cache key (leftClass, rightClass) used when caching `compareTo` methods. */
        private data class MethodCacheKey(val leftClass: Class<*>, val rightClass: Class<*>)
        /** Cache of reflectively resolved `compareTo` methods. */
        private val compareToMethodCache = ConcurrentHashMap<MethodCacheKey, Method>()

        /**
         * Resolves the enum from an operator code (case-insensitive).
         *
         * @param code operator code, such as `=`, `>=`, `LIKE`
         * @return the matching enum
         * @throws IllegalStateException when the code is invalid
         * @author K
         * @since 1.0.0
         */
        fun enumOf(code: String): OperatorEnum {
            var operatorCode = code
            if (operatorCode.isNotBlank()) {
                operatorCode = operatorCode.uppercase()
            }
            return EnumKit.enumOf(OperatorEnum::class, operatorCode) ?: error("Illegal Operator code: $operatorCode")
        }
    }
}
