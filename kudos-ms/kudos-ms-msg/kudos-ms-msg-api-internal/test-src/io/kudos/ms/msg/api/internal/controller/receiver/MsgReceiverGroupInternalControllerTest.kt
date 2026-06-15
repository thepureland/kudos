package io.kudos.ms.msg.api.internal.controller.receiver

import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry
import io.kudos.ms.msg.core.receiver.api.MsgReceiverGroupApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * test for MsgReceiverGroupInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiverGroupInternalControllerTest {

    private val api = mock(MsgReceiverGroupApi::class.java)
    private val controller = MsgReceiverGroupInternalController(api)

    @Test
    fun getReceiverGroupById_delegatesAndReturnsSameInstance() {
        val entry = mock(MsgReceiverGroupCacheEntry::class.java)
        whenCalled(api.getReceiverGroupById("g-1")).thenReturn(entry)

        assertSame(entry, controller.getReceiverGroupById("g-1"))
        verify(api).getReceiverGroupById("g-1")
    }

    @Test
    fun getReceiverGroupById_returnsNullWhenApiReturnsNull() {
        whenCalled(api.getReceiverGroupById("missing")).thenReturn(null)

        assertNull(controller.getReceiverGroupById("missing"))
        verify(api).getReceiverGroupById("missing")
    }

    @Test
    fun listActiveReceiverGroups_delegatesWithTypeFilter() {
        val list = listOf(mock(MsgReceiverGroupCacheEntry::class.java))
        whenCalled(api.listActiveReceiverGroups("type-a")).thenReturn(list)

        assertSame(list, controller.listActiveReceiverGroups("type-a"))
        verify(api).listActiveReceiverGroups("type-a")
    }

    @Test
    fun listActiveReceiverGroups_delegatesNullTypeFilter() {
        val list = listOf(
            mock(MsgReceiverGroupCacheEntry::class.java),
            mock(MsgReceiverGroupCacheEntry::class.java),
        )
        whenCalled(api.listActiveReceiverGroups(null)).thenReturn(list)

        assertSame(list, controller.listActiveReceiverGroups(null))
        verify(api).listActiveReceiverGroups(null)
    }

    @Test
    fun listActiveReceiverGroups_returnsEmptyListUnchanged() {
        whenCalled(api.listActiveReceiverGroups("type-none")).thenReturn(emptyList())

        assertTrue(controller.listActiveReceiverGroups("type-none").isEmpty())
        verify(api).listActiveReceiverGroups("type-none")
    }
}
