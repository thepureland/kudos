package io.kudos.ms.msg.common.template.api

import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for IMsgTemplateApi
 *
 * Drives the contract through a fake implementation: argument pass-through (incl. the
 * nullable localeDictCode), the nullable not-found return for both lookups.
 *
 * @author K
 * @since 1.0.0
 */
internal class IMsgTemplateApiTest {

    private class FakeApi : IMsgTemplateApi {
        var lastId: String? = null
        var lastTenantId: String? = null
        var lastEvent: String? = null
        var lastMsgType: String? = null
        var lastLocale: String? = "UNSET"
        var byId: MsgTemplateCacheEntry? = null
        var byEvent: MsgTemplateCacheEntry? = null

        override fun getTemplateById(id: String): MsgTemplateCacheEntry? {
            lastId = id
            return byId
        }

        override fun getTemplateByEvent(
            tenantId: String,
            eventTypeDictCode: String,
            msgTypeDictCode: String,
            localeDictCode: String?,
        ): MsgTemplateCacheEntry? {
            lastTenantId = tenantId
            lastEvent = eventTypeDictCode
            lastMsgType = msgTypeDictCode
            lastLocale = localeDictCode
            return byEvent
        }
    }

    private fun template(id: String) = MsgTemplateCacheEntry(
        id = id, sendTypeDictCode = null, eventTypeDictCode = "e", msgTypeDictCode = "m",
        receiverGroupCode = null, localeDictCode = "en", title = "t", content = "c",
        defaultActive = true, defaultTitle = null, defaultContent = null, tenantId = "tn",
    )

    @Test
    fun getTemplateByIdHit() {
        val api = FakeApi()
        api.byId = template("t-1")
        val t = api.getTemplateById("t-1")
        assertEquals("t-1", api.lastId)
        assertEquals("t-1", t?.id)
    }

    @Test
    fun getTemplateByIdMiss() {
        val api = FakeApi()
        assertNull(api.getTemplateById("missing"))
        assertEquals("missing", api.lastId)
    }

    @Test
    fun getTemplateByEventWithLocale() {
        val api = FakeApi()
        api.byEvent = template("t-2")
        val t = api.getTemplateByEvent("tn", "e", "m", "en_US")
        assertEquals("tn", api.lastTenantId)
        assertEquals("e", api.lastEvent)
        assertEquals("m", api.lastMsgType)
        assertEquals("en_US", api.lastLocale)
        assertEquals("t-2", t?.id)
    }

    @Test
    fun getTemplateByEventWithNullLocaleMiss() {
        val api = FakeApi()
        val t = api.getTemplateByEvent("tn", "e", "m", null)
        assertNull(api.lastLocale)
        assertNull(t)
    }

}
