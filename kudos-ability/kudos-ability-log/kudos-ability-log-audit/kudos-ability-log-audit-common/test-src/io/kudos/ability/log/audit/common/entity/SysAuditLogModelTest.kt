package io.kudos.ability.log.audit.common.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SysAuditLogModel].
 *
 * Coverage:
 *  - All properties default to null and round-trip through setters/getters.
 *  - [SysAuditLogModel.toString] serializes the whole aggregate to JSON (troubleshooting contract) —
 *    asserts key fields appear instead of the default `ClassName@hash` form.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAuditLogModelTest {

    @Test
    fun allProperties_defaultToNull() {
        val model = SysAuditLogModel()
        assertNull(model.sysAuditDetailLogs)
        assertNull(model.entities)
        assertNull(model.subSysCode)
        assertNull(model.tenantId)
    }

    @Test
    fun allProperties_roundTrip() {
        val entity = SysAuditLogVo().apply { id = "a-1" }
        val detail = SysAuditDetailLogVo().apply { auditId = "a-1" }
        val model = SysAuditLogModel().apply {
            entities = mutableListOf(entity)
            sysAuditDetailLogs = mutableListOf(detail)
            subSysCode = "user"
            tenantId = "t-1"
        }

        assertEquals(1, model.entities?.size)
        assertEquals("a-1", model.entities?.first()?.id)
        assertEquals(1, model.sysAuditDetailLogs?.size)
        assertEquals("a-1", model.sysAuditDetailLogs?.first()?.auditId)
        assertEquals("user", model.subSysCode)
        assertEquals("t-1", model.tenantId)
    }

    @Test
    fun toString_serializesToJsonWithFieldValues() {
        val model = SysAuditLogModel().apply {
            subSysCode = "user"
            tenantId = "t-9"
            entities = mutableListOf(SysAuditLogVo().apply { id = "a-9" })
            sysAuditDetailLogs = mutableListOf()
        }

        val text = model.toString()

        // JSON projection, not the default Object.toString — operators grep these values in logs.
        assertTrue("t-9" in text, "tenantId must appear in toString output: $text")
        assertTrue("user" in text, "subSysCode must appear in toString output: $text")
        assertTrue("a-9" in text, "nested entity id must appear in toString output: $text")
    }
}
