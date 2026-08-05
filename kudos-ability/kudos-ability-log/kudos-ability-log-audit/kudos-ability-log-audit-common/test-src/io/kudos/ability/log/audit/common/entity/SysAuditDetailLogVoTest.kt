package io.kudos.ability.log.audit.common.entity

import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SysAuditDetailLogVo].
 *
 * Coverage:
 *  - No-arg constructor leaves every field null (deserialization contract).
 *  - The id constructor sets only the primary key, including the null-id edge.
 *  - All properties round-trip through setters/getters.
 *  - The [SysAuditDetailLogVo.AUDIT_LOG_DESC] placeholder constant is stable — context lookups use the literal.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAuditDetailLogVoTest {

    @Test
    fun noArgConstructor_allFieldsNull() {
        val vo = SysAuditDetailLogVo()
        assertNull(vo.id)
        assertNull(vo.auditId)
        assertNull(vo.operateUrl)
        assertNull(vo.stringParams)
        assertNull(vo.objectParams)
        assertNull(vo.requestReferer)
        assertNull(vo.requestFormData)
        assertNull(vo.description)
    }

    @Test
    fun idConstructor_setsOnlyPrimaryKey() {
        val vo = SysAuditDetailLogVo("d-1")
        assertEquals("d-1", vo.id)
        assertNull(vo.auditId)
        assertNull(vo.description)

        val nullId = SysAuditDetailLogVo(null)
        assertNull(nullId.id)
    }

    @Test
    fun allProperties_roundTrip() {
        val vo = SysAuditDetailLogVo().apply {
            id = "d-2"
            auditId = "a-2"
            operateUrl = "/api/users?id=1"
            stringParams = "alice┼admin"
            objectParams = """{"user":{"name":"alice"}}"""
            requestReferer = "https://example.com/admin"
            requestFormData = """{"name":"alice"}"""
            description = "建立用戶"
        }

        assertEquals("d-2", vo.id)
        assertEquals("a-2", vo.auditId)
        assertEquals("/api/users?id=1", vo.operateUrl)
        assertEquals("alice┼admin", vo.stringParams)
        assertEquals("""{"user":{"name":"alice"}}""", vo.objectParams)
        assertEquals("https://example.com/admin", vo.requestReferer)
        assertEquals("""{"name":"alice"}""", vo.requestFormData)
        assertEquals("建立用戶", vo.description)
    }

    @Test
    fun descPlaceholderConstant_isStable() {
        assertEquals("__AUDIT_LOG_TMP_DESC__", SysAuditDetailLogVo.AUDIT_LOG_DESC)
        assertTrue(SysAuditDetailLogVo() is Serializable)
    }
}
