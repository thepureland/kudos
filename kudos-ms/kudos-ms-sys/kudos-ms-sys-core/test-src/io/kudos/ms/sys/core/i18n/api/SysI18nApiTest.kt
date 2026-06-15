package io.kudos.ms.sys.core.i18n.api

import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate
import io.kudos.ms.sys.core.i18n.service.iservice.ISysI18nService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysI18nApi (pure delegation, service mocked with Mockito, no Spring container).
 *
 * Coverage:
 * - getI18nValue delegates to the service (hit and null miss)
 * - getI18ns delegates unchanged (incl. empty result)
 * - batchSaveOrUpdate / updateActive delegate the return value unchanged
 *
 * @author K
 * @since 1.0.0
 */
internal class SysI18nApiTest {

    private val service = mock(ISysI18nService::class.java)
    private val api = SysI18nApi(service)

    @Test
    fun getI18nValue_delegates() {
        whenCalled(service.getI18nValueFromCache("zh_CN", "label", "ns", "svc-1", "k1")).thenReturn("v1")
        assertEquals("v1", api.getI18nValue("zh_CN", "label", "ns", "svc-1", "k1"))
        verify(service).getI18nValueFromCache("zh_CN", "label", "ns", "svc-1", "k1")
    }

    @Test
    fun getI18nValue_nullWhenMiss() {
        whenCalled(service.getI18nValueFromCache("zh_CN", "label", "ns", "svc-1", "miss")).thenReturn(null)
        assertNull(api.getI18nValue("zh_CN", "label", "ns", "svc-1", "miss"))
    }

    @Test
    fun getI18ns_delegates() {
        val map = mapOf("k1" to "v1", "k2" to "v2")
        whenCalled(service.getI18nsFromCache("zh_CN", "label", "ns", "svc-1")).thenReturn(map)
        assertEquals(map, api.getI18ns("zh_CN", "label", "ns", "svc-1"))

        whenCalled(service.getI18nsFromCache("zh_CN", "label", "empty", "svc-1")).thenReturn(emptyMap())
        assertTrue(api.getI18ns("zh_CN", "label", "empty", "svc-1").isEmpty())
    }

    @Test
    fun batchSaveOrUpdate_delegatesCount() {
        val forms = listOf(
            SysI18nFormUpdate(
                id = "",
                locale = "zh_CN",
                atomicServiceCode = "svc-1",
                i18nTypeDictCode = "label",
                namespace = "ns",
                key = "k1",
                value = "v1",
                remark = null,
            )
        )
        whenCalled(service.batchSaveOrUpdate(forms)).thenReturn(3)
        assertEquals(3, api.batchSaveOrUpdate(forms))
        verify(service).batchSaveOrUpdate(forms)
    }

    @Test
    fun updateActive_delegatesBoolean() {
        whenCalled(service.updateActive("id-1", true)).thenReturn(true)
        whenCalled(service.updateActive("id-2", false)).thenReturn(false)
        assertTrue(api.updateActive("id-1", true))
        assertFalse(api.updateActive("id-2", false))
    }
}
