package io.kudos.ms.sys.api.internal.controller.i18n

import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate
import io.kudos.ms.sys.core.i18n.api.SysI18nApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for SysI18nInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - getI18nValue delegates (hit and null miss, Unicode value passed through)
 * - getI18ns delegates (non-empty and empty maps)
 * - batchSaveOrUpdate delegates (non-empty and empty list -> 0)
 * - updateActive delegates for both true and false results
 *
 * @author K
 * @since 1.0.0
 */
internal class SysI18nInternalControllerTest {

    private val api = mock(SysI18nApi::class.java)
    private val controller = SysI18nInternalController(api)

    @Test
    fun getI18nValue_delegates() {
        whenCalled(api.getI18nValue("zh_CN", "type-a", "ns", "as-a", "greeting")).thenReturn("你好")

        assertEquals("你好", controller.getI18nValue("zh_CN", "type-a", "ns", "as-a", "greeting"))
        assertNull(controller.getI18nValue("zh_CN", "type-a", "ns", "as-a", "missing"))
        verify(api).getI18nValue("zh_CN", "type-a", "ns", "as-a", "greeting")
    }

    @Test
    fun getI18ns_delegates() {
        val map = mapOf("k1" to "v1", "k2" to "v2")
        whenCalled(api.getI18ns("en_US", "type-a", "ns", "as-a")).thenReturn(map)

        assertSame(map, controller.getI18ns("en_US", "type-a", "ns", "as-a"))
        assertTrue(controller.getI18ns("en_US", "type-none", "ns", "as-a").isEmpty())
        verify(api).getI18ns("en_US", "type-a", "ns", "as-a")
    }

    @Test
    fun batchSaveOrUpdate_delegates() {
        val forms = listOf(mock(SysI18nFormUpdate::class.java))
        whenCalled(api.batchSaveOrUpdate(forms)).thenReturn(1)

        assertEquals(1, controller.batchSaveOrUpdate(forms))
        assertEquals(0, controller.batchSaveOrUpdate(emptyList()))
        verify(api).batchSaveOrUpdate(forms)
        verify(api).batchSaveOrUpdate(emptyList())
    }

    @Test
    fun updateActive_delegates() {
        whenCalled(api.updateActive("id-1", true)).thenReturn(true)
        whenCalled(api.updateActive("id-2", false)).thenReturn(false)

        assertTrue(controller.updateActive("id-1", true))
        assertFalse(controller.updateActive("id-2", false))
        verify(api).updateActive("id-1", true)
        verify(api).updateActive("id-2", false)
    }
}
