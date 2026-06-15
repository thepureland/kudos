package io.kudos.ms.sys.api.internal.controller.outline

import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import io.kudos.ms.sys.core.outline.api.SysOutLineApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysOutLineInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - listOutLines delegates with a non-null tenantId and returns the same list instance
 * - listOutLines delegates with a null tenantId
 * - listOutLines propagates an empty result
 *
 * @author K
 * @since 1.0.0
 */
internal class SysOutLineInternalControllerTest {

    private val api = mock(SysOutLineApi::class.java)
    private val controller = SysOutLineInternalController(api)

    @Test
    fun listOutLines_delegates_withTenantId() {
        val lines = listOf(mock(SysOutLineCacheEntry::class.java))
        whenCalled(api.listOutLines("sys-a", "tenant-1")).thenReturn(lines)

        assertSame(lines, controller.listOutLines("sys-a", "tenant-1"))
        verify(api).listOutLines("sys-a", "tenant-1")
    }

    @Test
    fun listOutLines_delegates_withNullTenantId() {
        val lines = listOf(mock(SysOutLineCacheEntry::class.java), mock(SysOutLineCacheEntry::class.java))
        whenCalled(api.listOutLines("sys-b", null)).thenReturn(lines)

        assertSame(lines, controller.listOutLines("sys-b", null))
        verify(api).listOutLines("sys-b", null)
    }

    @Test
    fun listOutLines_delegates_emptyResult() {
        whenCalled(api.listOutLines("sys-empty", null)).thenReturn(emptyList())

        assertTrue(controller.listOutLines("sys-empty", null).isEmpty())
        verify(api).listOutLines("sys-empty", null)
    }
}
