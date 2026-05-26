package io.kudos.ability.log.audit.common.annotation

import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.log.audit.common.support.LogAuditContext
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.Collections
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Spring AOP integration tests for [WebLogAuditAspect].
 *
 * Coverage:
 *  - Success path: `@AfterReturning` submit; description does not start with the FAILED prefix; context cleared.
 *  - Failure path: `@AfterThrowing` submit; description carries the `[FAILED:..]` prefix; original exception
 *    propagates; context cleared.
 *  - **Multipart requests are skipped entirely** — no `LogAuditContext` write, no submit.
 *
 * A [MockHttpServletRequest] is bound to the current thread via [RequestContextHolder] so the aspect
 * believes it is on a web-request path.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest(properties = ["spring.flyway.enabled=false"])
@Import(WebLogAuditAspectTest.TestBeans::class, WebAuditedController::class)
class WebLogAuditAspectTest @Autowired constructor(
    private val controller: WebAuditedController,
    private val recorder: WebRecordingAuditService,
) {

    @BeforeTest
    fun setup() {
        recorder.clear()
        LogAuditContext.clear()
    }

    @AfterTest
    fun teardown() {
        RequestContextHolder.resetRequestAttributes()
        LogAuditContext.clear()
    }

    @Test
    fun success_submitsAudit_noFailedPrefix() {
        bindRequest(MockHttpServletRequest("GET", "/users"))
        controller.list()
        assertEquals(1, recorder.captured.size)
        val description = recorder.captured.single().entities?.firstOrNull()?.description ?: ""
        assertTrue(!description.startsWith("[FAILED:"), "Success-path description should not start with the FAILED prefix: $description")
    }

    @Test
    fun failure_submitsAudit_withFailedPrefix_rethrows() {
        bindRequest(MockHttpServletRequest("POST", "/users"))
        val ex = assertFails { controller.broken() }
        assertEquals("boom-web", ex.message)
        assertEquals(1, recorder.captured.size)
        val description = recorder.captured.single().entities?.firstOrNull()?.description
        assertNotNull(description)
        assertTrue(description.startsWith("[FAILED:IllegalStateException:boom-web]"))
    }

    @Test
    fun multipart_skipsAudit_entirely() {
        val request = MockHttpServletRequest("POST", "/upload")
        request.contentType = "multipart/form-data; boundary=abc"
        bindRequest(request)

        controller.upload()

        // The aspect bypasses multipart entirely: no context write, no submit
        assertEquals(0, recorder.captured.size, "multipart should be skipped entirely")
        assertNull(LogAuditContext.getOrNull(), "multipart path should not pollute the context")
    }

    @Test
    fun success_clearsContext() {
        bindRequest(MockHttpServletRequest("GET", "/users"))
        controller.list()
        assertNull(LogAuditContext.getOrNull())
    }

    @Test
    fun failure_clearsContext() {
        bindRequest(MockHttpServletRequest("POST", "/users"))
        assertFails { controller.broken() }
        assertNull(LogAuditContext.getOrNull())
    }

    private fun bindRequest(request: MockHttpServletRequest) {
        val attrs = ServletRequestAttributes(request) as RequestAttributes
        RequestContextHolder.setRequestAttributes(attrs)
    }

    @TestConfiguration
    open class TestBeans {
        @Bean
        @Primary
        open fun webRecordingAuditService(): WebRecordingAuditService = WebRecordingAuditService()
    }
}

/**
 * Test web controller.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class WebAuditedController {
    @WebAudit(opType = OperationTypeEnum.QUERY, moduleCode = "USER", desc = "list users")
    open fun list(): String = "ok"

    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = "USER", desc = "broken")
    open fun broken(): Nothing = error("boom-web")

    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = "FILE", desc = "upload")
    open fun upload(): String = "uploaded"
}

/**
 * Recording web audit service for tests.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class WebRecordingAuditService : IAuditService {
    val captured: MutableList<SysAuditLogModel> = Collections.synchronizedList(mutableListOf())
    override fun submit(sysAuditLogVo: SysAuditLogModel): Boolean {
        captured.add(sysAuditLogVo)
        return true
    }
    fun clear() = captured.clear()
}
