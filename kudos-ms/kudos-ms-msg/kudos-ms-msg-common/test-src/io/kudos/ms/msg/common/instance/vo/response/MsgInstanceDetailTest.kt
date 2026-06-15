package io.kudos.ms.msg.common.instance.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgInstanceDetail
 *
 * Covers defaults (id="" and nullable fields), full construction, IIdEntity contract
 * and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgInstanceDetailTest {

    @Test
    fun defaults() {
        val d = MsgInstanceDetail()
        assertEquals("", d.id)
        assertNull(d.localeDictCode)
        assertNull(d.title)
        assertNull(d.content)
        assertNull(d.validTimeStart)
        assertNull(d.tenantId)
        assertTrue(d is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val end = LocalDateTime.of(2026, 6, 1, 12, 0)
        val d = MsgInstanceDetail(
            id = "i-1", localeDictCode = "en", title = "t", content = "c", templateId = "tpl",
            sendTypeDictCode = "s", eventTypeDictCode = "e", msgTypeDictCode = "m",
            validTimeStart = null, validTimeEnd = end, tenantId = "tn",
        )
        assertEquals("i-1", d.id)
        assertEquals("tpl", d.templateId)
        assertEquals(end, d.validTimeEnd)
    }

    @Test
    fun dataClassContract() {
        val a = MsgInstanceDetail(id = "i-1", title = "t")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "i-2"))
        assertTrue(a.toString().contains("i-1"))
    }

}
