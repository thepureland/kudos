package io.kudos.ability.data.memdb.redis.dao.support

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import jakarta.annotation.Resource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.Date
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [CriteriaRedisResolver] (based on RedisTestContainer).
 *
 * Index data layout written directly through the RedisTemplate:
 *  - Set index `set:status:active` = {a, b}, `set:status:inactive` = {c}, plus a Unicode property/value pair.
 *  - ZSet index `zset:score`: a=1.0, b=2.5, c=-3.0, d=10.0 (negative score guards the -Double.MAX_VALUE lower bound).
 *
 * Covers: null/empty criteria, Set index EQ/IEQ/IN (string split / collection / array / other type),
 * ZSet index EQ/IN/GT/GE/LT/LE/BETWEEN/NOT_BETWEEN (range / array / list shapes and malformed ranges),
 * non-numeric score fallbacks, AND intersection with short-circuit, OR groups (criterions and nested
 * criterias), nested AND criteria, unsupported operators and null-accepting operators.
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
internal class CriteriaRedisResolverTest {

    @Resource
    private lateinit var redisTemplates: RedisTemplates

    private val prefix = "rdb:test:CriteriaRedisResolverTest:idx"

    private fun resolver() = CriteriaRedisResolver(prefix, redisTemplates.defaultRedisTemplate)

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry?) {
            RedisTestContainer.startIfNeeded(registry)
        }
    }

    @BeforeTest
    fun setupIndexData() {
        val template = redisTemplates.defaultRedisTemplate
        val keys = template.keys("$prefix*")
        if (!keys.isNullOrEmpty()) template.delete(keys)
        val set = template.opsForSet()
        set.add("$prefix:set:status:active", "a", "b")
        set.add("$prefix:set:status:inactive", "c")
        set.add("$prefix:set:名稱:張三", "u")
        val zset = template.opsForZSet()
        zset.add("$prefix:zset:score", "a", 1.0)
        zset.add("$prefix:zset:score", "b", 2.5)
        zset.add("$prefix:zset:score", "c", -3.0)
        zset.add("$prefix:zset:score", "d", 10.0)
    }

    // ---------- empty criteria ----------

    @Test
    fun nullOrEmptyCriteria_meansAll_returnsNull() {
        assertNull(resolver().resolveToIds(null))
        assertNull(resolver().resolveToIds(Criteria()))
    }

    // ---------- Set index ----------

    @Test
    fun eq_onNonNumericValue_usesSetIndex() {
        assertEquals(setOf("a", "b"), resolver().resolveToIds(Criteria.of("status", OperatorEnum.EQ, "active")))
        assertEquals(setOf("c"), resolver().resolveToIds(Criteria.of("status", OperatorEnum.EQ, "inactive")))
    }

    @Test
    fun ieq_usesSetIndexLikeEq() {
        assertEquals(setOf("a", "b"), resolver().resolveToIds(Criteria.of("status", OperatorEnum.IEQ, "active")))
    }

    @Test
    fun eq_missingIndexValue_returnsEmpty() {
        assertEquals(emptySet(), resolver().resolveToIds(Criteria.of("status", OperatorEnum.EQ, "archived")))
    }

    @Test
    fun in_commaSeparatedString_unionsSets_andTrims() {
        val ids = resolver().resolveToIds(Criteria.of("status", OperatorEnum.IN, "active , inactive"))
        assertEquals(setOf("a", "b", "c"), ids)
    }

    @Test
    fun in_stringCollection_unionsSets() {
        val ids = resolver().resolveToIds(Criteria.of("status", OperatorEnum.IN, listOf("active", "inactive")))
        assertEquals(setOf("a", "b", "c"), ids)
    }

    @Test
    fun in_array_unionsSets() {
        val ids = resolver().resolveToIds(Criteria.of("status", OperatorEnum.IN, arrayOf("active", "inactive")))
        assertEquals(setOf("a", "b", "c"), ids)
    }

    @Test
    fun in_otherSingleValue_usesToString() {
        val ids = resolver().resolveToIds(Criteria.of("status", OperatorEnum.IN, StringBuilder("active")))
        assertEquals(setOf("a", "b"), ids)
    }

    @Test
    fun unicode_propertyAndValue_resolveViaSetIndex() {
        assertEquals(setOf("u"), resolver().resolveToIds(Criteria.of("名稱", OperatorEnum.EQ, "張三")))
    }

    /**
     * A numeric value must not be routed to the ZSet index just because it looks numeric: an enum-like column
     * declared filterable-only (Set index, no ZSet index) would then silently match nothing.
     */
    @Test
    fun eq_onNumericValue_usesSetIndexWhenTheSetKeyExists() {
        val template = redisTemplates.defaultRedisTemplate
        // "type" is filterable-only: a Set index exists, no zset:type is ever written
        template.opsForSet().add("$prefix:set:type:1", "x", "y")
        template.opsForSet().add("$prefix:set:type:2", "z")

        assertEquals(setOf("x", "y"), resolver().resolveToIds(Criteria.of("type", OperatorEnum.EQ, 1)))
        assertEquals(setOf("z"), resolver().resolveToIds(Criteria.of("type", OperatorEnum.EQ, 2)))
        assertEquals(setOf("x", "y", "z"), resolver().resolveToIds(Criteria.of("type", OperatorEnum.IN, listOf(1, 2))))
    }

    /** With no Set index for the property, a numeric equality still falls back to the ZSet index. */
    @Test
    fun eq_onNumericValue_fallsBackToZSetWhenNoSetIndexExists() {
        // "score" is sortable-only: only zset:score exists
        assertEquals(setOf("b"), resolver().resolveToIds(Criteria.of("score", OperatorEnum.EQ, 2.5)))
    }

    @Test
    fun eq_withNullValue_isFilteredOutByCriteriaItself_meansAll() {
        // Criteria refuses null values for non-null-accepting operators -> the criteria stays empty
        // and the resolver reports "all" (null); the resolver's own null-value guard is unreachable
        assertNull(resolver().resolveToIds(Criteria.of("status", OperatorEnum.EQ, null)))
    }

    @Test
    fun in_emptyCollection_isFilteredOutByCriteriaItself_meansAll() {
        // Criteria refuses empty collections -> the criteria stays empty and the resolver reports "all"
        assertNull(resolver().resolveToIds(Criteria.of("status", OperatorEnum.IN, emptyList<String>())))
    }

    @Test
    fun unsupportedSetOperator_actsAsNoConstraint() {
        // LIKE on a non-numeric value is not supported by the Set index -> the only group resolves
        // to "no constraint" and the overall result degenerates to an empty id set
        assertEquals(emptySet(), resolver().resolveToIds(Criteria.of("status", OperatorEnum.LIKE, "act")))
    }

    // ---------- ZSet index ----------

    @Test
    fun eq_onNumber_usesZSetScoreEquality() {
        assertEquals(setOf("b"), resolver().resolveToIds(Criteria.of("score", OperatorEnum.EQ, 2.5)))
        assertEquals(setOf("c"), resolver().resolveToIds(Criteria.of("score", OperatorEnum.EQ, -3)))
    }

    @Test
    fun eq_onNumericString_usesZSet() {
        assertEquals(setOf("b"), resolver().resolveToIds(Criteria.of("score", OperatorEnum.EQ, "2.5")))
    }

    @Test
    fun in_singleNumber_usesZSet() {
        assertEquals(setOf("d"), resolver().resolveToIds(Criteria.of("score", OperatorEnum.IN, 10.0)))
    }

    @Test
    fun gt_excludesBoundary() {
        assertEquals(setOf("d"), resolver().resolveToIds(Criteria.of("score", OperatorEnum.GT, 2.5)))
    }

    @Test
    fun ge_includesBoundary() {
        assertEquals(setOf("b", "d"), resolver().resolveToIds(Criteria.of("score", OperatorEnum.GE, 2.5)))
    }

    @Test
    fun lt_excludesBoundary_andSupportsNegativeScores() {
        assertEquals(setOf("c"), resolver().resolveToIds(Criteria.of("score", OperatorEnum.LT, 1.0)))
        assertEquals(emptySet(), resolver().resolveToIds(Criteria.of("score", OperatorEnum.LT, -3.0)))
    }

    @Test
    fun le_includesBoundary() {
        assertEquals(setOf("a", "c"), resolver().resolveToIds(Criteria.of("score", OperatorEnum.LE, 1.0)))
    }

    @Test
    fun rangeOperator_withNonNumericString_fallsBackToMinScore() {
        // toDouble("abc") falls back to -Double.MAX_VALUE -> GT matches every member (incl. negative scores)
        assertEquals(setOf("a", "b", "c", "d"), resolver().resolveToIds(Criteria.of("score", OperatorEnum.GE, "abc")))
    }

    @Test
    fun rangeOperator_withNonNumericObject_fallsBackToMinScore() {
        assertEquals(setOf("a", "b", "c", "d"), resolver().resolveToIds(Criteria.of("score", OperatorEnum.GE, Date(0))))
    }

    @Test
    fun between_closedRange() {
        assertEquals(setOf("a", "b"), resolver().resolveToIds(Criteria.of("score", OperatorEnum.BETWEEN, 1.0..2.5)))
    }

    @Test
    fun between_arrayShape() {
        val ids = resolver().resolveToIds(Criteria.of("score", OperatorEnum.BETWEEN, arrayOf<Any>(1.0, 2.5)))
        assertEquals(setOf("a", "b"), ids)
    }

    @Test
    fun between_listShape() {
        val ids = resolver().resolveToIds(Criteria.of("score", OperatorEnum.BETWEEN, listOf(-3, 1)))
        assertEquals(setOf("a", "c"), ids)
    }

    @Test
    fun between_malformedRanges_returnEmpty() {
        val r = resolver()
        assertEquals(emptySet(), r.resolveToIds(Criteria.of("score", OperatorEnum.BETWEEN, Pair(1.0, 2.5))))
        assertEquals(emptySet(), r.resolveToIds(Criteria.of("score", OperatorEnum.BETWEEN, arrayOf<Any?>(1.0))))
        assertEquals(emptySet(), r.resolveToIds(Criteria.of("score", OperatorEnum.BETWEEN, arrayOf(1.0, null))))
        assertEquals(emptySet(), r.resolveToIds(Criteria.of("score", OperatorEnum.BETWEEN, listOf(5))))
        assertEquals(emptySet(), r.resolveToIds(Criteria.of("score", OperatorEnum.BETWEEN, listOf(null, 2.0))))
    }

    @Test
    fun notBetween_excludesRange() {
        val ids = resolver().resolveToIds(Criteria.of("score", OperatorEnum.NOT_BETWEEN, 1.0..2.5))
        assertEquals(setOf("c", "d"), ids)
    }

    @Test
    fun notBetween_malformedRange_returnsEmpty() {
        assertEquals(emptySet(), resolver().resolveToIds(Criteria.of("score", OperatorEnum.NOT_BETWEEN, Pair(1.0, 2.5))))
    }

    @Test
    fun unsupportedZSetOperator_actsAsNoConstraint() {
        // LIKE with a numeric value routes to the ZSet branch, which does not support it
        assertEquals(emptySet(), resolver().resolveToIds(Criteria.of("score", OperatorEnum.LIKE, 5)))
    }

    // ---------- AND / OR / nesting ----------

    @Test
    fun and_intersectsGroups() {
        val criteria = Criteria.of("status", OperatorEnum.EQ, "active")
            .addAnd("score", OperatorEnum.GE, 2)
        assertEquals(setOf("b"), resolver().resolveToIds(criteria))
    }

    @Test
    fun and_disjointGroups_shortCircuitToEmpty() {
        val criteria = Criteria.of("status", OperatorEnum.EQ, "inactive")
            .addAnd("score", OperatorEnum.GE, 5)
        assertEquals(emptySet(), resolver().resolveToIds(criteria))
    }

    @Test
    fun orGroup_ofCriterions_unionsIds() {
        val criteria = Criteria().addOr(
            Criterion("status", OperatorEnum.EQ, "active"),
            Criterion("status", OperatorEnum.EQ, "inactive")
        )
        assertEquals(setOf("a", "b", "c"), resolver().resolveToIds(criteria))
    }

    @Test
    fun orGroup_ofNestedCriterias_unionsIds() {
        val criteria = Criteria().addOr(
            Criteria.of("score", OperatorEnum.GT, 5),
            Criteria.of("status", OperatorEnum.EQ, "inactive")
        )
        assertEquals(setOf("c", "d"), resolver().resolveToIds(criteria))
    }

    @Test
    fun orGroup_allUnsupported_actsAsNoConstraint() {
        val criteria = Criteria().addOr(
            Criterion("status", OperatorEnum.LIKE, "x"),
            Criterion("status", OperatorEnum.LIKE, "y")
        )
        assertEquals(emptySet(), resolver().resolveToIds(criteria))
    }

    @Test
    fun orGroup_combinedWithAndGroup() {
        val criteria = Criteria()
            .addOr(
                Criterion("status", OperatorEnum.EQ, "active"),
                Criterion("status", OperatorEnum.EQ, "inactive")
            )
            .addAnd("score", OperatorEnum.LT, 0)
        assertEquals(setOf("c"), resolver().resolveToIds(criteria))
    }

    @Test
    fun nestedCriteria_joinedWithAnd() {
        val criteria = Criteria()
            .addAnd(Criteria.of("status", OperatorEnum.EQ, "active"))
            .addAnd("score", OperatorEnum.LE, 1.5)
        assertEquals(setOf("a"), resolver().resolveToIds(criteria))
    }

    /** Injects an element of an unsupported type into the (private) group list to drive the defensive else-branch. */
    private fun injectRawGroup(criteria: Criteria, group: Any) {
        val field = Criteria::class.java.getDeclaredField("criterionGroups")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(criteria) as MutableList<Any>).add(group)
    }

    @Test
    fun unknownGroupType_isIgnored() {
        // the public Criteria API only produces Criterion/Array/Criteria groups; an alien element
        // (injected reflectively) must be skipped without affecting the other groups
        val criteria = Criteria.of("status", OperatorEnum.EQ, "active")
        injectRawGroup(criteria, "garbage")
        assertEquals(setOf("a", "b"), resolver().resolveToIds(criteria))
    }

    @Test
    fun orGroup_unknownElementType_isIgnored() {
        val criteria = Criteria()
        injectRawGroup(criteria, arrayOf<Any>("garbage", Criterion("status", OperatorEnum.EQ, "inactive")))
        assertEquals(setOf("c"), resolver().resolveToIds(criteria))
    }

    @Test
    fun nullAcceptingOperator_isSkippedAsUnconstrainedGroup() {
        // IS_NULL accepts a null value -> the group imposes no constraint; with no other groups the
        // current implementation degenerates to an empty id set (see suspected-bug notes)
        assertEquals(emptySet(), resolver().resolveToIds(Criteria.of("status", OperatorEnum.IS_NULL, null)))
    }
}
