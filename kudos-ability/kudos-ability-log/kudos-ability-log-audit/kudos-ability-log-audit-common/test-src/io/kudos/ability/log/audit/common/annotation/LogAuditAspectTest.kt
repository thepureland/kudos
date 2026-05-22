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
 * [LogAuditAspect] 的 Spring AOP 集成测试。
 *
 * 验证 round-2 修复：
 *  - **`@AfterReturning`** 在正常返回时 submit 审计；description 不带 `[FAILED:..]` 前缀
 *  - **`@AfterThrowing`** 在业务方法抛异常时也 submit 审计，且 description 带 `[FAILED:..]` 前缀
 *  - 两条路径都在 finally [LogAuditContext.clear]，避免线程池 ThreadLocal 泄漏
 *
 * 用 [RecordingAuditService] 取代真正的 RDB/MQ 落地——它仅记录每次 submit 的 model 到列表，
 * 测试断言比 mock 框架更直接。
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
        assertEquals(1, submitted.size, "成功路径应当 submit 一次")
        val firstEntityDescription = submitted.single().entities?.firstOrNull()?.description ?: ""
        assertTrue(!firstEntityDescription.startsWith("[FAILED:"), "成功路径 description 不应带 FAILED 前缀: $firstEntityDescription")
    }

    @Test
    fun failure_submitsAuditWithFailedPrefix_andRethrows() {
        val ex = assertFails { service.fail(Model("bob")) }
        assertEquals("boom", ex.message, "原始异常应当被重新抛出，不被切面吞掉")

        val submitted = recorder.captured
        assertEquals(1, submitted.size, "失败路径仍应 submit 一次（业务方记得到失败动作）")
        val description = submitted.single().entities?.firstOrNull()?.description
        assertNotNull(description)
        assertTrue(description.startsWith("[FAILED:IllegalStateException:boom]"), "失败前缀应包含异常类型 + message: $description")
    }

    @Test
    fun success_clearsContext_afterReturn() {
        service.create(Model("c"))
        // 切面在 finally 调 LogAuditContext.clear()——之后只读侦测应当看到 null
        assertNull(LogAuditContext.getOrNull(), "成功路径完成后应当 clear 上下文")
    }

    @Test
    fun failure_clearsContext_afterThrow() {
        assertFails { service.fail(Model("d")) }
        assertNull(LogAuditContext.getOrNull(), "失败路径完成后应当 clear 上下文")
    }

    @Test
    fun modelArgIndex_picksConfiguredArgInsteadOfFirst() {
        // updateForTenant(tenantId, user) —— model 不在 args[0]，应当被注解里的 modelArgIndex=1 修正
        service.updateForTenant("tenant-T", Model("specified"))

        val submitted = recorder.captured
        assertEquals(1, submitted.size)
        // 切面把 Model 作为 model 序列化进 requestFormData——能看到 name=specified 即说明 index 生效
        val detail = submitted.single().sysAuditDetailLogs?.firstOrNull()
        assertNotNull(detail)
        assertTrue(
            detail.requestFormData?.contains("specified") == true,
            "modelArgIndex=1 应使切面取 args[1] (Model)，而非 args[0] (String tenantId)。实际 requestFormData=${detail.requestFormData}"
        )
    }

    @Test
    fun modelArgIndex_outOfBounds_fallsBackToFirstArg() {
        // updateBadIndex 注解指了一个越界的 modelArgIndex；应当回退到 args[0] 而非抛 IndexOutOfBoundsException
        service.updateBadIndex(Model("fallback"))
        assertEquals(1, recorder.captured.size, "越界 modelArgIndex 应当 silently 回退到 args[0]")
    }

    @TestConfiguration
    open class TestBeans {
        @Bean
        @Primary
        open fun recordingAuditService(): RecordingAuditService = RecordingAuditService()
    }
}

/**
 * 业务侧带 [Audit] 注解的服务——必须是 Spring bean 才能被切面拦下。
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
        // 业务方法签名 model 不在第一个位置——注解显式指定 args[1]
    }

    @Audit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = "USER",
        desc = "bad index",
        ignoreForm = YesNotEnum.NOT,
        modelArgIndex = 99,
    )
    open fun updateBadIndex(model: Model) {
        // 越界——切面应当回退到 args[0]，不抛
    }
}

/**
 * 测试用业务模型。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class Model(val name: String)

/**
 * 把每次 `submit` 的 [SysAuditLogModel] 记录下来供测试断言。
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
