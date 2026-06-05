package io.kudos.ms.msg.core.template.service

import io.kudos.ms.msg.core.template.service.iservice.IMsgTemplateService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for MsgTemplateService — id lookup and the optional-locale four-key event lookup.
 *
 * Test data source: `template/MsgTemplateServiceTest.sql`.
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class MsgTemplateServiceTest : RdbTestBase() {

    @Resource
    private lateinit var msgTemplateService: IMsgTemplateService

    private val tenantId = "svc-tenant-msg-tmpl"

    @Test
    fun getTemplateByIdReturnsEntry() {
        val entry = msgTemplateService.getTemplateById("f1000000-0000-0000-0000-000000000001")
        assertNotNull(entry)
        assertEquals("f1000000-0000-0000-0000-000000000001", entry.id)
        assertEquals("zh-CN", entry.localeDictCode)
    }

    @Test
    fun getTemplateByIdReturnsNullForUnknown() {
        assertNull(msgTemplateService.getTemplateById("no-such-template"))
    }

    @Test
    fun byEventWithLocaleMatchesExactLocale() {
        val zh = msgTemplateService.getTemplateByEvent(tenantId, "evt_login", "email", "zh-CN")
        assertNotNull(zh)
        assertEquals("zh-CN", zh.localeDictCode)

        val en = msgTemplateService.getTemplateByEvent(tenantId, "evt_login", "email", "en-US")
        assertNotNull(en)
        assertEquals("en-US", en.localeDictCode)
    }

    @Test
    fun byEventWithNullLocaleIgnoresLocaleFilter() {
        // locale not constrained -> any template of that event/msgType is acceptable
        val any = msgTemplateService.getTemplateByEvent(tenantId, "evt_login", "email", null)
        assertNotNull(any)
        assertEquals("evt_login", any.eventTypeDictCode)
        assertEquals("email", any.msgTypeDictCode)
    }

    @Test
    fun byEventWithUnmatchedLocaleReturnsNull() {
        // locale is constrained but no row has fr-FR
        assertNull(msgTemplateService.getTemplateByEvent(tenantId, "evt_login", "email", "fr-FR"))
    }

    @Test
    fun byEventWithUnknownEventReturnsNull() {
        assertNull(msgTemplateService.getTemplateByEvent(tenantId, "evt_unknown", "email", null))
    }
}
