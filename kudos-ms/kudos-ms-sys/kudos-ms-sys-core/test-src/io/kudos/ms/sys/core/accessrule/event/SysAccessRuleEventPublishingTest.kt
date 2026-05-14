package io.kudos.ms.sys.core.accessrule.event

import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleFormCreate
import io.kudos.ms.sys.core.accessrule.service.iservice.ISysAccessRuleService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 验证 [SysAccessRuleService][io.kudos.ms.sys.core.accessrule.service.impl.SysAccessRuleService]
 * 在 CRUD 完成后正确发布领域事件。
 *
 * 由于 [SqlTestBase][io.kudos.test.rdb.SqlTestBase] 在测试方法事务结束时自动回滚，
 * `@TransactionalEventListener(AFTER_COMMIT)` 不会触发；但 Spring 在 publishEvent 时**立即**派发到
 * 普通 [@EventListener][EventListener] 订阅者，所以这里用一个 capture bean 验证「服务确实发布了事件」。
 *
 * Listener 内部对事件 → 缓存写入的转换由 [AccessRuleIpsBySubSysAndTenantIdCacheTest] / [SysAccessRuleHashCacheTest]
 * 直接调用 listener 方法验证。两边合起来覆盖完整链路。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysAccessRuleEventPublishingTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysAccessRuleService: ISysAccessRuleService

    @Resource
    private lateinit var captor: AccessRuleEventCaptor

    override fun getTestDataSqlPath(): String = "sql/h2/accessrule/service/SysAccessRuleServiceTest.sql"

    @Test
    fun `insert publishes SysAccessRuleInserted with normalized dimensions`() {
        captor.clear()
        val id = sysAccessRuleService.insert(
            SysAccessRuleFormCreate(
                tenantId = "20000000-0000-0000-0000-000000099001",
                systemCode = "svc-system-evt-test-1",
                accessRuleTypeDictCode = "0",
                remark = "event-test",
            )
        )
        val event = assertNotNull(captor.lastOf<SysAccessRuleInserted>(), "应发布 SysAccessRuleInserted 事件")
        assertEquals(id, event.id)
        assertEquals("svc-system-evt-test-1", event.systemCode)
        assertEquals("20000000-0000-0000-0000-000000099001", event.tenantId)
    }

    @Test
    fun `updateActive publishes SysAccessRuleUpdated`() {
        captor.clear()
        val id = "20000000-0000-0000-0000-000000009316"
        assertTrue(sysAccessRuleService.updateActive(id, false))
        val event = assertNotNull(captor.lastOf<SysAccessRuleUpdated>())
        assertEquals(id, event.id)
        // updateActive 不改变维度键，before == after
        assertEquals(event.beforeSystemCode, event.systemCode)
        assertEquals(event.beforeTenantId, event.tenantId)
    }
}

/** 测试期间捕获领域事件的辅助 bean。 */
@Component
open class AccessRuleEventCaptor {
    val raw: MutableList<Any> = mutableListOf()

    @EventListener
    open fun capture(event: SysAccessRuleEvent) {
        raw.add(event)
    }

    @EventListener
    open fun capture(event: SysAccessRuleIpEvent) {
        raw.add(event)
    }

    fun clear() {
        raw.clear()
    }

    inline fun <reified T> lastOf(): T? = raw.filterIsInstance<T>().lastOrNull()
}
