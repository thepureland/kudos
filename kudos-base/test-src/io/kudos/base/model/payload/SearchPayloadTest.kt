package io.kudos.base.model.payload

import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import io.kudos.base.support.logic.AndOrEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for the search-payload hierarchy:
 * - [ISearchPayload] interface default-method bodies
 * - [ListSearchPayload] paging/sorting fields and overridable defaults
 * - [MutableListSearchPayload] writable backing fields exposed through the getters
 * - [UpdatePayload] / [FormPayload]
 *
 * @author K
 * @since 1.0.0
 */
internal class SearchPayloadTest {

    // ============================================================
    // ISearchPayload interface defaults (bare implementation exercises default bodies)
    // ============================================================

    private class BareSearchPayload : ISearchPayload

    @Test
    fun testISearchPayloadDefaults() {
        // Dispatch through the interface type so the default-method bodies (ISearchPayload$DefaultImpls) run.
        val p: ISearchPayload = BareSearchPayload()
        assertEquals(AndOrEnum.AND, p.getAndOr(), "default logical relation is AND")
        assertNull(p.getNullProperties())
        assertNull(p.getOperators())
        assertNull(p.getReturnEntityClass())
        assertNull(p.getReturnProperties())
        assertNull(p.getCriterions())
    }

    @Test
    fun testISearchPayloadOverrides() {
        val custom = object : ISearchPayload {
            override fun getAndOr() = AndOrEnum.OR
            override fun getNullProperties() = listOf("a")
            override fun getReturnProperties() = listOf("name")
            override fun getCriterions() = listOf(Criterion("x", OperatorEnum.EQ, "1"))
            override fun getReturnEntityClass() = String::class
        }
        assertEquals(AndOrEnum.OR, custom.getAndOr())
        assertEquals(listOf("a"), custom.getNullProperties())
        assertEquals(listOf("name"), custom.getReturnProperties())
        assertEquals(1, custom.getCriterions()!!.size)
        assertEquals(String::class, custom.getReturnEntityClass())
    }

    // ============================================================
    // ListSearchPayload
    // ============================================================

    @Test
    fun testListSearchPayloadDefaults() {
        val p = ListSearchPayload()
        assertNull(p.pageNo)
        assertNull(p.pageSize)
        assertNull(p.orders)
        assertEquals(100, p.getMaxPageSize(), "default max page size is 100")
        assertTrue(!p.isUnpagedSearchAllowed(), "unpaged search disabled by default")
        // also inherits ISearchPayload defaults
        assertEquals(AndOrEnum.AND, p.getAndOr())
    }

    @Test
    fun testListSearchPayloadMutators() {
        val p = ListSearchPayload()
        p.pageNo = 2
        p.pageSize = 20
        val orders = listOf(Order("name"))
        p.orders = orders
        assertEquals(2, p.pageNo)
        assertEquals(20, p.pageSize)
        assertSame(orders, p.orders)
    }

    @Test
    fun testListSearchPayloadOverrides() {
        val p = object : ListSearchPayload() {
            override fun getMaxPageSize() = 500
            override fun isUnpagedSearchAllowed() = true
        }
        assertEquals(500, p.getMaxPageSize())
        assertTrue(p.isUnpagedSearchAllowed())
    }

    // ============================================================
    // MutableListSearchPayload
    // ============================================================

    @Test
    fun testMutableListSearchPayloadDefaults() {
        val p = MutableListSearchPayload()
        assertEquals(AndOrEnum.AND, p.getAndOr())
        assertNull(p.getReturnEntityClass())
        assertNull(p.getNullProperties())
        assertNull(p.getOperators())
        assertNull(p.getCriterions())
        assertNull(p.getReturnProperties())
    }

    @Test
    fun testMutableListSearchPayloadSetters() {
        val p = MutableListSearchPayload()
        p.setAndOr(AndOrEnum.OR)
        p.setReturnEntityClass(String::class)
        p.setNullProperties(listOf("a", "b"))
        val criterions = listOf(Criterion("x", OperatorEnum.EQ, "1"))
        p.setCriterions(criterions)
        p.setReturnProperties(listOf("name", "code"))
        val nameProp = MutableListSearchPayload::pageNo
        val ops = mapOf<kotlin.reflect.KProperty0<*>, OperatorEnum>(p::pageNo to OperatorEnum.GT)
        p.setOperators(ops)

        assertEquals(AndOrEnum.OR, p.getAndOr())
        assertEquals(String::class, p.getReturnEntityClass())
        assertEquals(listOf("a", "b"), p.getNullProperties())
        assertSame(criterions, p.getCriterions())
        assertEquals(listOf("name", "code"), p.getReturnProperties())
        assertSame(ops, p.getOperators())
        assertNull(nameProp.get(p)) // sanity
    }

    @Test
    fun testMutableListSearchPayloadInheritsPaging() {
        val p = MutableListSearchPayload()
        p.pageNo = 1
        p.pageSize = 10
        assertEquals(1, p.pageNo)
        assertEquals(10, p.pageSize)
        assertEquals(100, p.getMaxPageSize())
    }

    // ============================================================
    // UpdatePayload
    // ============================================================

    @Test
    fun testUpdatePayloadDefaults() {
        val p = UpdatePayload<BareSearchPayload>()
        assertNull(p.nullProperties)
        assertNull(p.searchPayload)
    }

    @Test
    fun testUpdatePayloadMutators() {
        val p = UpdatePayload<BareSearchPayload>()
        val search = BareSearchPayload()
        p.nullProperties = listOf("remark")
        p.searchPayload = search
        assertEquals(listOf("remark"), p.nullProperties)
        assertSame(search, p.searchPayload)
    }

    // ============================================================
    // FormPayload (abstract; exercise via a concrete subclass)
    // ============================================================

    private class TestFormPayload(override val id: Long) : FormPayload<Long>()

    @Test
    fun testFormPayloadExposesId() {
        val form = TestFormPayload(123L)
        assertEquals(123L, form.id)
    }
}
