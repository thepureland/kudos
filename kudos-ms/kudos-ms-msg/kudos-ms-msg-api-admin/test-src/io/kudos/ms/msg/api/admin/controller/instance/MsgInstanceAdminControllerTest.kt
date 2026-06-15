package io.kudos.ms.msg.api.admin.controller.instance

import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.msg.common.instance.vo.request.MsgInstanceFormCreate
import io.kudos.ms.msg.common.instance.vo.request.MsgInstanceFormUpdate
import io.kudos.ms.msg.common.instance.vo.request.MsgInstanceQuery
import io.kudos.ms.msg.common.instance.vo.response.MsgInstanceDetail
import io.kudos.ms.msg.common.instance.vo.response.MsgInstanceEdit
import io.kudos.ms.msg.common.instance.vo.response.MsgInstanceRow
import io.kudos.ms.msg.core.instance.service.iservice.IMsgInstanceService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [MsgInstanceAdminController].
 *
 * The controller is a bare [io.kudos.ability.web.springmvc.controller.BaseCrudController] subclass with
 * no own logic, so this exercises the inherited endpoints against a mocked service (no Spring / DB) to
 * cover the lazily-resolved generic VO classes (detail / edit / create-form / update-form) and the
 * delegation/branching in the base controller. The service is injected via reflection.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgInstanceAdminControllerTest {

    private val service = mock(IMsgInstanceService::class.java)
    private val controller = MsgInstanceAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun save_delegatesToInsert() {
        val form = MsgInstanceFormCreate(
            localeDictCode = "zh", title = "t", content = "c", templateId = "tpl",
            sendTypeDictCode = "1", eventTypeDictCode = "2", msgTypeDictCode = "3",
            validTimeStart = null, validTimeEnd = null, tenantId = "t-1",
        )
        whenCalled(service.insert(form)).thenReturn("new-id")
        assertEquals("new-id", controller.save(form))
        verify(service).insert(form)
    }

    @Test
    fun update_delegatesToUpdate() {
        val form = MsgInstanceFormUpdate(
            id = "id-1", localeDictCode = "zh", title = "t", content = "c", templateId = "tpl",
            sendTypeDictCode = "1", eventTypeDictCode = "2", msgTypeDictCode = "3",
            validTimeStart = null, validTimeEnd = null, tenantId = "t-1",
        )
        whenCalled(service.update(form)).thenReturn(true)
        controller.update(form)
        verify(service).update(form)
    }

    @Test
    fun delete_delegatesBothOutcomes() {
        whenCalled(service.deleteById("a")).thenReturn(true)
        whenCalled(service.deleteById("b")).thenReturn(false)
        assertTrue(controller.delete("a"))
        assertFalse(controller.delete("b"))
    }

    @Test
    fun batchDelete_trueWhenAllDeleted() {
        val ids = listOf("a", "b", "c")
        whenCalled(service.batchDelete(ids)).thenReturn(3)
        assertTrue(controller.batchDelete(ids))
        verify(service).batchDelete(ids)
    }

    @Test
    fun batchDelete_falseWhenPartialDelete() {
        val ids = listOf("a", "b", "c")
        whenCalled(service.batchDelete(ids)).thenReturn(2)
        assertFalse(controller.batchDelete(ids))
    }

    @Test
    fun batchDelete_emptyListReturnsTrue() {
        whenCalled(service.batchDelete(emptyList())).thenReturn(0)
        assertTrue(controller.batchDelete(emptyList()))
    }

    @Test
    fun getEdit_returnsEditVoWhenFound() {
        val edit = MsgInstanceEdit(id = "id-1", title = "T")
        whenCalled(service.get("id-1", MsgInstanceEdit::class)).thenReturn(edit)
        assertSame(edit, controller.getEdit("id-1"))
    }

    @Test
    fun getEdit_throwsWhenMissing() {
        whenCalled(service.get("nope", MsgInstanceEdit::class)).thenReturn(null)
        val ex = assertFailsWith<ObjectNotFoundException> { controller.getEdit("nope") }
        assertEquals("Record not found!", ex.message)
    }

    @Test
    fun getDetail_returnsDetailVoWhenFound() {
        val detail = MsgInstanceDetail(id = "id-1", title = "T")
        whenCalled(service.get("id-1", MsgInstanceDetail::class)).thenReturn(detail)
        assertSame(detail, controller.getDetail("id-1"))
    }

    @Test
    fun getDetail_throwsWhenMissing() {
        whenCalled(service.get("nope", MsgInstanceDetail::class)).thenReturn(null)
        assertFailsWith<ObjectNotFoundException> { controller.getDetail("nope") }
    }

    @Test
    fun pagingSearch_delegatesAndCasts() {
        val query = MsgInstanceQuery(title = "t")
        val page: PagingSearchResult<*> = PagingSearchResult(
            data = listOf(MsgInstanceRow(id = "r1")),
            totalCount = 1,
        )
        whenCalled(service.pagingSearch(query)).thenReturn(page)

        val result: PagingSearchResult<MsgInstanceRow> = controller.pagingSearch(query)
        assertEquals(1, result.totalCount)
        assertEquals("r1", result.data.single().id)
        verify(service).pagingSearch(query)
    }

    @Test
    fun getCreateValidationRule_resolvesCreateFormVoClass() {
        // Reaching a Map (size >= 0) proves the lazy generic resolution (index 6) ran without throwing.
        assertTrue(controller.getCreateValidationRule().size >= 0)
    }

    @Test
    fun getUpdateValidationRule_resolvesUpdateFormVoClass() {
        assertTrue(controller.getUpdateValidationRule().size >= 0)
    }
}
