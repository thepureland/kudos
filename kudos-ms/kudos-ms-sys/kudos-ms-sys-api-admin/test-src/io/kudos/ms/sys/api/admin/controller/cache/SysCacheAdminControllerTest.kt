package io.kudos.ms.sys.api.admin.controller.cache

import io.kudos.ms.sys.core.cache.service.iservice.ISysCacheService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [SysCacheAdminController]: every extra cache-management endpoint delegates to
 * the service with the right args, and read endpoints relay the service result. The service is
 * mocked and injected via reflection; no Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysCacheAdminControllerTest {

    private val service = mock(ISysCacheService::class.java)
    private val controller = SysCacheAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun updateActiveDelegatesBothOutcomes() {
        whenCalled(service.updateActive("c1", true)).thenReturn(true)
        whenCalled(service.updateActive("c2", false)).thenReturn(false)
        assertTrue(controller.updateActive("c1", true))
        assertFalse(controller.updateActive("c2", false))
        verify(service).updateActive("c1", true)
        verify(service).updateActive("c2", false)
    }

    @Test
    fun reloadDelegates() {
        controller.reload("c1", "k1")
        verify(service).reload("c1", "k1")
    }

    @Test
    fun reloadAllDelegates() {
        controller.reloadAll("c1")
        verify(service).reloadAll("c1")
    }

    @Test
    fun evictDelegates() {
        controller.evict("c1", "k1")
        verify(service).evict("c1", "k1")
    }

    @Test
    fun evictAllDelegates() {
        controller.evictAll("c1")
        verify(service).evictAll("c1")
    }

    @Test
    fun existsKeyDelegatesBothOutcomes() {
        whenCalled(service.existsKey("c1", "present")).thenReturn(true)
        whenCalled(service.existsKey("c1", "absent")).thenReturn(false)
        assertTrue(controller.existsKey("c1", "present"))
        assertFalse(controller.existsKey("c1", "absent"))
        verify(service).existsKey("c1", "present")
        verify(service).existsKey("c1", "absent")
    }

    @Test
    fun getValueJsonRelaysServiceString() {
        whenCalled(service.getValueJson("c1", "k1")).thenReturn("""{"x":1}""")
        assertEquals("""{"x":1}""", controller.getValueJson("c1", "k1"))
        verify(service).getValueJson("c1", "k1")
    }

    @Test
    fun getValueJsonRelaysEmptyString() {
        whenCalled(service.getValueJson("c1", "missing")).thenReturn("")
        assertEquals("", controller.getValueJson("c1", "missing"))
    }
}
