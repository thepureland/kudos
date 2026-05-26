package io.kudos.base.query

import io.kudos.base.query.enums.OperatorEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Criteria test cases
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class CriteriaTest {

    @Test
    fun testDefaultConstructor() {
        val criteria = Criteria()
        assertTrue(criteria.isEmpty())
    }

    @Test
    fun testConstructorWithPropertyOperatorValue() {
        val criteria = Criteria("name", OperatorEnum.EQ, "test")
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testConstructorWithCriterion() {
        val criterion = Criterion("age", OperatorEnum.GT, 18)
        val criteria = Criteria(criterion)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddAndWithPropertyOperatorValue() {
        val criteria = Criteria()
            .addAnd("name", OperatorEnum.EQ, "test")
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddAndWithCriterion() {
        val criteria = Criteria()
            .addAnd(Criterion("name", OperatorEnum.EQ, "test"))
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddAndMultipleCriterions() {
        val criteria = Criteria()
            .addAnd(
                Criterion("name", OperatorEnum.EQ, "test"),
                Criterion("age", OperatorEnum.GT, 18)
            )
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddAndMultipleCriteria() {
        val criteria1 = Criteria("name", OperatorEnum.EQ, "test")
        val criteria2 = Criteria("age", OperatorEnum.GT, 18)
        val criteria = Criteria()
            .addAnd(criteria1, criteria2)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddAndCriterionAndCriteria() {
        val criterion = Criterion("name", OperatorEnum.EQ, "test")
        val nestedCriteria = Criteria("age", OperatorEnum.GT, 18)
        val criteria = Criteria()
            .addAnd(criterion, nestedCriteria)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddAndCriteriaAndCriterion() {
        val nestedCriteria = Criteria("age", OperatorEnum.GT, 18)
        val criterion = Criterion("name", OperatorEnum.EQ, "test")
        val criteria = Criteria()
            .addAnd(nestedCriteria, criterion)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddOrWithCriterions() {
        val criteria = Criteria()
            .addOr(
                Criterion("name", OperatorEnum.EQ, "test1"),
                Criterion("name", OperatorEnum.EQ, "test2")
            )
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddOrWithCriteria() {
        val criteria1 = Criteria("name", OperatorEnum.EQ, "test1")
        val criteria2 = Criteria("name", OperatorEnum.EQ, "test2")
        val criteria = Criteria()
            .addOr(criteria1, criteria2)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddOrCriterionAndCriteria() {
        val criterion = Criterion("name", OperatorEnum.EQ, "test1")
        val nestedCriteria = Criteria("name", OperatorEnum.EQ, "test2")
        val criteria = Criteria()
            .addOr(criterion, nestedCriteria)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testAddOrCriteriaAndCriterion() {
        val nestedCriteria = Criteria("name", OperatorEnum.EQ, "test1")
        val criterion = Criterion("name", OperatorEnum.EQ, "test2")
        val criteria = Criteria()
            .addOr(nestedCriteria, criterion)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testComplexCriteria() {
        val criteria = Criteria()
            .addAnd("name", OperatorEnum.EQ, "test")
            .addAnd("age", OperatorEnum.GT, 18)
            .addOr(
                Criterion("status", OperatorEnum.EQ, "active"),
                Criterion("status", OperatorEnum.EQ, "pending")
            )
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testNestedCriteria() {
        val innerCriteria = Criteria("age", OperatorEnum.GT, 18)
            .addAnd("age", OperatorEnum.LT, 65)
        val outerCriteria = Criteria("name", OperatorEnum.EQ, "test")
            .addAnd(innerCriteria)
        assertFalse(outerCriteria.isEmpty())
    }

    @Test
    fun testEmptyStringValueIsFiltered() {
        val criteria = Criteria()
            .addAnd("name", OperatorEnum.EQ, "")
        // Empty string should be filtered out
        assertTrue(criteria.isEmpty())
    }

    @Test
    fun testNullValueWithAcceptNullOperator() {
        val criteria = Criteria()
            .addAnd("name", OperatorEnum.IS_NULL, null)
        // IS_NULL operator has acceptNull=true, should be added
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testNullValueWithoutAcceptNullOperator() {
        val criteria = Criteria()
            .addAnd("name", OperatorEnum.EQ, null)
        // EQ operator has acceptNull=false, null value should be filtered
        assertTrue(criteria.isEmpty())
    }

    @Test
    fun testEmptyCollectionIsFiltered() {
        val criteria = Criteria()
            .addAnd("ids", OperatorEnum.IN, emptyList<Int>())
        assertTrue(criteria.isEmpty())
    }

    @Test
    fun testNonEmptyCollectionIsAdded() {
        val criteria = Criteria()
            .addAnd("ids", OperatorEnum.IN, listOf(1, 2, 3))
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testEmptyArrayIsFiltered() {
        val criteria = Criteria()
            .addAnd("ids", OperatorEnum.IN, emptyArray<Int>())
        assertTrue(criteria.isEmpty())
    }

    @Test
    fun testNonEmptyArrayIsAdded() {
        val criteria = Criteria()
            .addAnd("ids", OperatorEnum.IN, arrayOf(1, 2, 3))
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testEmptyNestedCriteriaIsFiltered() {
        val emptyCriteria = Criteria()
        val criteria = Criteria()
            .addAnd(emptyCriteria)
        assertTrue(criteria.isEmpty())
    }

    @Test
    fun testGetCriterionGroups() {
        val criteria = Criteria("name", OperatorEnum.EQ, "test")
        val groups = criteria.getCriterionGroups()
        assertFalse(groups.isEmpty())
    }

    @Test
    fun testToString() {
        val criteria = Criteria("name", OperatorEnum.EQ, "test")
            .addAnd("age", OperatorEnum.GT, 18)
        val result = criteria.toString()
        assertTrue(result.contains("name"))
        assertTrue(result.contains("age"))
        assertTrue(result.contains("test"))
    }

    @Test
    fun testToStringWithOr() {
        val criteria = Criteria()
            .addOr(
                Criterion("name", OperatorEnum.EQ, "test1"),
                Criterion("name", OperatorEnum.EQ, "test2")
            )
        val result = criteria.toString()
        assertTrue(result.contains("OR"))
    }

    @Test
    fun testStaticOfMethod() {
        val criteria = Criteria.of("name", OperatorEnum.EQ, "test")
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testStaticAndWithCriterions() {
        val criteria = Criteria.and(
            Criterion("name", OperatorEnum.EQ, "test1"),
            Criterion("age", OperatorEnum.GT, 18)
        )
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testStaticAndWithCriteria() {
        val criteria1 = Criteria("name", OperatorEnum.EQ, "test1")
        val criteria2 = Criteria("age", OperatorEnum.GT, 18)
        val criteria = Criteria.and(criteria1, criteria2)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testStaticAndWithCriterionAndCriteria() {
        val criterion = Criterion("name", OperatorEnum.EQ, "test")
        val nestedCriteria = Criteria("age", OperatorEnum.GT, 18)
        val criteria = Criteria.and(criterion, nestedCriteria)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testStaticOrWithCriterions() {
        val criteria = Criteria.or(
            Criterion("name", OperatorEnum.EQ, "test1"),
            Criterion("name", OperatorEnum.EQ, "test2")
        )
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testStaticOrWithCriteria() {
        val criteria1 = Criteria("name", OperatorEnum.EQ, "test1")
        val criteria2 = Criteria("name", OperatorEnum.EQ, "test2")
        val criteria = Criteria.or(criteria1, criteria2)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testStaticOrWithCriterionAndCriteria() {
        val criterion = Criterion("name", OperatorEnum.EQ, "test1")
        val nestedCriteria = Criteria("name", OperatorEnum.EQ, "test2")
        val criteria = Criteria.or(criterion, nestedCriteria)
        assertFalse(criteria.isEmpty())
    }

    @Test
    fun testChainCalls() {
        val criteria = Criteria()
            .addAnd("name", OperatorEnum.EQ, "test")
            .addAnd("age", OperatorEnum.GT, 18)
            .addOr(
                Criterion("status", OperatorEnum.EQ, "active"),
                Criterion("status", OperatorEnum.EQ, "pending")
            )
            .addAnd("deleted", OperatorEnum.EQ, false)
        assertFalse(criteria.isEmpty())
    }

    // ============================================================
    // Internal structure: actual representation of AND / OR / nesting in criterionGroups
    // ============================================================

    @Test
    fun testAndStoresEachCriterionAsIndividualElement() {
        val criteria = Criteria()
            .addAnd("a", OperatorEnum.EQ, "1")
            .addAnd("b", OperatorEnum.GT, 2)
        val groups = criteria.getCriterionGroups()
        assertEquals(2, groups.size, "Two addAnd calls each produce a separate element")
        assertTrue(groups[0] is Criterion)
        assertTrue(groups[1] is Criterion)
        assertEquals("a", (groups[0] as Criterion).property)
        assertEquals("b", (groups[1] as Criterion).property)
    }

    @Test
    fun testAndVarargStoresMultipleCriterionsSeparately() {
        val criteria = Criteria().addAnd(
            Criterion("a", OperatorEnum.EQ, "1"),
            Criterion("b", OperatorEnum.EQ, "2"),
            Criterion("c", OperatorEnum.EQ, "3")
        )
        val groups = criteria.getCriterionGroups()
        assertEquals(3, groups.size, "vararg AND expands to multiple separate Criterion elements")
        groups.forEach { assertTrue(it is Criterion) }
    }

    @Test
    fun testOrStoresGroupAsArrayWrapper() {
        val criteria = Criteria().addOr(
            Criterion("name", OperatorEnum.EQ, "a"),
            Criterion("name", OperatorEnum.EQ, "b")
        )
        val groups = criteria.getCriterionGroups()
        assertEquals(1, groups.size, "addOr as a whole produces a single group element")
        val group = groups[0]
        assertTrue(group is Array<*>, "OR group is represented as Array<*> in criterionGroups")
        assertEquals(2, group.size)
        assertTrue(group[0] is Criterion)
        assertTrue(group[1] is Criterion)
    }

    @Test
    fun testNestedCriteriaInAndStoredAsCriteriaNotArray() {
        val inner = Criteria("age", OperatorEnum.GT, 18)
        val outer = Criteria("name", OperatorEnum.EQ, "x").addAnd(inner)
        val groups = outer.getCriterionGroups()
        assertEquals(2, groups.size)
        assertTrue(groups[0] is Criterion)
        assertTrue(groups[1] is Criteria, "AND nesting is preserved as Criteria, not wrapped in Array")
        assertSame(inner, groups[1], "The same nested object is preserved, not a copy")
    }

    @Test
    fun testAndOrAndMixedSequenceStructure() {
        val criteria = Criteria()
            .addAnd("status", OperatorEnum.EQ, "active")
            .addOr(
                Criterion("type", OperatorEnum.EQ, "A"),
                Criterion("type", OperatorEnum.EQ, "B")
            )
            .addAnd("deleted", OperatorEnum.EQ, false)
        val groups = criteria.getCriterionGroups()
        assertEquals(3, groups.size, "AND-OR-AND each take one group element, preserving insertion order")
        assertTrue(groups[0] is Criterion)
        assertTrue(groups[1] is Array<*>)
        assertTrue(groups[2] is Criterion)
    }

    // ============================================================
    // Filtering behavior: vararg with mixed valid/invalid conditions
    // ============================================================

    @Test
    fun testAndVarargFiltersMixedValidAndInvalid() {
        val criteria = Criteria().addAnd(
            Criterion("a", OperatorEnum.EQ, "valid"),
            Criterion("b", OperatorEnum.EQ, ""),       // Empty string: filtered
            Criterion("c", OperatorEnum.EQ, null),     // null + EQ does not accept null: filtered
            Criterion("d", OperatorEnum.EQ, "also")    // Valid
        )
        val groups = criteria.getCriterionGroups()
        assertEquals(2, groups.size, "Only two valid criterions are retained")
        assertEquals("a", (groups[0] as Criterion).property)
        assertEquals("d", (groups[1] as Criterion).property)
    }

    @Test
    fun testOrVarargFiltersInvalidPreservingValidOnes() {
        val criteria = Criteria().addOr(
            Criterion("a", OperatorEnum.EQ, "valid"),
            Criterion("b", OperatorEnum.EQ, ""),
            Criterion("c", OperatorEnum.EQ, null)
        )
        val groups = criteria.getCriterionGroups()
        assertEquals(1, groups.size, "An OR group is still produced when at least one valid criterion exists")
        val orGroup = groups[0] as Array<*>
        assertEquals(1, orGroup.size)
        assertEquals("a", (orGroup[0] as Criterion).property)
    }

    @Test
    fun testOrWithAllCriterionsFilteredAddsNoGroup() {
        val criteria = Criteria().addOr(
            Criterion("a", OperatorEnum.EQ, ""),
            Criterion("b", OperatorEnum.EQ, null)
        )
        assertTrue(
            criteria.isEmpty(),
            "addOr should not leave an empty OR group when all entries are filtered"
        )
    }

    @Test
    fun testOrWithAllEmptyNestedCriteriaAddsNoGroup() {
        val criteria = Criteria().addOr(Criteria(), Criteria())
        assertTrue(criteria.isEmpty(), "addOr with all empty nested Criteria leaves no group either")
    }

    @Test
    fun testEmptyNestedCriteriaIsFilteredFromOrGroup() {
        val empty = Criteria()
        val nonEmpty = Criteria("a", OperatorEnum.EQ, "1")
        val criteria = Criteria().addOr(empty, nonEmpty)
        val orGroup = criteria.getCriterionGroups()[0] as Array<*>
        assertEquals(1, orGroup.size, "Empty nested entries inside an OR group are also dropped")
        assertSame(nonEmpty, orGroup[0])
    }

    @Test
    fun testAddAndEmptyVarargIsNoOp() {
        val criteria = Criteria("name", OperatorEnum.EQ, "x")
        val before = criteria.getCriterionGroups().size
        criteria.addAnd(*arrayOf<Criterion>())
        assertEquals(before, criteria.getCriterionGroups().size, "An empty vararg should not change anything")
    }

    @Test
    fun testAddOrEmptyVarargIsNoOp() {
        val criteria = Criteria("name", OperatorEnum.EQ, "x")
        val before = criteria.getCriterionGroups().size
        criteria.addOr(*arrayOf<Criterion>())
        assertEquals(before, criteria.getCriterionGroups().size)
    }

    // ============================================================
    // Single-value filtering boundaries: which "seemingly empty" values are actually preserved
    // ============================================================

    @Test
    fun testWhitespaceOnlyStringIsKept() {
        // shouldAddCriterion uses isNotEmpty rather than isNotBlank: pure-whitespace strings are retained
        val criteria = Criteria().addAnd("name", OperatorEnum.EQ, "   ")
        assertFalse(criteria.isEmpty(), "Pure-whitespace strings are not considered empty - isNotEmpty is used")
    }

    @Test
    fun testNumericZeroIsKept() {
        val criteria = Criteria().addAnd("count", OperatorEnum.EQ, 0)
        assertFalse(criteria.isEmpty(), "Numeric 0 is not empty")
    }

    @Test
    fun testBooleanFalseIsKept() {
        val criteria = Criteria().addAnd("active", OperatorEnum.EQ, false)
        assertFalse(criteria.isEmpty(), "false is not empty")
    }

    @Test
    fun testEmptyPrimitiveIntArrayIsFiltered() {
        val criteria = Criteria().addAnd("ids", OperatorEnum.IN, intArrayOf())
        assertTrue(
            criteria.isEmpty(),
            "Empty primitive arrays (IntArray, etc.) should be filtered just like object arrays"
        )
    }

    @Test
    fun testNonEmptyPrimitiveIntArrayIsAdded() {
        val criteria = Criteria().addAnd("ids", OperatorEnum.IN, intArrayOf(1, 2))
        assertFalse(criteria.isEmpty(), "Non-empty primitive arrays should be retained")
    }

    @Test
    fun testEmptyMapIsFiltered() {
        val criteria = Criteria().addAnd("attrs", OperatorEnum.IN, emptyMap<String, String>())
        assertTrue(
            criteria.isEmpty(),
            "Empty Map should be filtered just like empty Collection"
        )
    }

    @Test
    fun testNonEmptyMapIsAdded() {
        val criteria = Criteria().addAnd("attrs", OperatorEnum.IN, mapOf("k" to "v"))
        assertFalse(criteria.isEmpty(), "Non-empty Map should be retained")
    }

    // ============================================================
    // acceptNull operators (IS_NULL / IS_NOT_NULL / IS_EMPTY / IS_NOT_EMPTY)
    // ============================================================

    @Test
    fun testIsNullOperatorPassesWithAnyValue() {
        // Operators with acceptNull=true pass through directly
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_NULL, null).isEmpty())
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_NULL, "").isEmpty())
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_NULL, emptyList<Int>()).isEmpty())
    }

    @Test
    fun testIsNotNullOperatorPassesWithAnyValue() {
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_NOT_NULL, null).isEmpty())
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_NOT_NULL, "").isEmpty())
    }

    @Test
    fun testIsEmptyOperatorPassesWithAnyValue() {
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_EMPTY, null).isEmpty())
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_EMPTY, "").isEmpty())
    }

    @Test
    fun testIsNotEmptyOperatorPassesWithAnyValue() {
        assertFalse(Criteria().addAnd("x", OperatorEnum.IS_NOT_EMPTY, null).isEmpty())
    }

    // ============================================================
    // Order of Criterion + Criteria mix inside an OR group
    // ============================================================

    @Test
    fun testOrCriterionThenCriteriaPreservesOrder() {
        val criterion = Criterion("a", OperatorEnum.EQ, "1")
        val nested = Criteria("b", OperatorEnum.EQ, "2")
        val criteria = Criteria().addOr(criterion, nested)
        val orGroup = criteria.getCriterionGroups()[0] as Array<*>
        assertEquals(2, orGroup.size)
        assertTrue(orGroup[0] is Criterion, "Order: criterion first, criteria second")
        assertTrue(orGroup[1] is Criteria)
    }

    @Test
    fun testOrCriteriaThenCriterionPreservesOrder() {
        val nested = Criteria("a", OperatorEnum.EQ, "1")
        val criterion = Criterion("b", OperatorEnum.EQ, "2")
        val criteria = Criteria().addOr(nested, criterion)
        val orGroup = criteria.getCriterionGroups()[0] as Array<*>
        assertEquals(2, orGroup.size)
        assertTrue(orGroup[0] is Criteria, "Order: criteria first, criterion second")
        assertTrue(orGroup[1] is Criterion)
    }

    // ============================================================
    // toString exact output format
    // ============================================================

    @Test
    fun testEmptyCriteriaToStringIsEmpty() {
        assertEquals("", Criteria().toString())
    }

    @Test
    fun testSingleCriterionToStringFormat() {
        val criteria = Criteria("name", OperatorEnum.EQ, "alice")
        assertEquals("name = alice", criteria.toString())
    }

    @Test
    fun testMultipleAndJoinedByAndKeyword() {
        val criteria = Criteria("name", OperatorEnum.EQ, "alice")
            .addAnd("age", OperatorEnum.GT, 18)
        assertEquals("name = alice AND age > 18", criteria.toString())
    }

    @Test
    fun testOrWrappedInParenthesesWithOrKeyword() {
        val criteria = Criteria().addOr(
            Criterion("status", OperatorEnum.EQ, "active"),
            Criterion("status", OperatorEnum.EQ, "pending")
        )
        assertEquals("(status = active OR status = pending)", criteria.toString())
    }

    @Test
    fun testCombinedAndOrToStringStructure() {
        val criteria = Criteria("flag", OperatorEnum.EQ, true)
            .addOr(
                Criterion("a", OperatorEnum.EQ, "1"),
                Criterion("b", OperatorEnum.EQ, "2")
            )
            .addAnd("c", OperatorEnum.EQ, "3")
        assertEquals(
            "flag = true AND (a = 1 OR b = 2) AND c = 3",
            criteria.toString()
        )
    }

    @Test
    fun testToStringRendersIsNullWithEmptyValue() {
        // Criterion.toString trims trailing whitespace, so the null value of IS NULL is not displayed
        val criteria = Criteria("x", OperatorEnum.IS_NULL, null)
        assertEquals("x IS NULL", criteria.toString())
    }

    // ============================================================
    // Chained call return value and reference identity
    // ============================================================

    @Test
    fun testAddAndReturnsSameInstanceForChaining() {
        val criteria = Criteria()
        val returned = criteria.addAnd("a", OperatorEnum.EQ, "1")
        assertSame(criteria, returned, "addAnd returns this, supports chaining")
    }

    @Test
    fun testAddOrReturnsSameInstanceForChaining() {
        val criteria = Criteria()
        val returned = criteria.addOr(Criterion("a", OperatorEnum.EQ, "1"))
        assertSame(criteria, returned, "addOr returns this, supports chaining")
    }

    // ============================================================
    // Static factory overloads not covered above
    // ============================================================

    @Test
    fun testStaticOrCriteriaAndCriterionStructure() {
        val nested = Criteria("a", OperatorEnum.EQ, "1")
        val criterion = Criterion("b", OperatorEnum.EQ, "2")
        val criteria = Criteria.or(nested, criterion)
        val orGroup = criteria.getCriterionGroups()[0] as Array<*>
        assertEquals(2, orGroup.size)
        assertTrue(orGroup[0] is Criteria)
        assertTrue(orGroup[1] is Criterion)
    }

    @Test
    fun testStaticAndCriteriaAndCriterionStructure() {
        val nested = Criteria("a", OperatorEnum.EQ, "1")
        val criterion = Criterion("b", OperatorEnum.EQ, "2")
        val criteria = Criteria.and(nested, criterion)
        val groups = criteria.getCriterionGroups()
        assertEquals(2, groups.size, "AND does not wrap in Array; the two elements each take a slot")
        assertTrue(groups[0] is Criteria)
        assertTrue(groups[1] is Criterion)
    }

    // ============================================================
    // getCriterionGroups is an unmodifiable view (defends against encapsulation leaks)
    // ============================================================

    @Test
    fun testGetCriterionGroupsIsUnmodifiable() {
        val criteria = Criteria("a", OperatorEnum.EQ, "1")
        val groups = criteria.getCriterionGroups()
        @Suppress("UNCHECKED_CAST")
        val mutableView = groups as MutableList<Any>
        // The cast compiles (on the JVM List is backed by java.util.List), but any mutation should throw
        val mutationThrew = runCatching {
            mutableView.add(Criterion("b", OperatorEnum.EQ, "2"))
        }.exceptionOrNull() is UnsupportedOperationException
        assertTrue(mutationThrew, "The view returned by getCriterionGroups should prevent mutation")
    }

    @Test
    fun testGetCriterionGroupsViewReflectsLaterChanges() {
        // It is a view, not a copy: subsequent addAnd calls are visible through the view
        val criteria = Criteria()
        val view = criteria.getCriterionGroups()
        assertEquals(0, view.size)
        criteria.addAnd("a", OperatorEnum.EQ, "1")
        assertEquals(1, view.size, "Returned view should be a live view, not a snapshot")
    }

    // ============================================================
    // Multi-level composition of nested Criteria
    // ============================================================

    @Test
    fun testDeeplyNestedCriteriaStructure() {
        val innermost = Criteria("c", OperatorEnum.EQ, "3")
        val middle = Criteria("b", OperatorEnum.EQ, "2").addAnd(innermost)
        val outer = Criteria("a", OperatorEnum.EQ, "1").addAnd(middle)
        val groups = outer.getCriterionGroups()
        assertEquals(2, groups.size)
        val nestedGroups = (groups[1] as Criteria).getCriterionGroups()
        assertEquals(2, nestedGroups.size)
        assertSame(innermost, nestedGroups[1])
    }
}
