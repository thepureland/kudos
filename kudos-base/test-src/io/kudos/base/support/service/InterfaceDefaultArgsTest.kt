package io.kudos.base.support.service

import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.model.payload.UpdatePayload
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.dao.IBaseCrudDao
import io.kudos.base.support.dao.IBaseReadOnlyDao
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.base.support.service.iservice.IBaseReadOnlyService
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Exercises the default-parameter synthetic bridges (`xxx$default`) declared on the read-only / CRUD DAO
 * and Service interfaces. These bridges only run when a caller invokes the method through the *interface*
 * type while omitting the defaulted argument; the concrete impl methods cannot redeclare those defaults,
 * so they would otherwise stay uncovered.
 *
 * @author K
 * @since 1.0.0
 */
internal class InterfaceDefaultArgsTest {

    private data class Po(override val id: Long, val name: String = "") : IIdEntity<Long>

    private class PoService(dao: FakeCrudDao<Long, Po>) :
        BaseCrudService<Long, Po, FakeCrudDao<Long, Po>>(dao)

    // ============================================================
    // DAO interface default-arg bridges
    // ============================================================

    @Test
    fun testReadOnlyDaoDefaultArgBridges() {
        val dao: IBaseReadOnlyDao<Long, Po> = FakeCrudDao<Long, Po>().apply {
            getResult = Po(1); getTypedResult = "t"; listResult = emptyList<Any?>(); countResult = 1; numberResult = 2
        }
        // omit countOfEachBatch
        dao.getByIds(listOf(1))
        dao.getByIds(listOf(1), String::class)
        // omit returnType
        dao.get(1)
        // omit criteria
        dao.search()
        dao.search(returnItemClass = String::class)
        dao.pagingSearch(pageNo = 1, pageSize = 10)
        dao.pagingSearch(returnItemClass = String::class, pageNo = 1, pageSize = 10)
        dao.count()
        dao.sum(Po::id)
        dao.avg(Po::id)
        dao.max(Po::id)
        dao.min(Po::id)
        assertEquals(1, dao.count())
    }

    @Test
    fun testCrudDaoDefaultArgBridges() {
        val dao: IBaseCrudDao<Long, Po> = FakeCrudDao<Long, Po>().apply {
            insertResult = 1L; intResult = 1
        }
        val c = Criteria("id", OperatorEnum.EQ, 1L)
        // omit countOfEachBatch in every batch method
        dao.batchInsert(listOf(Po(1)))
        dao.batchInsertOnly(listOf(Po(1)))
        dao.batchInsertExclude(listOf(Po(1)))
        dao.batchUpdate(listOf(Po(1)))
        dao.batchUpdateWhen(listOf(Po(1)), c)
        dao.batchUpdateOnly(listOf(Po(1)))
        dao.batchUpdateOnlyWhen(listOf(Po(1)), c)
        dao.batchUpdateExcludeProperties(listOf(Po(1)))
        dao.batchUpdateExcludePropertiesWhen(listOf(Po(1)), c)
        assertEquals(1, dao.batchInsert(listOf(Po(1))))
    }

    // ============================================================
    // Service interface default-arg bridges (via interface-typed reference)
    // ============================================================

    @Test
    fun testReadOnlyServiceDefaultArgBridges() {
        val dao = FakeCrudDao<Long, Po>().apply {
            getTypedResult = "t"; listResult = emptyList<Any?>(); countResult = 1; numberResult = 2
        }
        val service: IBaseReadOnlyService<Long, Po> = PoService(dao)
        service.getByIds(listOf(1))
        service.search()
        service.search(returnItemClass = String::class)
        service.pagingSearch(pageNo = 1, pageSize = 10)
        service.pagingSearch(returnItemClass = String::class, pageNo = 1, pageSize = 10)
        service.count()
        service.sum(Po::id)
        service.avg(Po::id)
        service.max(Po::id)
        service.min(Po::id)
        assertEquals(1, service.count())
    }

    @Test
    fun testCrudServiceDefaultArgBridges() {
        val dao = FakeCrudDao<Long, Po>().apply { insertResult = 1L; intResult = 1 }
        val service: IBaseCrudService<Long, Po> = PoService(dao)
        val c = Criteria("id", OperatorEnum.EQ, 1L)
        service.batchInsert(listOf(Po(1)))
        service.batchInsertOnly(listOf(Po(1)))
        service.batchInsertExclude(listOf(Po(1)))
        service.batchUpdate(listOf(Po(1)))
        service.batchUpdateWhen(listOf(Po(1)), c)
        service.batchUpdateOnly(listOf(Po(1)))
        service.batchUpdateOnlyWhen(listOf(Po(1)), c)
        service.batchUpdateExcludeProperties(listOf(Po(1)))
        service.batchUpdateExcludePropertiesWhen(listOf(Po(1)), c)
        // batchUpdateWhen(updatePayload) has no defaulted args but include for completeness
        service.batchUpdateWhen(UpdatePayload<ListSearchPayload>())
        assertEquals(1, service.batchInsert(listOf(Po(1))))
    }
}
