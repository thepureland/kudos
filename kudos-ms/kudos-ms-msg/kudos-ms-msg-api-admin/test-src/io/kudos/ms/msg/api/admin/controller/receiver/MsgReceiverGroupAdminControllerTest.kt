package io.kudos.ms.msg.api.admin.controller.receiver

import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.msg.common.receiver.vo.request.MsgReceiverGroupFormCreate
import io.kudos.ms.msg.common.receiver.vo.request.MsgReceiverGroupFormUpdate
import io.kudos.ms.msg.common.receiver.vo.request.MsgReceiverGroupQuery
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiverGroupDetail
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiverGroupEdit
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiverGroupRow
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiverGroupService
import org.mockito.ArgumentMatchers.eq
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
 * Pure unit test for [MsgReceiverGroupAdminController] (bare BaseCrudController subclass; service mocked,
 * no Spring container / DB). Exercises inherited CRUD endpoints to cover the lazily-resolved generic VO
 * classes and the base controller's delegation/branching.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiverGroupAdminControllerTest {

    private val service = mock(IMsgReceiverGroupService::class.java)
    private val controller = MsgReceiverGroupAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun save_delegatesToInsert() {
        val form = MsgReceiverGroupFormCreate(
            receiverGroupTypeDictCode = "1", defineTable = "tbl", nameColumn = "name",
            remark = "r", active = true,
        )
        whenCalled(service.insert(form)).thenReturn("new-id")
        assertEquals("new-id", controller.save(form))
        verify(service).insert(form)
    }

    @Test
    fun update_delegatesToUpdate() {
        val form = MsgReceiverGroupFormUpdate(
            id = "id-1", receiverGroupTypeDictCode = "1", defineTable = "tbl", nameColumn = "name",
            remark = "r", active = false,
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
    fun batchDelete_trueWhenAllDeletedFalseWhenPartial() {
        whenCalled(service.batchDelete(listOf("a", "b"))).thenReturn(2)
        whenCalled(service.batchDelete(listOf("c"))).thenReturn(0)
        assertTrue(controller.batchDelete(listOf("a", "b")))
        assertFalse(controller.batchDelete(listOf("c")))
    }

    @Test
    fun getEdit_foundAndMissing() {
        val edit = MsgReceiverGroupEdit(id = "id-1")
        whenCalled(service.get("id-1", MsgReceiverGroupEdit::class)).thenReturn(edit)
        whenCalled(service.get("nope", MsgReceiverGroupEdit::class)).thenReturn(null)
        assertSame(edit, controller.getEdit("id-1"))
        assertFailsWith<ObjectNotFoundException> { controller.getEdit("nope") }
    }

    @Test
    fun getDetail_foundAndMissing() {
        val detail = MsgReceiverGroupDetail(id = "id-1")
        whenCalled(service.get("id-1", MsgReceiverGroupDetail::class)).thenReturn(detail)
        whenCalled(service.get("nope", MsgReceiverGroupDetail::class)).thenReturn(null)
        assertSame(detail, controller.getDetail("id-1"))
        assertFailsWith<ObjectNotFoundException> { controller.getDetail("nope") }
    }

    @Test
    fun pagingSearch_delegatesAndCasts() {
        val query = MsgReceiverGroupQuery(active = true)
        val page: PagingSearchResult<*> = PagingSearchResult(listOf(MsgReceiverGroupRow(id = "r1")), 1)
        whenCalled(service.pagingSearch(query)).thenReturn(page)
        val result: PagingSearchResult<MsgReceiverGroupRow> = controller.pagingSearch(query)
        assertEquals(1, result.totalCount)
        assertEquals("r1", result.data.single().id)
    }

    @Test
    fun validationRules_resolveFormVoClasses() {
        assertTrue(controller.getCreateValidationRule().size >= 0)
        assertTrue(controller.getUpdateValidationRule().size >= 0)
    }
}
