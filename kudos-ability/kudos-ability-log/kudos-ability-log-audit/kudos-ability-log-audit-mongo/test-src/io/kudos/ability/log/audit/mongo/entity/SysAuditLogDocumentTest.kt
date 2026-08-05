package io.kudos.ability.log.audit.mongo.entity

import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Unit tests for [SysAuditLogDocument]:
 * - no-arg constructor leaves every field (including the embedded detail) null.
 * - the (entity, detail, model) constructor maps every [SysAuditLogVo] field one-to-one.
 * - tenantId / subSysCode fallback rule: entity value wins, model value fills the gap,
 *   both-null stays null.
 * - the detail argument is attached as-is (same instance) and null detail stays null.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysAuditLogDocumentTest {

    @Test
    fun noArgConstructor_allFieldsNull() {
        val doc = SysAuditLogDocument()
        assertNull(doc.id)
        assertNull(doc.entityId)
        assertNull(doc.operateTypeId)
        assertNull(doc.operateType)
        assertNull(doc.moduleName)
        assertNull(doc.moduleCode)
        assertNull(doc.moduleId)
        assertNull(doc.description)
        assertNull(doc.operator)
        assertNull(doc.tenantId)
        assertNull(doc.sourceTenantId)
        assertNull(doc.subSysCode)
        assertNull(doc.operateTime)
        assertNull(doc.operateIp)
        assertNull(doc.operateIpDictCode)
        assertNull(doc.operatorId)
        assertNull(doc.operatorUserType)
        assertNull(doc.clientOs)
        assertNull(doc.clientBrowser)
        assertNull(doc.requestType)
        assertNull(doc.detail)
    }

    @Test
    fun fullConstructor_mapsEveryEntityFieldOneToOne() {
        val time = Date(1_700_000_000_000L)
        val entity = SysAuditLogVo().apply {
            id = "a-1"
            entityId = "user-7"
            operateTypeId = 2
            operateType = "UPDATE"
            moduleName = "User/Login"
            moduleCode = "user"
            moduleId = 42
            description = "update user 7"
            operator = "alice"
            tenantId = "T-entity"
            sourceTenantId = "T-source"
            subSysCode = "SYS-entity"
            operateTime = time
            operateIp = 3232235777L // 192.168.1.1
            operateIpDictCode = "CN-GD"
            operatorId = "op-1"
            operatorUserType = "sys_user"
            clientOs = "Windows 11"
            clientBrowser = "Chrome 130"
            requestType = "POST"
        }
        val detail = SysAuditDetailLogDocument().apply { id = "d-1" }

        val doc = SysAuditLogDocument(entity, detail, SysAuditLogModel())

        assertEquals("a-1", doc.id)
        assertEquals("user-7", doc.entityId)
        assertEquals(2, doc.operateTypeId)
        assertEquals("UPDATE", doc.operateType)
        assertEquals("User/Login", doc.moduleName)
        assertEquals("user", doc.moduleCode)
        assertEquals(42, doc.moduleId)
        assertEquals("update user 7", doc.description)
        assertEquals("alice", doc.operator)
        assertEquals("T-entity", doc.tenantId)
        assertEquals("T-source", doc.sourceTenantId)
        assertEquals("SYS-entity", doc.subSysCode)
        assertEquals(time, doc.operateTime)
        assertEquals(3232235777L, doc.operateIp)
        assertEquals("CN-GD", doc.operateIpDictCode)
        assertEquals("op-1", doc.operatorId)
        assertEquals("sys_user", doc.operatorUserType)
        assertEquals("Windows 11", doc.clientOs)
        assertEquals("Chrome 130", doc.clientBrowser)
        assertEquals("POST", doc.requestType)
        assertSame(detail, doc.detail, "detail must be attached as the same instance, not copied")
    }

    @Test
    fun fullConstructor_entityTenantAndSubSysWinOverModel() {
        val entity = SysAuditLogVo().apply {
            tenantId = "T-entity"
            subSysCode = "SYS-entity"
        }
        val model = SysAuditLogModel().apply {
            tenantId = "T-model"
            subSysCode = "SYS-model"
        }

        val doc = SysAuditLogDocument(entity, null, model)

        assertEquals("T-entity", doc.tenantId, "entity's own tenantId wins over the model-level value")
        assertEquals("SYS-entity", doc.subSysCode, "entity's own subSysCode wins over the model-level value")
    }

    @Test
    fun fullConstructor_nullEntityTenantAndSubSys_fallBackToModel() {
        val model = SysAuditLogModel().apply {
            tenantId = "T-model"
            subSysCode = "SYS-model"
        }

        val doc = SysAuditLogDocument(SysAuditLogVo(), null, model)

        assertEquals("T-model", doc.tenantId)
        assertEquals("SYS-model", doc.subSysCode)
    }

    @Test
    fun fullConstructor_bothEntityAndModelNull_staysNull() {
        val doc = SysAuditLogDocument(SysAuditLogVo(), null, SysAuditLogModel())
        assertNull(doc.tenantId)
        assertNull(doc.subSysCode)
        assertNull(doc.detail, "null detail argument stays null")
    }
}
