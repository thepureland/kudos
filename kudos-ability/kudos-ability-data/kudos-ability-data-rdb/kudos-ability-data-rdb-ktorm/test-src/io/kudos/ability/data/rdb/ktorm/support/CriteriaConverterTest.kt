package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorms
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [CriteriaConverter].
 *
 * The happy paths (AND / OR / nested combinations) are exercised end-to-end by
 * BaseReadOnlyDaoTest; this class locks the defensive error branches for unsupported element
 * types inside the criterion groups. The public Criteria API can only produce
 * Criterion / Criteria / Array elements, so the invalid elements are injected via reflection
 * into the private `criterionGroups` list — exactly the corruption the error branches guard
 * against.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class CriteriaConverterTest {

    @Suppress("UNCHECKED_CAST")
    private fun groupsOf(criteria: Criteria): MutableList<Any> {
        val field = Criteria::class.java.getDeclaredField("criterionGroups")
        field.isAccessible = true
        return field.get(criteria) as MutableList<Any>
    }

    @Test
    fun convert_supportsNestedCriteriaInsideOrGroup() {
        // Array group containing both a Criterion and a nested Criteria
        val nested = Criteria.of("name", OperatorEnum.EQ, "name1")
        val criteria = Criteria().addOr(Criterion("name", OperatorEnum.EQ, "name2"), nested)
        groupsOf(criteria) // sanity: reflection works
        assertNotNull(CriteriaConverter.convert(criteria, TestTableKtorms))
    }

    @Test
    fun convert_rejectsUnsupportedTopLevelElement() {
        val criteria = Criteria.of("name", OperatorEnum.EQ, "x")
        groupsOf(criteria).add(42) // not a Criterion / Criteria / Array
        val ex = assertFailsWith<IllegalStateException> {
            CriteriaConverter.convert(criteria, TestTableKtorms)
        }
        assertTrue(ex.message!!.contains("Unsupported element type"), "must fail fast on corrupted criterion groups")
    }

    @Test
    fun convert_rejectsUnsupportedElementInsideOrGroup() {
        val criteria = Criteria.of("name", OperatorEnum.EQ, "x")
        groupsOf(criteria).add(arrayOf<Any>(42)) // OR group containing garbage
        val ex = assertFailsWith<IllegalStateException> {
            CriteriaConverter.convert(criteria, TestTableKtorms)
        }
        assertTrue(ex.message!!.contains("Unsupported element type"))
    }
}
