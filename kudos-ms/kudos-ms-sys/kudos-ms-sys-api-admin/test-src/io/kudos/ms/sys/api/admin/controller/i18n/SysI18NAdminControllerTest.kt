package io.kudos.ms.sys.api.admin.controller.i18n

import io.kudos.ms.sys.common.i18n.vo.request.SysI18nBatchPayload
import io.kudos.ms.sys.core.i18n.service.iservice.ISysI18nService
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
 * Pure unit test for [SysI18NAdminController]: getI18ns / batchGetI18ns delegation (incl. unpacking
 * the batch payload's three fields) and updateActive. The service is mocked and injected via
 * reflection; no Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysI18NAdminControllerTest {

    private val service = mock(ISysI18nService::class.java)
    private val controller = SysI18NAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun getI18nsDelegatesAndForwardsAllFourArgs() {
        val map = mapOf("k1" to "v1", "k2" to "v2")
        whenCalled(service.getI18nsFromCache("zh_CN", "ui", "ns", "svc")).thenReturn(map)
        assertSame(map, controller.getI18ns("zh_CN", "ui", "ns", "svc"))
        verify(service).getI18nsFromCache("zh_CN", "ui", "ns", "svc")
    }

    @Test
    fun getI18nsEmpty() {
        whenCalled(service.getI18nsFromCache("zh_CN", "ui", "missing", "svc")).thenReturn(emptyMap())
        assertTrue(controller.getI18ns("zh_CN", "ui", "missing", "svc").isEmpty())
    }

    @Test
    fun batchGetI18nsUnpacksPayloadFields() {
        val payload = SysI18nBatchPayload(
            locale = "en_US",
            namespacesByI18nTypeDictCode = mapOf("ui" to listOf("ns1", "ns2")),
            atomicServiceCodes = setOf("svcA", "svcB"),
        )
        val ret = mapOf("ui" to mapOf("ns1" to mapOf("k" to "v")))
        whenCalled(
            service.batchGetI18nsFromCache(
                payload.locale,
                payload.namespacesByI18nTypeDictCode,
                payload.atomicServiceCodes,
            )
        ).thenReturn(ret)

        assertSame(ret, controller.batchGetI18ns(payload))
        verify(service).batchGetI18nsFromCache(
            payload.locale,
            payload.namespacesByI18nTypeDictCode,
            payload.atomicServiceCodes,
        )
    }

    @Test
    fun updateActiveDelegatesBothOutcomes() {
        whenCalled(service.updateActive("x1", true)).thenReturn(true)
        whenCalled(service.updateActive("x2", false)).thenReturn(false)
        assertTrue(controller.updateActive("x1", true))
        assertFalse(controller.updateActive("x2", false))
        verify(service).updateActive("x1", true)
        verify(service).updateActive("x2", false)
    }
}
