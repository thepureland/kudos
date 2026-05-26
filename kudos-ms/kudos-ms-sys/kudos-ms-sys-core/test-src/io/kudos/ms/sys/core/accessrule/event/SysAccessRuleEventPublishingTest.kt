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
 * Verifies that [SysAccessRuleService][io.kudos.ms.sys.core.accessrule.service.impl.SysAccessRuleService]
 * publishes domain events correctly after CRUD operations complete.
 *
 * Because [SqlTestBase][io.kudos.test.rdb.SqlTestBase] auto-rolls back at the end of the test method's transaction,
 * `@TransactionalEventListener(AFTER_COMMIT)` will not fire; however, Spring dispatches publishEvent **immediately**
 * to regular [@EventListener][EventListener] subscribers, so we use a capture bean here to verify "the service did publish the event".
 *
 * The listener's event-to-cache write conversion is verified directly by [AccessRuleIpsBySubSysAndTenantIdCacheTest] /
 * [SysAccessRuleHashCacheTest] by invoking the listener methods. The two together cover the full chain.
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
        val event = assertNotNull(captor.lastOf<SysAccessRuleInserted>(), "SysAccessRuleInserted event should be published")
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
        // updateActive does not change the dimension keys, so before == after
        assertEquals(event.beforeSystemCode, event.systemCode)
        assertEquals(event.beforeTenantId, event.tenantId)
    }
}

/** Helper bean that captures domain events during tests. */
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
