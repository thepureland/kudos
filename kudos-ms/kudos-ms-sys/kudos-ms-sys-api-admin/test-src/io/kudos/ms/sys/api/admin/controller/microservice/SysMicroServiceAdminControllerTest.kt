package io.kudos.ms.sys.api.admin.controller.microservice

import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.core.microservice.service.iservice.ISysMicroServiceService
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
 * Pure unit test for [SysMicroServiceAdminController]: active-code listings, the full tree, and
 * updateActive. The service is mocked and injected via reflection; no Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysMicroServiceAdminControllerTest {

    private val service = mock(ISysMicroServiceService::class.java)
    private val controller = SysMicroServiceAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun getAllActiveAtomicServiceCodesDelegatesAndPreservesOrder() {
        val codes = listOf("a", "b", "c")
        whenCalled(service.getActiveAtomicServiceCodes()).thenReturn(codes)
        val result = controller.getAllActiveAtomicServiceCodes()
        assertSame(codes, result)
        assertEquals(listOf("a", "b", "c"), result)
        verify(service).getActiveAtomicServiceCodes()
    }

    @Test
    fun getAllActiveMicroServiceCodesDelegates() {
        val codes = listOf("m1", "m2")
        whenCalled(service.getActiveMicroServiceCodes()).thenReturn(codes)
        assertSame(codes, controller.getAllActiveMicroServiceCodes())
        verify(service).getActiveMicroServiceCodes()
    }

    @Test
    fun getFullMicroServiceTreeDelegates() {
        @Suppress("UNCHECKED_CAST")
        val node = mock(IdAndNameTreeNode::class.java) as IdAndNameTreeNode<String>
        val tree = listOf(node)
        whenCalled(service.getFullMicroServiceTree()).thenReturn(tree)
        assertSame(tree, controller.getFullMicroServiceTree())
        verify(service).getFullMicroServiceTree()
    }

    @Test
    fun updateActiveDelegatesBothOutcomes() {
        whenCalled(service.updateActive("ms1", true)).thenReturn(true)
        whenCalled(service.updateActive("ms2", false)).thenReturn(false)
        assertTrue(controller.updateActive("ms1", true))
        assertFalse(controller.updateActive("ms2", false))
        verify(service).updateActive("ms1", true)
        verify(service).updateActive("ms2", false)
    }
}
