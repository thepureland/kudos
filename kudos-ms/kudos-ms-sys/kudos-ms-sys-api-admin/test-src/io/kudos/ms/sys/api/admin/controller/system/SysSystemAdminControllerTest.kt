package io.kudos.ms.sys.api.admin.controller.system

import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.core.system.service.iservice.ISysSystemService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [SysSystemAdminController]: active sub-system / system code listings, the full
 * tree, and updateActive. The service is mocked and injected via reflection; no Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysSystemAdminControllerTest {

    private val service = mock(ISysSystemService::class.java)
    private val controller = SysSystemAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun getAllActiveSubSystemCodesDelegatesAndPreservesOrder() {
        val codes = listOf("s1", "s2")
        whenCalled(service.getActiveSubSystemCodes()).thenReturn(codes)
        val result = controller.getAllActiveSubSystemCodes()
        assertSame(codes, result)
        assertEquals(listOf("s1", "s2"), result)
        verify(service).getActiveSubSystemCodes()
    }

    @Test
    fun getAllActiveSystemCodesDelegates() {
        val codes = listOf("sys1")
        whenCalled(service.getActiveSystemCodes()).thenReturn(codes)
        assertSame(codes, controller.getAllActiveSystemCodes())
        verify(service).getActiveSystemCodes()
    }

    @Test
    fun getFullSystemTreeDelegates() {
        @Suppress("UNCHECKED_CAST")
        val node = mock(IdAndNameTreeNode::class.java) as IdAndNameTreeNode<String>
        val tree = listOf(node)
        whenCalled(service.getFullSystemTree()).thenReturn(tree)
        assertSame(tree, controller.getFullSystemTree())
        verify(service).getFullSystemTree()
    }

    @Test
    fun updateActiveDelegatesBothOutcomes() {
        whenCalled(service.updateActive("s1", true)).thenReturn(true)
        whenCalled(service.updateActive("s2", false)).thenReturn(false)
        assertTrue(controller.updateActive("s1", true))
        assertFalse(controller.updateActive("s2", false))
        verify(service).updateActive("s1", true)
        verify(service).updateActive("s2", false)
    }
}
