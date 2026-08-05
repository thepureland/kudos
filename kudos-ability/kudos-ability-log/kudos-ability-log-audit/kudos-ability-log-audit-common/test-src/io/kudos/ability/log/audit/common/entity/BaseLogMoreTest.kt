package io.kudos.ability.log.audit.common.entity

import io.kudos.ability.log.audit.common.annotation.Audit
import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.log.audit.common.support.DefaultAuditLogDetailDescriptionFormatter
import io.kudos.ability.log.audit.common.support.ISysAuditModule
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import org.springframework.context.support.StaticApplicationContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Complementary unit tests for [BaseLog] beyond the stringParams / initModule suites.
 *
 * Coverage:
 *  - [WebAudit]-based constructor maps subsysCode / desc+opTypeExt / ignoreForm / formatter class.
 *  - Blank subsysCode falls back to `KudosContextHolder.subSystemCode` during module resolution.
 *  - [BaseLog.getOpType] / [BaseLog.setOpType] round-trip.
 *  - [BaseLog.getObjectParams]: null when empty, JSON projection when populated; `addParam(name, value)` and
 *    `addParam(LogParamVo)` accumulate into the same map; LogParamVo without a name is rejected.
 *  - [BaseLog.toSysLogVo] happy path maps every field; the defensive catch (opType nulled via reflection,
 *    which cannot happen through the public API) returns a partially filled VO instead of throwing.
 *  - [BaseLog.escapeSegment] null/empty edges; [BaseLog.splitStringParams] trailing lone-backslash edge.
 *  - Plain property holders ([BaseLog.entityId] / [BaseLog.entityUserId] / [BaseLog.oldBizData] /
 *    [BaseLog.paramArgs]) round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class BaseLogMoreTest {

    private lateinit var ctx: StaticApplicationContext

    @BeforeTest
    fun setup() {
        ctx = StaticApplicationContext().apply { refresh() }
        SpringKit.applicationContext = ctx
        KudosContextHolder.clear()
    }

    @AfterTest
    fun teardown() {
        KudosContextHolder.clear()
        ctx.close()
    }

    @Test
    fun webAuditConstructor_mapsAnnotationMetadata() {
        val audit = WebAudit(
            opType = OperationTypeEnum.LOGIN,
            subsysCode = "portal",
            moduleCode = "AUTH",
            desc = "user login",
            ignoreForm = YesNotEnum.NOT,
            opTypeExt = "-sso",
        )

        val log = BaseLog(audit)

        assertEquals("AUTH", log.moduleCode)
        assertEquals(OperationTypeEnum.LOGIN, log.getOpType())
        assertEquals("user login-sso", log.description, "desc and opTypeExt must be concatenated")
        assertEquals(false, log.ignoreForm, "YesNotEnum.NOT means 'do not ignore form' => false")
        assertEquals(DefaultAuditLogDetailDescriptionFormatter::class, log.descriptionFormatterClass)
    }

    @Test
    fun blankSubsysCode_fallsBackToContextSubSystemCode() {
        val seenSysCodes = mutableListOf<String?>()
        ctx.beanFactory.registerSingleton("capturing", object : ISysAuditModule {
            override fun module(subsysCode: String?, moduleCode: String?): Pair<Int?, String?> {
                seenSysCodes += subsysCode
                return 1 to "m"
            }
        })
        KudosContextHolder.get().subSystemCode = "ctx-subsys"

        BaseLog(Audit(opType = OperationTypeEnum.CREATE, moduleCode = "M")) // subSysCode defaults to ""

        assertEquals(listOf<String?>("ctx-subsys"), seenSysCodes,
            "blank annotation subsysCode must fall back to KudosContextHolder.subSystemCode")
    }

    @Test
    fun opType_setterGetterRoundTrip() {
        val log = newBaseLog()
        assertEquals(OperationTypeEnum.CREATE, log.getOpType())
        log.setOpType(OperationTypeEnum.DELETE)
        assertEquals(OperationTypeEnum.DELETE, log.getOpType())
    }

    @Test
    fun getObjectParams_nullWhenEmpty_jsonWhenPopulated() {
        val log = newBaseLog()
        assertNull(log.getObjectParams(), "no object params -> null (storage writes NULL, not '{}')")

        log.addParam("user", "alice")
        log.addParam(LogParamVo("amount", 12.5))
        val json = log.getObjectParams()

        assertNotNull(json)
        assertTrue("user" in json && "alice" in json, "named param must serialize: $json")
        assertTrue("amount" in json, "LogParamVo param must serialize into the same map: $json")
    }

    @Test
    fun addParam_returnsSelfForChaining() {
        val log = newBaseLog()
        assertSame(log, log.addParam("p1"))
        assertSame(log, log.addParam("k", "v"))
        assertSame(log, log.addParam(LogParamVo("n", 1)))
    }

    @Test
    fun addParam_logParamVoWithoutName_isRejected() {
        val log = newBaseLog()
        val ex = assertFailsWith<IllegalArgumentException> { log.addParam(LogParamVo(null, "v")) }
        assertTrue(ex.message!!.contains("LogParamVo.name is null"), "message should pinpoint the missing name: ${ex.message}")
    }

    @Test
    fun toSysLogVo_mapsAllFields() {
        val log = newBaseLog().apply {
            entityId = "e-7"
            moduleName = "用戶"
            moduleId = 3
        }

        val vo = log.toSysLogVo()

        assertEquals("e-7", vo.entityId)
        assertEquals("用戶", vo.moduleName)
        assertEquals("TEST", vo.moduleCode)
        assertEquals(2, vo.operateTypeId, "CREATE code '2' must convert to Int 2")
        assertEquals("create", vo.operateType)
        assertEquals("create something", vo.description)
        assertEquals(3, vo.moduleId)
        assertNotNull(vo.id, "a fresh uuid must be assigned")
    }

    @Test
    fun toSysLogVo_defensiveCatch_returnsPartialVoInsteadOfThrowing() {
        // opType can never be null through the public API (constructors and setOpType both demand it);
        // null it via reflection to prove the defensive catch swallows and returns a partial VO.
        val log = newBaseLog().apply { entityId = "e-8" }
        BaseLog::class.java.getDeclaredField("opType").apply { isAccessible = true }.set(log, null)

        val vo = log.toSysLogVo()

        assertEquals("e-8", vo.entityId, "fields assigned before the failure point are kept")
        assertNull(vo.operateTypeId, "failure point: opType null -> requireNotNull throws -> caught")
        assertNull(vo.id, "uuid assignment sits after the failure point, so it is skipped")
    }

    @Test
    fun escapeSegment_nullAndEmpty_returnEmptyString() {
        assertEquals("", BaseLog.escapeSegment(null))
        assertEquals("", BaseLog.escapeSegment(""))
        assertEquals("plain", BaseLog.escapeSegment("plain"))
    }

    @Test
    fun splitStringParams_trailingLoneBackslash_keptVerbatim() {
        // A '\' at end-of-input has no follow-up char, so it cannot start an escape sequence —
        // the parser must keep it as a literal instead of dropping it or reading out of bounds.
        assertEquals(listOf("ab\\"), BaseLog.splitStringParams("ab\\"))
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun jvmStaticBridges_behaveLikeCompanionFunctions() {
        // @JvmStatic generates static bridge methods on BaseLog for Java callers; Kotlin call sites
        // go straight to the companion, so the bridges are exercised here via the Java entry point
        // to guarantee both routes share the same escape/parse semantics.
        val escape = BaseLog::class.java.getMethod("escapeSegment", String::class.java)
        assertEquals("a\\┼b", escape.invoke(null, "a┼b"))

        val split = BaseLog::class.java.getMethod("splitStringParams", String::class.java)
        assertEquals(listOf("a┼b", "c"), split.invoke(null, "a\\┼b┼c") as List<String>)
    }

    @Test
    fun plainPropertyHolders_roundTrip() {
        val args = arrayOf<Any?>("a", 1, null)
        val log = newBaseLog().apply {
            entityId = "e-1"
            entityUserId = 42
            oldBizData = mapOf("k" to "v")
            paramArgs = args
        }

        assertEquals("e-1", log.entityId)
        assertEquals(42, log.entityUserId)
        assertEquals(mapOf("k" to "v"), log.oldBizData)
        assertSame(args, log.paramArgs)
    }

    private fun newBaseLog(): BaseLog =
        BaseLog(Audit(opType = OperationTypeEnum.CREATE, moduleCode = "TEST", desc = "create something"))
}
