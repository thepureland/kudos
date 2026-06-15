package io.kudos.ms.msg.core.receiver.api

import io.kudos.ms.msg.common.receiver.vo.MsgReceiveCacheEntry
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiveService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for MsgReceiveApi (pure delegation, service mocked with Mockito, no Spring container).
 *
 * The service dependency is a private `@Resource lateinit var`, so it is injected by reflection.
 *
 * Coverage: all 4 delegate methods forward arguments and return values unchanged
 * (getReceivesByUserId / getUnreadCountByUserId / markRead / markAllReadByUserId).
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveApiTest {

    private val service = mock(IMsgReceiveService::class.java)
    private val api = MsgReceiveApi().apply {
        val field = MsgReceiveApi::class.java.getDeclaredField("msgReceiveService")
        field.isAccessible = true
        field.set(this, service)
    }

    private fun entry(id: String) = MsgReceiveCacheEntry(
        id = id,
        receiverId = "u-$id",
        sendId = "s-$id",
        receiveStatusDictCode = "01",
        createTime = null,
        updateTime = null,
        tenantId = "t-1",
    )

    @Test
    fun getReceivesByUserId_delegates() {
        val list = listOf(entry("a"), entry("b"))
        whenCalled(service.getReceivesByUserId("u-1")).thenReturn(list)
        assertEquals(list, api.getReceivesByUserId("u-1"))
        verify(service).getReceivesByUserId("u-1")
    }

    @Test
    fun getReceivesByUserId_delegatesEmpty() {
        whenCalled(service.getReceivesByUserId("nobody")).thenReturn(emptyList())
        assertTrue(api.getReceivesByUserId("nobody").isEmpty())
    }

    @Test
    fun getUnreadCountByUserId_delegates() {
        whenCalled(service.getUnreadCountByUserId("u-1")).thenReturn(7)
        assertEquals(7, api.getUnreadCountByUserId("u-1"))
        verify(service).getUnreadCountByUserId("u-1")
    }

    @Test
    fun markRead_delegates() {
        whenCalled(service.markRead("r-1")).thenReturn(true)
        whenCalled(service.markRead("r-2")).thenReturn(false)
        assertTrue(api.markRead("r-1"))
        assertFalse(api.markRead("r-2"))
    }

    @Test
    fun markAllReadByUserId_delegates() {
        whenCalled(service.markAllReadByUserId("u-1")).thenReturn(3)
        assertEquals(3, api.markAllReadByUserId("u-1"))
        verify(service).markAllReadByUserId("u-1")
    }
}
