package io.kudos.ability.web.springmvc.controller

import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.support.service.iservice.IBaseCrudService
import jakarta.validation.constraints.NotBlank
import org.mockito.Mockito
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [BaseController] / [BaseReadOnlyController] / [BaseCrudController].
 *
 * Covers, via a concrete subclass with all 8 generic parameters bound and a Mockito-mocked
 * service:
 *  - pagingSearch delegation and result cast
 *  - getDetail / getEdit found and not-found (ObjectNotFoundException with message) paths
 *  - getCreateValidationRule / getUpdateValidationRule lazy generic resolution
 *  - save / update / delete delegation
 *  - batchDelete true (all deleted) / false (partial) / empty-list edge cases
 *
 * @author K
 * @since 1.0.0
 */
internal class BaseCrudControllerTest {

    private val service: TestCrudService = Mockito.mock(TestCrudService::class.java)

    private val controller = TestCrudController().apply { attachService(service) }

    @Test
    fun pagingSearch_delegatesToService() {
        val payload = TestSearchPayload()
        val expected = PagingSearchResult(listOf(TestRecord("1", "kudos")), 1)
        Mockito.`when`(service.pagingSearch(payload)).thenReturn(expected)

        val result = controller.pagingSearch(payload)

        assertEquals(1, result.totalCount)
        assertEquals(TestRecord("1", "kudos"), result.data.single())
    }

    @Test
    fun pagingSearch_emptyResult() {
        val payload = TestSearchPayload()
        Mockito.`when`(service.pagingSearch(payload)).thenReturn(PagingSearchResult(emptyList<TestRecord>(), 0))

        val result = controller.pagingSearch(payload)

        assertEquals(0, result.totalCount)
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun getDetail_found() {
        val detail = TestDetail("1")
        Mockito.`when`(service.get("1", TestDetail::class)).thenReturn(detail)

        assertSame(detail, controller.getDetail("1"))
    }

    @Test
    fun getDetail_notFound_throwsObjectNotFoundException() {
        Mockito.`when`(service.get("missing", TestDetail::class)).thenReturn(null)

        val ex = assertFailsWith<ObjectNotFoundException> { controller.getDetail("missing") }
        assertEquals("Record not found!", ex.message)
    }

    @Test
    fun getEdit_found() {
        val edit = TestEdit("1")
        Mockito.`when`(service.get("1", TestEdit::class)).thenReturn(edit)

        assertSame(edit, controller.getEdit("1"))
    }

    @Test
    fun getEdit_notFound_throwsObjectNotFoundException() {
        Mockito.`when`(service.get("missing", TestEdit::class)).thenReturn(null)

        val ex = assertFailsWith<ObjectNotFoundException> { controller.getEdit("missing") }
        assertEquals("Record not found!", ex.message)
    }

    @Test
    fun getCreateValidationRule_resolvesCreateFormGeneric() {
        val rules = controller.getCreateValidationRule()
        assertTrue(rules.containsKey("name"), "Create form NotBlank rule should be exported, got: $rules")
    }

    @Test
    fun getUpdateValidationRule_resolvesUpdateFormGeneric() {
        val rules = controller.getUpdateValidationRule()
        assertTrue(rules.containsKey("remark"), "Update form NotBlank rule should be exported, got: $rules")
    }

    @Test
    fun save_returnsPrimaryKeyFromService() {
        val form = TestCreateForm()
        Mockito.`when`(service.insert(form)).thenReturn("new-id")

        assertEquals("new-id", controller.save(form))
    }

    @Test
    fun update_delegatesToService() {
        val form = TestUpdateForm()
        Mockito.`when`(service.update(form)).thenReturn(true)

        controller.update(form)

        Mockito.verify(service).update(form)
    }

    @Test
    fun delete_delegatesToService() {
        Mockito.`when`(service.deleteById("1")).thenReturn(true)
        assertTrue(controller.delete("1"))

        Mockito.`when`(service.deleteById("2")).thenReturn(false)
        assertFalse(controller.delete("2"))
    }

    @Test
    fun batchDelete_trueWhenAllDeleted() {
        val ids = listOf("1", "2")
        Mockito.`when`(service.batchDelete(ids)).thenReturn(2)

        assertTrue(controller.batchDelete(ids))
    }

    @Test
    fun batchDelete_falseWhenPartiallyDeleted() {
        val ids = listOf("1", "2", "3")
        Mockito.`when`(service.batchDelete(ids)).thenReturn(2)

        assertFalse(controller.batchDelete(ids))
    }

    @Test
    fun batchDelete_emptyList_trueWhenServiceReturnsZero() {
        val ids = emptyList<String>()
        Mockito.`when`(service.batchDelete(ids)).thenReturn(0)

        assertTrue(controller.batchDelete(ids))
    }

}

/** Test entity. */
internal data class TestEntity(override val id: String) : IIdEntity<String>

/** Test CRUD service interface (mocked by Mockito). */
internal interface TestCrudService : IBaseCrudService<String, TestEntity>

/** Test list search payload. */
internal class TestSearchPayload : ListSearchPayload()

/** Test list row VO. */
internal data class TestRecord(val id: String? = null, val name: String? = null)

/** Test detail VO. */
internal data class TestDetail(val id: String? = null)

/** Test edit VO. */
internal data class TestEdit(val id: String? = null)

/** Test create-form VO. */
internal class TestCreateForm {
    @get:NotBlank
    var name: String? = null
}

/** Test update-form VO. */
internal class TestUpdateForm {
    @get:NotBlank
    var remark: String? = null
}

/** Concrete controller binding all generic parameters, exposing service injection for tests. */
internal class TestCrudController : BaseCrudController<
        String,
        TestCrudService,
        TestSearchPayload,
        TestRecord,
        TestDetail,
        TestEdit,
        TestCreateForm,
        TestUpdateForm>() {

    fun attachService(svc: TestCrudService) {
        service = svc
    }

}
