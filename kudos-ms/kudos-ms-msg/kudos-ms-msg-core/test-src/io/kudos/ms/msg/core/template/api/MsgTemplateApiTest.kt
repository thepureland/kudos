package io.kudos.ms.msg.core.template.api

import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import io.kudos.ms.msg.core.template.service.iservice.IMsgTemplateService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for MsgTemplateApi (pure delegation, service mocked with Mockito, no Spring container).
 *
 * The service dependency is a private `@Resource lateinit var`, so it is injected by reflection.
 *
 * Coverage: both delegate methods forward arguments and return values unchanged
 * (getTemplateById hit + null miss / getTemplateByEvent with and without locale).
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgTemplateApiTest {

    private val service = mock(IMsgTemplateService::class.java)
    private val api = MsgTemplateApi().apply {
        val field = MsgTemplateApi::class.java.getDeclaredField("msgTemplateService")
        field.isAccessible = true
        field.set(this, service)
    }

    private fun entry(id: String) = MsgTemplateCacheEntry(
        id = id,
        sendTypeDictCode = null,
        eventTypeDictCode = "evt_login",
        msgTypeDictCode = "email",
        receiverGroupCode = null,
        localeDictCode = "zh-CN",
        title = "t",
        content = "c",
        defaultActive = true,
        defaultTitle = null,
        defaultContent = null,
        tenantId = "tenant-1",
    )

    @Test
    fun getTemplateById_delegatesHit() {
        val t = entry("tpl-1")
        whenCalled(service.getTemplateById("tpl-1")).thenReturn(t)
        assertEquals(t, api.getTemplateById("tpl-1"))
        verify(service).getTemplateById("tpl-1")
    }

    @Test
    fun getTemplateById_delegatesNull() {
        whenCalled(service.getTemplateById("missing")).thenReturn(null)
        assertNull(api.getTemplateById("missing"))
    }

    @Test
    fun getTemplateByEvent_delegatesWithLocale() {
        val t = entry("tpl-1")
        whenCalled(service.getTemplateByEvent("tenant-1", "evt_login", "email", "zh-CN")).thenReturn(t)
        assertEquals(t, api.getTemplateByEvent("tenant-1", "evt_login", "email", "zh-CN"))
        verify(service).getTemplateByEvent("tenant-1", "evt_login", "email", "zh-CN")
    }

    @Test
    fun getTemplateByEvent_delegatesNullLocale() {
        val t = entry("tpl-1")
        whenCalled(service.getTemplateByEvent("tenant-1", "evt_login", "email", null)).thenReturn(t)
        assertEquals(t, api.getTemplateByEvent("tenant-1", "evt_login", "email", null))
    }

    @Test
    fun getTemplateByEvent_delegatesNullResult() {
        whenCalled(service.getTemplateByEvent("tenant-1", "evt_x", "email", null)).thenReturn(null)
        assertNull(api.getTemplateByEvent("tenant-1", "evt_x", "email", null))
    }
}
