package io.kudos.ms.sys.core.tenant.event

import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantService
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
 * Verifies that SysTenantService correctly publishes domain events after CRUD operations.
 *
 * Same pattern as `SysAccessRuleEventPublishingTest` in the access rule domain:
 * `SqlTestBase` rolls back the transaction at the end of each test method, so
 * `@TransactionalEventListener(AFTER_COMMIT)` will not fire; here a plain
 * `@EventListener` capture bean is used to verify that the service actually publishes the events.
 *
 * The listener's event-to-cache write conversion is verified directly by unit tests such as
 * [TenantByIdCacheTest][io.kudos.ms.sys.core.tenant.cache.TenantByIdCacheTest].
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantEventPublishingTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysTenantService: ISysTenantService

    @Resource
    private lateinit var captor: TenantEventCaptor

    override fun getTestDataSqlPath(): String = "sql/h2/tenant/service/SysTenantServiceTest.sql"

    @Test
    fun `updateActive publishes SysTenantUpdated`() {
        captor.clear()
        val id = "20000000-0000-0000-0000-000000006144"
        assertTrue(sysTenantService.updateActive(id, true))
        val event = assertNotNull(captor.lastOf<SysTenantUpdated>(), "should publish SysTenantUpdated")
        assertEquals(id, event.id)
    }

    @Test
    fun `deleteById publishes SysTenantDeleted when row exists`() {
        captor.clear()
        // Use a tenant id that exists in the test data SQL
        val id = "20000000-0000-0000-0000-000000006144"
        assertTrue(sysTenantService.deleteById(id))
        val event = assertNotNull(captor.lastOf<SysTenantDeleted>(), "should publish SysTenantDeleted")
        assertEquals(id, event.id)
    }
}

/** Helper bean that captures tenant domain events during tests. */
@Component
open class TenantEventCaptor {
    val raw: MutableList<Any> = mutableListOf()

    @EventListener
    open fun capture(event: SysTenantEvent) {
        raw.add(event)
    }

    fun clear() {
        raw.clear()
    }

    inline fun <reified T> lastOf(): T? = raw.filterIsInstance<T>().lastOrNull()
}
