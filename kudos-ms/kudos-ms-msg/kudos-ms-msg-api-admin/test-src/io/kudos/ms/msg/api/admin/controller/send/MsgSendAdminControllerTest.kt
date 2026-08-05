package io.kudos.ms.msg.api.admin.controller.send

import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.msg.common.send.vo.request.MsgSendFormCreate
import io.kudos.ms.msg.common.send.vo.request.MsgSendFormUpdate
import io.kudos.ms.msg.common.send.vo.request.MsgSendQuery
import io.kudos.ms.msg.common.send.vo.response.MsgSendDetail
import io.kudos.ms.msg.common.send.vo.response.MsgSendEdit
import io.kudos.ms.msg.common.send.vo.response.MsgSendRow
import io.kudos.ms.msg.core.send.service.iservice.IMsgSendService
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
 * Pure unit test for [MsgSendAdminController] (bare BaseCrudController subclass; service mocked, no Spring
 * container / DB). Exercises inherited CRUD endpoints to cover the lazily-resolved generic VO classes and
 * the base controller's delegation/branching.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendAdminControllerTest {

    private val service = mock(IMsgSendService::class.java)
    private val controller = MsgSendAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    private fun create() = MsgSendFormCreate(
        receiverGroupTypeDictCode = "1", receiverGroupId = "g-1", instanceId = "i-1",
        msgTypeDictCode = "1", localeDictCode = "zh", sendStatusDictCode = "0",
        createTime = null, updateTime = null, successCount = 0, failCount = 0,
        jobId = null, tenantId = "t-1",
    )

    private fun update() = MsgSendFormUpdate(
        id = "id-1", receiverGroupTypeDictCode = "1", receiverGroupId = "g-1", instanceId = "i-1",
        msgTypeDictCode = "1", localeDictCode = "zh", sendStatusDictCode = "1",
        createTime = null, updateTime = null, successCount = 5, failCount = 1,
        jobId = "job", tenantId = "t-1",
    )

    @Test
    fun save_delegatesToInsert() {
        val form = create()
        whenCalled(service.insert(form)).thenReturn("new-id")
        assertEquals("new-id", controller.save(form))
        verify(service).insert(form)
    }

    @Test
    fun update_delegatesToUpdate() {
        val form = update()
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
        val edit = MsgSendEdit(id = "id-1")
        whenCalled(service.get("id-1", MsgSendEdit::class)).thenReturn(edit)
        whenCalled(service.get("nope", MsgSendEdit::class)).thenReturn(null)
        assertSame(edit, controller.getEdit("id-1"))
        assertFailsWith<ObjectNotFoundException> { controller.getEdit("nope") }
    }

    @Test
    fun getDetail_foundAndMissing() {
        val detail = MsgSendDetail(id = "id-1")
        whenCalled(service.get("id-1", MsgSendDetail::class)).thenReturn(detail)
        whenCalled(service.get("nope", MsgSendDetail::class)).thenReturn(null)
        assertSame(detail, controller.getDetail("id-1"))
        assertFailsWith<ObjectNotFoundException> { controller.getDetail("nope") }
    }

    @Test
    fun pagingSearch_delegatesAndCasts() {
        val query = MsgSendQuery(instanceId = "i-1")
        val page: PagingSearchResult<*> = PagingSearchResult(listOf(MsgSendRow(id = "r1")), 1)
        whenCalled(service.pagingSearch(query)).thenReturn(page)
        val result: PagingSearchResult<MsgSendRow> = controller.pagingSearch(query)
        assertEquals(1, result.totalCount)
        assertEquals("r1", result.data.single().id)
    }

    @Test
    fun validationRules_resolveFormVoClasses() {
        assertTrue(controller.getCreateValidationRule().size >= 0)
        assertTrue(controller.getUpdateValidationRule().size >= 0)
    }
}
