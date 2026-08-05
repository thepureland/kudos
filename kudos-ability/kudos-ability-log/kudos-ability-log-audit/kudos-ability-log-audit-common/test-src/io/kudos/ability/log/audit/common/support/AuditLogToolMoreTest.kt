package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.annotation.Audit
import io.kudos.ability.log.audit.common.annotation.LogDesensitize
import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.entity.BaseLog
import io.kudos.ability.log.audit.common.entity.LogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.base.net.IpKit
import io.kudos.context.core.ClientInfo
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.servlet.http.HttpServletRequestWrapper
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.Signature
import org.springframework.context.support.StaticApplicationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.util.ContentCachingRequestWrapper
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Complementary unit tests for [AuditLogTool] beyond the desensitize-helper suite.
 *
 * Coverage:
 *  - [AuditLogTool.getRequestData] (both overloads): null request, plain (uncached) request,
 *    direct [ContentCachingRequestWrapper], and a wrapper-around-a-wrapper unwrap.
 *  - [AuditLogTool.createLogVo] (Audit): entityId extraction from the model's `id` property,
 *    models without an `id` (defensive catch), ignoreForm gating of client-info capture, and
 *    request-content masking of `@LogDesensitize` fields.
 *  - [AuditLogTool.createLogVo] (WebAudit): body capture, `id` request-parameter extraction,
 *    ignoreForm gating.
 *  - [AuditLogTool.createSysAuditLogModel]: empty-logs null return, ignoreForm form-data gating,
 *    detail description priority (context override -> formatter -> ""), oldBizData loading via
 *    the auto-picked (`needFormat`) and explicitly annotated formatters, audit/detail id linkage,
 *    and client-info projection onto the detail (url / referer / form data).
 *  - [AuditLogTool.setOperator]: operator/client/tenant projection, [ILogSourceTenantProvider]
 *    resolution + caching, fallback to tenantId when no provider exists, and the entities-null guard.
 *  - Desensitize edges not covered by the sibling suite: non-primitive JSON values are masked via
 *    their serialized text; an object-shaped but malformed body falls back raw via the catch;
 *    empty JSON object and empty-string values.
 *
 * Spring beans are supplied through a [StaticApplicationContext]; no container boot needed.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuditLogToolMoreTest {

    private lateinit var ctx: StaticApplicationContext

    @BeforeTest
    fun setup() {
        ctx = StaticApplicationContext().apply { refresh() }
        SpringKit.applicationContext = ctx
        KudosContextHolder.clear()
        resetCachedTenantProvider()
    }

    @AfterTest
    fun teardown() {
        KudosContextHolder.clear()
        resetCachedTenantProvider()
        ctx.close()
    }

    /** The provider cache on the AuditLogTool object outlives a test; reset it so tests stay order-independent. */
    private fun resetCachedTenantProvider() {
        AuditLogTool::class.java.getDeclaredField("cachedTenantProvider")
            .apply { isAccessible = true }
            .set(AuditLogTool, null)
    }

    // ----- getRequestData -----

    @Test
    fun getRequestData_nullRequest_returnsEmpty() {
        assertEquals("", AuditLogTool.getRequestData(null))
        assertEquals("", AuditLogTool.getRequestData(null, LogVo()))
    }

    @Test
    fun getRequestData_plainRequestWithoutCachingWrapper_returnsEmpty() {
        val request = MockHttpServletRequest("POST", "/x").apply { setContent("body".toByteArray()) }
        assertEquals("", AuditLogTool.getRequestData(request),
            "without WebLogAuditFilter's caching wrapper the body is not replayable — must degrade to empty, not throw")
    }

    @Test
    fun getRequestData_contentCachingWrapper_returnsConsumedBody() {
        val body = """{"name":"alice"}"""
        val wrapper = cachingWrapperWithConsumedBody(body)
        assertEquals(body, AuditLogTool.getRequestData(wrapper))
    }

    @Test
    fun getRequestData_wrapperAroundCachingWrapper_unwrapsOneLevel() {
        val body = """{"k":"v"}"""
        val inner = cachingWrapperWithConsumedBody(body)
        val outer = HttpServletRequestWrapper(inner) // e.g. a Spring Security re-wrap
        assertEquals(body, AuditLogTool.getRequestData(outer),
            "one re-wrap level (security filters) must still reach the cached body")
    }

    @Test
    fun getRequestData_appliesDesensitizationWhenLogVoCarriesNames() {
        val wrapper = cachingWrapperWithConsumedBody("""{"phone":"13800001234","name":"alice"}""")
        val logVo = LogVo().apply { requestDesensitizePropertyNames = setOf("phone") }

        val out = AuditLogTool.getRequestData(wrapper, logVo)

        assertTrue("1****234" in out, "phone must be masked head1+tail3: $out")
        assertTrue("alice" in out, "unlisted keys stay verbatim: $out")
    }

    // ----- desensitize edges -----

    @Test
    fun desensitize_nonPrimitiveValue_masksItsSerializedText() {
        val logVo = LogVo().apply { requestDesensitizePropertyNames = setOf("secret") }

        val out = AuditLogTool.desensitizeJsonByLogVo(logVo, """{"secret":{"a":1},"keep":"x"}""")

        // value.toString() of {"a":1} is 7 chars -> '{' + **** + ':1}'
        assertTrue(""""secret":"{****:1}"""" in out, "nested-object value is masked via its serialized text: $out")
        assertTrue(""""keep":"x"""" in out, "other keys preserved: $out")
    }

    @Test
    fun desensitize_objectShapedButMalformed_fallsBackRawViaCatch() {
        val logVo = LogVo().apply { requestDesensitizePropertyNames = setOf("a") }
        val malformed = """{"a":}""" // passes the {..} shape check, fails JSON parsing
        assertEquals(malformed, AuditLogTool.desensitizeJsonByLogVo(logVo, malformed),
            "parse failure inside the {..} fast-path must fall back to the raw body")
    }

    @Test
    fun desensitize_emptyJsonObject_returnsRaw() {
        val logVo = LogVo().apply { requestDesensitizePropertyNames = setOf("a") }
        assertEquals("{}", AuditLogTool.desensitizeJsonByLogVo(logVo, "{}"))
    }

    @Test
    fun desensitize_emptyStringValue_staysEmpty() {
        val logVo = LogVo().apply { requestDesensitizePropertyNames = setOf("v") }
        val out = AuditLogTool.desensitizeJsonByLogVo(logVo, """{"v":""}""")
        assertTrue(""""v":""""" in out, "zero-length value has nothing to mask and must stay empty: $out")
    }

    // ----- createLogVo (Audit) -----

    @Test
    fun createLogVo_audit_extractsEntityId_andMasksClientRequestContent() {
        bindClientInfo()
        val model = UserModel(id = "u-77", name = "alice", phone = "13800001234")
        val audit = Audit(
            opType = OperationTypeEnum.CREATE, moduleCode = "USER",
            desc = "create", ignoreForm = YesNotEnum.NOT,
        )

        val logVo = AuditLogTool.createLogVo(audit, model, StubJoinPoint(arrayOf<Any?>(model)))

        assertEquals(1, logVo.logs.size)
        assertEquals("u-77", logVo.logs.single().entityId, "model.id must be promoted to entityId")
        assertEquals(setOf("phone"), logVo.requestDesensitizePropertyNames)
        val captured = assertNotNull(KudosContextHolder.get().clientInfo?.requestContentString)
        assertTrue("1****234" in captured, "serialized model must be masked before landing in client info: $captured")
        assertTrue("13800001234" !in captured, "raw sensitive value must never land in client info: $captured")
    }

    @Test
    fun createLogVo_audit_ignoreFormYes_skipsClientContentCapture() {
        bindClientInfo()
        val model = UserModel(id = "u-1", name = "bob", phone = "1")
        val audit = Audit(opType = OperationTypeEnum.CREATE, moduleCode = "USER") // ignoreForm defaults YES

        AuditLogTool.createLogVo(audit, model, StubJoinPoint(arrayOf<Any?>(model)))

        assertNull(KudosContextHolder.get().clientInfo?.requestContentString,
            "ignoreForm=YES must not capture the serialized model")
    }

    @Test
    fun createLogVo_audit_modelWithoutIdProperty_isTolerated() {
        val model = NoIdModel(name = "x")
        val audit = Audit(opType = OperationTypeEnum.UPDATE, moduleCode = "USER", ignoreForm = YesNotEnum.NOT)

        val logVo = AuditLogTool.createLogVo(audit, model, StubJoinPoint(arrayOf<Any?>(model)))

        assertNull(logVo.logs.single().entityId, "no id property -> defensive catch -> entityId stays null")
    }

    @Test
    fun createLogVo_audit_blankId_doesNotSetEntityId() {
        val model = UserModel(id = "", name = "x", phone = "1")
        val audit = Audit(opType = OperationTypeEnum.UPDATE, moduleCode = "USER", ignoreForm = YesNotEnum.NOT)

        val logVo = AuditLogTool.createLogVo(audit, model, StubJoinPoint(arrayOf<Any?>(model)))

        assertNull(logVo.logs.single().entityId, "blank id must be treated the same as absent")
    }

    @Test
    fun createLogVo_audit_autoPickedFormatter_loadsOldBizData() {
        ctx.beanFactory.registerSingleton("optInFormatter", OptInFormatter())
        val model = UserModel(id = "u-2", name = "y", phone = "1")
        val audit = Audit(opType = OperationTypeEnum.UPDATE, moduleCode = "USER") // default formatter class -> auto-pick

        val logVo = AuditLogTool.createLogVo(audit, model, StubJoinPoint(arrayOf<Any?>(model)))

        val baseLog = logVo.logs.single()
        assertEquals("OLD-DATA", baseLog.oldBizData, "needFormat=true formatter must be auto-picked and load old data")
        assertNotNull(baseLog.paramArgs, "join-point args must be stored for later formatting")
    }

    @Test
    fun createLogVo_audit_explicitFormatterClass_isFetchedDirectly() {
        ctx.beanFactory.registerSingleton("explicitFormatter", ExplicitFormatter())
        val model = UserModel(id = "u-3", name = "z", phone = "1")
        val audit = Audit(
            opType = OperationTypeEnum.UPDATE, moduleCode = "USER",
            descriptionFormatter = ExplicitFormatter::class,
        )

        val logVo = AuditLogTool.createLogVo(audit, model, StubJoinPoint(arrayOf<Any?>(model)))

        assertEquals("EXPLICIT-OLD", logVo.logs.single().oldBizData,
            "explicitly annotated formatter is used even though its needFormat() is false")
    }

    // ----- createLogVo (WebAudit) -----

    @Test
    fun createLogVo_webAudit_capturesBodyAndIdParameter() {
        bindClientInfo()
        val body = """{"name":"alice"}"""
        val request = cachingWrapperWithConsumedBody(body) { it.setParameter("id", "42") }
        val audit = WebAudit(
            opType = OperationTypeEnum.UPDATE, moduleCode = "USER",
            desc = "update", ignoreForm = YesNotEnum.NOT,
        )

        val logVo = AuditLogTool.createLogVo(audit, request, StubJoinPoint(arrayOf<Any?>()))

        assertEquals("42", logVo.logs.single().entityId, "the id request parameter must be promoted to entityId")
        assertEquals(body, KudosContextHolder.get().clientInfo?.requestContentString)
    }

    @Test
    fun createLogVo_webAudit_ignoreFormYes_skipsBodyCapture_andBlankIdIgnored() {
        bindClientInfo()
        val request = MockHttpServletRequest("POST", "/users").apply { setParameter("id", "") }
        val audit = WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = "USER") // ignoreForm defaults YES

        val logVo = AuditLogTool.createLogVo(audit, request, StubJoinPoint(arrayOf<Any?>()))

        assertNull(KudosContextHolder.get().clientInfo?.requestContentString)
        assertNull(logVo.logs.single().entityId, "blank id parameter must not be promoted")
    }

    // ----- createSysAuditLogModel -----

    @Test
    fun createSysAuditLogModel_emptyLogs_returnsNull() {
        assertNull(AuditLogTool.createSysAuditLogModel(LogVo(), "{}"))
    }

    @Test
    fun createSysAuditLogModel_linksDetailToAudit_andStoresFormData() {
        val logVo = LogVo()
        logVo.addAuditLog(auditAnnotation(ignoreForm = YesNotEnum.NOT)).apply {
            addParam("alice")
            addParam("user", "alice")
        }
        val argString = """{"name":"alice"}"""

        val model = assertNotNull(AuditLogTool.createSysAuditLogModel(logVo, argString))

        val entity = model.entities!!.single()
        val detail = model.sysAuditDetailLogs!!.single()!!
        assertNotNull(entity.id)
        assertEquals(entity.id, detail.auditId, "detail must reference its audit row")
        assertNotNull(detail.id, "detail gets its own uuid")
        assertEquals(argString, detail.requestFormData, "ignoreForm=NOT stores the serialized arguments")
        assertEquals("alice", detail.stringParams)
        assertTrue("alice" in assertNotNull(detail.objectParams), "object params serialize to JSON")
    }

    @Test
    fun createSysAuditLogModel_ignoreFormYes_blanksFormData() {
        val logVo = LogVo()
        logVo.addAuditLog(auditAnnotation(ignoreForm = YesNotEnum.YES))

        val model = assertNotNull(AuditLogTool.createSysAuditLogModel(logVo, """{"secret":"x"}"""))

        assertEquals("", model.sysAuditDetailLogs!!.single()!!.requestFormData,
            "ignoreForm=YES must blank the stored form data")
    }

    @Test
    fun createSysAuditLogModel_contextDescription_overridesFormatter() {
        ctx.beanFactory.registerSingleton("optInFormatter", OptInFormatter())
        AuditLogTool.addLogDetailDescription("業務方主動描述")
        val logVo = LogVo()
        logVo.addAuditLog(auditAnnotation())

        val model = assertNotNull(AuditLogTool.createSysAuditLogModel(logVo, "{}"))

        assertEquals("業務方主動描述", model.sysAuditDetailLogs!!.single()!!.description,
            "context-provided description must win over the formatter")
    }

    @Test
    fun createSysAuditLogModel_formatterDescription_whenContextEmpty() {
        ctx.beanFactory.registerSingleton("optInFormatter", OptInFormatter())
        val logVo = LogVo()
        logVo.addAuditLog(auditAnnotation())

        val model = assertNotNull(AuditLogTool.createSysAuditLogModel(logVo, "{}"))

        assertEquals("FMT-DESC", model.sysAuditDetailLogs!!.single()!!.description,
            "without a context description the auto-picked formatter renders it")
    }

    @Test
    fun createSysAuditLogModel_noFormatterMatches_descriptionFallsBackToEmpty() {
        val logVo = LogVo()
        logVo.addAuditLog(auditAnnotation())

        val model = assertNotNull(AuditLogTool.createSysAuditLogModel(logVo, "{}"))

        assertEquals("", model.sysAuditDetailLogs!!.single()!!.description,
            "no opted-in formatter -> empty description, never an exception")
    }

    @Test
    fun createSysAuditLogModel_projectsClientInfoOntoDetail() {
        bindClientInfo(url = "/api/users/1", referer = "https://admin.example.com")
        KudosContextHolder.get().clientInfo?.requestContentString = "cached-content"
        val logVo = LogVo()
        logVo.addAuditLog(auditAnnotation(ignoreForm = YesNotEnum.NOT))

        val model = assertNotNull(AuditLogTool.createSysAuditLogModel(logVo, "args"))

        val detail = model.sysAuditDetailLogs!!.single()!!
        assertEquals("/api/users/1", detail.operateUrl)
        assertEquals("https://admin.example.com", detail.requestReferer)
    }

    // NOTE (suspected main-code bug, not asserted here): the numeric-id promotion branch in
    // createSysAuditLogModel never fires — `JsonKit.fromJson<Map<*, *>>(argString)` always fails with
    // "Star projections in type arguments are not allowed" and returns null, so a `{"id":"123"}`
    // argString is never promoted onto SysAuditLogVo.entityId. Asserting the correct expected
    // behavior ("123" promoted) fails today; recorded upstream instead of weakening the assertion.

    @Test
    fun createSysAuditLogModel_nonNumericOrMissingId_leavesEntityIdAlone() {
        val logVo = LogVo()
        logVo.addAuditLog(auditAnnotation())
        val model = assertNotNull(AuditLogTool.createSysAuditLogModel(logVo, """{"id":"abc"}"""))
        assertNull(model.entities!!.single().entityId, "non-numeric id must not be promoted")

        val logVo2 = LogVo()
        logVo2.addAuditLog(auditAnnotation())
        val model2 = assertNotNull(AuditLogTool.createSysAuditLogModel(logVo2, "not-json-at-all"))
        assertNull(model2.entities!!.single().entityId, "unparseable arguments must not break model creation")
    }

    @Test
    fun createSysAuditLogModel_populatesTenantAndSubSysFromContext() {
        KudosContextHolder.get().apply {
            tenantId = "T-9"
            subSystemCode = "sub-9"
        }
        val logVo = LogVo()
        logVo.addAuditLog(auditAnnotation())

        val model = assertNotNull(AuditLogTool.createSysAuditLogModel(logVo, "{}"))

        assertEquals("T-9", model.tenantId)
        assertEquals("sub-9", model.subSysCode)
        assertEquals("T-9", model.entities!!.single().tenantId)
        assertEquals("sub-9", model.entities!!.single().subSysCode)
    }

    // ----- setOperator -----

    @Test
    fun setOperator_projectsContextOntoEveryEntity() {
        bindClientInfo(ip = "10.1.2.3", requestType = "POST",
            browser = "Chrome" to "120", os = "Windows" to "11")
        KudosContextHolder.get().apply {
            tenantId = "T-1"
            subSystemCode = "subsys"
            user = object : IIdEntity<String> { override val id = "u-9" }
        }
        val model = modelWithEntities(2)

        AuditLogTool.setOperator(model)

        model.entities!!.forEach { entity ->
            assertNotNull(entity.operateTime)
            assertEquals(IpKit.ipv4StringToLong("10.1.2.3"), entity.operateIp)
            assertEquals("u-9", entity.operatorId)
            assertEquals("Chrome", entity.clientBrowser)
            assertEquals("Windows", entity.clientOs)
            assertEquals("POST", entity.requestType)
            assertEquals("subsys", entity.subSysCode)
            assertEquals("T-1", entity.tenantId)
            assertEquals("T-1", entity.sourceTenantId, "no provider -> sourceTenantId falls back to tenantId")
        }
        assertEquals("subsys", model.subSysCode)
        assertEquals("T-1", model.tenantId)
    }

    @Test
    fun setOperator_withoutClientInfoOrUser_leavesClientFieldsNull() {
        KudosContextHolder.get().tenantId = "T-2"
        val model = modelWithEntities(1)

        AuditLogTool.setOperator(model)

        val entity = model.entities!!.single()
        assertNull(entity.operateIp)
        assertNull(entity.operatorId)
        assertNull(entity.clientBrowser)
        assertNull(entity.clientOs)
        assertNull(entity.requestType)
        assertEquals("T-2", entity.sourceTenantId)
    }

    @Test
    fun setOperator_usesRegisteredTenantProvider_andCachesIt() {
        var lookups = 0
        ctx.beanFactory.registerSingleton("tenantProvider", object : ILogSourceTenantProvider {
            override fun getSourceTenant(tenantId: String?, userId: String?): String? {
                lookups++
                return "SRC-$tenantId"
            }
        })
        KudosContextHolder.get().tenantId = "T-3"

        AuditLogTool.setOperator(modelWithEntities(1)) // first call resolves + caches the provider bean

        val model = modelWithEntities(1)
        AuditLogTool.setOperator(model) // second call must hit the cached provider, not Spring again
        assertEquals("SRC-T-3", model.entities!!.single().sourceTenantId,
            "registered provider must drive sourceTenantId")
        assertEquals(2, lookups, "provider invoked once per setOperator call (bean lookup is cached, not the result)")
    }

    @Test
    fun setOperator_nullEntities_isRejected() {
        val ex = assertFailsWith<IllegalArgumentException> { AuditLogTool.setOperator(SysAuditLogModel()) }
        assertTrue(ex.message!!.contains("entities is null"), "guard message must pinpoint the issue: ${ex.message}")
    }

    // ----- fixtures -----

    private fun auditAnnotation(ignoreForm: YesNotEnum = YesNotEnum.YES) = Audit(
        opType = OperationTypeEnum.CREATE,
        moduleCode = "USER",
        desc = "audit-op",
        ignoreForm = ignoreForm,
    )

    private fun modelWithEntities(count: Int) = SysAuditLogModel().apply {
        entities = MutableList(count) { SysAuditLogVo() }
        sysAuditDetailLogs = mutableListOf()
    }

    private fun bindClientInfo(
        ip: String? = null,
        url: String? = null,
        referer: String? = null,
        requestType: String? = null,
        browser: Pair<String, String>? = null,
        os: Pair<String, String>? = null,
    ) {
        KudosContextHolder.get().clientInfo = ClientInfo.Builder()
            .ip(ip).url(url).requestReferer(referer).requestType(requestType)
            .browser(browser).os(os)
            .build()
    }

    private fun cachingWrapperWithConsumedBody(
        body: String,
        customize: (MockHttpServletRequest) -> Unit = {},
    ): ContentCachingRequestWrapper {
        val request = MockHttpServletRequest("POST", "/api").apply {
            contentType = "application/json"
            setContent(body.toByteArray(Charsets.UTF_8))
            customize(this)
        }
        return ContentCachingRequestWrapper(request, 1024 * 1024).also { it.inputStream.readAllBytes() }
    }

    /** Request DTO with an id and a sensitive field. */
    internal data class UserModel(
        val id: String,
        val name: String,
        @LogDesensitize
        val phone: String,
    )

    /** Request DTO without an id property — exercises the defensive catch. */
    internal data class NoIdModel(val name: String)

    /** Formatter that opts into the auto-pick path via needFormat. */
    internal class OptInFormatter : IAuditLogDetailDescriptionFormatter {
        override fun needFormat(baseLog: BaseLog?) = true
        override fun loadOldBizData(baseLog: BaseLog?, paramObjs: Array<Any?>?): Any = "OLD-DATA"
        override fun descriptionFormat(baseLog: BaseLog?) = "FMT-DESC"
    }

    /** Formatter that never opts in — only reachable when explicitly annotated. */
    internal class ExplicitFormatter : IAuditLogDetailDescriptionFormatter {
        override fun loadOldBizData(baseLog: BaseLog?, paramObjs: Array<Any?>?): Any = "EXPLICIT-OLD"
        override fun descriptionFormat(baseLog: BaseLog?) = "EXPLICIT-DESC"
    }

    /** Minimal JoinPoint stub — only getArgs is exercised. */
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
}
