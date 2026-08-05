package io.kudos.ms.msg.core.receiver.api

import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiverGroupService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgReceiverGroupApi (pure delegation, service mocked with Mockito, no Spring container).
 *
 * The service dependency is a private `@Resource lateinit var`, so it is injected by reflection.
 *
 * Coverage: both delegate methods forward arguments and return values unchanged
 * (getReceiverGroupById hit + null miss / listActiveReceiverGroups with and without type filter).
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiverGroupApiTest {

    private val service = mock(IMsgReceiverGroupService::class.java)
    private val api = MsgReceiverGroupApi().apply {
        val field = MsgReceiverGroupApi::class.java.getDeclaredField("msgReceiverGroupService")
        field.isAccessible = true
        field.set(this, service)
    }

    private fun entry(id: String, type: String = "USER") = MsgReceiverGroupCacheEntry(
        id = id,
        receiverGroupTypeDictCode = type,
        defineTable = "tbl-$id",
        nameColumn = "name",
        remark = null,
        active = true,
        builtIn = false,
        createUserId = null,
        createUserName = null,
        createTime = null,
        updateUserId = null,
        updateUserName = null,
        updateTime = null,
    )

    @Test
    fun getReceiverGroupById_delegatesHit() {
        val g = entry("g-1")
        whenCalled(service.getReceiverGroupById("g-1")).thenReturn(g)
        assertEquals(g, api.getReceiverGroupById("g-1"))
        verify(service).getReceiverGroupById("g-1")
    }

    @Test
    fun getReceiverGroupById_delegatesNull() {
        whenCalled(service.getReceiverGroupById("missing")).thenReturn(null)
        assertNull(api.getReceiverGroupById("missing"))
    }

    @Test
    fun listActiveReceiverGroups_delegatesWithType() {
        val list = listOf(entry("g-1"))
        whenCalled(service.listActiveReceiverGroups("USER")).thenReturn(list)
        assertEquals(list, api.listActiveReceiverGroups("USER"))
        verify(service).listActiveReceiverGroups("USER")
    }

    @Test
    fun listActiveReceiverGroups_delegatesNullType() {
        val list = listOf(entry("g-1"), entry("g-2", type = "DEPT"))
        whenCalled(service.listActiveReceiverGroups(null)).thenReturn(list)
        assertEquals(list, api.listActiveReceiverGroups(null))
    }

    @Test
    fun listActiveReceiverGroups_delegatesEmpty() {
        whenCalled(service.listActiveReceiverGroups("NONE")).thenReturn(emptyList())
        assertTrue(api.listActiveReceiverGroups("NONE").isEmpty())
    }
}
