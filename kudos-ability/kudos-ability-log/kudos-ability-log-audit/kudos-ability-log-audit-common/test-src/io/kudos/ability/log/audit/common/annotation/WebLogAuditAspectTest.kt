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
 * [WebLogAuditAspect] 的 Spring AOP 集成测试。
 *
 * 覆盖：
 *  - 成功路径：`@AfterReturning` submit；description 不带 FAILED 前缀；context clear
 *  - 失败路径：`@AfterThrowing` submit；description 带 `[FAILED:..]` 前缀；原异常透传；context clear
 *  - **Multipart 请求被整体跳过**——不写 `LogAuditContext`、不 submit
 *
 * 用 [MockHttpServletRequest] 通过 [RequestContextHolder] 设到当前线程，让切面以为
 * 处于 Web 请求路径中。
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
        assertTrue(!description.startsWith("[FAILED:"), "成功路径 description 不应带 FAILED 前缀: $description")
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

        // 切面对 multipart 直接放行：不写 context、不 submit
        assertEquals(0, recorder.captured.size, "multipart 应当被整体跳过")
        assertNull(LogAuditContext.getOrNull(), "multipart 路径不应污染上下文")
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

@Component
open class WebAuditedController {
    @WebAudit(opType = OperationTypeEnum.QUERY, moduleCode = "USER", desc = "list users")
    open fun list(): String = "ok"

    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = "USER", desc = "broken")
    open fun broken(): Nothing = error("boom-web")

    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = "FILE", desc = "upload")
    open fun upload(): String = "uploaded"
}

open class WebRecordingAuditService : IAuditService {
    val captured: MutableList<SysAuditLogModel> = Collections.synchronizedList(mutableListOf())
    override fun submit(sysAuditLogVo: SysAuditLogModel): Boolean {
        captured.add(sysAuditLogVo)
        return true
    }
    fun clear() = captured.clear()
}
