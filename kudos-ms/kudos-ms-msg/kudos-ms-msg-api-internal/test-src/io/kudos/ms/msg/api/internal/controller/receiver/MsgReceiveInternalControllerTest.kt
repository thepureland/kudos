package io.kudos.ms.msg.api.internal.controller.receiver

import io.kudos.ms.msg.common.receiver.vo.MsgReceiveCacheEntry
import io.kudos.ms.msg.core.receiver.api.MsgReceiveApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * test for MsgReceiveInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveInternalControllerTest {

    private val api = mock(MsgReceiveApi::class.java)
    private val controller = MsgReceiveInternalController(api)

    @Test
    fun getReceivesByUserId_delegatesAndPreservesListIdentityAndOrder() {
        val list = listOf(mock(MsgReceiveCacheEntry::class.java), mock(MsgReceiveCacheEntry::class.java))
        whenCalled(api.getReceivesByUserId("u-1")).thenReturn(list)

        val result = controller.getReceivesByUserId("u-1")
        assertSame(list, result)
        assertEquals(2, result.size)
        verify(api).getReceivesByUserId("u-1")
    }

    @Test
    fun getReceivesByUserId_returnsEmptyListUnchanged() {
        whenCalled(api.getReceivesByUserId("u-none")).thenReturn(emptyList())

        assertTrue(controller.getReceivesByUserId("u-none").isEmpty())
        verify(api).getReceivesByUserId("u-none")
    }

    @Test
    fun getUnreadCountByUserId_delegatesNonZeroCount() {
        whenCalled(api.getUnreadCountByUserId("u-1")).thenReturn(7)

        assertEquals(7, controller.getUnreadCountByUserId("u-1"))
        verify(api).getUnreadCountByUserId("u-1")
    }

    @Test
    fun getUnreadCountByUserId_delegatesZeroCount() {
        whenCalled(api.getUnreadCountByUserId("u-empty")).thenReturn(0)

        assertEquals(0, controller.getUnreadCountByUserId("u-empty"))
        verify(api).getUnreadCountByUserId("u-empty")
    }

    @Test
    fun markRead_delegatesTrue() {
        whenCalled(api.markRead("r-1")).thenReturn(true)

        assertTrue(controller.markRead("r-1"))
        verify(api).markRead("r-1")
    }

    @Test
    fun markRead_delegatesFalse() {
        whenCalled(api.markRead("r-already-read")).thenReturn(false)

        assertFalse(controller.markRead("r-already-read"))
        verify(api).markRead("r-already-read")
    }

    @Test
    fun markAllReadByUserId_delegatesUpdatedCount() {
        whenCalled(api.markAllReadByUserId("u-1")).thenReturn(3)

        assertEquals(3, controller.markAllReadByUserId("u-1"))
        verify(api).markAllReadByUserId("u-1")
    }

    @Test
    fun markAllReadByUserId_delegatesZeroWhenNothingToUpdate() {
        whenCalled(api.markAllReadByUserId("u-clean")).thenReturn(0)

        assertEquals(0, controller.markAllReadByUserId("u-clean"))
        verify(api).markAllReadByUserId("u-clean")
    }
}
