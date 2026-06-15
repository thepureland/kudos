package io.kudos.ability.log.audit.mongo.entity

import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [SysAuditDetailLogDocument]:
 * - no-arg constructor leaves every field null (Mongo mapping requirement).
 * - VO copy constructor maps all 8 fields one-to-one without renames or conversions.
 * - all-null VO copies to an all-null document (no NPE, no surprise defaults).
 * - Unicode / empty-string / very long values survive the copy untouched.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysAuditDetailLogDocumentTest {

    @Test
    fun noArgConstructor_allFieldsNull() {
        val doc = SysAuditDetailLogDocument()
        assertNull(doc.id)
        assertNull(doc.auditId)
        assertNull(doc.operateUrl)
        assertNull(doc.stringParams)
        assertNull(doc.objectParams)
        assertNull(doc.requestReferer)
        assertNull(doc.requestFormData)
        assertNull(doc.description)
    }

    @Test
    fun voConstructor_mapsAllFieldsOneToOne() {
        val vo = SysAuditDetailLogVo().apply {
            id = "d-1"
            auditId = "a-1"
            operateUrl = "/api/users/1"
            stringParams = "p0,p1"
            objectParams = """{"k":"v"}"""
            requestReferer = "https://admin.example.com/users"
            requestFormData = "name=alice&age=30"
            description = "create user alice"
        }

        val doc = SysAuditDetailLogDocument(vo)

        assertEquals("d-1", doc.id)
        assertEquals("a-1", doc.auditId)
        assertEquals("/api/users/1", doc.operateUrl)
        assertEquals("p0,p1", doc.stringParams)
        assertEquals("""{"k":"v"}""", doc.objectParams)
        assertEquals("https://admin.example.com/users", doc.requestReferer)
        assertEquals("name=alice&age=30", doc.requestFormData)
        assertEquals("create user alice", doc.description)
    }

    @Test
    fun voConstructor_allNullVo_yieldsAllNullDocument() {
        val doc = SysAuditDetailLogDocument(SysAuditDetailLogVo())
        assertNull(doc.id)
        assertNull(doc.auditId)
        assertNull(doc.operateUrl)
        assertNull(doc.stringParams)
        assertNull(doc.objectParams)
        assertNull(doc.requestReferer)
        assertNull(doc.requestFormData)
        assertNull(doc.description)
    }

    @Test
    fun voConstructor_unicodeEmptyAndLongValues_surviveUnchanged() {
        val longUrl = "/api/" + "x".repeat(10_000)
        val vo = SysAuditDetailLogVo().apply {
            id = ""
            auditId = "审计-1"
            operateUrl = longUrl
            description = "操作說明 🚀 emoji + ümlaut"
        }

        val doc = SysAuditDetailLogDocument(vo)

        assertEquals("", doc.id, "empty string must be preserved, not nulled")
        assertEquals("审计-1", doc.auditId)
        assertEquals(longUrl, doc.operateUrl)
        assertEquals("操作說明 🚀 emoji + ümlaut", doc.description)
    }
}
