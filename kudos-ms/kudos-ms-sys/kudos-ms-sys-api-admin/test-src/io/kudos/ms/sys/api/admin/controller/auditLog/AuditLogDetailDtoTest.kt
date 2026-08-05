package io.kudos.ms.sys.api.admin.controller.auditLog

import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure unit test for [AuditLogDetailDto]: the [AuditLogDetailDto.from] flattening that merges a
 * [SysAuditLogVo] main row with an optional [SysAuditDetailLogVo] detail row, including the
 * null-detail path and the deliberate non-merge of the detail row's `description`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AuditLogDetailDtoTest {

    private fun mainRow(): SysAuditLogVo = SysAuditLogVo().apply {
        id = "log-1"
        entityId = "ent-1"
        operateTypeId = 3
        operateType = "UPDATE"
        moduleId = 11
        moduleName = "User / Account"
        moduleCode = "user.account"
        description = "main summary"
        operator = "alice"
        operatorId = "u-1"
        operatorUserType = "ADMIN"
        tenantId = "t-1"
        sourceTenantId = "st-1"
        subSysCode = "user"
        operateTime = Date(1_700_000_000_000L)
        operateIp = 2130706433L
        operateIpDictCode = "CN.GD"
        clientOs = "Windows"
        clientBrowser = "Chrome"
        requestType = "POST"
    }

    private fun detailRow(): SysAuditDetailLogVo = SysAuditDetailLogVo().apply {
        operateUrl = "/api/admin/sys/x"
        stringParams = "p0"
        objectParams = """{"k":"v"}"""
        requestReferer = "https://console/x"
        requestFormData = "form=data"
        description = "DETAIL description that must NOT override main"
    }

    @Test
    fun fromMergesAllMainFields() {
        val main = mainRow()
        val dto = AuditLogDetailDto.from(main, null)

        assertEquals("log-1", dto.id)
        assertEquals("ent-1", dto.entityId)
        assertEquals(3, dto.operateTypeId)
        assertEquals("UPDATE", dto.operateType)
        assertEquals(11, dto.moduleId)
        assertEquals("User / Account", dto.moduleName)
        assertEquals("user.account", dto.moduleCode)
        assertEquals("main summary", dto.description)
        assertEquals("alice", dto.operator)
        assertEquals("u-1", dto.operatorId)
        assertEquals("ADMIN", dto.operatorUserType)
        assertEquals("t-1", dto.tenantId)
        assertEquals("st-1", dto.sourceTenantId)
        assertEquals("user", dto.subSysCode)
        assertEquals(Date(1_700_000_000_000L), dto.operateTime)
        assertEquals(2130706433L, dto.operateIp)
        assertEquals("CN.GD", dto.operateIpDictCode)
        assertEquals("Windows", dto.clientOs)
        assertEquals("Chrome", dto.clientBrowser)
        assertEquals("POST", dto.requestType)
    }

    @Test
    fun fromWithNullDetailLeavesDetailFieldsNull() {
        val dto = AuditLogDetailDto.from(mainRow(), null)
        assertNull(dto.operateUrl)
        assertNull(dto.stringParams)
        assertNull(dto.objectParams)
        assertNull(dto.requestReferer)
        assertNull(dto.requestFormData)
    }

    @Test
    fun fromMergesDetailFields() {
        val dto = AuditLogDetailDto.from(mainRow(), detailRow())
        assertEquals("/api/admin/sys/x", dto.operateUrl)
        assertEquals("p0", dto.stringParams)
        assertEquals("""{"k":"v"}""", dto.objectParams)
        assertEquals("https://console/x", dto.requestReferer)
        assertEquals("form=data", dto.requestFormData)
    }

    @Test
    fun fromKeepsMainDescriptionNotDetailDescription() {
        val dto = AuditLogDetailDto.from(mainRow(), detailRow())
        // The detail row's description is intentionally NOT merged.
        assertEquals("main summary", dto.description)
    }

    @Test
    fun fromPreservesNullMainFields() {
        // An all-null main row maps every field to null without throwing.
        val dto = AuditLogDetailDto.from(SysAuditLogVo(), SysAuditDetailLogVo())
        assertNull(dto.id)
        assertNull(dto.operateTime)
        assertNull(dto.operateIp)
        assertNull(dto.moduleId)
        assertNull(dto.operateUrl)
        assertNull(dto.requestFormData)
    }

    @Test
    fun dataClassCopyAndEquality() {
        val dto = AuditLogDetailDto.from(mainRow(), detailRow())
        val same = AuditLogDetailDto.from(mainRow(), detailRow())
        assertEquals(dto, same)
        assertEquals(dto.hashCode(), same.hashCode())
        assertEquals("new", dto.copy(id = "new").id)
    }
}
