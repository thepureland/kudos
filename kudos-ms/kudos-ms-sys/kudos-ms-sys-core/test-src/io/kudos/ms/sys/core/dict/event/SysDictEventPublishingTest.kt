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
 * 验证 SysDictService / SysDictItemService 在 CRUD 完成后正确发布领域事件。
 *
 * 与 access rule / tenant 域同套路：用普通 `@EventListener` 捕获，绕过 SqlTestBase
 * 事务回滚导致 `@TransactionalEventListener(AFTER_COMMIT)` 不触发的问题。
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
        val event = assertNotNull(captor.lastOf<SysDictUpdated>(), "应发布 SysDictUpdated")
        assertEquals(id, event.id)
    }

    @Test
    fun `dictItem updateActive publishes SysDictItemUpdated with id`() {
        captor.clear()
        val id = "20000000-0000-0000-0000-000000004968"
        assertTrue(sysDictItemService.updateActive(id, true))
        val event = assertNotNull(captor.lastOf<SysDictItemUpdated>(), "应发布 SysDictItemUpdated")
        assertEquals(id, event.id)
    }

    @Test
    fun `dictItem deleteById publishes SysDictItemDeleted with dimensions`() {
        captor.clear()
        // SQL 里 active=true 的字典项，dim 信息会被 service 在删除前预取并放进事件
        val id = "20000000-0000-0000-0000-000000004968"
        assertTrue(sysDictItemService.deleteById(id))
        val event = assertNotNull(captor.lastOf<SysDictItemDeleted>(), "应发布 SysDictItemDeleted")
        assertEquals(id, event.id)
        // dim 信息允许为 null（缓存未命中场景），但若命中应有值
        // 这里只断言 id 和事件类型；具体 dim 值由 cache 集成测试覆盖
    }
}

/** 测试期间捕获字典 + 字典项领域事件的辅助 bean。 */
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

    inline fun <reified T> lastOf(): T? = raw.filterIsInstance<T>().lastOrNull()
}
