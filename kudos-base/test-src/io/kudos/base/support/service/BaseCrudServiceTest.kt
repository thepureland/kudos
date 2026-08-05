package io.kudos.base.support.service

import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.error.ServiceException
import io.kudos.base.model.contract.common.IHasBuiltIn
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.model.payload.MutableListSearchPayload
import io.kudos.base.query.Criteria
import io.kudos.base.support.service.impl.BaseCrudService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * BaseCrudService test cases.
 *
 * Two entity flavors are exercised:
 * - A plain entity (no IHasBuiltIn): every insert/update/delete delegates straight to the DAO.
 * - A built-in entity (IHasBuiltIn): the Service-layer built-in-row protection branches
 *   (deleteById / batchDelete / batchDeleteCriteria / batchDeleteWhen / delete) including the
 *   "built-in not deletable" exception paths and the "record does not exist" paths.
 *
 * @author K
 * @since 1.0.0
 */
internal class BaseCrudServiceTest {

    // ---------- plain entity (no built-in marker) ----------

    private data class PlainPo(override val id: Long, val name: String = "") : IIdEntity<Long>

    private class PlainService(dao: FakeCrudDao<Long, PlainPo>) :
        BaseCrudService<Long, PlainPo, FakeCrudDao<Long, PlainPo>>(dao)

    // ---------- built-in entity ----------

    private data class BuiltInPo(
        override val id: Long,
        override var builtIn: Boolean = false,
        val name: String = ""
    ) : IIdEntity<Long>, IHasBuiltIn

    private class BuiltInService(dao: FakeCrudDao<Long, BuiltInPo>) :
        BaseCrudService<Long, BuiltInPo, FakeCrudDao<Long, BuiltInPo>>(dao)

    // ============================================================
    // Insert delegation
    // ============================================================

    @Test
    fun testInsertDelegation() {
        val dao = FakeCrudDao<Long, PlainPo>()
        val service = PlainService(dao)
        dao.insertResult = 7L
        assertEquals(7L, service.insert(PlainPo(0)))
        assertEquals("insert", dao.lastCall)
        assertEquals(7L, service.insertOnly(PlainPo(0), "name"))
        assertEquals("insertOnly", dao.lastCall)
        assertEquals(7L, service.insertExclude(PlainPo(0), "name"))
        assertEquals("insertExclude", dao.lastCall)

        dao.intResult = 3
        assertEquals(3, service.batchInsert(listOf(PlainPo(1))))
        assertEquals("batchInsert", dao.lastCall)
        assertEquals(3, service.batchInsertOnly(listOf(PlainPo(1)), 100, "name"))
        assertEquals("batchInsertOnly", dao.lastCall)
        assertEquals(3, service.batchInsertExclude(listOf(PlainPo(1)), 100, "name"))
        assertEquals("batchInsertExclude", dao.lastCall)
    }

    // ============================================================
    // Update delegation
    // ============================================================

    @Test
    fun testUpdateDelegation() {
        val dao = FakeCrudDao<Long, PlainPo>()
        val service = PlainService(dao)
        dao.updateResult = true
        val c = Criteria("id", io.kudos.base.query.enums.OperatorEnum.EQ, 1L)
        assertTrue(service.update(PlainPo(1)))
        assertEquals("update", dao.lastCall)
        assertTrue(service.updateWhen(PlainPo(1), c))
        assertEquals("updateWhen", dao.lastCall)
        assertTrue(service.updateProperties(1, mapOf("name" to "x")))
        assertEquals("updateProperties", dao.lastCall)
        assertTrue(service.updatePropertiesWhen(1, mapOf("name" to "x"), c))
        assertEquals("updatePropertiesWhen", dao.lastCall)
        assertTrue(service.updateOnly(PlainPo(1), "name"))
        assertEquals("updateOnly", dao.lastCall)
        assertTrue(service.updateOnlyWhen(PlainPo(1), c, "name"))
        assertEquals("updateOnlyWhen", dao.lastCall)
        assertTrue(service.updateExcludeProperties(PlainPo(1), "name"))
        assertEquals("updateExcludeProperties", dao.lastCall)
        assertTrue(service.updateExcludePropertiesWhen(PlainPo(1), c, "name"))
        assertEquals("updateExcludePropertiesWhen", dao.lastCall)
    }

    @Test
    fun testBatchUpdateDelegation() {
        val dao = FakeCrudDao<Long, PlainPo>()
        val service = PlainService(dao)
        dao.intResult = 4
        val c = Criteria("id", io.kudos.base.query.enums.OperatorEnum.EQ, 1L)
        assertEquals(4, service.batchUpdate(listOf(PlainPo(1))))
        assertEquals("batchUpdate", dao.lastCall)
        assertEquals(4, service.batchUpdateWhen(listOf(PlainPo(1)), c))
        assertEquals("batchUpdateWhen", dao.lastCall)
        assertEquals(4, service.batchUpdateProperties(c, mapOf("name" to "x")))
        assertEquals("batchUpdateProperties", dao.lastCall)
        assertEquals(4, service.batchUpdateOnly(listOf(PlainPo(1)), 100, "name"))
        assertEquals("batchUpdateOnly", dao.lastCall)
        assertEquals(4, service.batchUpdateOnlyWhen(listOf(PlainPo(1)), c, 100, "name"))
        assertEquals("batchUpdateOnlyWhen", dao.lastCall)
        assertEquals(4, service.batchUpdateExcludeProperties(listOf(PlainPo(1)), 100, "name"))
        assertEquals("batchUpdateExcludeProperties", dao.lastCall)
        assertEquals(4, service.batchUpdateExcludePropertiesWhen(listOf(PlainPo(1)), c, 100, "name"))
        assertEquals("batchUpdateExcludePropertiesWhen", dao.lastCall)
    }

    @Test
    fun testBatchUpdateWhenPayloadDelegation() {
        val dao = FakeCrudDao<Long, PlainPo>()
        val service = PlainService(dao)
        dao.intResult = 2
        val payload = io.kudos.base.model.payload.UpdatePayload<ListSearchPayload>()
        assertEquals(2, service.batchUpdateWhen(payload))
        assertEquals("batchUpdateWhenPayload", dao.lastCall)
    }

    // ============================================================
    // Delete delegation for plain (non-built-in) entity
    // ============================================================

    @Test
    fun testPlainDeleteDelegatesDirectly() {
        val dao = FakeCrudDao<Long, PlainPo>()
        val service = PlainService(dao)
        dao.deleteResult = true
        dao.intResult = 5
        dao.batchDeleteCriteriaHandler = { 5 }

        assertTrue(service.deleteById(1))
        assertEquals("deleteById", dao.lastCall)

        assertTrue(service.delete(PlainPo(1)))
        assertEquals("delete", dao.lastCall)

        assertEquals(5, service.batchDelete(listOf(1, 2)))
        assertEquals("batchDelete", dao.lastCall)

        assertEquals(5, service.batchDeleteCriteria(Criteria("id", io.kudos.base.query.enums.OperatorEnum.EQ, 1L)))
        assertEquals("batchDeleteCriteria", dao.lastCall)

        assertEquals(5, service.batchDeleteWhen(ListSearchPayload()))
        assertEquals("batchDeleteWhen", dao.lastCall)
    }

    // ============================================================
    // deleteById / delete for built-in entity
    // ============================================================

    @Test
    fun testBuiltInDeleteByIdSuccessWhenOneRowDeleted() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        dao.batchDeleteCriteriaHandler = { 1 } // builtIn=false row deleted
        assertTrue(service.deleteById(1))
    }

    @Test
    fun testBuiltInDeleteByIdReturnsFalseWhenRecordMissing() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        dao.batchDeleteCriteriaHandler = { 0 }
        dao.searchPropertyHandler = { emptyList<Any?>() } // record does not exist
        assertFalse(service.deleteById(1))
    }

    @Test
    fun testBuiltInDeleteByIdThrowsWhenRowIsBuiltIn() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        dao.batchDeleteCriteriaHandler = { 0 }
        dao.searchPropertyHandler = { listOf(true) } // the row exists and is built-in
        val ex = assertFailsWith<ServiceException> { service.deleteById(1) }
        assertEquals(CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE, ex.errorCode)
    }

    @Test
    fun testBuiltInDeleteByIdFalseWhenRowExistsButNotBuiltIn() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        dao.batchDeleteCriteriaHandler = { 0 }
        dao.searchPropertyHandler = { listOf(false) } // exists, builtIn=false but already gone in delete step
        assertFalse(service.deleteById(1))
    }

    @Test
    fun testBuiltInDeleteEntityDelegatesToDeleteById() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        dao.batchDeleteCriteriaHandler = { 1 }
        assertTrue(service.delete(BuiltInPo(1)))
    }

    // ============================================================
    // batchDelete (by ids) for built-in entity
    // ============================================================

    @Test
    fun testBuiltInBatchDeleteAllDeleted() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        dao.batchDeleteCriteriaHandler = { 2 } // both distinct ids deleted
        assertEquals(2, service.batchDelete(listOf(1, 2, 2))) // distinct -> 2
    }

    @Test
    fun testBuiltInBatchDeleteEmptyThrows() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        assertFailsWith<IllegalArgumentException> { service.batchDelete(emptyList()) }
    }

    @Test
    fun testBuiltInBatchDeletePartialNonBuiltInReturnsDeleted() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        dao.batchDeleteCriteriaHandler = { 1 } // only 1 of 2 deleted
        // the remaining row is not built-in -> just nonexistent; return actual deleted count
        dao.inSearchPropertiesByIdHandler = { listOf(mapOf("id" to 1L, "builtIn" to false)) }
        assertEquals(1, service.batchDelete(listOf(1, 2)))
    }

    @Test
    fun testBuiltInBatchDeletePartialWithBuiltInThrows() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        dao.batchDeleteCriteriaHandler = { 1 }
        dao.inSearchPropertiesByIdHandler = { listOf(mapOf("id" to 2L, "builtIn" to true)) }
        val ex = assertFailsWith<ServiceException> { service.batchDelete(listOf(1, 2)) }
        assertEquals(CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE, ex.errorCode)
    }

    // ============================================================
    // batchDeleteCriteria for built-in entity
    // ============================================================

    @Test
    fun testBuiltInBatchDeleteCriteriaEmptyThrows() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        assertFailsWith<IllegalArgumentException> { service.batchDeleteCriteria(Criteria()) }
    }

    @Test
    fun testBuiltInBatchDeleteCriteriaReturnsDeletedWhenPositive() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        dao.batchDeleteCriteriaHandler = { 3 }
        assertEquals(3, service.batchDeleteCriteria(Criteria("name", io.kudos.base.query.enums.OperatorEnum.EQ, "x")))
    }

    @Test
    fun testBuiltInBatchDeleteCriteriaZeroNoBuiltInReturnsZero() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        dao.batchDeleteCriteriaHandler = { 0 }
        dao.searchPropertiesHandler = { emptyList() } // probe finds no built-in rows
        assertEquals(0, service.batchDeleteCriteria(Criteria("name", io.kudos.base.query.enums.OperatorEnum.EQ, "x")))
    }

    @Test
    fun testBuiltInBatchDeleteCriteriaZeroWithBuiltInThrows() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        dao.batchDeleteCriteriaHandler = { 0 }
        dao.searchPropertiesHandler = { listOf(mapOf("builtIn" to true)) }
        val ex = assertFailsWith<ServiceException> {
            service.batchDeleteCriteria(Criteria("name", io.kudos.base.query.enums.OperatorEnum.EQ, "x"))
        }
        assertEquals(CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE, ex.errorCode)
    }

    // ============================================================
    // batchDeleteWhen for built-in entity
    // ============================================================

    @Test
    fun testBuiltInBatchDeleteWhenNonListPayloadThrows() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        val nonListPayload = object : io.kudos.base.model.payload.ISearchPayload {}
        assertFailsWith<IllegalArgumentException> { service.batchDeleteWhen(nonListPayload) }
    }

    @Test
    fun testBuiltInBatchDeleteWhenWithBuiltInRowThrows() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        dao.searchPayloadHandler = { listOf(mapOf<String, Any?>("id" to 1L, "builtIn" to true)) }
        val ex = assertFailsWith<ServiceException> { service.batchDeleteWhen(ListSearchPayload()) }
        assertEquals(CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE, ex.errorCode)
    }

    @Test
    fun testBuiltInBatchDeleteWhenNoMatchingIdsReturnsZero() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        dao.searchPayloadHandler = { emptyList<Map<String, Any?>>() } // no rows -> ids empty -> 0
        assertEquals(0, service.batchDeleteWhen(ListSearchPayload()))
    }

    @Test
    fun testBuiltInBatchDeleteWhenDeletesFilteredIds() {
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        // probe returns two non-built-in rows
        dao.searchPayloadHandler = {
            listOf(
                mapOf<String, Any?>("id" to 1L, "builtIn" to false),
                mapOf<String, Any?>("id" to 2L, "builtIn" to false)
            )
        }
        // the subsequent batchDeleteForBuiltInEntity deletes both
        dao.batchDeleteCriteriaHandler = { 2 }
        assertEquals(2, service.batchDeleteWhen(ListSearchPayload()))
    }

    @Test
    fun testToIdBuiltInProbePayloadCopiesPagingAndSetsReturnProps() {
        // Indirectly exercises toIdBuiltInProbePayload via batchDeleteWhen: assert the probe payload built
        // from the source preserves paging and restricts the returned columns.
        val dao = FakeCrudDao<Long, BuiltInPo>()
        val service = BuiltInService(dao)
        var captured: ListSearchPayload? = null
        dao.searchPayloadHandler = { p ->
            captured = p
            emptyList<Map<String, Any?>>()
        }
        val source = ListSearchPayload().apply { pageNo = 3; pageSize = 25 }
        service.batchDeleteWhen(source)
        val probe = captured as MutableListSearchPayload
        assertEquals(3, probe.pageNo)
        assertEquals(25, probe.pageSize)
        assertEquals(listOf("id", "builtIn"), probe.getReturnProperties())
    }
}
