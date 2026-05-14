package io.kudos.base.query.enums

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * OperatorEnum.compare() 测试用例
 *
 * 按操作符分组组织：
 * - 等值：EQ / IEQ / NE / LG
 * - 大小比较：GE / LE / GT / LT
 * - 字符串匹配：LIKE / LIKE_S / LIKE_E / ILIKE / ILIKE_S / ILIKE_E
 * - 集合成员：IN / NOT_IN
 * - 空值/空集合：IS_NULL / IS_NOT_NULL / IS_EMPTY / IS_NOT_EMPTY
 * - 范围：BETWEEN / NOT_BETWEEN
 * - 属性间比较（*_P）：当前实现下 compare() 一律返回 false
 *
 * @author K
 * @since 1.0.0
 */
internal class OperatorEnumTest {

    // ============================================================
    // EQ - 严格相等
    // ============================================================

    @Test fun eqBothNullIsTrue() = assertTrue(OperatorEnum.EQ.compare(null, null))
    @Test fun eqNullVsValueIsFalse() = assertFalse(OperatorEnum.EQ.compare(null, "x"))
    @Test fun eqValueVsNullIsFalse() = assertFalse(OperatorEnum.EQ.compare("x", null))
    @Test fun eqEqualStringsIsTrue() = assertTrue(OperatorEnum.EQ.compare("alice", "alice"))
    @Test fun eqDifferentStringsIsFalse() = assertFalse(OperatorEnum.EQ.compare("alice", "bob"))
    @Test fun eqDifferentTypesIsFalse() = assertFalse(OperatorEnum.EQ.compare(1, "1"))

    // ============================================================
    // IEQ - 忽略大小写相等（string-only，非 string 走 v1 == v2）
    // ============================================================

    @Test fun ieqBothNullIsTrue() = assertTrue(OperatorEnum.IEQ.compare(null, null))
    @Test fun ieqNullVsValueIsFalse() = assertFalse(OperatorEnum.IEQ.compare(null, "x"))
    @Test fun ieqSameCaseIsTrue() = assertTrue(OperatorEnum.IEQ.compare("Alice", "Alice"))
    @Test fun ieqDifferentCaseIsTrue() = assertTrue(OperatorEnum.IEQ.compare("Alice", "ALICE"))
    @Test fun ieqDifferentContentIsFalse() = assertFalse(OperatorEnum.IEQ.compare("Alice", "Bob"))
    @Test fun ieqNonStringEqualFallsBackToEquals() = assertTrue(OperatorEnum.IEQ.compare(42, 42))
    @Test fun ieqMixedTypesIsFalse() = assertFalse(OperatorEnum.IEQ.compare(42, "42"))

    // ============================================================
    // NE / LG - 不等
    // ============================================================

    @Test fun neBothNullIsFalse() {
        // 语义：null != null → false（两者都是 null，"不相等"不成立）
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
    // GE / LE / GT / LT - 大小比较
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
    // 数值跨类型比较 - 通过 BigDecimal 兜底
    // ============================================================

    @Test fun gtMixedNumericIntVsLong() = assertTrue(OperatorEnum.GT.compare(5, 3L))
    @Test fun gtMixedNumericIntVsBigDecimal() = assertTrue(OperatorEnum.GT.compare(5, BigDecimal("3.5")))
    @Test fun gtMixedNumericFloatVsDouble() = assertTrue(OperatorEnum.GT.compare(5.5f, 3.0))
    @Test fun eqMixedNumericIntVsLong() {
        // EQ 用 == 不走 compareNumbers，所以 Int(5) != Long(5)
        assertFalse(OperatorEnum.EQ.compare(5, 5L))
    }

    @Test fun gtNonComparableTypesFalse() {
        // 自定义类不实现 Comparable，compareComparableValues 返回 null → false
        val o1 = Any()
        val o2 = Any()
        assertFalse(OperatorEnum.GT.compare(o1, o2))
    }

    // ============================================================
    // LIKE / LIKE_S / LIKE_E - 字符串匹配（区分大小写）
    // ============================================================

    @Test fun likeContainsIsTrue() = assertTrue(OperatorEnum.LIKE.compare("hello world", "lo wo"))
    @Test fun likeNotContainsIsFalse() = assertFalse(OperatorEnum.LIKE.compare("hello", "xyz"))
    @Test fun likeCaseSensitiveIsFalseForDifferentCase() =
        assertFalse(OperatorEnum.LIKE.compare("Hello", "hello"))
    @Test fun likeNonStringIsFalse() = assertFalse(OperatorEnum.LIKE.compare(123, "12"))
    @Test fun likeEmptyPatternIsTrue() {
        // String.contains("") 在 Kotlin/Java 里恒为 true
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
    // ILIKE / ILIKE_S / ILIKE_E - 忽略大小写
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
        // 两侧都是 String 时，右值按逗号 split
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
        // 左值是 Collection 时，判断 v2.containsAll(v1)
        assertTrue(OperatorEnum.IN.compare(listOf(1, 2), listOf(1, 2, 3)))
        assertFalse(OperatorEnum.IN.compare(listOf(1, 4), listOf(1, 2, 3)))
    }

    @Test fun inArrayAsLeftConvertedToList() {
        // 左值是 Array 时，自动转 List 走 subset 判断
        assertTrue(OperatorEnum.IN.compare(arrayOf(1, 2), listOf(1, 2, 3)))
    }

    @Test fun inMapContainsAllEntries() {
        assertTrue(OperatorEnum.IN.compare(mapOf("a" to 1), mapOf("a" to 1, "b" to 2)))
        assertFalse(OperatorEnum.IN.compare(mapOf("a" to 99), mapOf("a" to 1, "b" to 2)))
    }

    @Test fun inTypeMismatchIsFalse() {
        // v2 既不是 String 也不是 Collection/Array/Map → 落到最后 false 分支
        assertFalse(OperatorEnum.IN.compare(1, 1))
    }

    @Test fun notInIsOppositeOfIn() {
        assertFalse(OperatorEnum.NOT_IN.compare(2, listOf(1, 2, 3)))
        assertTrue(OperatorEnum.NOT_IN.compare(4, listOf(1, 2, 3)))
    }

    @Test fun notInTypeMismatchIsTrue() {
        // IN 返回 false → NOT_IN 取反 → true（即使类型不匹配也"不在"）
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
        // 文档约定：null 对 IS_EMPTY 返回 false（与 IS_NOT_EMPTY 返回 true 配对）
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
        // 落到默认分支：v1.toString().isEmpty()
        // 任何会 toString 出非空内容的对象都返回 false
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
        // 落到默认分支：v1.toString().isNotEmpty()
        // Int 42 → toString = "42" → 非空 → true
        assertTrue(OperatorEnum.IS_NOT_EMPTY.compare(42, null))
        assertTrue(OperatorEnum.IS_NOT_EMPTY.compare(false, null))
    }

    // ============================================================
    // BETWEEN / NOT_BETWEEN - 只接受 ClosedFloatingPointRange
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
        // KNOWN LIMITATION：BETWEEN 只识别 ClosedFloatingPointRange<*>，
        // Kotlin 的 IntRange（即 1..10）不是 ClosedFloatingPointRange，会返回 false。
        // 这是设计取舍——如要查整数区间，调用方需写成 1.0..10.0
        assertFalse(
            OperatorEnum.BETWEEN.compare(5, 1..10),
            "IntRange 不被 BETWEEN 识别，需用 1.0..10.0"
        )
    }

    @Test fun notBetweenStrictlyOutsideIsTrue() {
        // 严格在范围之外（< start 或 > end）应返回 true
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
        // v2 不是 ClosedFloatingPointRange → 早返回 true（"不在范围内"）
        assertTrue(OperatorEnum.NOT_BETWEEN.compare(5, "not a range"))
    }

    // ============================================================
    // 属性间比较 *_P：compare() 不处理，全部返回 false
    // ============================================================

    @Test fun propertyOperatorsAlwaysReturnFalseInCompare() {
        // *_P 操作符是属性间比较，在 compare() 的 when 里走 else 分支返回 false
        // 真正的比较在 SQL 生成阶段做，不在内存里
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
                "$op 在 compare() 中应返回 false（属性比较不在此处理）"
            )
        }
    }

    // ============================================================
    // BigInteger / BigDecimal 跨类型
    // ============================================================

    @Test fun gtBigIntegerVsInt() {
        assertTrue(OperatorEnum.GT.compare(BigInteger("100"), 50))
    }

    @Test fun eqBigDecimalSameScaleSameValue() {
        // EQ 用 == ，BigDecimal 的 equals 严格区分 scale (1.0 != 1.00)
        assertTrue(OperatorEnum.EQ.compare(BigDecimal("1.0"), BigDecimal("1.0")))
        assertFalse(
            OperatorEnum.EQ.compare(BigDecimal("1.0"), BigDecimal("1.00")),
            "BigDecimal.equals 区分 scale，KNOWN BEHAVIOR"
        )
    }

    // ============================================================
    // enumOf 解析
    // ============================================================

    @Test fun enumOfAcceptsUppercaseCode() {
        assertEquals(OperatorEnum.EQ, OperatorEnum.enumOf("="))
        assertEquals(OperatorEnum.LIKE, OperatorEnum.enumOf("LIKE"))
    }

    @Test fun enumOfNormalizesLowercaseToUppercase() {
        // 内部 uppercase()，所以小写 like 也能识别
        assertEquals(OperatorEnum.LIKE, OperatorEnum.enumOf("like"))
    }

    @Test fun enumOfUnknownCodeThrows() {
        runCatching { OperatorEnum.enumOf("UNKNOWN_OP") }.also {
            assertTrue(it.isFailure, "未知 code 应抛异常")
        }
    }
}
