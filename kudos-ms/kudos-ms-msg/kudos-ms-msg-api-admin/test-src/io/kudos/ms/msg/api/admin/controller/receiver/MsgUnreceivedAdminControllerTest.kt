package io.kudos.ms.msg.api.admin.controller.receiver

import io.kudos.ms.msg.core.receiver.model.po.MsgUnreceived
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgUnreceivedService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [MsgUnreceivedAdminController].
 *
 * The controller does not extend [io.kudos.ability.web.springmvc.controller.BaseCrudController]; it owns
 * three thin endpoints plus the private `MsgUnreceived.toRow()` PO -> VO mapper. The service is mocked
 * via Mockito (no Spring container / DB). Covered:
 * - listUnresolvedBySend: delegates, maps every field 1:1 through toRow(), preserves order, handles
 *   empty result and null nullable columns (failReason / lastRetryTime / updateTime).
 * - resolve / bumpRetry: forward the id and propagate both boolean outcomes.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgUnreceivedAdminControllerTest {

    private val service = mock(IMsgUnreceivedService::class.java)
    private val controller = MsgUnreceivedAdminController(service)

    private fun po(
        id: String = "x-1",
        receiverId: String = "u-1",
        sendId: String = "s-1",
        publishMethodDictCode: String = "201",
        failReason: String? = "boom",
        retryCount: Int = 0,
        lastRetryTime: LocalDateTime? = null,
        resolved: Boolean = false,
        createTime: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0),
        updateTime: LocalDateTime? = null,
        tenantId: String = "t-1",
    ): MsgUnreceived = MsgUnreceived().apply {
        this.id = id
        this.receiverId = receiverId
        this.sendId = sendId
        this.publishMethodDictCode = publishMethodDictCode
        this.failReason = failReason
        this.retryCount = retryCount
        this.lastRetryTime = lastRetryTime
        this.resolved = resolved
        this.createTime = createTime
        this.updateTime = updateTime
        this.tenantId = tenantId
    }

    @Test
    fun listUnresolvedBySend_mapsEveryFieldAndPreservesOrder() {
        val now = LocalDateTime.of(2026, 6, 15, 12, 30, 45)
        val create = LocalDateTime.of(2026, 6, 1, 9, 0)
        val update = LocalDateTime.of(2026, 6, 10, 10, 0)
        val first = po(
            id = "id-1", receiverId = "u-1", sendId = "s-9", publishMethodDictCode = "201",
            failReason = "NO_CONTACT", retryCount = 3, lastRetryTime = now, resolved = false,
            createTime = create, updateTime = update, tenantId = "t-7",
        )
        val second = po(
            id = "id-2", receiverId = "u-2", sendId = "s-9", publishMethodDictCode = "202",
            failReason = "渠道拒绝", retryCount = 0, lastRetryTime = null, resolved = false,
            createTime = create, updateTime = null, tenantId = "t-7",
        )
        whenCalled(service.findUnresolvedBySend("s-9")).thenReturn(listOf(first, second))

        val rows = controller.listUnresolvedBySend("s-9")

        assertEquals(2, rows.size)
        // order preserved
        assertEquals(listOf("id-1", "id-2"), rows.map { it.id })

        val r0 = rows[0]
        assertEquals("id-1", r0.id)
        assertEquals("u-1", r0.receiverId)
        assertEquals("s-9", r0.sendId)
        assertEquals("201", r0.publishMethodDictCode)
        assertEquals("NO_CONTACT", r0.failReason)
        assertEquals(3, r0.retryCount)
        assertEquals(now, r0.lastRetryTime)
        assertEquals(false, r0.resolved)
        assertEquals(create, r0.createTime)
        assertEquals(update, r0.updateTime)
        assertEquals("t-7", r0.tenantId)

        // Unicode failReason preserved; nullable columns mapped through as null
        val r1 = rows[1]
        assertEquals("渠道拒绝", r1.failReason)
        assertNull(r1.lastRetryTime)
        assertNull(r1.updateTime)
        assertEquals("202", r1.publishMethodDictCode)
        assertEquals(0, r1.retryCount)

        verify(service).findUnresolvedBySend("s-9")
    }

    @Test
    fun listUnresolvedBySend_nullFailReasonMappedThrough() {
        whenCalled(service.findUnresolvedBySend("s-1")).thenReturn(listOf(po(failReason = null)))
        val rows = controller.listUnresolvedBySend("s-1")
        assertEquals(1, rows.size)
        assertNull(rows.single().failReason)
    }

    @Test
    fun listUnresolvedBySend_emptyResult() {
        whenCalled(service.findUnresolvedBySend("none")).thenReturn(emptyList())
        assertTrue(controller.listUnresolvedBySend("none").isEmpty())
        verify(service).findUnresolvedBySend("none")
    }

    @Test
    fun resolve_delegatesBothOutcomes() {
        whenCalled(service.resolve("a")).thenReturn(true)
        whenCalled(service.resolve("b")).thenReturn(false)
        assertTrue(controller.resolve("a"))
        assertFalse(controller.resolve("b"))
        verify(service).resolve("a")
        verify(service).resolve("b")
    }

    @Test
    fun bumpRetry_delegatesBothOutcomes() {
        whenCalled(service.bumpRetry("a")).thenReturn(true)
        whenCalled(service.bumpRetry("b")).thenReturn(false)
        assertTrue(controller.bumpRetry("a"))
        assertFalse(controller.bumpRetry("b"))
        verify(service).bumpRetry("a")
        verify(service).bumpRetry("b")
    }
}
