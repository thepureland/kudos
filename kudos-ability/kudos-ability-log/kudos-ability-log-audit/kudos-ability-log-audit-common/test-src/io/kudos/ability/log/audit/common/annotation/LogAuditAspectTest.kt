package io.kudos.ability.log.audit.common.annotation

import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.common.support.LogAuditContext
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Spring AOP integration tests for [LogAuditAspect].
 *
 * Verifies the round-2 fixes:
 *  - **`@AfterReturning`** submits the audit on normal return; description does not carry the `[FAILED:..]` prefix
 *  - **`@AfterThrowing`** also submits the audit when the business method throws, and the description carries the
 *    `[FAILED:..]` prefix
 *  - Both paths call [LogAuditContext.clear] in finally to avoid thread-pool ThreadLocal leaks
 *
 * Uses [RecordingAuditService] in place of real RDB/MQ persistence — it merely records each submit's model into a
 * list, which makes test assertions more direct than using a mock framework.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest(properties = ["spring.flyway.enabled=false"])
@Import(LogAuditAspectTest.TestBeans::class, AuditedService::class)
class LogAuditAspectTest @Autowired constructor(
    private val service: AuditedService,
    private val recorder: RecordingAuditService,
) {

    @org.junit.jupiter.api.BeforeEach
    fun reset() {
        recorder.clear()
        LogAuditContext.clear()
    }

    @Test
    fun success_submitsAuditWithoutFailedPrefix() {
        service.create(Model("alice"))

        val submitted = recorder.captured
        assertEquals(1, submitted.size, "Success path should submit once")
        val firstEntityDescription = submitted.single().entities?.firstOrNull()?.description ?: ""
        assertTrue(!firstEntityDescription.startsWith("[FAILED:"), "Success path description should not carry the FAILED prefix: $firstEntityDescription")
    }

    @Test
    fun failure_submitsAuditWithFailedPrefix_andRethrows() {
        val ex = assertFails { service.fail(Model("bob")) }
        assertEquals("boom", ex.message, "Original exception should be rethrown, not swallowed by the aspect")

        val submitted = recorder.captured
        assertEquals(1, submitted.size, "Failure path should still submit once (so the business side records the failed action)")
        val description = submitted.single().entities?.firstOrNull()?.description
        assertNotNull(description)
        assertTrue(description.startsWith("[FAILED:IllegalStateException:boom]"), "Failure prefix should include exception type + message: $description")
    }

    @Test
    fun success_clearsContext_afterReturn() {
        service.create(Model("c"))
        // The aspect calls LogAuditContext.clear() in finally — afterwards a read-only probe should see null
        assertNull(LogAuditContext.getOrNull(), "Context should be cleared after the success path completes")
    }

    @Test
    fun failure_clearsContext_afterThrow() {
        assertFails { service.fail(Model("d")) }
        assertNull(LogAuditContext.getOrNull(), "Context should be cleared after the failure path completes")
    }

    @Test
    fun modelArgIndex_picksConfiguredArgInsteadOfFirst() {
        // updateForTenant(tenantId, user) — model is not at args[0], should be corrected by modelArgIndex=1 in the annotation
        service.updateForTenant("tenant-T", Model("specified"))

        val submitted = recorder.captured
        assertEquals(1, submitted.size)
        // The aspect serializes Model into requestFormData — seeing name=specified proves the index took effect
        val detail = submitted.single().sysAuditDetailLogs?.firstOrNull()
        assertNotNull(detail)
        assertTrue(
            detail.requestFormData?.contains("specified") == true,
            "modelArgIndex=1 should cause the aspect to pick args[1] (Model) instead of args[0] (String tenantId). Actual requestFormData=${detail.requestFormData}"
        )
    }

    @Test
    fun modelArgIndex_outOfBounds_fallsBackToFirstArg() {
        // updateBadIndex points at an out-of-bounds modelArgIndex; should fall back to args[0] rather than throw IndexOutOfBoundsException
        service.updateBadIndex(Model("fallback"))
        assertEquals(1, recorder.captured.size, "Out-of-bounds modelArgIndex should silently fall back to args[0]")
    }

    @TestConfiguration
    open class TestBeans {
        @Bean
        @Primary
        open fun recordingAuditService(): RecordingAuditService = RecordingAuditService()
    }
}

/**
 * Business-side service with [Audit] annotations — must be a Spring bean for the aspect to intercept it.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class AuditedService {

    @Audit(opType = OperationTypeEnum.CREATE, moduleCode = "USER", desc = "create user", ignoreForm = YesNotEnum.NOT)
    open fun create(model: Model) {
        // no-op
    }

    @Audit(opType = OperationTypeEnum.UPDATE, moduleCode = "USER", desc = "fail user", ignoreForm = YesNotEnum.NOT)
    open fun fail(model: Model) {
        error("boom")
    }

    @Audit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = "USER",
        desc = "update for tenant",
        ignoreForm = YesNotEnum.NOT,
        modelArgIndex = 1,
    )
    open fun updateForTenant(tenantId: String, user: Model) {
        // The business method signature does not have model at the first position — the annotation explicitly indicates args[1]
    }

    @Audit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = "USER",
        desc = "bad index",
        ignoreForm = YesNotEnum.NOT,
        modelArgIndex = 99,
    )
    open fun updateBadIndex(model: Model) {
        // Out of bounds — the aspect should fall back to args[0] and not throw
    }
}

/**
 * Business model used for tests.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class Model(val name: String)

/**
 * Records the [SysAuditLogModel] of every `submit` call for test assertions.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class RecordingAuditService : IAuditService {
    val captured: MutableList<SysAuditLogModel> = Collections.synchronizedList(mutableListOf())
    override fun submit(sysAuditLogVo: SysAuditLogModel): Boolean {
        captured.add(sysAuditLogVo)
        return true
    }
    fun clear() = captured.clear()
}
