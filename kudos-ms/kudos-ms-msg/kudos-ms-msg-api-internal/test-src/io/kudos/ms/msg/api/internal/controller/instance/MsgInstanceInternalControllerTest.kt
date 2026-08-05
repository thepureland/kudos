package io.kudos.ms.msg.api.internal.controller.instance

import io.kudos.ms.msg.common.instance.vo.MsgInstanceCacheEntry
import io.kudos.ms.msg.core.instance.api.MsgInstanceApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.mockito.Mockito.`when` as whenCalled

/**
 * test for MsgInstanceInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgInstanceInternalControllerTest {

    private val api = mock(MsgInstanceApi::class.java)
    private val controller = MsgInstanceInternalController(api)

    @Test
    fun getInstanceById_delegatesAndReturnsSameInstance() {
        val entry = mock(MsgInstanceCacheEntry::class.java)
        whenCalled(api.getInstanceById("id-1")).thenReturn(entry)

        assertSame(entry, controller.getInstanceById("id-1"))
        verify(api).getInstanceById("id-1")
        verifyNoMoreInteractions(api)
    }

    @Test
    fun getInstanceById_returnsNullWhenApiReturnsNull() {
        whenCalled(api.getInstanceById("missing")).thenReturn(null)

        assertNull(controller.getInstanceById("missing"))
        verify(api).getInstanceById("missing")
    }

    @Test
    fun getInstanceById_passesArgumentThroughVerbatim() {
        val weird = "  Ünïcödé/特殊\t值  "
        val entry = mock(MsgInstanceCacheEntry::class.java)
        whenCalled(api.getInstanceById(weird)).thenReturn(entry)

        assertSame(entry, controller.getInstanceById(weird))
        verify(api).getInstanceById(weird)
    }
}
