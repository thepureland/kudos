package io.kudos.ms.msg.common.receiver.api

import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for IMsgReceiverGroupApi
 *
 * Drives the contract through a fake implementation: argument pass-through (incl. the
 * nullable type filter), the nullable not-found return and list shape.
 *
 * @author K
 * @since 1.0.0
 */
internal class IMsgReceiverGroupApiTest {

    private class FakeApi : IMsgReceiverGroupApi {
        var lastId: String? = null
        var lastType: String? = "UNSET"
        var byId: MsgReceiverGroupCacheEntry? = null
        var list: List<MsgReceiverGroupCacheEntry> = emptyList()

        override fun getReceiverGroupById(id: String): MsgReceiverGroupCacheEntry? {
            lastId = id
            return byId
        }

        override fun listActiveReceiverGroups(receiverGroupTypeDictCode: String?): List<MsgReceiverGroupCacheEntry> {
            lastType = receiverGroupTypeDictCode
            return list
        }
    }

    private fun group(id: String) = MsgReceiverGroupCacheEntry(
        id = id, receiverGroupTypeDictCode = "dept", defineTable = null, nameColumn = null,
        remark = null, active = true, builtIn = false, createUserId = null, createUserName = null,
        createTime = null, updateUserId = null, updateUserName = null, updateTime = null,
    )

    @Test
    fun getReceiverGroupByIdHit() {
        val api = FakeApi()
        api.byId = group("g-1")
        val g = api.getReceiverGroupById("g-1")
        assertEquals("g-1", api.lastId)
        assertEquals("g-1", g?.id)
    }

    @Test
    fun getReceiverGroupByIdMiss() {
        val api = FakeApi()
        assertNull(api.getReceiverGroupById("missing"))
        assertEquals("missing", api.lastId)
    }

    @Test
    fun listActiveReceiverGroupsWithType() {
        val api = FakeApi()
        api.list = listOf(group("g-1"), group("g-2"))
        val result = api.listActiveReceiverGroups("dept")
        assertEquals("dept", api.lastType)
        assertEquals(2, result.size)
    }

    @Test
    fun listActiveReceiverGroupsWithNullType() {
        val api = FakeApi()
        val result = api.listActiveReceiverGroups(null)
        assertNull(api.lastType)
        assertTrue(result.isEmpty())
    }

}
