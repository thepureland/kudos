package io.kudos.base.query.enums

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * OperatorEnum.compare() test cases.
 *
 * Organized by operator group:
 * - Equality: EQ / IEQ / NE / LG
 * - Magnitude: GE / LE / GT / LT
 * - String match: LIKE / LIKE_S / LIKE_E / ILIKE / ILIKE_S / ILIKE_E
 * - Collection membership: IN / NOT_IN
 * - Null/empty: IS_NULL / IS_NOT_NULL / IS_EMPTY / IS_NOT_EMPTY
 * - Range: BETWEEN / NOT_BETWEEN
 * - Property-to-property (*_P): compare() always returns false under current implementation
 *
 * @author K
 * @since 1.0.0
 */
internal class OperatorEnumTest {

    // ============================================================
    // EQ - strict equality
    // ============================================================

    @Test fun eqBothNullIsTrue() = assertTrue(OperatorEnum.EQ.compare(null, null))
    @Test fun eqNullVsValueIsFalse() = assertFalse(OperatorEnum.EQ.compare(null, "x"))
    @Test fun eqValueVsNullIsFalse() = assertFalse(OperatorEnum.EQ.compare("x", null))
    @Test fun eqEqualStringsIsTrue() = assertTrue(OperatorEnum.EQ.compare("alice", "alice"))
    @Test fun eqDifferentStringsIsFalse() = assertFalse(OperatorEnum.EQ.compare("alice", "bob"))
    @Test fun eqDifferentTypesIsFalse() = assertFalse(OperatorEnum.EQ.compare(1, "1"))

    // ============================================================
    // IEQ - case-insensitive equality (string-only; non-string falls back to v1 == v2)
    // ============================================================

    @Test fun ieqBothNullIsTrue() = assertTrue(OperatorEnum.IEQ.compare(null, null))
    @Test fun ieqNullVsValueIsFalse() = assertFalse(OperatorEnum.IEQ.compare(null, "x"))
    @Test fun ieqSameCaseIsTrue() = assertTrue(OperatorEnum.IEQ.compare("Alice", "Alice"))
    @Test fun ieqDifferentCaseIsTrue() = assertTrue(OperatorEnum.IEQ.compare("Alice", "ALICE"))
    @Test fun ieqDifferentContentIsFalse() = assertFalse(OperatorEnum.IEQ.compare("Alice", "Bob"))
    @Test fun ieqNonStringEqualFallsBackToEquals() = assertTrue(OperatorEnum.IEQ.compare(42, 42))
    @Test fun ieqMixedTypesIsFalse() = assertFalse(OperatorEnum.IEQ.compare(42, "42"))

    // ============================================================
    // NE / LG - inequality
    // ============================================================

    @Test fun neBothNullIsFalse() {
        // Semantics: null != null -> false (both are null; "not equal" does not hold)
        assertFalse(OperatorEnum.NE.compare(null, null))
        assertFalse(OperatorEnum.LG.compare(null, null))
    }

    @Test fun neNullVsValueIsTrue() = assertTrue(OperatorEnum.NE.compare(null, "x"))
    @Test fun neDifferentValuesIsTrue() = assertTrue(OperatorEnum.NE.compare("a", "b"))
    @Test fun neEqualValuesIsFalse() = assertFalse(OperatorEnum.NE.compare("a", "a"))
    @Test fun lgSameAsNe() = assertEquals(
        OperatorEnum.NE.compare("a", "b"),
        OperatorEnum.LG.compare("a", "b")
    )

    // ============================================================
    // GE / LE / GT / LT - magnitude comparison
    // ============================================================

    @Test fun geBothNullIsTrue() = assertTrue(OperatorEnum.GE.compare(null, null))
    @Test fun geNullVsValueIsFalse() = assertFalse(OperatorEnum.GE.compare(null, 1))
    @Test fun geGreaterIsTrue() = assertTrue(OperatorEnum.GE.compare(5, 3))
    @Test fun geEqualIsTrue() = assertTrue(OperatorEnum.GE.compare(5, 5))
    @Test fun geLessIsFalse() = assertFalse(OperatorEnum.GE.compare(3, 5))

    @Test fun leBothNullIsTrue() = assertTrue(OperatorEnum.LE.compare(null, null))
    @Test fun leLessIsTrue() = assertTrue(OperatorEnum.LE.compare(3, 5))
    @Test fun leEqualIsTrue() = assertTrue(OperatorEnum.LE.compare(5, 5))
    @Test fun leGreaterIsFalse() = assertFalse(OperatorEnum.LE.compare(5, 3))

    @Test fun gtBothNullIsFalse() = assertFalse(OperatorEnum.GT.compare(null, null))
    @Test fun gtGreaterIsTrue() = assertTrue(OperatorEnum.GT.compare(5, 3))
    @Test fun gtEqualIsFalse() = assertFalse(OperatorEnum.GT.compare(5, 5))
    @Test fun gtLessIsFalse() = assertFalse(OperatorEnum.GT.compare(3, 5))

    @Test fun ltBothNullIsFalse() = assertFalse(OperatorEnum.LT.compare(null, null))
    @Test fun ltLessIsTrue() = assertTrue(OperatorEnum.LT.compare(3, 5))
    @Test fun ltEqualIsFalse() = assertFalse(OperatorEnum.LT.compare(5, 5))
    @Test fun ltGreaterIsFalse() = assertFalse(OperatorEnum.LT.compare(5, 3))

    // ============================================================
    // Cross-type numeric comparison - falls back via BigDecimal
    // ============================================================

    @Test fun gtMixedNumericIntVsLong() = assertTrue(OperatorEnum.GT.compare(5, 3L))
    @Test fun gtMixedNumericIntVsBigDecimal() = assertTrue(OperatorEnum.GT.compare(5, BigDecimal("3.5")))
    @Test fun gtMixedNumericFloatVsDouble() = assertTrue(OperatorEnum.GT.compare(5.5f, 3.0))
    @Test fun eqMixedNumericIntVsLong() {
        // EQ uses ==, not compareNumbers, so Int(5) != Long(5)
        assertFalse(OperatorEnum.EQ.compare(5, 5L))
    }

    @Test fun gtNonComparableTypesFalse() {
        // Custom class does not implement Comparable; compareComparableValues returns null -> false
        val o1 = Any()
        val o2 = Any()
        assertFalse(OperatorEnum.GT.compare(o1, o2))
    }

    // ============================================================
    // LIKE / LIKE_S / LIKE_E - string match (case-sensitive)
    // ============================================================

    @Test fun likeContainsIsTrue() = assertTrue(OperatorEnum.LIKE.compare("hello world", "lo wo"))
    @Test fun likeNotContainsIsFalse() = assertFalse(OperatorEnum.LIKE.compare("hello", "xyz"))
    @Test fun likeCaseSensitiveIsFalseForDifferentCase() =
        assertFalse(OperatorEnum.LIKE.compare("Hello", "hello"))
    @Test fun likeNonStringIsFalse() = assertFalse(OperatorEnum.LIKE.compare(123, "12"))
    @Test fun likeEmptyPatternIsTrue() {
        // String.contains("") is always true in Kotlin/Java
        assertTrue(OperatorEnum.LIKE.compare("anything", ""))
    }

    @Test fun likeSStartsWithIsTrue() = assertTrue(OperatorEnum.LIKE_S.compare("hello world", "hello"))
    @Test fun likeSStartsWithTrimsLeadingWhitespace() =
        assertTrue(OperatorEnum.LIKE_S.compare("   hello world", "hello"))
    @Test fun likeSNotStartsWithIsFalse() = assertFalse(OperatorEnum.LIKE_S.compare("hello", "world"))
    @Test fun likeSNonStringIsFalse() = assertFalse(OperatorEnum.LIKE_S.compare(123, "12"))

    @Test fun likeEEndsWithIsTrue() = assertTrue(OperatorEnum.LIKE_E.compare("hello world", "world"))
    @Test fun likeEEndsWithTrimsTrailingWhitespace() =
        assertTrue(OperatorEnum.LIKE_E.compare("hello world   ", "world"))
    @Test fun likeENotEndsWithIsFalse() = assertFalse(OperatorEnum.LIKE_E.compare("hello", "abc"))

    // ============================================================
    // ILIKE / ILIKE_S / ILIKE_E - case-insensitive
    // ============================================================

    @Test fun ilikeContainsIgnoresCase() = assertTrue(OperatorEnum.ILIKE.compare("Hello WORLD", "lo wo"))
    @Test fun ilikeNotContainsIsFalse() = assertFalse(OperatorEnum.ILIKE.compare("Hello", "xyz"))
    @Test fun ilikeNonStringIsFalse() = assertFalse(OperatorEnum.ILIKE.compare(123, "12"))

    @Test fun ilikeSStartsWithIgnoresCase() = assertTrue(OperatorEnum.ILIKE_S.compare("HELLO world", "hello"))
    @Test fun ilikeSStartsWithTrimsAndIgnoresCase() =
        assertTrue(OperatorEnum.ILIKE_S.compare("   HELLO world", "hello"))

    @Test fun ilikeEEndsWithIgnoresCase() = assertTrue(OperatorEnum.ILIKE_E.compare("hello WORLD", "world"))
    @Test fun ilikeEEndsWithTrimsAndIgnoresCase() =
        assertTrue(OperatorEnum.ILIKE_E.compare("hello WORLD   ", "world"))

    // ============================================================
    // IN / NOT_IN
    // ============================================================

    @Test fun inStringSplitsRightByComma() {
        // When both sides are String, the right value is split by comma
        assertTrue(OperatorEnum.IN.compare("b", "a,b,c"))
        assertFalse(OperatorEnum.IN.compare("d", "a,b,c"))
    }

    @Test fun inValueInCollection() {
        assertTrue(OperatorEnum.IN.compare(2, listOf(1, 2, 3)))
        assertFalse(OperatorEnum.IN.compare(4, listOf(1, 2, 3)))
    }

    @Test fun inValueInArray() {
        assertTrue(OperatorEnum.IN.compare(2, arrayOf(1, 2, 3)))
        assertFalse(OperatorEnum.IN.compare(4, arrayOf(1, 2, 3)))
    }

    @Test fun inCollectionIsSubsetOfRight() {
        // When the left value is a Collection, check v2.containsAll(v1)
        assertTrue(OperatorEnum.IN.compare(listOf(1, 2), listOf(1, 2, 3)))
        assertFalse(OperatorEnum.IN.compare(listOf(1, 4), listOf(1, 2, 3)))
    }

    @Test fun inArrayAsLeftConvertedToList() {
        // When the left value is an Array, it is auto-converted to List and goes through subset check
        assertTrue(OperatorEnum.IN.compare(arrayOf(1, 2), listOf(1, 2, 3)))
    }

    @Test fun inMapContainsAllEntries() {
        assertTrue(OperatorEnum.IN.compare(mapOf("a" to 1), mapOf("a" to 1, "b" to 2)))
        assertFalse(OperatorEnum.IN.compare(mapOf("a" to 99), mapOf("a" to 1, "b" to 2)))
    }

    @Test fun inTypeMismatchIsFalse() {
        // v2 is neither String nor Collection/Array/Map -> falls through to false branch
        assertFalse(OperatorEnum.IN.compare(1, 1))
    }

    @Test fun notInIsOppositeOfIn() {
        assertFalse(OperatorEnum.NOT_IN.compare(2, listOf(1, 2, 3)))
        assertTrue(OperatorEnum.NOT_IN.compare(4, listOf(1, 2, 3)))
    }

    @Test fun notInTypeMismatchIsTrue() {
        // IN returns false -> NOT_IN negates -> true (even with type mismatch, it is "not in")
        assertTrue(OperatorEnum.NOT_IN.compare(1, 1))
    }

    // ============================================================
    // IS_NULL / IS_NOT_NULL
    // ============================================================

    @Test fun isNullForNullIsTrue() = assertTrue(OperatorEnum.IS_NULL.compare(null, "irrelevant"))
    @Test fun isNullForValueIsFalse() = assertFalse(OperatorEnum.IS_NULL.compare("x", null))
    @Test fun isNotNullForNullIsFalse() = assertFalse(OperatorEnum.IS_NOT_NULL.compare(null, null))
    @Test fun isNotNullForValueIsTrue() = assertTrue(OperatorEnum.IS_NOT_NULL.compare("x", null))

    // ============================================================
    // IS_EMPTY
    // ============================================================

    @Test fun isEmptyNullIsFalse() {
        // Documented convention: null returns false for IS_EMPTY (paired with IS_NOT_EMPTY returning true)
        assertFalse(OperatorEnum.IS_EMPTY.compare(null, null))
    }

    @Test fun isEmptyEmptyStringIsTrue() = assertTrue(OperatorEnum.IS_EMPTY.compare("", null))
    @Test fun isEmptyNonEmptyStringIsFalse() = assertFalse(OperatorEnum.IS_EMPTY.compare("x", null))
    @Test fun isEmptyEmptyArrayIsTrue() = assertTrue(OperatorEnum.IS_EMPTY.compare(emptyArray<Int>(), null))
    @Test fun isEmptyNonEmptyArrayIsFalse() = assertFalse(OperatorEnum.IS_EMPTY.compare(arrayOf(1), null))
    @Test fun isEmptyEmptyCollectionIsTrue() = assertTrue(OperatorEnum.IS_EMPTY.compare(emptyList<Int>(), null))
    @Test fun isEmptyEmptyMapIsTrue() = assertTrue(OperatorEnum.IS_EMPTY.compare(emptyMap<String, Int>(), null))
    @Test fun isEmptyNonEmptyMapIsFalse() = assertFalse(OperatorEnum.IS_EMPTY.compare(mapOf("a" to 1), null))

    @Test fun isEmptyOtherTypeUsesToString() {
        // Falls through to default branch: v1.toString().isEmpty()
        // Any object whose toString yields non-empty content returns false
        assertFalse(OperatorEnum.IS_EMPTY.compare(42, null))
        assertFalse(OperatorEnum.IS_EMPTY.compare(false, null))
    }

    // ============================================================
    // IS_NOT_EMPTY
    // ============================================================

    @Test fun isNotEmptyNullIsTrue() = assertTrue(OperatorEnum.IS_NOT_EMPTY.compare(null, null))
    @Test fun isNotEmptyEmptyStringIsFalse() = assertFalse(OperatorEnum.IS_NOT_EMPTY.compare("", null))
    @Test fun isNotEmptyNonEmptyStringIsTrue() = assertTrue(OperatorEnum.IS_NOT_EMPTY.compare("x", null))
    @Test fun isNotEmptyEmptyArrayIsFalse() =
        assertFalse(OperatorEnum.IS_NOT_EMPTY.compare(emptyArray<Int>(), null))
    @Test fun isNotEmptyNonEmptyArrayIsTrue() =
        assertTrue(OperatorEnum.IS_NOT_EMPTY.compare(arrayOf(1), null))
    @Test fun isNotEmptyEmptyCollectionIsFalse() =
        assertFalse(OperatorEnum.IS_NOT_EMPTY.compare(emptyList<Int>(), null))
    @Test fun isNotEmptyNonEmptyMapIsTrue() =
        assertTrue(OperatorEnum.IS_NOT_EMPTY.compare(mapOf("a" to 1), null))

    @Test fun isNotEmptyOtherTypeUsesToString() {
        // Falls through to default branch: v1.toString().isNotEmpty()
        // Int 42 -> toString = "42" -> non-empty -> true
        assertTrue(OperatorEnum.IS_NOT_EMPTY.compare(42, null))
        assertTrue(OperatorEnum.IS_NOT_EMPTY.compare(false, null))
    }

    // ============================================================
    // BETWEEN / NOT_BETWEEN - only accept ClosedFloatingPointRange
    // ============================================================

    @Test fun betweenInsideRangeIsTrue() {
        assertTrue(OperatorEnum.BETWEEN.compare(5.0, 1.0..10.0))
    }

    @Test fun betweenAtBoundsIsTrue() {
        assertTrue(OperatorEnum.BETWEEN.compare(1.0, 1.0..10.0))
        assertTrue(OperatorEnum.BETWEEN.compare(10.0, 1.0..10.0))
    }

    @Test fun betweenOutsideRangeIsFalse() {
        assertFalse(OperatorEnum.BETWEEN.compare(0.5, 1.0..10.0))
        assertFalse(OperatorEnum.BETWEEN.compare(11.0, 1.0..10.0))
    }

    @Test fun betweenIntRangeNotSupportedKnownLimitation() {
        // KNOWN LIMITATION: BETWEEN recognizes only ClosedFloatingPointRange<*>;
        // Kotlin's IntRange (i.e. 1..10) is not a ClosedFloatingPointRange and will return false.
        // This is a design tradeoff - to query an integer interval, callers must write 1.0..10.0
        assertFalse(
            OperatorEnum.BETWEEN.compare(5, 1..10),
            "IntRange is not recognized by BETWEEN; use 1.0..10.0"
        )
    }

    @Test fun notBetweenStrictlyOutsideIsTrue() {
        // Strictly outside the range (< start or > end) should return true
        assertTrue(OperatorEnum.NOT_BETWEEN.compare(0.5, 1.0..10.0))
        assertTrue(OperatorEnum.NOT_BETWEEN.compare(11.0, 1.0..10.0))
    }

    @Test fun notBetweenAtBoundsIsFalse() {
        assertFalse(OperatorEnum.NOT_BETWEEN.compare(1.0, 1.0..10.0))
        assertFalse(OperatorEnum.NOT_BETWEEN.compare(10.0, 1.0..10.0))
    }

    @Test fun notBetweenInsideRangeIsFalse() {
        assertFalse(OperatorEnum.NOT_BETWEEN.compare(5.0, 1.0..10.0))
    }

    @Test fun notBetweenWithNonRangeIsTrue() {
        // v2 is not a ClosedFloatingPointRange -> early-returns true ("not in range")
        assertTrue(OperatorEnum.NOT_BETWEEN.compare(5, "not a range"))
    }

    // ============================================================
    // Property-to-property *_P: not handled by compare(); always returns false
    // ============================================================

    @Test fun propertyOperatorsAlwaysReturnFalseInCompare() {
        // *_P operators are property-to-property comparisons; in compare()'s when block they hit the else branch and return false.
        // The actual comparison happens at SQL generation time, not in memory.
        listOf(
            OperatorEnum.EQ_P,
            OperatorEnum.NE_P,
            OperatorEnum.GE_P,
            OperatorEnum.LE_P,
            OperatorEnum.GT_P,
            OperatorEnum.LT_P
        ).forEach { op ->
            assertFalse(
                op.compare(5, 3),
                "$op should return false in compare() (property comparison is not handled here)"
            )
        }
    }

    // ============================================================
    // BigInteger / BigDecimal cross-type
    // ============================================================

    @Test fun gtBigIntegerVsInt() {
        assertTrue(OperatorEnum.GT.compare(BigInteger("100"), 50))
    }

    @Test fun eqBigDecimalSameScaleSameValue() {
        // EQ uses ==; BigDecimal.equals strictly distinguishes scale (1.0 != 1.00)
        assertTrue(OperatorEnum.EQ.compare(BigDecimal("1.0"), BigDecimal("1.0")))
        assertFalse(
            OperatorEnum.EQ.compare(BigDecimal("1.0"), BigDecimal("1.00")),
            "BigDecimal.equals distinguishes scale, KNOWN BEHAVIOR"
        )
    }

    // ============================================================
    // enumOf parsing
    // ============================================================

    @Test fun enumOfAcceptsUppercaseCode() {
        assertEquals(OperatorEnum.EQ, OperatorEnum.enumOf("="))
        assertEquals(OperatorEnum.LIKE, OperatorEnum.enumOf("LIKE"))
    }

    @Test fun enumOfNormalizesLowercaseToUppercase() {
        // Internally calls uppercase(), so lowercase like is also recognized
        assertEquals(OperatorEnum.LIKE, OperatorEnum.enumOf("like"))
    }

    @Test fun enumOfUnknownCodeThrows() {
        runCatching { OperatorEnum.enumOf("UNKNOWN_OP") }.also {
            assertTrue(it.isFailure, "Unknown code should throw an exception")
        }
    }
}
