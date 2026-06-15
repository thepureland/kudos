package io.kudos.ms.msg.common.template.vo.request

import io.kudos.ms.msg.common.template.vo.response.MsgTemplateRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgTemplateQuery
 *
 * Covers all-null defaults, full construction, ListSearchPayload overrides and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgTemplateQueryTest {

    @Test
    fun defaultsAreAllNull() {
        val q = MsgTemplateQuery()
        assertNull(q.sendTypeDictCode)
        assertNull(q.eventTypeDictCode)
        assertNull(q.msgTypeDictCode)
        assertNull(q.receiverGroupCode)
        assertNull(q.localeDictCode)
        assertNull(q.title)
        assertNull(q.content)
        assertNull(q.defaultActive)
        assertNull(q.defaultTitle)
        assertNull(q.defaultContent)
        assertNull(q.tenantId)
    }

    @Test
    fun fullArgsConstruction() {
        val q = MsgTemplateQuery(
            sendTypeDictCode = "s", eventTypeDictCode = "e", msgTypeDictCode = "m",
            receiverGroupCode = "rg", localeDictCode = "en", title = "t", content = "c",
            defaultActive = true, defaultTitle = "dt", defaultContent = "dc", tenantId = "tn",
        )
        assertEquals("s", q.sendTypeDictCode)
        assertEquals("rg", q.receiverGroupCode)
        assertEquals(true, q.defaultActive)
        assertEquals("tn", q.tenantId)
    }

    @Test
    fun payloadOverrides() {
        val q = MsgTemplateQuery()
        assertEquals(MsgTemplateRow::class, q.getReturnEntityClass())
        assertFalse(q.isUnpagedSearchAllowed())
    }

    @Test
    fun dataClassContract() {
        val a = MsgTemplateQuery(title = "t")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(defaultActive = false))
        assertTrue(a.toString().contains("t"))
    }

}
