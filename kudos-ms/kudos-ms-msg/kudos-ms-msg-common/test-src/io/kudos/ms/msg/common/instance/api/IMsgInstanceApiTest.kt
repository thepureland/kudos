package io.kudos.ms.msg.common.instance.api

import io.kudos.ms.msg.common.instance.vo.MsgInstanceCacheEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for IMsgInstanceApi
 *
 * Drives the contract through a fake implementation: argument pass-through and the
 * nullable not-found (null) return.
 *
 * @author K
 * @since 1.0.0
 */
internal class IMsgInstanceApiTest {

    private class FakeApi : IMsgInstanceApi {
        var lastId: String? = null
        var result: MsgInstanceCacheEntry? = null
        override fun getInstanceById(id: String): MsgInstanceCacheEntry? {
            lastId = id
            return result
        }
    }

    @Test
    fun getInstanceByIdPassesArgAndReturnsEntry() {
        val api = FakeApi()
        api.result = MsgInstanceCacheEntry(
            id = "i-1", localeDictCode = null, title = "t", content = null, templateId = null,
            sendTypeDictCode = null, eventTypeDictCode = null, msgTypeDictCode = null,
            validTimeStart = null, validTimeEnd = null, tenantId = null,
        )
        val entry = api.getInstanceById("i-1")
        assertEquals("i-1", api.lastId)
        assertEquals("i-1", entry?.id)
        assertEquals("t", entry?.title)
    }

    @Test
    fun getInstanceByIdReturnsNullWhenNotFound() {
        val api = FakeApi()
        assertNull(api.getInstanceById("missing"))
        assertEquals("missing", api.lastId)
    }

}
