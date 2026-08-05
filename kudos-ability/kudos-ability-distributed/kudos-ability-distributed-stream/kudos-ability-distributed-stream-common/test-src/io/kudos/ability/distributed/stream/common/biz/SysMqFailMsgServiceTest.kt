package io.kudos.ability.distributed.stream.common.biz

import io.kudos.ability.distributed.stream.common.dao.StreamExceptionMsgDao
import io.kudos.ability.distributed.stream.common.model.po.SysMqFailMsg
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for [SysMqFailMsgService] using a lightweight fake [StreamExceptionMsgDao]:
 * - [SysMqFailMsgService.save] inserting the entity and returning true
 * - [SysMqFailMsgService.query] building a topic + createTime criteria and returning the dao result
 * - [SysMqFailMsgService.delete] delegating to batchDelete, including the empty-id-list edge case
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysMqFailMsgServiceTest {

    @Test
    fun save_insertsEntityAndReturnsTrue() {
        val dao = FakeDao()
        val service = SysMqFailMsgService(dao)
        val msg = SysMqFailMsg().apply {
            topic = "t-1"
            msgHeaderJson = "{}"
            msgBodyJson = "{}"
            createTime = LocalDateTime.now()
        }

        assertTrue(service.save(msg))

        assertSame(msg, dao.inserted)
    }

    @Test
    fun query_buildsTopicAndStartTimeCriteriaAndReturnsDaoResult() {
        val dao = FakeDao()
        val expected = listOf(SysMqFailMsg().apply { topic = "t-1" })
        dao.searchResult = expected
        val service = SysMqFailMsgService(dao)

        val result = service.query("t-1", LocalDateTime.of(2026, 1, 1, 0, 0))

        assertSame(expected, result)
        val criteria = assertNotNull(dao.lastCriteria, "a criteria must be passed to the dao")
        // The service builds the criteria from the table column names (topic / create_time).
        val text = criteria.toString()
        assertTrue(text.contains("topic"), "criteria should constrain the topic column: $text")
        assertTrue(text.contains("create_time"), "criteria should constrain the create time: $text")
    }

    @Test
    fun delete_delegatesToBatchDelete() {
        val dao = FakeDao()
        val service = SysMqFailMsgService(dao)

        service.delete(listOf("id-1", "id-2"))

        assertEquals(listOf("id-1", "id-2"), dao.deletedIds)
    }

    @Test
    fun delete_acceptsEmptyIdList() {
        val dao = FakeDao()
        val service = SysMqFailMsgService(dao)

        service.delete(emptyList())

        assertEquals(emptyList(), dao.deletedIds)
    }

    /** In-memory fake recording dao interactions without any database. */
    private class FakeDao : StreamExceptionMsgDao() {
        var inserted: Any? = null
        var lastCriteria: Criteria? = null
        var searchResult: List<SysMqFailMsg> = emptyList()
        var deletedIds: List<String>? = null

        override fun insert(any: Any): String {
            inserted = any
            return "generated-id"
        }

        override fun search(criteria: Criteria?, vararg orders: Order): List<SysMqFailMsg> {
            lastCriteria = criteria
            return searchResult
        }

        override fun batchDelete(ids: Collection<String>): Int {
            deletedIds = ids.toList()
            return ids.size
        }
    }

}
