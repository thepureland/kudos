package io.kudos.ms.msg.core.instance.api

import io.kudos.ms.msg.common.instance.vo.MsgInstanceCacheEntry
import io.kudos.ms.msg.core.instance.service.iservice.IMsgInstanceService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for MsgInstanceApi (pure delegation, service mocked with Mockito, no Spring container).
 *
 * The service dependency is a private `@Resource lateinit var`, injected by reflection.
 *
 * Coverage: getInstanceById forwards the id and returns both a found entry and null.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgInstanceApiTest {

    private val service = mock(IMsgInstanceService::class.java)
    private val api = MsgInstanceApi().apply {
        val field = MsgInstanceApi::class.java.getDeclaredField("msgInstanceService")
        field.isAccessible = true
        field.set(this, service)
    }

    private fun entry(id: String) = MsgInstanceCacheEntry(
        id = id,
        localeDictCode = "zh_CN",
        title = "t",
        content = "c",
        templateId = "tmpl-1",
        sendTypeDictCode = "01",
        eventTypeDictCode = "user_registered",
        msgTypeDictCode = "welcome",
        validTimeStart = null,
        validTimeEnd = null,
        tenantId = "t1",
    )

    @Test
    fun getInstanceById_delegates() {
        val e = entry("i-1")
        whenCalled(service.getInstanceById("i-1")).thenReturn(e)
        assertEquals(e, api.getInstanceById("i-1"))
        verify(service).getInstanceById("i-1")
    }

    @Test
    fun getInstanceById_delegatesNull() {
        whenCalled(service.getInstanceById("missing")).thenReturn(null)
        assertNull(api.getInstanceById("missing"))
        verify(service).getInstanceById("missing")
    }
}
