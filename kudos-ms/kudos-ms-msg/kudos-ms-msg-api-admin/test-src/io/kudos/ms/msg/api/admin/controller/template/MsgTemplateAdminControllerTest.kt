package io.kudos.ms.msg.api.admin.controller.template

import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.msg.common.template.vo.request.MsgTemplateFormCreate
import io.kudos.ms.msg.common.template.vo.request.MsgTemplateFormUpdate
import io.kudos.ms.msg.common.template.vo.request.MsgTemplateQuery
import io.kudos.ms.msg.common.template.vo.response.MsgTemplateDetail
import io.kudos.ms.msg.common.template.vo.response.MsgTemplateEdit
import io.kudos.ms.msg.common.template.vo.response.MsgTemplateRow
import io.kudos.ms.msg.core.template.service.iservice.IMsgTemplateService
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
 * Pure unit test for [MsgTemplateAdminController].
 *
 * The four mutation endpoints (save / update / delete / batchDelete) are overridden only to attach
 * [io.kudos.ability.log.audit.common.annotation.WebAudit]; each delegates straight back to super, so the
 * tests assert behavior is unchanged versus the base controller. Read endpoints (getEdit / getDetail /
 * pagingSearch / validation rules) are inherited. The service is mocked (no Spring container / DB) and
 * injected via reflection; the @WebAudit aspect is not woven in a plain unit test, so the overrides run
 * as straight delegation.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgTemplateAdminControllerTest {

    private val service = mock(IMsgTemplateService::class.java)
    private val controller = MsgTemplateAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    private fun create() = MsgTemplateFormCreate(
        sendTypeDictCode = "1", eventTypeDictCode = "2", msgTypeDictCode = "3",
        receiverGroupCode = "g", localeDictCode = "zh", title = "标题", content = "内容",
        defaultActive = true, defaultTitle = "dt", defaultContent = "dc", tenantId = "t-1",
    )

    private fun update() = MsgTemplateFormUpdate(
        id = "id-1", sendTypeDictCode = "1", eventTypeDictCode = "2", msgTypeDictCode = "3",
        receiverGroupCode = "g", localeDictCode = "zh", title = "标题", content = "内容",
        defaultActive = false, defaultTitle = "dt", defaultContent = "dc", tenantId = "t-1",
    )

    @Test
    fun save_overrideDelegatesToSuperThenInsert() {
        val form = create()
        whenCalled(service.insert(form)).thenReturn("new-id")
        assertEquals("new-id", controller.save(form))
        verify(service).insert(form)
    }

    @Test
    fun update_overrideDelegatesToSuperThenUpdate() {
        val form = update()
        whenCalled(service.update(form)).thenReturn(true)
        controller.update(form)
        verify(service).update(form)
    }

    @Test
    fun delete_overrideDelegatesBothOutcomes() {
        whenCalled(service.deleteById("a")).thenReturn(true)
        whenCalled(service.deleteById("b")).thenReturn(false)
        assertTrue(controller.delete("a"))
        assertFalse(controller.delete("b"))
        verify(service).deleteById("a")
        verify(service).deleteById("b")
    }

    @Test
    fun batchDelete_overrideTrueWhenAllDeleted() {
        val ids = listOf("a", "b", "c")
        whenCalled(service.batchDelete(ids)).thenReturn(3)
        assertTrue(controller.batchDelete(ids))
        verify(service).batchDelete(ids)
    }

    @Test
    fun batchDelete_overrideFalseWhenPartial() {
        val ids = listOf("a", "b", "c")
        whenCalled(service.batchDelete(ids)).thenReturn(2)
        assertFalse(controller.batchDelete(ids))
    }

    @Test
    fun batchDelete_overrideEmptyListReturnsTrue() {
        whenCalled(service.batchDelete(emptyList())).thenReturn(0)
        assertTrue(controller.batchDelete(emptyList()))
    }

    @Test
    fun getEdit_foundAndMissing() {
        val edit = MsgTemplateEdit(id = "id-1")
        whenCalled(service.get("id-1", MsgTemplateEdit::class)).thenReturn(edit)
        whenCalled(service.get("nope", MsgTemplateEdit::class)).thenReturn(null)
        assertSame(edit, controller.getEdit("id-1"))
        assertFailsWith<ObjectNotFoundException> { controller.getEdit("nope") }
    }

    @Test
    fun getDetail_foundAndMissing() {
        val detail = MsgTemplateDetail(id = "id-1")
        whenCalled(service.get("id-1", MsgTemplateDetail::class)).thenReturn(detail)
        whenCalled(service.get("nope", MsgTemplateDetail::class)).thenReturn(null)
        assertSame(detail, controller.getDetail("id-1"))
        assertFailsWith<ObjectNotFoundException> { controller.getDetail("nope") }
    }

    @Test
    fun pagingSearch_delegatesAndCasts() {
        val query = MsgTemplateQuery(title = "标题")
        val page: PagingSearchResult<*> = PagingSearchResult(listOf(MsgTemplateRow(id = "r1")), 1)
        whenCalled(service.pagingSearch(query)).thenReturn(page)
        val result: PagingSearchResult<MsgTemplateRow> = controller.pagingSearch(query)
        assertEquals(1, result.totalCount)
        assertEquals("r1", result.data.single().id)
    }

    @Test
    fun validationRules_resolveFormVoClasses() {
        assertTrue(controller.getCreateValidationRule().size >= 0)
        assertTrue(controller.getUpdateValidationRule().size >= 0)
    }
}
