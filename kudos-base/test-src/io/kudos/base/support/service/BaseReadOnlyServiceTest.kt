package io.kudos.base.support.service

import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.query.Criteria
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.support.service.impl.BaseReadOnlyService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * BaseReadOnlyService test cases.
 *
 * Verifies that every read-only method delegates to the underlying DAO (returning its value and forwarding
 * the call), plus the two pagingSearch(listSearchPayload) branches (pageNo null vs. non-null) and the
 * inline reified helpers getAs / getByIdsAs / searchAs / pagingSearchAs.
 *
 * @author K
 * @since 1.0.0
 */
internal class BaseReadOnlyServiceTest {

    private data class Po(override val id: Long, val name: String = "") : IIdEntity<Long>

    private class PoReadService(dao: FakeCrudDao<Long, Po>) :
        BaseReadOnlyService<Long, Po, FakeCrudDao<Long, Po>>(dao)

    private val dao = FakeCrudDao<Long, Po>()
    private val service = PoReadService(dao)

    @Test
    fun testGet() {
        val po = Po(1, "a")
        dao.getResult = po
        assertSame(po, service.get(1))
        assertEquals("get", dao.lastCall)
    }

    @Test
    fun testGetTyped() {
        dao.getTypedResult = "x"
        assertEquals("x", service.get(1, String::class))
        assertEquals("getTyped", dao.lastCall)
    }

    @Test
    fun testGetAsInline() {
        dao.getTypedResult = "y"
        assertEquals("y", service.getAs<String>(1))
        assertEquals("getTyped", dao.lastCall)
    }

    @Test
    fun testGetByIds() {
        dao.listResult = listOf(Po(1), Po(2))
        assertEquals(2, service.getByIds(listOf(1, 2)).size)
        assertEquals("getByIds", dao.lastCall)
    }

    @Test
    fun testGetByIdsTypedAndInline() {
        dao.listResult = listOf("a")
        assertEquals(listOf("a"), service.getByIds(listOf(1), String::class))
        assertEquals("getByIdsTyped", dao.lastCall)
        assertEquals(listOf("a"), service.getByIdsAs<String>(listOf(1)))
        assertEquals("getByIdsTyped", dao.lastCall)
    }

    @Test
    fun testOneSearchFamily() {
        dao.listResult = listOf(Po(1))
        assertEquals(1, service.oneSearch(Po::name, "x").size)
        assertEquals("oneSearch", dao.lastCall)

        dao.listResult = listOf("v")
        assertEquals(listOf("v"), service.oneSearchProperty(Po::name, "x", Po::name))
        assertEquals("oneSearchProperty", dao.lastCall)

        dao.mapListResult = listOf(mapOf("name" to "v"))
        assertEquals(1, service.oneSearchProperties(Po::name, "x", listOf(Po::name)).size)
        assertEquals("oneSearchProperties", dao.lastCall)
    }

    @Test
    fun testAllSearchFamily() {
        dao.listResult = listOf(Po(1))
        assertEquals(1, service.allSearch().size)
        assertEquals("allSearch", dao.lastCall)

        dao.listResult = listOf("v")
        assertEquals(listOf("v"), service.allSearchProperty(Po::name))
        assertEquals("allSearchProperty", dao.lastCall)

        dao.mapListResult = listOf(mapOf("name" to "v"))
        assertEquals(1, service.allSearchProperties(listOf(Po::name)).size)
        assertEquals("allSearchProperties", dao.lastCall)
    }

    @Test
    fun testAndOrInSearch() {
        dao.listResult = listOf(Po(1))
        assertEquals(1, service.andSearch(mapOf(Po::name to "x")).size)
        assertEquals("andSearch", dao.lastCall)
        assertEquals(1, service.orSearch(mapOf(Po::name to "x")).size)
        assertEquals("orSearch", dao.lastCall)
        assertEquals(1, service.inSearch(Po::name, listOf("x")).size)
        assertEquals("inSearch", dao.lastCall)
    }

    @Test
    fun testInSearchByIdFamily() {
        dao.listResult = listOf(Po(1))
        assertEquals(1, service.inSearchById(listOf(1)).size)
        assertEquals("inSearchById", dao.lastCall)

        dao.listResult = listOf("v")
        assertEquals(listOf("v"), service.inSearchPropertyById(listOf(1), "name"))
        assertEquals("inSearchPropertyByIdStr", dao.lastCall)

        dao.listResult = listOf("v")
        assertEquals(listOf("v"), service.inSearchPropertyById(listOf(1), Po::name))
        assertEquals("inSearchPropertyByIdTyped", dao.lastCall)

        dao.inSearchPropertiesByIdHandler = { listOf(mapOf("id" to 1L)) }
        assertEquals(1, service.inSearchPropertiesById(listOf(1), listOf("id")).size)
        assertEquals("inSearchPropertiesById", dao.lastCall)
    }

    @Test
    fun testSearchCriteriaFamily() {
        dao.listResult = listOf(Po(1))
        assertEquals(1, service.search(Criteria()).size)
        assertEquals("searchCriteria", dao.lastCall)

        dao.listResult = listOf("v")
        assertEquals(listOf("v"), service.search(Criteria(), String::class))
        assertEquals("searchCriteriaTyped", dao.lastCall)
        assertEquals(listOf("v"), service.searchAs<String>(Criteria()))
        assertEquals("searchCriteriaTyped", dao.lastCall)
    }

    @Test
    fun testPagingSearchCriteriaFamily() {
        dao.listResult = listOf(Po(1))
        assertEquals(1, service.pagingSearch(Criteria(), 1, 10).size)
        assertEquals("pagingSearchCriteria", dao.lastCall)

        dao.listResult = listOf("v")
        assertEquals(listOf("v"), service.pagingSearch(Criteria(), String::class, 1, 10))
        assertEquals("pagingSearchCriteriaTyped", dao.lastCall)
        assertEquals(listOf("v"), service.pagingSearchAs<String>(Criteria(), 1, 10))
        assertEquals("pagingSearchCriteriaTyped", dao.lastCall)
    }

    @Test
    fun testSearchPropertyAndProperties() {
        dao.searchPropertyHandler = { listOf("v") }
        assertEquals(listOf("v"), service.searchProperty(Criteria(), Po::name))
        assertEquals("searchProperty", dao.lastCall)

        dao.searchPropertiesHandler = { listOf(mapOf("name" to "v")) }
        assertEquals(1, service.searchProperties(Criteria(), listOf(Po::name)).size)
        assertEquals("searchProperties", dao.lastCall)
    }

    @Test
    fun testPagingReturnPropertyAndProperties() {
        dao.listResult = listOf("v")
        assertEquals(listOf("v"), service.pagingReturnProperty(Criteria(), Po::name, 1, 10))
        assertEquals("pagingReturnProperty", dao.lastCall)

        dao.mapListResult = listOf(mapOf("name" to "v"))
        assertEquals(1, service.pagingReturnProperties(Criteria(), listOf(Po::name), 1, 10).size)
        assertEquals("pagingReturnProperties", dao.lastCall)
    }

    @Test
    fun testSearchListPayloadFamily() {
        dao.searchPayloadHandler = { listOf(Po(1)) }
        assertEquals(1, service.search(ListSearchPayload()).size)
        assertEquals("searchPayload", dao.lastCall)

        dao.listResult = listOf("v")
        assertEquals(listOf("v"), service.search(ListSearchPayload(), String::class))
        assertEquals("searchPayloadTyped", dao.lastCall)
    }

    @Test
    fun testPagingSearchPayloadWithPageNoUsesCount() {
        dao.searchPayloadHandler = { listOf(Po(1), Po(2)) }
        dao.countResult = 57
        val payload = ListSearchPayload().apply { pageNo = 1; pageSize = 10 }
        val result: PagingSearchResult<*> = service.pagingSearch(payload)
        assertEquals(2, result.data.size)
        assertEquals(57, result.totalCount, "with pageNo set, totalCount comes from dao.count")
    }

    @Test
    fun testPagingSearchPayloadWithoutPageNoUsesResultSize() {
        dao.searchPayloadHandler = { listOf(Po(1), Po(2), Po(3)) }
        dao.countResult = 999 // must NOT be used
        val payload = ListSearchPayload() // pageNo == null
        val result = service.pagingSearch(payload)
        assertEquals(3, result.data.size)
        assertEquals(3, result.totalCount, "without pageNo, totalCount is the result list size")
    }

    @Test
    fun testCountFamily() {
        dao.countResult = 5
        assertEquals(5, service.count(Criteria()))
        assertEquals("countCriteria", dao.lastCall)
        assertEquals(5, service.count(ListSearchPayload()))
        assertEquals("countPayload", dao.lastCall)
    }

    @Test
    fun testAggregations() {
        dao.numberResult = 42
        assertEquals(42, service.sum(Po::id))
        assertEquals("sum", dao.lastCall)
        assertEquals(42, service.avg(Po::id))
        assertEquals("avg", dao.lastCall)

        dao.comparableResult = 99L
        assertEquals(99L, service.max(Po::id))
        assertEquals("max", dao.lastCall)
        assertEquals(99L, service.min(Po::id))
        assertEquals("min", dao.lastCall)

        dao.comparableResult = null
        assertNull(service.max(Po::id))
        assertEquals("max", dao.lastCall)
    }
}
