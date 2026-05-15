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
 * 验证 SysTenantService 在 CRUD 完成后正确发布领域事件。
 *
 * 与 access rule 域的 `SysAccessRuleEventPublishingTest` 同一套路：
 * `SqlTestBase` 在测试方法事务结束时自动回滚，所以 `@TransactionalEventListener(AFTER_COMMIT)` 不会触发；
 * 这里改用普通 `@EventListener` 捕获 bean，验证「服务确实发布了事件」。
 *
 * Listener 内部对事件 → 缓存写入的转换由 [TenantByIdCacheTest][io.kudos.ms.sys.core.tenant.cache.TenantByIdCacheTest]
 * 等单元测试直接验证。
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
        val event = assertNotNull(captor.lastOf<SysTenantUpdated>(), "应发布 SysTenantUpdated")
        assertEquals(id, event.id)
    }

    @Test
    fun `deleteById publishes SysTenantDeleted when row exists`() {
        captor.clear()
        // 使用一个测试数据 SQL 已存在的租户 id
        val id = "20000000-0000-0000-0000-000000006144"
        assertTrue(sysTenantService.deleteById(id))
        val event = assertNotNull(captor.lastOf<SysTenantDeleted>(), "应发布 SysTenantDeleted")
        assertEquals(id, event.id)
    }
}

/** 测试期间捕获租户领域事件的辅助 bean。 */
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
