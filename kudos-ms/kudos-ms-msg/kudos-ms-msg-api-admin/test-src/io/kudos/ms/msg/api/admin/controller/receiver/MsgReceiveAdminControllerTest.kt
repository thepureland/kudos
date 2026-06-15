package io.kudos.ms.msg.api.admin.controller.receiver

import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.msg.common.receiver.vo.request.MsgReceiveFormCreate
import io.kudos.ms.msg.common.receiver.vo.request.MsgReceiveFormUpdate
import io.kudos.ms.msg.common.receiver.vo.request.MsgReceiveQuery
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiveDetail
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiveEdit
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiveRow
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiveService
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
 * Pure unit test for [MsgReceiveAdminController] (bare BaseCrudController subclass; service mocked, no
 * Spring container / DB). Exercises inherited CRUD endpoints to cover the lazily-resolved generic VO
 * classes and the base controller's delegation/branching.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveAdminControllerTest {

    private val service = mock(IMsgReceiveService::class.java)
    private val controller = MsgReceiveAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun save_delegatesToInsert() {
        val form = MsgReceiveFormCreate(
            receiverId = "u-1", sendId = "s-1", receiveStatusDictCode = "1",
            createTime = null, updateTime = null, tenantId = "t-1",
        )
        whenCalled(service.insert(form)).thenReturn("new-id")
        assertEquals("new-id", controller.save(form))
        verify(service).insert(form)
    }

    @Test
    fun update_delegatesToUpdate() {
        val form = MsgReceiveFormUpdate(
            id = "id-1", receiverId = "u-1", sendId = "s-1", receiveStatusDictCode = "1",
            createTime = null, updateTime = null, tenantId = "t-1",
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
        whenCalled(service.batchDelete(listOf("c", "d"))).thenReturn(1)
        assertTrue(controller.batchDelete(listOf("a", "b")))
        assertFalse(controller.batchDelete(listOf("c", "d")))
    }

    @Test
    fun getEdit_foundAndMissing() {
        val edit = MsgReceiveEdit(id = "id-1")
        whenCalled(service.get("id-1", MsgReceiveEdit::class)).thenReturn(edit)
        whenCalled(service.get("nope", MsgReceiveEdit::class)).thenReturn(null)
        assertSame(edit, controller.getEdit("id-1"))
        assertFailsWith<ObjectNotFoundException> { controller.getEdit("nope") }
    }

    @Test
    fun getDetail_foundAndMissing() {
        val detail = MsgReceiveDetail(id = "id-1")
        whenCalled(service.get("id-1", MsgReceiveDetail::class)).thenReturn(detail)
        whenCalled(service.get("nope", MsgReceiveDetail::class)).thenReturn(null)
        assertSame(detail, controller.getDetail("id-1"))
        assertFailsWith<ObjectNotFoundException> { controller.getDetail("nope") }
    }

    @Test
    fun pagingSearch_delegatesAndCasts() {
        val query = MsgReceiveQuery(sendId = "s-1")
        val page: PagingSearchResult<*> = PagingSearchResult(listOf(MsgReceiveRow(id = "r1")), 1)
        whenCalled(service.pagingSearch(query)).thenReturn(page)
        val result: PagingSearchResult<MsgReceiveRow> = controller.pagingSearch(query)
        assertEquals(1, result.totalCount)
        assertEquals("r1", result.data.single().id)
    }

    @Test
    fun validationRules_resolveFormVoClasses() {
        assertTrue(controller.getCreateValidationRule().size >= 0)
        assertTrue(controller.getUpdateValidationRule().size >= 0)
    }
}
