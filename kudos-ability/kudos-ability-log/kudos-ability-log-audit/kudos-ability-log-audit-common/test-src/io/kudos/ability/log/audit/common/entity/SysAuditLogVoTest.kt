package io.kudos.ability.log.audit.common.entity

import java.io.Serializable
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SysAuditLogVo].
 *
 * Coverage:
 *  - All properties default to null and round-trip through setters/getters.
 *  - The [SysAuditLogVo.AUDIT_LOG] temp-key constant is stable (downstream context lookups depend on the literal).
 *  - The VO is [Serializable] for MQ delivery.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAuditLogVoTest {

    @Test
    fun allProperties_defaultToNull() {
        val vo = SysAuditLogVo()
        assertNull(vo.id)
        assertNull(vo.entityId)
        assertNull(vo.operateTypeId)
        assertNull(vo.operateType)
        assertNull(vo.moduleName)
        assertNull(vo.moduleCode)
        assertNull(vo.description)
        assertNull(vo.operator)
        assertNull(vo.tenantId)
        assertNull(vo.sourceTenantId)
        assertNull(vo.subSysCode)
        assertNull(vo.operateTime)
        assertNull(vo.operateIp)
        assertNull(vo.operateIpDictCode)
        assertNull(vo.operatorId)
        assertNull(vo.operatorUserType)
        assertNull(vo.clientOs)
        assertNull(vo.clientBrowser)
        assertNull(vo.requestType)
        assertNull(vo.moduleId)
    }

    @Test
    fun allProperties_roundTrip() {
        val time = Date(1_700_000_000_000L)
        val vo = SysAuditLogVo().apply {
            id = "uuid-1"
            entityId = "e-1"
            operateTypeId = 2
            operateType = "create"
            moduleName = "用戶管理"
            moduleCode = "USER"
            description = "create user"
            operator = "alice"
            tenantId = "t-1"
            sourceTenantId = "src-1"
            subSysCode = "user"
            operateTime = time
            operateIp = 2130706433L // 127.0.0.1
            operateIpDictCode = "CN"
            operatorId = "op-1"
            operatorUserType = "ADMIN"
            clientOs = "Windows 11"
            clientBrowser = "Chrome"
            requestType = "POST"
            moduleId = 9
        }

        assertEquals("uuid-1", vo.id)
        assertEquals("e-1", vo.entityId)
        assertEquals(2, vo.operateTypeId)
        assertEquals("create", vo.operateType)
        assertEquals("用戶管理", vo.moduleName)
        assertEquals("USER", vo.moduleCode)
        assertEquals("create user", vo.description)
        assertEquals("alice", vo.operator)
        assertEquals("t-1", vo.tenantId)
        assertEquals("src-1", vo.sourceTenantId)
        assertEquals("user", vo.subSysCode)
        assertEquals(time, vo.operateTime)
        assertEquals(2130706433L, vo.operateIp)
        assertEquals("CN", vo.operateIpDictCode)
        assertEquals("op-1", vo.operatorId)
        assertEquals("ADMIN", vo.operatorUserType)
        assertEquals("Windows 11", vo.clientOs)
        assertEquals("Chrome", vo.clientBrowser)
        assertEquals("POST", vo.requestType)
        assertEquals(9, vo.moduleId)
    }

    @Test
    fun auditLogTempKeyConstant_isStable() {
        assertEquals("__AUDIT_LOG_TMP__", SysAuditLogVo.AUDIT_LOG)
        assertTrue(SysAuditLogVo() is Serializable)
    }
}
