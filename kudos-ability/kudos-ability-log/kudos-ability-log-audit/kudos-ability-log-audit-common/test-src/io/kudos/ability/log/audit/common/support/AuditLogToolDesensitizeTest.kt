package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.annotation.LogDesensitize
import io.kudos.ability.log.audit.common.entity.LogVo
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.Signature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [AuditLogTool.applyRequestDesensitizeFromFirstJoinPointArg] and the JSON-body
 * desensitization helpers. Covers:
 *  - annotation scan finds backing-field annotations on Kotlin properties + walks the class hierarchy
 *  - static fields are skipped
 *  - JSON masking applies head-1 + tail-3 (and `"****"` for values ≤ 4 chars) only to the listed keys
 *  - non-JSON / empty / malformed bodies short-circuit to the raw text — never block the audit submit
 *
 * No Spring context required — uses a hand-rolled [StubJoinPoint] so the scanner runs in isolation.
 */
internal class AuditLogToolDesensitizeTest {

    @Test
    fun applyRequestDesensitize_collectsAnnotatedKotlinPropertyFields() {
        val logVo = LogVo()
        val joinPoint = StubJoinPoint(arrayOf<Any?>(BasicRequest(username = "alice", phone = "13800001234", idCard = "110101199001011234")))

        AuditLogTool.applyRequestDesensitizeFromFirstJoinPointArg(joinPoint, logVo)

        assertEquals(setOf("phone", "idCard"), logVo.requestDesensitizePropertyNames,
            "Kotlin `@LogDesensitize var phone` lands on the backing field (target=FIELD), so reflection picks it up without `@field:` prefix")
    }

    @Test
    fun applyRequestDesensitize_walksClassHierarchy() {
        val logVo = LogVo()
        val joinPoint = StubJoinPoint(arrayOf<Any?>(ExtendedRequest(phone = "111", idCard = "222", remark = "no")))

        AuditLogTool.applyRequestDesensitizeFromFirstJoinPointArg(joinPoint, logVo)

        assertEquals(setOf("phone", "idCard"), logVo.requestDesensitizePropertyNames,
            "Fields inherited from the base class must be collected — typical DTOs derive from a common base")
    }

    @Test
    fun applyRequestDesensitize_skipsStaticFields() {
        val logVo = LogVo()
        val joinPoint = StubJoinPoint(arrayOf<Any?>(WithStaticField()))

        AuditLogTool.applyRequestDesensitizeFromFirstJoinPointArg(joinPoint, logVo)

        // STATIC_FIELD is annotated but lives in the companion object's backing field; skipping
        // static fields keeps reflection from picking up framework / synthetic state.
        assertNull(logVo.requestDesensitizePropertyNames,
            "static fields must be skipped; no annotated instance field on this class means no names set")
    }

    @Test
    fun applyRequestDesensitize_nullArgs_leavesLogVoUntouched() {
        val logVo = LogVo()
        AuditLogTool.applyRequestDesensitizeFromFirstJoinPointArg(null, logVo)
        assertNull(logVo.requestDesensitizePropertyNames)

        val noArgs = StubJoinPoint(arrayOf<Any?>())
        AuditLogTool.applyRequestDesensitizeFromFirstJoinPointArg(noArgs, logVo)
        assertNull(logVo.requestDesensitizePropertyNames)

        val nullFirst = StubJoinPoint(arrayOf<Any?>(null))
        AuditLogTool.applyRequestDesensitizeFromFirstJoinPointArg(nullFirst, logVo)
        assertNull(logVo.requestDesensitizePropertyNames)
    }

    @Test
    fun desensitizeJsonByLogVo_masksConfiguredKeys() {
        val logVo = LogVo().apply { requestDesensitizePropertyNames = setOf("phone", "idCard") }

        val out = AuditLogTool.desensitizeJsonByLogVo(
            logVo,
            """{"username":"alice","phone":"13800001234","idCard":"110101199001011234"}""",
        )

        // head 1 + **** + tail 3 → '1' + **** + '234' = "1****234"
        // and idCard: '1' + **** + '234' = "1****234"
        assertEquals("1****234", extractJsonString(out, "phone"))
        assertEquals("1****234", extractJsonString(out, "idCard"))
        assertEquals("alice", extractJsonString(out, "username"), "unlisted keys must be left verbatim")
    }

    @Test
    fun desensitizeJsonByLogVo_shortValuesAreFullyMasked() {
        val logVo = LogVo().apply { requestDesensitizePropertyNames = setOf("v") }

        // Length 1, 4, and 5 — only length > 4 should partially mask; ≤ 4 collapses to "****"
        assertEquals("****", extractJsonString(AuditLogTool.desensitizeJsonByLogVo(logVo, """{"v":"a"}"""), "v"))
        assertEquals("****", extractJsonString(AuditLogTool.desensitizeJsonByLogVo(logVo, """{"v":"abcd"}"""), "v"))
        assertEquals("a****cde", extractJsonString(AuditLogTool.desensitizeJsonByLogVo(logVo, """{"v":"abcde"}"""), "v"))
    }

    @Test
    fun desensitizeJsonByLogVo_emptyConfig_returnsRawUntouched() {
        val raw = """{"phone":"13800001234"}"""

        assertEquals(raw, AuditLogTool.desensitizeJsonByLogVo(logVo = null, json = raw))
        assertEquals(raw, AuditLogTool.desensitizeJsonByLogVo(LogVo(), raw))
        assertEquals(raw, AuditLogTool.desensitizeJsonByLogVo(LogVo().apply { requestDesensitizePropertyNames = emptySet() }, raw))
    }

    @Test
    fun desensitizeJsonByLogVo_nonJsonBody_returnsRawUntouched() {
        val logVo = LogVo().apply { requestDesensitizePropertyNames = setOf("phone") }

        // Form-encoded body
        assertEquals("phone=13800001234&name=alice",
            AuditLogTool.desensitizeJsonByLogVo(logVo, "phone=13800001234&name=alice"))
        // JSON array, not object
        assertEquals("""[{"phone":"x"}]""",
            AuditLogTool.desensitizeJsonByLogVo(logVo, """[{"phone":"x"}]"""))
        // Blank
        assertEquals("", AuditLogTool.desensitizeJsonByLogVo(logVo, ""))
        // Plain text
        assertEquals("not json", AuditLogTool.desensitizeJsonByLogVo(logVo, "not json"))
    }

    @Test
    fun desensitizeJsonByLogVo_missingKeyOrNullValue_leavesOthersAlone() {
        val logVo = LogVo().apply { requestDesensitizePropertyNames = setOf("phone", "missingKey") }

        val out = AuditLogTool.desensitizeJsonByLogVo(
            logVo,
            """{"phone":"13800001234","other":"keep"}""",
        )

        assertEquals("1****234", extractJsonString(out, "phone"), "configured key with a real value is masked")
        assertEquals("keep", extractJsonString(out, "other"), "non-listed key is preserved")
        assertTrue("missingKey" !in out, "configured-but-absent key must not be synthesized")
    }

    @Test
    fun desensitizeJsonByLogVo_jsonNullValue_isPreserved() {
        val logVo = LogVo().apply { requestDesensitizePropertyNames = setOf("phone") }

        val out = AuditLogTool.desensitizeJsonByLogVo(logVo, """{"phone":null,"name":"alice"}""")

        // null stays null — there's nothing to mask, and replacing null with "****" would change
        // downstream semantics (caller may distinguish "explicit null" from "redacted").
        assertTrue("\"phone\":null" in out, "JSON null values must be preserved verbatim, not turned into '****'")
        assertEquals("alice", extractJsonString(out, "name"))
    }

    @Test
    fun desensitizeJsonByLogVo_malformedJson_fallsBackToRaw() {
        val logVo = LogVo().apply { requestDesensitizePropertyNames = setOf("phone") }
        val malformed = """{"phone":"13800001234""" // missing closing brace + quote

        val out = AuditLogTool.desensitizeJsonByLogVo(logVo, malformed)

        // A masking failure on a malformed body must never block audit submit — return raw,
        // operators will see the unparseable content in the audit trail.
        assertEquals(malformed, out)
    }

    // ----- Fixtures -----

    /** Top-level request DTO with two `@LogDesensitize` properties. */
    private data class BasicRequest(
        val username: String,
        @LogDesensitize
        val phone: String,
        @LogDesensitize
        val idCard: String,
    )

    /** Base DTO carrying common sensitive fields. */
    private open class BaseRequest(
        @LogDesensitize
        open val phone: String,
        @LogDesensitize
        open val idCard: String,
    )

    /** Subclass DTO — annotations on inherited fields must still be picked up. */
    private class ExtendedRequest(
        phone: String,
        idCard: String,
        val remark: String,
    ) : BaseRequest(phone, idCard)

    /** Has a static (companion) field annotated; the scanner must ignore it. */
    private class WithStaticField {
        val plain: String = "x"

        companion object {
            @LogDesensitize
            const val STATIC_FIELD: String = "should-not-be-collected"
        }
    }

    /** Minimal [JoinPoint] stub — only [getArgs] is exercised by the scanner. */
    private class StubJoinPoint(private val args: Array<Any?>) : JoinPoint {
        override fun getArgs(): Array<Any?> = args
        override fun getKind(): String = "stub"
        override fun getSignature(): Signature? = null
        override fun getSourceLocation(): org.aspectj.lang.reflect.SourceLocation? = null
        override fun getThis(): Any? = null
        override fun getTarget(): Any? = null
        override fun getStaticPart(): JoinPoint.StaticPart? = null
        override fun toLongString(): String = "StubJoinPoint"
        override fun toShortString(): String = "StubJoinPoint"
    }

    /** Reads a string value from a flat JSON object — keeps assertions independent of property ordering. */
    private fun extractJsonString(json: String, key: String): String? {
        val element = kotlinx.serialization.json.Json.parseToJsonElement(json) as? kotlinx.serialization.json.JsonObject
        val value = element?.get(key) as? kotlinx.serialization.json.JsonPrimitive
        assertNotNull(element)
        return value?.content
    }
}
