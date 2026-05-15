package io.kudos.ms.sys.core.resource.event

import io.kudos.ms.sys.core.resource.service.iservice.ISysResourceService
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
 * 验证 SysResourceService 在 CRUD 完成后正确发布资源领域事件。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysResourceEventPublishingTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysResourceService: ISysResourceService

    @Resource
    private lateinit var captor: ResourceEventCaptor

    override fun getTestDataSqlPath(): String = "sql/h2/resource/service/SysResourceServiceTest.sql"

    @Test
    fun `updateActive publishes SysResourceUpdated`() {
        captor.clear()
        val id = "20000000-0000-0000-0000-000000001461"
        assertTrue(sysResourceService.updateActive(id, true))
        val event = assertNotNull(captor.lastOf<SysResourceUpdated>(), "应发布 SysResourceUpdated")
        assertEquals(id, event.id)
    }

    @Test
    fun `deleteById publishes SysResourceDeleted when row exists`() {
        captor.clear()
        val id = "20000000-0000-0000-0000-000000001461"
        assertTrue(sysResourceService.deleteById(id))
        val event = assertNotNull(captor.lastOf<SysResourceDeleted>(), "应发布 SysResourceDeleted")
        assertEquals(id, event.id)
    }
}

@Component
open class ResourceEventCaptor {
    val raw: MutableList<Any> = mutableListOf()

    @EventListener
    open fun capture(event: SysResourceEvent) {
        raw.add(event)
    }

    fun clear() {
        raw.clear()
    }

    inline fun <reified T> lastOf(): T? = raw.filterIsInstance<T>().lastOrNull()
}
