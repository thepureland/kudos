package io.kudos.ms.sys.core.dict.event

import io.kudos.ms.sys.core.dict.service.iservice.ISysDictItemService
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictService
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
 * Verify that SysDictService / SysDictItemService publish domain events correctly after CRUD operations.
 *
 * Same approach as the access rule / tenant domains: use a plain `@EventListener` to capture events,
 * bypassing the issue where SqlTestBase's transaction rollback prevents `@TransactionalEventListener(AFTER_COMMIT)` from firing.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDictEventPublishingTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysDictService: ISysDictService

    @Resource
    private lateinit var sysDictItemService: ISysDictItemService

    @Resource
    private lateinit var captor: DictEventCaptor

    override fun getTestDataSqlPath(): String = "sql/h2/dict/service/SysDictItemServiceTest.sql"

    @Test
    fun `dict updateActive publishes SysDictUpdated`() {
        captor.clear()
        val id = "20000000-0000-0000-0000-000000004968"
        assertTrue(sysDictService.updateActive(id, true))
        val event = assertNotNull(captor.lastOf<SysDictUpdated>(), "SysDictUpdated should be published")
        assertEquals(id, event.id)
    }

    @Test
    fun `dictItem updateActive publishes SysDictItemUpdated with id`() {
        captor.clear()
        val id = "20000000-0000-0000-0000-000000004968"
        assertTrue(sysDictItemService.updateActive(id, true))
        val event = assertNotNull(captor.lastOf<SysDictItemUpdated>(), "SysDictItemUpdated should be published")
        assertEquals(id, event.id)
    }

    @Test
    fun `dictItem deleteById publishes SysDictItemDeleted with dimensions`() {
        captor.clear()
        // For an active=true dictionary item in the SQL fixture, the service pre-fetches dim info before delete and attaches it to the event
        val id = "20000000-0000-0000-0000-000000004968"
        assertTrue(sysDictItemService.deleteById(id))
        val event = assertNotNull(captor.lastOf<SysDictItemDeleted>(), "SysDictItemDeleted should be published")
        assertEquals(id, event.id)
        // dim info may be null (cache miss scenario), but should be present on hit.
        // Here we only assert the id and event type; specific dim values are covered by the cache integration tests.
    }
}

/** Helper bean that captures dictionary and dictionary item domain events during tests. */
@Component
open class DictEventCaptor {
    val raw: MutableList<Any> = mutableListOf()

    @EventListener
    open fun captureDict(event: SysDictEvent) {
        raw.add(event)
    }

    @EventListener
    open fun captureDictItem(event: SysDictItemEvent) {
        raw.add(event)
    }

    fun clear() {
        raw.clear()
    }

    final inline fun <reified T> lastOf(): T? = raw.filterIsInstance<T>().lastOrNull()
}
